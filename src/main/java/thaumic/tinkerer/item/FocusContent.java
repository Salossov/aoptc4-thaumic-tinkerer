package thaumic.tinkerer.item;

import java.util.List;
import java.util.Map;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.api.addon.AddonRegistrationEvent;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.research.ResearchDefinition;
import thaumcraft.api.wands.FocusBehaviors;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.research.TinkererResearch;

public final class FocusContent {
    public static final DeferredItem<TinkererFocusItem> SMELT = ThaumicTinkerer.ITEMS.register("focus_smelt", () -> new TinkererFocusItem(TinkererFocusItem.Kind.SMELT));
    public static final DeferredItem<TinkererFocusItem> FLIGHT = ThaumicTinkerer.ITEMS.register("focus_flight", () -> new TinkererFocusItem(TinkererFocusItem.Kind.FLIGHT));
    public static final DeferredItem<TinkererFocusItem> TELEKINESIS = ThaumicTinkerer.ITEMS.register("focus_telekinesis", () -> new TinkererFocusItem(TinkererFocusItem.Kind.TELEKINESIS));
    public static final DeferredItem<TinkererFocusItem> HEAL = ThaumicTinkerer.ITEMS.register("focus_heal", () -> new TinkererFocusItem(TinkererFocusItem.Kind.HEAL));
    public static final DeferredItem<TinkererFocusItem> DEFLECT = ThaumicTinkerer.ITEMS.register("focus_deflect", () -> new TinkererFocusItem(TinkererFocusItem.Kind.DEFLECT));
    public static final DeferredItem<TinkererFocusItem> ENDER_CHEST = ThaumicTinkerer.ITEMS.register("focus_ender_chest", () -> new TinkererFocusItem(TinkererFocusItem.Kind.ENDER_CHEST));
    public static final DeferredItem<DislocationFocusItem> DISLOCATION=ThaumicTinkerer.ITEMS.register("focus_dislocation",DislocationFocusItem::new);
    public static void initialize() { }
    public static void registerAddon(AddonRegistrationEvent event) {
        FocusBehaviors.register(DISLOCATION.get(),DISLOCATION.get(),ThaumicTinkerer.id("wand/focus_dislocation"),ThaumicTinkerer.id("item/focus_dislocation_ornament"),ThaumicTinkerer.id("staff/focus_dislocation"));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:focus_dislocation",TinkererResearch.CATEGORY,List.of("thaumictinkerer:focus_flight"),Map.of("alienis",2,"praecantatio",1,"permutatio",1),2,false,true,-5,-5,
                List.of("page.thaumictinkerer.focus_dislocation.0","page.thaumictinkerer.focus_dislocation.1"),List.of("thaumictinkerer:focus_dislocation"),"thaumictinkerer:focus_dislocation",false,false,false,true,List.of(),List.of("page.thaumictinkerer.focus_dislocation.0","page.thaumictinkerer.focus_dislocation.1","thaumictinkerer:focus_dislocation"),false,0,List.of(),List.of(),List.of(),List.of("INFUSION")),thaumcraft.api.world.ContentRules::mirrorsEnabled);
        for (var entry : Map.of("focus_smelt", SMELT, "focus_flight", FLIGHT, "focus_telekinesis", TELEKINESIS,
                "focus_heal", HEAL, "focus_deflect", DEFLECT, "focus_ender_chest", ENDER_CHEST).entrySet()) {
            var item = entry.getValue().get();
            boolean ornament = item.kind() == TinkererFocusItem.Kind.SMELT || item.kind() == TinkererFocusItem.Kind.FLIGHT || item.kind() == TinkererFocusItem.Kind.TELEKINESIS;
            FocusBehaviors.register(item, item, ThaumicTinkerer.id("wand/" + entry.getKey()), ornament ? ThaumicTinkerer.id("item/" + entry.getKey() + "_ornament") : null, ThaumicTinkerer.id("staff/" + entry.getKey()));
        }
        research("focus_smelt", "FOCUSEXCAVATION", Map.of("ignis", 2, "potentia", 1, "praecantatio", 1), -2, -2, false, false);
        research("focus_flight", "thaumictinkerer:focus_smelt", Map.of("motus", 1, "praecantatio", 1, "aer", 2), -3, -4, false, true);
        research("focus_telekinesis", "thaumictinkerer:focus_flight", Map.of("alienis", 2, "praecantatio", 1, "motus", 1), -4, -6, true, true);
        research("focus_deflect", "thaumictinkerer:focus_smelt", Map.of("motus", 2, "aer", 1, "ordo", 1, "mortuus", 1), -4, -3, 3, true, true, 0, true);
        research("focus_heal", "thaumictinkerer:focus_deflect", Map.of("sano", 2, "spiritus", 1, "praecantatio", 1), -6, -4, 2, true, true, 0, true);
        research("focus_ender_chest", "thaumictinkerer:focus_deflect", Map.of("alienis", 2, "vacuos", 1, "praecantatio", 1), -6, -2, 2, false, false, 1, true);
    }
    private static void research(String path, String parent, Map<String, Integer> aspects, int x, int y, boolean secondary, boolean infusion) {
        research(path, parent, aspects, x, y, 2, secondary, infusion, 0, false);
    }
    private static void research(String path, String parent, Map<String, Integer> aspects, int x, int y, int complexity, boolean secondary, boolean infusion, int warp, boolean mirrorsRequired) {
        String id = "thaumictinkerer:" + path, page = "page.thaumictinkerer." + path;
        if(path.equals("focus_ender_chest")&&net.neoforged.fml.ModList.get().isLoaded("enderstorage"))page+="_enderstorage";
        AddonRegistry.research(new ResearchDefinition(id, TinkererResearch.CATEGORY, List.of(parent), aspects, complexity,
                false, true, x, y, List.of(page), List.of(id), id, false, false, false, secondary, List.of(), List.of(page, id),
                false, warp, List.of(), List.of(), List.of(), infusion ? List.of("INFUSION") : List.of()),
                () -> !mirrorsRequired || thaumcraft.api.world.ContentRules.mirrorsEnabled());
    }
    public static void display(CreativeModeTab.Output output) { output.accept(SMELT); output.accept(FLIGHT); output.accept(TELEKINESIS); output.accept(DEFLECT); output.accept(HEAL); output.accept(ENDER_CHEST); output.accept(DISLOCATION); }
    private FocusContent() { }
}
