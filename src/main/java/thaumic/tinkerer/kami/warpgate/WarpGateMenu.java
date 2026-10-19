package thaumic.tinkerer.kami.warpgate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WarpGateMenu extends AbstractContainerMenu {
    private final WarpGateBlockEntity gate;

    public WarpGateMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInv, getBlockEntity(playerInv, buffer.readBlockPos()));
    }

    public WarpGateMenu(int containerId, Inventory playerInv, WarpGateBlockEntity gate) {
        super(WarpGateContent.MENU.get(), containerId);
        this.gate = gate;

        Container container = gate != null ? gate : new SimpleContainer(10);

        // 10 Sky Pearl slots (2 rows of 5)
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 5; x++) {
                int index = y * 5 + x;
                this.addSlot(new Slot(container, index, 30 + x * 25, 27 + y * 25) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(WarpGateContent.SKY_PEARL.get());
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }

        // Player Inventory (3 rows of 9)
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142));
        }
    }

    private static WarpGateBlockEntity getBlockEntity(Inventory playerInv, net.minecraft.core.BlockPos pos) {
        if (playerInv.player.level().getBlockEntity(pos) instanceof WarpGateBlockEntity be) {
            return be;
        }
        return null;
    }

    public WarpGateBlockEntity getBlockEntity() {
        return this.gate;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.gate == null) return false;
        return Container.stillValidBlockEntity(this.gate, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if(index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (index < 10) {
                // From gate to player inventory
                if (!this.moveItemStackTo(stackInSlot, 10, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to gate
                if (stackInSlot.is(WarpGateContent.SKY_PEARL.get())) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }
        return itemstack;
    }
}
