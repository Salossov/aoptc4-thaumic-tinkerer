package thaumic.tinkerer.menu;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.block.*;

public final class AnimationTabletMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData controls;
    public AnimationTabletMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(1),new SimpleContainerData(2));}
    public AnimationTabletMenu(int id,Inventory inventory,AnimationTabletBlockEntity tile){this(id,inventory,tile,new ContainerData(){public int get(int i){return (i==0?tile.leftClick:tile.redstone)?1:0;}public void set(int i,int value){if(i==0)tile.leftClick=value!=0;else tile.redstone=value!=0;tile.sync();}public int getCount(){return 2;}});}
    private AnimationTabletMenu(int id,Inventory inventory,Container container,ContainerData controls){
        super(AnimationTabletContent.MENU.get(),id);this.container=container;this.controls=controls;addDataSlots(controls);addSlot(new Slot(container,0,80,15));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    public boolean leftClick(){return controls.get(0)!=0;}
    public boolean redstone(){return controls.get(1)!=0;}
    @Override public boolean clickMenuButton(Player player,int button){if(!stillValid(player))return false;if(button==0)controls.set(1,redstone()?0:1);else if(button==1||button==2)controls.set(0,button==1?1:0);else return false;broadcastChanges();return true;}
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();if(!(index==0?moveItemStackTo(stack,1,37,false):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return copy;}
}
