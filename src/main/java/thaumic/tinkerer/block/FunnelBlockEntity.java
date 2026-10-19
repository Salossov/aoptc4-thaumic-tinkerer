package thaumic.tinkerer.block;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;

/** Release-164 funnel: one unit per tick, following the real hopper output even when powered. */
public final class FunnelBlockEntity extends BlockEntity implements WorldlyContainer,IAspectContainer,thaumcraft.api.world.IMovableBlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public FunnelBlockEntity(BlockPos pos,BlockState state){super(FunnelContent.TILE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,FunnelBlockEntity tile){
        if(level.isClientSide()||!level.getServer().isSameThread())return;
        var stack=tile.getItem(0);if(!(stack.getItem() instanceof IEssentiaJarItem jar))return;
        var contents=jar.getAspects(stack);if(contents==null||contents.size()!=1)return;
        var below=pos.below();if(!level.hasChunkAt(below)||!level.getBlockState(below).is(Blocks.HOPPER)||!(level.getBlockEntity(below) instanceof HopperBlockEntity))return;
        var target=below.relative(level.getBlockState(below).getValue(HopperBlock.FACING));
        if(!level.hasChunkAt(target)||!(level.getBlockEntity(target) instanceof IAspectContainer container)||container.getJarType()==JarType.NONE)return;
        var aspect=contents.getAspects()[0];var current=container.getAspects();int amount=current==null?0:current.visSize();
        if(amount==0){var filter=container.getAspectFilter();if(filter!=null&&filter!=aspect)return;}
        else if(current.size()!=1||current.getAspects()[0]!=aspect||amount>=64&&container.getJarType()!=JarType.VOID)return;
        container.addToContainer(aspect,1);
        var remaining=contents.getAmount(aspect)-1;jar.setAspects(stack,remaining>0?new AspectList().add(aspect,remaining):new AspectList());tile.setChanged();
    }
    @Override public void setChanged(){super.setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.getFirst().isEmpty();}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public int getMaxStackSize(){return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return true;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items.set(0,ItemStack.EMPTY);setChanged();}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[0]:new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return stack.getItem() instanceof IEssentiaJarItem;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return side!=Direction.DOWN;}
    @Override public AspectList getAspects(){var stack=getItem(0);return stack.getItem() instanceof IAspectItem item?item.getAspects(stack):null;}
    @Override public void setAspects(AspectList aspects){}
    @Override public boolean doesContainerAccept(Aspect aspect){return false;}
    @Override public int addToContainer(Aspect aspect,int amount){return 0;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){return false;}
    @Override public boolean doesContainerContainAmount(Aspect aspect,int amount){return false;}
    @Override public int containerContains(Aspect aspect){return 0;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items.set(0,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
