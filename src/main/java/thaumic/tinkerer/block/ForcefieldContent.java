package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;

public final class ForcefieldContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicTinkerer.ID);
    public static final DeferredBlock<ForcefieldBlock> BLOCK = BLOCKS.register("forcefield", ForcefieldBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForcefieldBlockEntity>> TILE = TILES.register("forcefield", () -> BlockEntityType.Builder.of(ForcefieldBlockEntity::new, BLOCK.get()).build(null));
    static { ThaumicTinkerer.ITEMS.register("forcefield", () -> new BlockItem(BLOCK.get(), new Item.Properties())); }
    public static void register(IEventBus bus) { BLOCKS.register(bus); TILES.register(bus); }
    private ForcefieldContent() { }
}
