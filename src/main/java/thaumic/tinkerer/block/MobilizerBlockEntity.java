package thaumic.tinkerer.block;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.world.BlockEntityMovementApi;
import thaumcraft.api.world.IMovableBlockEntity;

/** Release-164 carrier phases and passenger quirks; state changes belong to the server thread. */
public final class MobilizerBlockEntity extends BlockEntity {
    public boolean linked,dead;
    public int firstRelayX,firstRelayZ,secondRelayX,secondRelayZ;
    public Direction movementDirection;
    private boolean pendingCheck,pendingMove;
    private final MobilizerChunkWork relayWork=new MobilizerChunkWork(this),movementWork=new MobilizerChunkWork(this);
    public MobilizerBlockEntity(BlockPos pos,BlockState state){super(MobilizerContent.LOCOMOTIVE_TILE.get(),pos,state);}
    private BlockPos firstRelay(){return new BlockPos(firstRelayX,worldPosition.getY(),firstRelayZ);}
    /** False means that verification is deferred, not that a cold relay has disappeared. */
    public boolean verifyRelay(){
        if(!linked){relayWork.close();return true;}
        var positions=new ArrayList<BlockPos>(List.of(firstRelay()));
        if(!relayWork.ready(positions))return false;
        var first=level.getBlockEntity(firstRelay());
        if(first instanceof MobilizerRelayBlockEntity relay){
            if(relay.hasPartner){positions.add(relay.partnerPos());if(!relayWork.ready(positions))return false;}
            relay.verifyPartnerLoaded();
        }
        if(!(first instanceof MobilizerRelayBlockEntity relay)||relay.partnerX!=secondRelayX||relay.partnerZ!=secondRelayZ){linked=false;changed();}
        relayWork.close();return true;
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||dead||isRemoved())return;
        long phase=level.getGameTime()%100;
        if(linked){if(phase==0)pendingCheck=true;if(phase==1)pendingMove=true;}
        if(pendingCheck||pendingMove){
            boolean powerLoaded=true;for(var pos:powerPositions())if(!MobilizerChunkWork.loaded(server,pos)){powerLoaded=false;break;}
            if(powerLoaded&&server.hasNeighborSignal(worldPosition))cancelMovement();
        }
        if(!verifyRelay())return;
        if(!linked){cancelMovement();return;}
        if(!pendingCheck&&!pendingMove)return;
        if(!movementWork.ready(stepPositions()))return;
        if(server.hasNeighborSignal(worldPosition)){cancelMovement();return;}
        if(pendingCheck){
            var target=target();
            // Phase 0 compares literal vanilla air, unlike phase 1's isAir checks.
            if(!server.getBlockState(target).is(Blocks.AIR)||!server.getBlockState(target.above()).is(Blocks.AIR)){
                movementDirection=movementDirection.getOpposite();changed();
            }
            pendingCheck=false;
        }
        if(pendingMove){
            if(!movementWork.ready(stepPositions()))return;
            if(server.hasNeighborSignal(worldPosition)){cancelMovement();return;}
            moveLoaded(server);pendingMove=false;
        }
        if(!pendingMove)movementWork.close();
    }
    private BlockPos target(){return worldPosition.offset(movementDirection.getStepX(),0,movementDirection.getStepZ());}
    private List<BlockPos> stepPositions(){
        var result=new ArrayList<BlockPos>();var target=target();
        // Native redstone checks may look through a neighbouring conductor.
        for(var center:List.of(worldPosition,target))for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)result.add(center.offset(x,0,z));
        return result;
    }
    private List<BlockPos> powerPositions(){var result=new ArrayList<BlockPos>();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)result.add(worldPosition.offset(x,0,z));return result;}
    private void moveLoaded(ServerLevel server){
        var origin=worldPosition.immutable();var target=target();
        if(!server.getBlockState(origin).is(MobilizerContent.LOCOMOTIVE.get()))return;
        // Approved source-bug fix: neither destination cell may be overwritten.
        if(!server.getBlockState(target).isAir()||!server.getBlockState(target.above()).isAir())return;
        var passengerPos=origin.above();var passengerState=server.getBlockState(passengerPos);var passenger=server.getBlockEntity(passengerPos);
        // Legacy flags=0 had no neighbour propagation; suppress modern shape propagation too.
        server.setBlock(target,Blocks.STONE.defaultBlockState(),Block.UPDATE_KNOWN_SHAPE);
        if(passengerState.isAir()||passengerState.canSurvive(server,target.above())){
            if(passenger==null){
                if(!passengerState.is(Blocks.BEDROCK)){
                    server.setBlock(target.above(),passengerState,3);
                    if(!passengerState.is(Blocks.AIR)&&!passengerState.is(Blocks.PISTON_HEAD))server.setBlock(passengerPos,Blocks.AIR.defaultBlockState(),2);
                }
            }else if(thaumic.tinkerer.integration.Ae2Movement.present()){
                // Approved modern AE2 exception: its public strategy owns
                // passenger reconstruction; the carrier still retains identity.
                thaumic.tinkerer.integration.Ae2Movement.move(server,passenger,passengerState,target.above());
            }else if(movable(passenger)){
                var callbacks=passenger instanceof IMovableBlockEntity marked?marked:null;
                // The original no-AE registry fallback ignores this callback's boolean.
                if(callbacks!=null)callbacks.prepareToMove();
                if(moveEntity(server,passenger,passengerState,target.above())&&callbacks!=null)callbacks.doneMoving();
            }
        }
        // A refused/unsupported passenger stays behind while the carrier still moves.
        moveEntity(server,this,getBlockState(),target);
        server.updateNeighborsAt(origin,Blocks.AIR);
    }
    public static boolean movable(BlockEntity passenger){return passenger instanceof IMovableBlockEntity||passenger.getClass().getName().startsWith("net.minecraft.world.level.block.entity.");}
    private static boolean moveEntity(ServerLevel level,BlockEntity entity,BlockState state,BlockPos target){
        var origin=entity.getBlockPos().immutable();var replaced=level.getBlockState(target);
        if(!level.setBlock(target,state,3))return false;
        if(!BlockEntityMovementApi.detach(level,entity)){level.setBlock(target,replaced,3);return false;}
        level.setBlock(origin,Blocks.AIR.defaultBlockState(),entity instanceof MobilizerBlockEntity?2:3);
        if(BlockEntityMovementApi.relocate(level,entity,target))return true;
        // Keep the retained object and its inventory even if another placement hook replaces the target.
        level.setBlock(target,replaced,3);level.setBlock(origin,state,3);
        if(!BlockEntityMovementApi.relocate(level,entity,origin))throw new IllegalStateException("Could not restore moved block entity at "+origin);
        return false;
    }
    void changed(){setChanged();if(level instanceof ServerLevel)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    private void cancelMovement(){pendingCheck=false;pendingMove=false;movementWork.close();}
    public void destroyed(){dead=true;cancelMovement();relayWork.close();}
    @Override public void setRemoved(){cancelMovement();relayWork.close();super.setRemoved();}
    @Override public void onChunkUnloaded(){cancelMovement();relayWork.close();super.onChunkUnloaded();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putBoolean("Linked",linked);tag.putInt("FirstRelayX",firstRelayX);tag.putInt("FirstRelayZ",firstRelayZ);tag.putInt("SecondRelayX",secondRelayX);tag.putInt("SecondRelayZ",secondRelayZ);tag.putInt("Direction",movementDirection==null?0:movementDirection.get3DDataValue());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);linked=tag.getBoolean("Linked");firstRelayX=tag.getInt("FirstRelayX");firstRelayZ=tag.getInt("FirstRelayZ");secondRelayX=tag.getInt("SecondRelayX");secondRelayZ=tag.getInt("SecondRelayZ");movementDirection=Direction.values()[tag.getInt("Direction")];
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
