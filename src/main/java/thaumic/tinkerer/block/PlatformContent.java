package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;

public final class PlatformContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredBlock<PlatformBlock> BLOCK=BLOCKS.register("platform",PlatformBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PlatformBlockEntity>> TILE=TILES.register("platform",()->BlockEntityType.Builder.of(PlatformBlockEntity::new,BLOCK.get()).build(null));
    static{ThaumicTinkerer.ITEMS.register("platform",()->new BlockItem(BLOCK.get(),new Item.Properties()));}
    public static void register(IEventBus bus){BLOCKS.register(bus);TILES.register(bus);}
    private PlatformContent(){}
}
