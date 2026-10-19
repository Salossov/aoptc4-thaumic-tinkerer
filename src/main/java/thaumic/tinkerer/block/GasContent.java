package thaumic.tinkerer.block;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.BrightNitorItem;
import thaumic.tinkerer.item.GasItem;
import thaumic.tinkerer.item.GasRemoverItem;

public final class GasContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    public static final DeferredBlock<GasBlock> LIGHT = BLOCKS.register("gaseous_light", () -> new GasBlock(false));
    public static final DeferredBlock<GasBlock> SHADOW = BLOCKS.register("gaseous_shadow", () -> new GasBlock(true));
    public static final DeferredBlock<NitorGasBlock> NITOR = BLOCKS.register("nitor_gas", NitorGasBlock::new);
    public static final DeferredItem<GasItem> LIGHT_ITEM = ThaumicTinkerer.ITEMS.register("gaseous_light", () -> new GasItem(LIGHT));
    public static final DeferredItem<GasItem> SHADOW_ITEM = ThaumicTinkerer.ITEMS.register("gaseous_shadow", () -> new GasItem(SHADOW));
    public static final DeferredItem<BrightNitorItem> BRIGHT_NITOR = ThaumicTinkerer.ITEMS.register("bright_nitor", BrightNitorItem::new);
    public static final DeferredItem<GasRemoverItem> REMOVER = ThaumicTinkerer.ITEMS.register("gas_remover", GasRemoverItem::new);
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    public static void display(CreativeModeTab.Output output) {
        output.accept(LIGHT_ITEM); output.accept(SHADOW_ITEM); output.accept(BRIGHT_NITOR); output.accept(REMOVER);
    }
    private GasContent() { }
}
