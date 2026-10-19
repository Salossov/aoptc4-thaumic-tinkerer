package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** Test-only fluid/energy/cycle target. Excluded from all release JARs. */
@EventBusSubscriber(modid = ThaumicTinkerer.ID)
public final class CapabilityFixture extends BlockEntity {
    public static final class FixtureBlock extends Block implements EntityBlock {
        FixtureBlock() { super(Properties.of().strength(1)); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CapabilityFixture(pos, state); }
    }
    public static FixtureBlock BLOCK;
    private static BlockEntityType<CapabilityFixture> type;
    public final FluidTank northFluid = new FluidTank(1000), otherFluid = new FluidTank(500);
    public final EnergyStorage northEnergy = new EnergyStorage(1000), otherEnergy = new EnergyStorage(400, 0, 400);
    public IItemHandler redirectedItems;
    private CapabilityFixture(BlockPos pos, BlockState state) { super(type, pos, state); }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            BLOCK = new FixtureBlock();
            helper.register(ThaumicTinkerer.id("test_capability_fixture"), BLOCK);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            type = BlockEntityType.Builder.of(CapabilityFixture::new, BLOCK).build(null);
            helper.register(ThaumicTinkerer.id("test_capability_fixture"), type);
        });
    }
    @SubscribeEvent public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (tile, side) -> side == net.minecraft.core.Direction.NORTH ? tile.northFluid : tile.otherFluid);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (tile, side) -> side == net.minecraft.core.Direction.NORTH ? tile.northEnergy : tile.otherEnergy);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (tile, side) -> tile.redirectedItems);
    }
}
