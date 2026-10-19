package thaumic.tinkerer.block;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TransvectorInterfaceBlockEntity extends TransvectorBlockEntity implements WorldlyContainer, thaumcraft.api.aspects.IAspectContainer, thaumcraft.api.aspects.IEssentiaTransport {
    private boolean forwardingDirty;
    public final ForwardedCapabilities forwarded = new ForwardedCapabilities(this::resolve);
    public TransvectorInterfaceBlockEntity(BlockPos pos, BlockState state) { super(TransvectorContent.TILE.get(), pos, state); }
    @Override public int maxDistance() { return 4; }
    @Override public void setChanged() {
        super.setChanged();
        if (forwardingDirty) return;
        forwardingDirty = true;
        try { var tile = resolve(); if (tile != null && tile != this) tile.setChanged(); }
        finally { forwardingDirty = false; }
    }
    private Container inventory() {
        var visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<BlockEntity, Boolean>()); visited.add(this);
        var tile = resolve();
        while (tile instanceof TransvectorInterfaceBlockEntity proxy) {
            if (!visited.add(proxy)) return null;
            tile = proxy.resolve();
        }
        return tile instanceof Container container ? container : null;
    }
    @Override public int getContainerSize() { var inventory = inventory(); return inventory == null ? 0 : inventory.getContainerSize(); }
    @Override public boolean isEmpty() { var inventory = inventory(); return inventory == null || inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { var inventory = inventory(); return inventory == null ? ItemStack.EMPTY : inventory.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int count) { var inventory = inventory(); return inventory == null ? ItemStack.EMPTY : inventory.removeItem(slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { var inventory = inventory(); return inventory == null ? ItemStack.EMPTY : inventory.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { var inventory = inventory(); if (inventory != null) inventory.setItem(slot, stack); }
    @Override public int getMaxStackSize() { var inventory = inventory(); return inventory == null ? 0 : inventory.getMaxStackSize(); }
    @Override public boolean stillValid(Player player) { var inventory = inventory(); return inventory != null && inventory.stillValid(player); }
    @Override public void clearContent() { var inventory = inventory(); if (inventory != null) inventory.clearContent(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { var inventory = inventory(); return inventory != null && inventory.canPlaceItem(slot, stack); }
    @Override public void startOpen(Player player) { var inventory = inventory(); if (inventory != null) inventory.startOpen(player); }
    @Override public void stopOpen(Player player) { var inventory = inventory(); if (inventory != null) inventory.stopOpen(player); }
    @Override public int[] getSlotsForFace(Direction side) {
        var inventory = inventory(); return inventory instanceof WorldlyContainer sided ? sided.getSlotsForFace(side) : IntStream.range(0, getContainerSize()).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        var inventory = inventory(); return inventory instanceof WorldlyContainer sided ? sided.canPlaceItemThroughFace(slot, stack, side) : inventory != null;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        var inventory = inventory(); return inventory instanceof WorldlyContainer sided ? sided.canTakeItemThroughFace(slot, stack, side) : inventory != null;
    }
    @Override public thaumcraft.api.aspects.AspectList getAspects() { return forwarded.aspects.getAspects(); }
    @Override public void setAspects(thaumcraft.api.aspects.AspectList aspects) { forwarded.aspects.setAspects(aspects); }
    @Override public boolean doesContainerAccept(thaumcraft.api.aspects.Aspect aspect) { return forwarded.aspects.doesContainerAccept(aspect); }
    @Override public int addToContainer(thaumcraft.api.aspects.Aspect aspect, int amount) { return forwarded.aspects.addToContainer(aspect, amount); }
    @Override public boolean takeFromContainer(thaumcraft.api.aspects.Aspect aspect, int amount) { return forwarded.aspects.takeFromContainer(aspect, amount); }
    @Override public boolean doesContainerContainAmount(thaumcraft.api.aspects.Aspect aspect, int amount) { return forwarded.aspects.doesContainerContainAmount(aspect, amount); }
    @Override public int containerContains(thaumcraft.api.aspects.Aspect aspect) { return forwarded.aspects.containerContains(aspect); }
    @Override public boolean doesContainerContain(thaumcraft.api.aspects.AspectList aspects) { return forwarded.aspects.doesContainerContain(aspects); }
    @Override public boolean takeFromContainer(thaumcraft.api.aspects.AspectList aspects) { return forwarded.aspects.takeFromContainer(aspects); }
    @Override public boolean isConnectable(Direction side) { return forwarded.transport.isConnectable(side); }
    @Override public boolean canInputFrom(Direction side) { return forwarded.transport.canInputFrom(side); }
    @Override public boolean canOutputTo(Direction side) { return forwarded.transport.canOutputTo(side); }
    @Override public void setSuction(thaumcraft.api.aspects.Aspect aspect, int amount) { forwarded.transport.setSuction(aspect, amount); }
    @Override public thaumcraft.api.aspects.Aspect getSuctionType(Direction side) { return forwarded.transport.getSuctionType(side); }
    @Override public int getSuctionAmount(Direction side) { return forwarded.transport.getSuctionAmount(side); }
    @Override public int takeEssentia(thaumcraft.api.aspects.Aspect aspect, int amount, Direction side) { return forwarded.transport.takeEssentia(aspect, amount, side); }
    @Override public int addEssentia(thaumcraft.api.aspects.Aspect aspect, int amount, Direction side) { return forwarded.transport.addEssentia(aspect, amount, side); }
    @Override public thaumcraft.api.aspects.Aspect getEssentiaType(Direction side) { return forwarded.transport.getEssentiaType(side); }
    @Override public int getEssentiaAmount(Direction side) { return forwarded.transport.getEssentiaAmount(side); }
    @Override public int getMinimumSuction() { return forwarded.transport.getMinimumSuction(); }
    @Override public boolean renderExtendedTube() { return forwarded.transport.renderExtendedTube(); }
}
