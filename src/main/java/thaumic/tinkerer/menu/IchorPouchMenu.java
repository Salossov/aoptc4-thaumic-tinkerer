package thaumic.tinkerer.menu;

import java.util.ArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.kami.pouch.IchorPouchContent;
import thaumic.tinkerer.kami.pouch.IchorPouchItem;

public final class IchorPouchMenu extends AbstractContainerMenu {
    public static final int POUCH_SLOTS = 117;
    public static final int GRID_COLUMNS = 13;
    public static final int GRID_ROWS = 9;

    private final Inventory playerInventory;
    private final Player player;
    private final int lockedSlot;
    private final ItemStack pouchStack;
    private final Container pouchContainer;
    private boolean loading=true;

    public IchorPouchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    public IchorPouchMenu(int id, Inventory inventory, int lockedSlot) {
        super(IchorPouchContent.MENU.get(), id);
        if(lockedSlot<0||lockedSlot>=36&&lockedSlot!=40||!(inventory.getItem(lockedSlot).getItem() instanceof IchorPouchItem))
            throw new IllegalArgumentException("Invalid ichor pouch slot: "+lockedSlot);
        this.playerInventory = inventory;
        this.player = inventory.player;
        this.lockedSlot = lockedSlot;
        this.pouchStack = (lockedSlot >= 0 && lockedSlot < inventory.getContainerSize())
            ? inventory.getItem(lockedSlot)
            : ItemStack.EMPTY;

        var initial=new ItemStack[POUCH_SLOTS];
        java.util.Arrays.fill(initial,ItemStack.EMPTY);
        if(!player.level().isClientSide()) {
            var saved=IchorPouchItem.getInventory(pouchStack);
            for(int slot=0;slot<POUCH_SLOTS;slot++)initial[slot]=saved.get(slot).copy();
        }
        // The source assigns its loaded array directly. setItem would clamp an
        // existing oversized stack to64 and discard the remainder on next save.
        this.pouchContainer = new SimpleContainer(initial) {
            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }
        };

        loading=false;

        // 117 pouch slots: 13 columns x 9 rows, starting at x = 12, y = 8
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLUMNS; x++) {
                int slotIndex = y * GRID_COLUMNS + x;
                addSlot(new PouchSlot(pouchContainer, slotIndex, 12 + x * 18, 8 + y * 18));
            }
        }

        // Player main inventory (3 rows x 9 columns) at x = 48, y = 177
        int invXStart = 48;
        int invYStart = 177;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int playerSlot = col + row * 9 + 9;
                addPlayerSlot(playerSlot,invXStart + col * 18,invYStart + row * 18);
            }
        }

        // Player hotbar (1 row x 9 columns) at x = 48, y = 235 (177 + 58)
        for (int col = 0; col < 9; col++) {
            addPlayerSlot(col,invXStart + col * 18,invYStart + 58);
        }
    }

    private void addPlayerSlot(int slot,int x,int y) {
        addSlot(new Slot(playerInventory,slot,x,y) {
            @Override public boolean mayPickup(Player player){return slot!=lockedSlot;}
            @Override public boolean mayPlace(ItemStack stack){return slot!=lockedSlot;}
        });
    }

    public int getPouchSlot() {
        return lockedSlot;
    }

    public Container getPouchContainer() {
        return pouchContainer;
    }

    public ItemStack getPouchStack() {
        return pouchStack;
    }

    public int getLockedContainerSlot() {
        if (lockedSlot >= 0 && lockedSlot < 9) {
            return 117 + 27 + lockedSlot; // 144 + lockedSlot
        }
        if (lockedSlot >= 9 && lockedSlot < 36) {
            return 117 + (lockedSlot - 9);
        }
        return -1; // off-hand (slot 40) is outside the 36 inventory container slots
    }

    private void save() {
        if (loading||player.level().isClientSide()||!stillValid(player))return;
        var items = new ArrayList<ItemStack>(POUCH_SLOTS);
        for (int i = 0; i < POUCH_SLOTS; i++) {
            items.add(pouchContainer.getItem(i));
        }
        IchorPouchItem.setInventory(pouchStack, items);
        playerInventory.setChanged();
    }

    @Override
    public void removed(Player player) {
        save();
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != this.player) return false;
        if (lockedSlot < 0 || lockedSlot >= playerInventory.getContainerSize()) return false;
        ItemStack current = playerInventory.getItem(lockedSlot);
        if (current.isEmpty() || !(current.getItem() instanceof IchorPouchItem)) return false;
        return player.level().isClientSide() || current == this.pouchStack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if(!stillValid(player))return;
        if (slotId >= 0 && slotId == getLockedContainerSlot()) {
            return;
        }
        if (clickType == ClickType.SWAP) {
            if (button >= 0 && button < 9 && button == lockedSlot) {
                return;
            }
            if (button == 40 && lockedSlot == 40) {
                return;
            }
        }
        if (clickType == ClickType.THROW && slotId == getLockedContainerSlot()) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
        save();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()||!stillValid(player))return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        if (index == getLockedContainerSlot()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        // The source inventory's validity check applies in both directions.
        if(IchorPouchItem.isPouch(stack))return ItemStack.EMPTY;
        ItemStack copy = stack.copy();

        if (index < POUCH_SLOTS) {
            // Shift-click from pouch into player inventory (hotbar first: slots 152 down to 117)
            if (!moveItemStackTo(stack, POUCH_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Shift-click from player inventory into pouch
            if (IchorPouchItem.isPouch(stack) || !moveItemStackTo(stack, 0, POUCH_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        save();
        return copy;
    }

    public static final class PouchSlot extends Slot {
        public PouchSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty() && !IchorPouchItem.isPouch(stack);
        }
    }
}
