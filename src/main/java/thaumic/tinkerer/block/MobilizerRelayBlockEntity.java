package thaumic.tinkerer.block;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.world.IMovableBlockEntity;

/** Original scan and traversal ordering, with deferred preparation at cold chunk boundaries. */
public final class MobilizerRelayBlockEntity extends BlockEntity implements IMovableBlockEntity {
    public boolean hasPartner;
    public int partnerX,partnerZ;
    private boolean cyclePending;
    private final MobilizerChunkWork partnerWork=new MobilizerChunkWork(this),cycleWork=new MobilizerChunkWork(this);
    public MobilizerRelayBlockEntity(BlockPos pos,BlockState state){super(MobilizerContent.RELAY_TILE.get(),pos,state);}
    public BlockPos partnerPos(){return new BlockPos(partnerX,worldPosition.getY(),partnerZ);}
    /** Returns false while the source lookup is waiting for its chunk. */
    public boolean verifyPartner(){
        if(!hasPartner){partnerWork.close();return true;}
        if(!partnerWork.ready(List.of(partnerPos())))return false;
        verifyPartnerLoaded();partnerWork.close();return true;
    }
    void verifyPartnerLoaded(){
        if(!hasPartner)return;
        var partner=level.getBlockEntity(partnerPos());
        if(!hasPartner||!(partner instanceof MobilizerRelayBlockEntity other)||other.partnerX!=worldPosition.getX()||other.partnerZ!=worldPosition.getZ()){
            if(hasPartner){hasPartner=false;changed();}
        }
    }
    public void tick(){
        if(level==null||isRemoved())return;
        if(level.isClientSide()){if(hasPartner)particles();return;}
        if(level.getGameTime()%40==0)cyclePending=true;
        if(!verifyPartner())return;
        if(!cyclePending)return;
        if(!hasPartner){
            if(!cycleWork.ready(searchPositions()))return;
            checkForPartnerLoaded();
        }
        if(hasPartner){
            var route=new ArrayList<BlockPos>();route.add(partnerPos());visitRoute(route::add);
            // Each carrier also checks the relay pair. All positions on this route are FULL now.
            if(!cycleWork.ready(route))return;
            visitRoute(pos->{
                if(level.getBlockEntity(pos) instanceof MobilizerBlockEntity carrier&&carrier.verifyRelay()&&!carrier.linked){
                    carrier.firstRelayX=worldPosition.getX();carrier.firstRelayZ=worldPosition.getZ();
                    carrier.secondRelayX=partnerX;carrier.secondRelayZ=partnerZ;carrier.linked=true;
                    carrier.movementDirection=worldPosition.getX()!=partnerX?Direction.EAST:Direction.NORTH;carrier.changed();
                }
            });
        }
        cyclePending=false;cycleWork.close();
    }
    List<BlockPos> searchPositions(){
        var result=new ArrayList<BlockPos>(126);
        for(int offset=-32;offset<32;offset++)if(offset!=0){result.add(worldPosition.offset(0,0,offset));result.add(worldPosition.offset(offset,0,0));}
        return result;
    }
    private void checkForPartnerLoaded(){if(!hasPartner)for(var pos:searchPositions())setPartners(level.getBlockEntity(pos));}
    /** Source's public search operation; false defers until all 126 lookups are safe. */
    public boolean checkForPartner(){
        if(hasPartner)return true;
        if(!cycleWork.ready(searchPositions()))return false;
        checkForPartnerLoaded();cycleWork.close();return true;
    }
    private void setPartners(BlockEntity candidate){
        if(candidate instanceof MobilizerRelayBlockEntity other){
            other.partnerX=worldPosition.getX();other.partnerZ=worldPosition.getZ();other.hasPartner=true;
            partnerX=other.getBlockPos().getX();partnerZ=other.getBlockPos().getZ();hasPartner=true;
            other.changed();changed();
        }
    }
    /** Source do/while asymmetry and endpoint exclusion; approved integer step avoids float stalls. */
    public void visitRoute(Consumer<BlockPos> visitor){
        int xStep=partnerX<worldPosition.getX()?-1:1,zStep=partnerZ<worldPosition.getZ()?-1:1;
        int x=worldPosition.getX();do{
            int z=worldPosition.getZ();do{
                visitor.accept(new BlockPos(x,worldPosition.getY(),z));z+=zStep;
            }while(z<partnerZ);
            x+=xStep;
        }while(x<partnerX);
    }
    private void particles(){
        float xInc=partnerX==worldPosition.getX()?0:(float)Math.copySign(.05,partnerX-worldPosition.getX())*(float)(level.getGameTime()%20);
        float zInc=partnerZ==worldPosition.getZ()?0:(float)Math.copySign(.05,partnerZ-worldPosition.getZ())*(float)(level.getGameTime()%20);
        int color=worldPosition.getX()>=partnerX&&worldPosition.getZ()<=partnerX?14:2;
        visitRoute(pos->ParticleApi.sparkle(level,(float)(.5+pos.getX()+xInc),(float)(worldPosition.getY()+.5),(float)(pos.getZ()+.5+zInc),color));
    }
    void changed(){setChanged();if(level instanceof ServerLevel)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    public void cancelWork(){cyclePending=false;partnerWork.close();cycleWork.close();}
    @Override public void setRemoved(){cancelWork();super.setRemoved();}
    @Override public void onChunkUnloaded(){cancelWork();super.onChunkUnloaded();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putBoolean("HasPartner",hasPartner);tag.putInt("PartnerX",partnerX);tag.putInt("PartnerZ",partnerZ);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);hasPartner=tag.getBoolean("HasPartner");partnerX=tag.getInt("PartnerX");partnerZ=tag.getInt("PartnerZ");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
