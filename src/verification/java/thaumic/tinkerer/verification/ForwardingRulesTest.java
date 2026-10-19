package thaumic.tinkerer.verification;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.ForwardedCapabilities;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ForwardingRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void forwardedCapabilities(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var base = helper.absolutePos(new BlockPos(25, 5, 25));
        var target = new AtomicReference<BlockEntity>(); var views = new ForwardedCapabilities(target::get);
        var item = views.items(Direction.NORTH); var fluid = views.fluids(Direction.NORTH); var energy = views.energy(Direction.NORTH);
        try {
            check(item == views.items(Direction.NORTH) && fluid == views.fluids(Direction.NORTH) && energy == views.energy(Direction.NORTH), "stable views can be held by external capability caches");
            var offered = new ItemStack(Items.DIRT, 12);
            check(item.getSlots() == 0 && item.insertItem(0, offered, false) == offered && item.extractItem(0, 9, false).isEmpty(), "unbound item view rejects input and supplies no phantom inventory");
            check(fluid.getTanks() == 0 && fluid.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == 0
                    && fluid.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "unbound fluid view neither creates nor consumes fluid");
            check(energy.receiveEnergy(100, false) == 0 && energy.getEnergyStored() == 0 && !energy.canReceive(), "unbound energy view is inert");
            check(views.aspects.getAspects() == null && views.aspects.addToContainer(Aspects.AIR, 5) == 0, "original missing-target aspect null/zero behavior");
            check(views.transport.isConnectable(Direction.NORTH) && !views.transport.canInputFrom(Direction.NORTH)
                    && views.transport.getSuctionType(Direction.NORTH) == null, "original empty interface remains connectable without providing transport");
            level.setBlockAndUpdate(base, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
            level.setBlockAndUpdate(base.east(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
            var chest = (ChestBlockEntity) level.getBlockEntity(base); target.set(chest);
            check(item.getSlots() == 27, "bound chest half retains original twenty-seven slots even beside another chest");
            check(item.insertItem(0, offered, true).isEmpty() && chest.getItem(0).isEmpty(), "simulated insertion leaves actual inventory unchanged");
            check(item.insertItem(0, offered, false).isEmpty() && chest.getItem(0).getCount() == 12, "committed insertion reaches actual target inventory");
            check(item.extractItem(0, 4, true).getCount() == 4 && chest.getItem(0).getCount() == 12, "simulated extraction is non-mutating");
            check(item.extractItem(0, 4, false).getCount() == 4 && chest.getItem(0).getCount() == 8, "committed extraction conserves target items");
            chest.setItem(3, new ItemStack(Items.DIAMOND, 2));
            check(item.getStackInSlot(3).is(Items.DIAMOND) && item.getSlotLimit(3) == chest.getMaxStackSize(), "cached view observes direct target changes and its native slot limit");
            boolean rejected = false; try { views.aspects.setAspects(new AspectList()); } catch (ClassCastException expected) { rejected = true; }
            check(rejected, "original setAspects cast fails for an existing non-aspect target");
            level.setBlockAndUpdate(base.offset(3, 0, 0), CapabilityFixture.BLOCK.defaultBlockState());
            var fixture = (CapabilityFixture) level.getBlockEntity(base.offset(3, 0, 0)); target.set(fixture);
            check(fluid.getTanks() == 1 && fluid.getTankCapacity(0) == 1000, "same cached fluid view retargets dynamically");
            check(fluid.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.SIMULATE) == 250 && fixture.northFluid.isEmpty(), "simulated fluid insertion is non-mutating");
            check(fluid.fill(new FluidStack(Fluids.WATER, 250), IFluidHandler.FluidAction.EXECUTE) == 250 && fixture.northFluid.getFluidAmount() == 250
                    && fixture.otherFluid.isEmpty(), "original input side is forwarded without reversal");
            check(views.fluids(Direction.SOUTH).getTankCapacity(0) == 500 && views.fluids(Direction.SOUTH).getFluidInTank(0).isEmpty(), "different faces expose their corresponding target tanks");
            check(fluid.drain(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE).getAmount() == 100 && fixture.northFluid.getFluidAmount() == 250, "simulated typed drain is non-mutating");
            check(fluid.drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100 && fixture.northFluid.getFluidAmount() == 150, "committed drain preserves fluid amount");
            check(energy.receiveEnergy(300, true) == 300 && fixture.northEnergy.getEnergyStored() == 0, "simulated FE/RF insertion does not charge target");
            check(energy.receiveEnergy(300, false) == 300 && fixture.northEnergy.getEnergyStored() == 300, "committed FE/RF insertion charges target");
            check(energy.extractEnergy(50, true) == 50 && fixture.northEnergy.getEnergyStored() == 300, "simulated energy extraction is non-mutating");
            check(energy.extractEnergy(50, false) == 50 && fixture.northEnergy.getEnergyStored() == 250, "committed energy extraction conserves energy");
            check(!views.energy(Direction.SOUTH).canReceive() && views.energy(Direction.SOUTH).getMaxEnergyStored() == 400, "energy side rules are forwarded unchanged");
            fixture.redirectedItems = item;
            check(item.getSlots() == 0, "malformed capability cycle terminates without recursion overflow");
            target.set(chest); check(item.getSlots() == 27, "cycle guard is cleared after the rejected nested call");
            var jarPos = base.offset(0, 0, 3);
            var jarBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:warded_jar"));
            level.setBlockAndUpdate(jarPos, jarBlock.defaultBlockState()); target.set(level.getBlockEntity(jarPos));
            var nativeAspects = level.getCapability(EssentiaCapabilities.CONTAINER, jarPos, null);
            check(nativeAspects != null, "actual core jar exposes public aspect capability");
            views.aspects.setAspects(new AspectList().add(Aspects.AIR, 12));
            check(nativeAspects.containerContains(Aspects.AIR) == 12 && views.aspects.containerContains(Aspects.AIR) == 12, "forwarded setter updates real core jar contents");
            check(views.aspects.addToContainer(Aspects.AIR, 4) == 0 && nativeAspects.containerContains(Aspects.AIR) == 16, "aspect remainder convention is preserved");
            check(views.aspects.takeFromContainer(Aspects.AIR, 2) && nativeAspects.containerContains(Aspects.AIR) == 14, "aspect removal mutates actual jar");
            var nativeTransport = level.getCapability(EssentiaCapabilities.TRANSPORT, jarPos, Direction.UP);
            check(nativeTransport != null && views.transport.canInputFrom(Direction.UP) == nativeTransport.canInputFrom(Direction.UP)
                    && views.transport.getSuctionAmount(Direction.UP) == nativeTransport.getSuctionAmount(Direction.UP), "transport direction and suction resolve the actual public capability");
            int expected = nativeTransport.takeEssentia(Aspects.AIR, 3, Direction.UP);
            int expectedRemaining = nativeAspects.containerContains(Aspects.AIR);
            nativeAspects.setAspects(new AspectList().add(Aspects.AIR, 14));
            check(views.transport.takeEssentia(Aspects.AIR, 3, Direction.UP) == expected
                    && nativeAspects.containerContains(Aspects.AIR) == expectedRemaining, "forwarded withdrawal matches actual native transport and conserves jar contents");
            target.set(null); check(item.getSlots() == 0 && fluid.getTanks() == 0 && energy.getEnergyStored() == 0, "all cached views become empty immediately after unlinking");
            System.out.println("TT_FORWARDING_PASS checks=" + checks); helper.succeed();
        } finally {
            for (var p : java.util.List.of(base, base.east(), base.offset(3, 0, 0), base.offset(0, 0, 3))) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.Container container) container.clearContent();
                level.removeBlock(p, false);
            }
        }
    }
}
