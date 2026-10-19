package thaumic.tinkerer.kami;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

public final class KamiUtilityContent {
    public static final DeferredItem<CatAmuletItem> CAT_AMULET=ThaumicTinkerer.ITEMS.register("cat_amulet",CatAmuletItem::new);
    public static void register() {}
    public static void display(CreativeModeTab.Output output) {output.accept(CAT_AMULET.get());}
    private KamiUtilityContent() {}
}
