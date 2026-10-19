package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;

/** Flattened metadata 0/1/2 and the original separate stair/half/full slab registrations. */
public final class DarkQuartzContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    public static final DeferredItem<Item> QUARTZ = ThaumicTinkerer.ITEMS.registerSimpleItem("dark_quartz_item");
    public static final DeferredBlock<Block> BLOCK = BLOCKS.register("dark_quartz", () -> new Block(properties()));
    public static final DeferredBlock<Block> CHISELED = BLOCKS.register("chiseled_dark_quartz", () -> new Block(properties()));
    public static final DeferredBlock<RotatedPillarBlock> PILLAR = BLOCKS.register("dark_quartz_pillar", () -> new RotatedPillarBlock(properties()));
    public static final DeferredBlock<DarkQuartzStairs> STAIRS = BLOCKS.register("dark_quartz_stairs",
            () -> new DarkQuartzStairs(BLOCK.get().defaultBlockState(), properties()));
    public static final DeferredBlock<DarkQuartzSlab> SLAB = BLOCKS.register("dark_quartz_slab", () -> new DarkQuartzSlab(properties(), false));
    public static final DeferredBlock<DarkQuartzSlab> FULL_SLAB = BLOCKS.register("dark_quartz_slab_full", () -> new DarkQuartzSlab(properties(), true));

    private static BlockBehaviour.Properties properties() {
        // Confirmed against the official 1.7.10 client bytecode: setResistance(10) stores
        // 10*3 and getExplosionResistance divides by 5. The effective target value is 6.
        // Material.rock used the stone map color even though the original textures are dark.
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(.8f, 6f)
                .sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public static void register(IEventBus bus) {
        for (var block : java.util.List.of(BLOCK, CHISELED, PILLAR, STAIRS)) {
            ThaumicTinkerer.ITEMS.registerSimpleBlockItem(block);
        }
        ThaumicTinkerer.ITEMS.register("dark_quartz_slab", () -> new DarkQuartzSlabItem(SLAB.get()));
        ThaumicTinkerer.ITEMS.register("dark_quartz_slab_full", () -> new DarkQuartzSlabItem(FULL_SLAB.get()));
        BLOCKS.register(bus);
    }

    public static void display(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(QUARTZ.get());
        for (var block : java.util.List.of(BLOCK, CHISELED, PILLAR, STAIRS, SLAB)) output.accept(block.get());
    }

    private DarkQuartzContent() {}
}
