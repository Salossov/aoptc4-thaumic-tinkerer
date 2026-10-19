package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;

/** Initialized/registered only with ComputerCraft, as BlockGolemConnector.shouldRegister specifies. */
public final class GolemConnectorContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicTinkerer.ID);
    public static final DeferredBlock<GolemConnectorBlock> BLOCK = BLOCKS.register("golem_connector", GolemConnectorBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GolemConnectorBlockEntity>> TILE = TILES.register("golem_connector", () -> BlockEntityType.Builder.of(GolemConnectorBlockEntity::new, BLOCK.get()).build(null));
    public static boolean enabled() { return ModList.get().isLoaded("computercraft"); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); TILES.register(bus);
        bus.addListener(thaumic.tinkerer.compat.ComputerCraftCompat::capabilities);
        thaumic.tinkerer.compat.FumeTurtleUpgrade.register(bus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) thaumic.tinkerer.client.FumeTurtleModels.register(bus);
        ThaumicTinkerer.ITEMS.register("golem_connector", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    }
    private GolemConnectorContent() { }
}
