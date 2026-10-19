package thaumic.tinkerer.crop;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;

public final class InfusedFarmlandBlockEntity extends BlockEntity implements IAspectContainer {
    public AspectList aspectList=new AspectList();
    public InfusedFarmlandBlockEntity(BlockPos pos,BlockState state){super(InfusedContent.SOIL_TILE.get(),pos,state);}
    public void reduceSaturatedAspects(){CropAspectData.saturate(aspectList,20);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public AspectList getAspects(){return aspectList;}
    @Override public void setAspects(AspectList values){aspectList=values;reduceSaturatedAspects();}
    @Override public boolean doesContainerAccept(Aspect aspect){return false;}
    @Override public int addToContainer(Aspect aspect,int amount){return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return false;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount){return false;}
    @Override public int containerContains(Aspect aspect){return 0;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.put("aspectList",CropAspectData.write(aspectList));}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);aspectList=CropAspectData.read(tag.getCompound("aspectList"));reduceSaturatedAspects();}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
