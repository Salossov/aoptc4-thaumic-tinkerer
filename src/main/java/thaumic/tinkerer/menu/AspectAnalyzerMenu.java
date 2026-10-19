package thaumic.tinkerer.menu;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectApi;
import thaumic.tinkerer.block.AspectAnalyzerContent;

public final class AspectAnalyzerMenu extends AbstractContainerMenu {
    private final Container container;
    public AspectAnalyzerMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(1));}
    public AspectAnalyzerMenu(int id,Inventory inventory,Container container){
        super(AspectAnalyzerContent.MENU.get(),id);this.container=container;
        addSlot(new Slot(container,0,20,30){@Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty()&&!AspectApi.aspects(inventory.player.level(),stack).isEmpty();}});
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(inventory,column+row*9+9,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(inventory,column,8+column*18,142));
    }
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    /** Original stable positive-amount bubble order, including its treatment of zero entries. */
    public static java.util.List<java.util.Map.Entry<String,Integer>> sortedAmounts(java.util.Map<String,Integer> aspects){
        var result=new java.util.ArrayList<>(aspects.entrySet());boolean changed;
        do{changed=false;for(int i=0;i<result.size()-1;i++){int first=result.get(i).getValue(),second=result.get(i+1).getValue();if(first>0&&second>0&&second>first){java.util.Collections.swap(result,i,i+1);changed=true;break;}}}while(changed);
        return java.util.List.copyOf(result);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem()||index!=0&&!slots.getFirst().mayPlace(slot.getItem()))return ItemStack.EMPTY;
        var source=slot.getItem();var copy=source.copy();
        if(!(index==0?moveItemStackTo(source,1,37,false):moveItemStackTo(source,0,1,false)))return ItemStack.EMPTY;
        if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(source.getCount()==copy.getCount())return ItemStack.EMPTY;
        slot.onTake(player,source);return copy;
    }
}
