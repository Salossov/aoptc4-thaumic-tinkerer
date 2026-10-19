package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.ConnectorItem;

public final class TransvectorContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicTinkerer.ID);
    public static final DeferredBlock<TransvectorInterfaceBlock> INTERFACE = BLOCKS.register("interface", TransvectorInterfaceBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransvectorInterfaceBlockEntity>> TILE = TILES.register("interface", () -> BlockEntityType.Builder.of(TransvectorInterfaceBlockEntity::new, INTERFACE.get()).build(null));
    public static final DeferredBlock<TransvectorDislocatorBlock> DISLOCATOR = BLOCKS.register("dislocator", TransvectorDislocatorBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransvectorDislocatorBlockEntity>> DISLOCATOR_TILE = TILES.register("dislocator", () -> BlockEntityType.Builder.of(TransvectorDislocatorBlockEntity::new, DISLOCATOR.get()).build(null));
    public static final DeferredItem<ConnectorItem> CONNECTOR = ThaumicTinkerer.ITEMS.register("connector", ConnectorItem::new);
    static { ThaumicTinkerer.ITEMS.register("dislocator", () -> new BlockItem(DISLOCATOR.get(), new Item.Properties())); ThaumicTinkerer.ITEMS.register("interface", () -> new BlockItem(INTERFACE.get(), new Item.Properties())); }
    public static void register(IEventBus bus) { BLOCKS.register(bus); TILES.register(bus); bus.addListener(TransvectorContent::capabilities); }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TILE.get(), (tile, side) -> tile.forwarded.items(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TILE.get(), (tile, side) -> tile.forwarded.fluids(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TILE.get(), (tile, side) -> tile.forwarded.energy(side));
        event.registerBlockEntity(EssentiaCapabilities.CONTAINER, TILE.get(), (tile, unused) -> tile.forwarded.aspects);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, TILE.get(), (tile, side) -> tile.forwarded.transport);
    }
    private TransvectorContent() { }
}
