package thaumic.tinkerer.block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Stable capability views which resolve the current linked tile for every operation. */
public final class ForwardedCapabilities {
    private static final ThreadLocal<Set<ForwardedCapabilities>> ACTIVE = ThreadLocal.withInitial(HashSet::new);
    private final Supplier<BlockEntity> target;
    private final Map<Direction, IItemHandler> items = new HashMap<>();
    private final Map<Direction, IFluidHandler> fluids = new HashMap<>();
    private final Map<Direction, IEnergyStorage> energy = new HashMap<>();
    public ForwardedCapabilities(Supplier<BlockEntity> target) { this.target = target; }
    private <T, R> R use(Supplier<T> lookup, Function<T, R> operation, R absent) {
        var active = ACTIVE.get();
        // Normal connector use disallows interface-to-interface links. This also bounds
        // malformed cyclic NBT, rather than overflowing during a capability query.
        if (!active.add(this)) return absent;
        try { var value = lookup.get(); return value == null ? absent : operation.apply(value); }
        finally { active.remove(this); if (active.isEmpty()) ACTIVE.remove(); }
    }
    private <T, C> T lookup(BlockCapability<T, C> capability, C context) {
        var tile = target.get();
        if (tile == null || tile.isRemoved() || tile.getLevel() == null) return null;
        return tile.getLevel().getCapability(capability, tile.getBlockPos(), context);
    }
    private IItemHandler inventory(Direction face) {
        var tile = target.get();
        if (tile == null || tile.isRemoved() || tile.getLevel() == null) return null;
        // Original IInventory exposes the bound chest half, not the combined double chest.
        if (tile instanceof WorldlyContainer container && face != null) return new SidedInvWrapper(container, face);
        if (tile instanceof Container container) return new InvWrapper(container);
        return tile.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), face);
    }
    public IItemHandler items(Direction face) {
        return items.computeIfAbsent(face, side -> new IItemHandler() {
            private <R> R get(Function<IItemHandler, R> operation, R absent) { return use(() -> inventory(side), operation, absent); }
            public int getSlots() { return get(IItemHandler::getSlots, 0); }
            public ItemStack getStackInSlot(int slot) { return get(value -> value.getStackInSlot(slot), ItemStack.EMPTY); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return get(value -> value.insertItem(slot, stack, simulate), stack); }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return get(value -> value.extractItem(slot, amount, simulate), ItemStack.EMPTY); }
            public int getSlotLimit(int slot) { return get(value -> value.getSlotLimit(slot), 0); }
            public boolean isItemValid(int slot, ItemStack stack) { return get(value -> value.isItemValid(slot, stack), false); }
        });
    }
    public IFluidHandler fluids(Direction face) {
        return fluids.computeIfAbsent(face, side -> new IFluidHandler() {
            private <R> R get(Function<IFluidHandler, R> operation, R absent) { return use(() -> lookup(Capabilities.FluidHandler.BLOCK, side), operation, absent); }
            public int getTanks() { return get(IFluidHandler::getTanks, 0); }
            public FluidStack getFluidInTank(int tank) { return get(value -> value.getFluidInTank(tank), FluidStack.EMPTY); }
            public int getTankCapacity(int tank) { return get(value -> value.getTankCapacity(tank), 0); }
            public boolean isFluidValid(int tank, FluidStack fluid) { return get(value -> value.isFluidValid(tank, fluid), false); }
            public int fill(FluidStack fluid, FluidAction action) { return get(value -> value.fill(fluid, action), 0); }
            public FluidStack drain(FluidStack fluid, FluidAction action) { return get(value -> value.drain(fluid, action), FluidStack.EMPTY); }
            public FluidStack drain(int amount, FluidAction action) { return get(value -> value.drain(amount, action), FluidStack.EMPTY); }
        });
    }
    public IEnergyStorage energy(Direction face) {
        return energy.computeIfAbsent(face, side -> new IEnergyStorage() {
            private <R> R get(Function<IEnergyStorage, R> operation, R absent) { return use(() -> lookup(Capabilities.EnergyStorage.BLOCK, side), operation, absent); }
            public int receiveEnergy(int amount, boolean simulate) { return get(value -> value.receiveEnergy(amount, simulate), 0); }
            public int extractEnergy(int amount, boolean simulate) { return get(value -> value.extractEnergy(amount, simulate), 0); }
            public int getEnergyStored() { return get(IEnergyStorage::getEnergyStored, 0); }
            public int getMaxEnergyStored() { return get(IEnergyStorage::getMaxEnergyStored, 0); }
            public boolean canExtract() { return get(IEnergyStorage::canExtract, false); }
            public boolean canReceive() { return get(IEnergyStorage::canReceive, false); }
        });
    }
    public final IAspectContainer aspects = new IAspectContainer() {
        private <R> R get(Function<IAspectContainer, R> operation, R absent) { return use(() -> lookup(EssentiaCapabilities.CONTAINER, null), operation, absent); }
        public AspectList getAspects() { return get(IAspectContainer::getAspects, null); }
        public void setAspects(AspectList values) {
            use(() -> target.get(), tile -> {
                var container = lookup(EssentiaCapabilities.CONTAINER, null);
                // Release 164 casts an existing non-aspect target here; keep its failure contract.
                if (container == null) throw new ClassCastException("Linked block is not an aspect container");
                container.setAspects(values); return null;
            }, null);
        }
        public boolean doesContainerAccept(Aspect aspect) { return get(value -> value.doesContainerAccept(aspect), false); }
        // Original missing-target return is zero, even though this means no remainder.
        public int addToContainer(Aspect aspect, int amount) { return get(value -> value.addToContainer(aspect, amount), 0); }
        public boolean takeFromContainer(Aspect aspect, int amount) { return get(value -> value.takeFromContainer(aspect, amount), false); }
        public boolean doesContainerContainAmount(Aspect aspect, int amount) { return get(value -> value.doesContainerContainAmount(aspect, amount), false); }
        public int containerContains(Aspect aspect) { return get(value -> value.containerContains(aspect), 0); }
        @Override public boolean doesContainerContain(AspectList values) { return get(value -> value.doesContainerContain(values), false); }
        @Override public boolean takeFromContainer(AspectList values) { return get(value -> value.takeFromContainer(values), false); }
    };
    public final IEssentiaTransport transport = new IEssentiaTransport() {
        private <R> R get(Direction face, Function<IEssentiaTransport, R> operation, R absent) { return use(() -> lookup(EssentiaCapabilities.TRANSPORT, face), operation, absent); }
        public boolean isConnectable(Direction face) { return true; }
        public boolean canInputFrom(Direction face) { return get(face, value -> value.canInputFrom(face), false); }
        public boolean canOutputTo(Direction face) { return get(face, value -> value.canOutputTo(face), false); }
        public void setSuction(Aspect aspect, int amount) { get(null, value -> { value.setSuction(aspect, amount); return null; }, null); }
        public Aspect getSuctionType(Direction face) { return get(face, value -> value.getSuctionType(face), null); }
        public int getSuctionAmount(Direction face) { return get(face, value -> value.getSuctionAmount(face), 0); }
        public int takeEssentia(Aspect aspect, int amount, Direction face) { return get(face, value -> value.takeEssentia(aspect, amount, face), 0); }
        public int addEssentia(Aspect aspect, int amount, Direction face) { return get(face, value -> value.addEssentia(aspect, amount, face), 0); }
        public Aspect getEssentiaType(Direction face) { return get(face, value -> value.getEssentiaType(face), null); }
        public int getEssentiaAmount(Direction face) { return get(face, value -> value.getEssentiaAmount(face), 0); }
        public int getMinimumSuction() { return get(null, IEssentiaTransport::getMinimumSuction, 0); }
        public boolean renderExtendedTube() { return get(null, IEssentiaTransport::renderExtendedTube, false); }
    };
}
