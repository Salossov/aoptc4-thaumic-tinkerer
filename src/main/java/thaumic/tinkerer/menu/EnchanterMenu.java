package thaumic.tinkerer.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.enchantment.EnchanterCatalog;

public final class EnchanterMenu extends AbstractContainerMenu {
    public final EnchanterBlockEntity tile;
    public EnchanterMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){this(id,inventory,(EnchanterBlockEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()));}
    public EnchanterMenu(int id,Inventory inventory,EnchanterBlockEntity tile){
        super(EnchanterContent.MENU.get(),id);this.tile=java.util.Objects.requireNonNull(tile);
        addSlot(new Slot(tile,0,6,6){@Override public boolean mayPlace(ItemStack stack){return !tile.working&&EnchanterCatalog.tool(stack);}@Override public boolean mayPickup(Player player){return !tile.working;}@Override public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(tile,1,6,31){@Override public boolean mayPlace(ItemStack stack){return EnchanterBlockEntity.acceptsWand(stack);}@Override public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public static int button(EnchanterCatalog.Entry entry,int level){return 1+EnchanterCatalog.ENTRIES.indexOf(entry)*32+(level+1);}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!stillValid(player)||player.level().isClientSide())return false;if(button==0){
            if(!tile.checkPillars()){
                tile.sync();player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.thaumictinkerer.enchanter.pillars"),false);return false;
            }
            if(!EnchanterBlockEntity.acceptsWand(tile.getItem(1))){
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.thaumictinkerer.enchanter.wand"),false);return false;
            }
            return tile.start();
        }
        int index=(button-1)/32,value=(button-1)%32-1;if(button<1||index>=EnchanterCatalog.ENTRIES.size()||value>20)return false;
        return tile.select(player,net.minecraft.resources.ResourceLocation.parse(EnchanterCatalog.ENTRIES.get(index).id()),value);
    }
    @Override public boolean stillValid(Player player){return tile.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;var stack=slot.getItem();if(stack.is(Items.BOOK))return ItemStack.EMPTY;var copy=stack.copy();
        boolean moved=index<2?moveItemStackTo(stack,2,38,false):EnchanterBlockEntity.acceptsWand(stack)?moveItemStackTo(stack,1,2,false):EnchanterCatalog.tool(stack)&&moveItemStackTo(stack,0,1,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return copy;
    }
}
