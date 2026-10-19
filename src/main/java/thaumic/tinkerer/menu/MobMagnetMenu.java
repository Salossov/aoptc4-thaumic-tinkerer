package thaumic.tinkerer.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.block.MagnetContent;
import thaumic.tinkerer.block.MobMagnetBlockEntity;
import thaumic.tinkerer.item.SoulMouldItem;

public final class MobMagnetMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public MobMagnetMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1), new SimpleContainerData(1)); }
    public MobMagnetMenu(int id, Inventory inventory, MobMagnetBlockEntity tile) {
        this(id, inventory, tile, new ContainerData() {
            public int get(int index) { return tile.searchesAdults() ? 1 : 0; }
            public void set(int index, int value) { tile.setSearchAdults(value != 0); }
            public int getCount() { return 1; }
        });
    }
    private MobMagnetMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(MagnetContent.MENU.get(), id); this.container = container; this.data = data;
        addSlot(new Slot(container, 0, 53, 37) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SoulMouldItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }
    public boolean searchesAdults() { return data.get(0) != 0; }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button < 0 || button > 1 || !stillValid(player)) return false;
        data.set(0, button == 0 ? 1 : 0); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index); if (!slot.hasItem() || !(slot.getItem().getItem() instanceof SoulMouldItem)) return ItemStack.EMPTY;
        var source = slot.getItem(); var copy = source.copy();
        if (!(index == 0 ? moveItemStackTo(source, 1, 37, false) : moveItemStackTo(source, 0, 1, false))) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (source.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source); return copy;
    }
}
