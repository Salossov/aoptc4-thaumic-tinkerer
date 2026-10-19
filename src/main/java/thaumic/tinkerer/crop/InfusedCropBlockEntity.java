package thaumic.tinkerer.crop;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.api.client.ParticleApi;

/** Release tick ordering and directional early exits; age changes retain this tile. */
public final class InfusedCropBlockEntity extends BlockEntity implements IAspectContainer {
    public Aspect aspect;
    public AspectList primalTendencies=new AspectList();
    public InfusedCropBlockEntity(BlockPos pos,BlockState state){super(InfusedContent.CROP_TILE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,InfusedCropBlockEntity tile){
        if(tile.aspect==null)return;
        tile.reduceSaturatedAspects();
        if(!level.isClientSide()&&level.getMaxLocalRawBrightness(pos.above())>=9){
            int age=state.getValue(InfusedCropBlock.AGE);
            if(age<7&&level.random.nextInt((2510-(int)Math.pow(tile.primalTendencies.getAmount(Aspects.WATER),2))*6)==0){level.setBlock(pos,state.setValue(InfusedCropBlock.AGE,age+1),3);tile.changed();}
        }
        if(level.random.nextInt((2550-(int)Math.pow(tile.primalTendencies.getAmount(Aspects.AIR),2))*10)==0&&!tile.aspect.isPrimal())tile.learn();
    }
    public void learn(){
        if(aspect==null||level==null)return;
        for(var direction:Direction.values()){
            var neighbour=worldPosition.relative(direction);
            if(!(level.getBlockEntity(neighbour) instanceof InfusedCropBlockEntity other))continue;
            var learned=other.aspect;
            if(learned==null)continue;
            if(!learned.isPrimal()){
                var values=other.primalTendencies;
                if(values.size()!=0&&values.getAspects()[0]!=null){
                    learned=values.getAspects()[level.random.nextInt(values.size())];
                    if(values.getAmount(learned)>=primalTendencies.getAmount(learned)){
                        primalTendencies.add(learned,1);values.reduce(learned,1);reduceSaturatedAspects();trail(neighbour,learned);changed();
                    }
                }
                return;
            }
            if(primalTendencies.getAmount(learned)<5){primalTendencies.add(learned,1);reduceSaturatedAspects();changed();trail(neighbour,learned);return;}
        }
    }
    private void trail(BlockPos neighbour,Aspect learned){if(level.isClientSide())for(int i=0;i<50;i++)ParticleApi.essentiaTrail(level,neighbour,worldPosition,50,learned.getColor(),1);}
    public void reduceSaturatedAspects(){CropAspectData.saturate(primalTendencies,50);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public AspectList getAspects(){return aspect==null?null:new AspectList().add(aspect,1);}
    @Override public void setAspects(AspectList values){}
    @Override public boolean doesContainerAccept(Aspect aspect){return false;}
    @Override public int addToContainer(Aspect aspect,int amount){return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return false;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount){return false;}
    @Override public int containerContains(Aspect aspect){return 0;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);write(tag);}
    private void write(CompoundTag tag){tag.put("mainAspect",CropAspectData.write(new AspectList().add(aspect,1)));tag.put("aspectTendencies",CropAspectData.write(primalTendencies));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);aspect=CropAspectData.first(CropAspectData.read(tag.getCompound("mainAspect")));primalTendencies=CropAspectData.read(tag.getCompound("aspectTendencies"));reduceSaturatedAspects();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();write(tag);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
