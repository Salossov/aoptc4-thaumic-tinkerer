package thaumic.tinkerer.kami.focus;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.api.addon.AddonRegistrationEvent;
import thaumcraft.api.wands.FocusBehaviors;
import thaumic.tinkerer.ThaumicTinkerer;

/** Register reviewed foci; the pending Shadowbeam integration stays isolated. */
public final class KamiFocusContent {
    public static final DeferredItem<FocusXPDrainItem> FOCUS_XP_DRAIN=ThaumicTinkerer.ITEMS.register("focus_xp_drain",FocusXPDrainItem::new);
    private static boolean registered;
    public static synchronized void registerBehaviors(){
        if(registered)return;
        FocusBehaviors.register(FOCUS_XP_DRAIN.get(),FOCUS_XP_DRAIN.get(),ThaumicTinkerer.id("wand/focus_xp_drain"),null);
        registered=true;
    }
    public static void register(IEventBus bus){AddonRegistrationEvent.BUS.addListener((AddonRegistrationEvent event)->registerBehaviors());}
    public static void display(CreativeModeTab.Output output){output.accept(FOCUS_XP_DRAIN.get());}
    private KamiFocusContent(){}
}
