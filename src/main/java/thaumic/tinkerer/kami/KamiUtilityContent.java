package thaumic.tinkerer.kami;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

public final class KamiUtilityContent {
    public static final DeferredItem<CatAmuletItem> CAT_AMULET=ThaumicTinkerer.ITEMS.register("cat_amulet",CatAmuletItem::new);
    public static void register(IEventBus bus) {
        KamiMaterials.register();
        thaumic.tinkerer.kami.tool.IchorTools.register(bus);
        SoulHearts.register(bus);
        // Release: the blood sword subscribes during preInit, shard drops during
        // init. Its replacement drops must be processed before shard additions.
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(DimensionalShardDrops::register));
    }
    public static void display(CreativeModeTab.Output output) {KamiMaterials.display(output);output.accept(CAT_AMULET.get());thaumic.tinkerer.kami.tool.IchorTools.display(output);}
    private KamiUtilityContent() {}
}
