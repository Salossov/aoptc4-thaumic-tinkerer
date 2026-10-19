package thaumic.tinkerer.block;

import java.util.Map;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.AspectApi;
import thaumic.tinkerer.menu.AspectAnalyzerMenu;

/** The analyzer exposes effective item composition, not essentia stored inside an item. */
public final class AspectAnalyzerBlockEntity extends BlockEntity implements Container,MenuProvider,thaumcraft.api.world.IMovableBlockEntity {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public AspectAnalyzerBlockEntity(BlockPos pos,BlockState state){super(AspectAnalyzerContent.TILE.get(),pos,state);}
    public Map<String,Integer> aspects(){return isEmpty()?Map.of():AspectApi.aspects(level,getItem(0));}
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return items.getFirst().isEmpty();}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
    @Override public int getMaxStackSize(){return 64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return true;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items.set(0,ItemStack.EMPTY);setChanged();}
    @Override public Component getDisplayName(){return Component.translatable("block.thaumictinkerer.aspect_analyzer");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new AspectAnalyzerMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items.set(0,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);}
}
