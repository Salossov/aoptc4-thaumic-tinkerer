package thaumic.tinkerer.kami.construction;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

public final class KamiConstructionContent {
    public static final DeferredItem<PlacementMirrorItem> PLACEMENT_MIRROR=ThaumicTinkerer.ITEMS.register("placement_mirror",PlacementMirrorItem::new);
    public static final DeferredItem<ProtoclayItem> PROTOCLAY=ThaumicTinkerer.ITEMS.register("protoclay",ProtoclayItem::new);
    public static void initialize(){}
    public static void display(CreativeModeTab.Output output){output.accept(PLACEMENT_MIRROR.get());output.accept(PROTOCLAY.get());}
    private KamiConstructionContent(){}
}
