package thaumic.tinkerer.research;

import java.util.List;
import java.util.Map;
import thaumcraft.api.addon.AddonRegistrationEvent;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.book.BookCategory;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.ThaumicTinkerer;

public final class TinkererResearch {
    public static final String CATEGORY = ThaumicTinkerer.ID + ":tinkering";
    public static final String SPELL_CLOTH = ThaumicTinkerer.ID + ":spell_cloth";

    public static void register(AddonRegistrationEvent event) {
        if(thaumic.tinkerer.TinkererConfig.KAMI_FORCE_ENABLED.get())thaumic.tinkerer.kami.KamiFoundationResearch.register();
        thaumic.tinkerer.enchantment.EnchanterCatalog.research();
        thaumic.tinkerer.fire.ImbuedFireContent.research();
        thaumic.tinkerer.crop.InfusedResearch.register();
        thaumic.tinkerer.summon.SummonContent.research();
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:animation_tablet",CATEGORY,List.of("thaumictinkerer:magnets"),Map.of("machina",2,"metallum",1,"motus",1,"potentia",1),4,false,false,-8,2,List.of("page.thaumictinkerer.animation_tablet.0"),List.of("thaumictinkerer:animation_tablet"),"thaumictinkerer:animation_tablet",false,false,false,false,List.of(),List.of("page.thaumictinkerer.animation_tablet.0","thaumictinkerer:animation_tablet"),false,1,List.of(),List.of(),List.of(),List.of()));
        AddonRegistry.category(new BookCategory(CATEGORY, "category.thaumictinkerer.tinkering",
                "thaumictinkerer:textures/misc/r_enchanting.png",
                "thaumcraft:textures/gui/gui_researchback.png", 100, ""));
        gasResearch("gaseous_light", List.of("NITOR"), Map.of("lux", 2, "aer", 1), 0, -3, 1, false, false, false);
        boolean shareRecipe=thaumic.tinkerer.TinkererConfig.SURVIVAL_SHARE_TOME.get();
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:share_tome",CATEGORY,List.of(),Map.of(),0,true,false,0,-1,
                List.of("page.thaumictinkerer.share_tome.0"),shareRecipe?List.of("thaumictinkerer:share_tome"):List.of(),"thaumictinkerer:share_tome",
                false,true,true,false,List.of(),shareRecipe?List.of("page.thaumictinkerer.share_tome.0","thaumictinkerer:share_tome"):List.of("page.thaumictinkerer.share_tome.0"),
                false,0,List.of(),List.of(),List.of(),List.of()));
        gasResearch("gaseous_shadow", List.of("thaumictinkerer:gaseous_light"), Map.of("tenebrae", 2, "aer", 1, "motus", 4), -1, -5, 2, true, false, false);
        gasResearch("bright_nitor", List.of("thaumictinkerer:gaseous_light"), Map.of("lux", 2, "ignis", 1, "potentia", 1, "aer", 1), 1, -5, 2, true, true, false);
        gasResearch("gas_remover", List.of("thaumictinkerer:gaseous_shadow"), Map.of("tenebrae", 2, "lux", 2), -2, -7, 0, false, false, true);
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:revealing_helm", CATEGORY, List.of("GOGGLES"),
                Map.of("auram", 2, "tutamen", 1), 1, false, false, 0, 0, List.of("page.thaumictinkerer.revealing_helm"),
                List.of("thaumictinkerer:revealing_helm"), "thaumictinkerer:revealing_helm", false, false, false, false,
                List.of(), List.of("page.thaumictinkerer.revealing_helm", "thaumictinkerer:revealing_helm"), false, 0,
                List.of(), List.of(), List.of(), List.of("THAUMIUM")));
        // ItemSpellCloth: (3,2), complexity 2, praecantatio 2 / pannus 1, hidden ENCHFABRIC parent.
        AddonRegistry.research(new ResearchDefinition(SPELL_CLOTH, CATEGORY, List.of(),
                Map.of("praecantatio", 2, "pannus", 1), 2, false, false, 3, 2,
                List.of("page.thaumictinkerer.spell_cloth"), List.of(SPELL_CLOTH), SPELL_CLOTH,
                false, false, false, false, List.of(),
                List.of("page.thaumictinkerer.spell_cloth", SPELL_CLOTH), false, 0,
                List.of(), List.of(), List.of(), List.of("ENCHFABRIC")));
        // ConfigResearch overwrites DARK_QUARTZ0 with the second (charcoal) recipe.
        // Both recipes remain craftable; preserve the last-write book-page choice.
        List<String> quartzRecipes = List.of("thaumictinkerer:dark_quartz0_1", "thaumictinkerer:dark_quartz1",
                "thaumictinkerer:dark_quartz2", "thaumictinkerer:dark_quartz3",
                "thaumictinkerer:dark_quartz4", "thaumictinkerer:dark_quartz5");
        var quartzPages = new java.util.ArrayList<String>();
        quartzPages.add("page.thaumictinkerer.dark_quartz");
        quartzPages.addAll(quartzRecipes);
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:dark_quartz", CATEGORY, List.of(),
                Map.of(), 0, true, false, -2, 2, List.of("page.thaumictinkerer.dark_quartz"),
                quartzRecipes, "thaumictinkerer:dark_quartz_item", false, true, true, false,
                List.of(), quartzPages, false, 0, List.of(), List.of(), List.of(), List.of()));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:interface", CATEGORY, List.of("thaumictinkerer:dark_quartz"),
                Map.of("perditio", 4, "ordo", 4), 1, false, false, -4, 2,
                List.of("page.thaumictinkerer.interface.0", "page.thaumictinkerer.interface.1", "page.thaumictinkerer.interface.2"),
                List.of("thaumictinkerer:interface", "thaumictinkerer:connector"), "thaumictinkerer:interface",
                false, false, false, false, List.of(), List.of("page.thaumictinkerer.interface.0", "thaumictinkerer:interface", "page.thaumictinkerer.interface.1", "thaumictinkerer:connector", "page.thaumictinkerer.interface.2"),
                false, 0, List.of(), List.of(), List.of(), List.of()));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:dislocator", CATEGORY, List.of("thaumictinkerer:interface"),
                Map.of("iter", 2, "machina", 1, "alienis", 1), 3, false, true, -6, 1,
                List.of("page.thaumictinkerer.dislocator.0"), List.of("thaumictinkerer:dislocator"), "thaumictinkerer:dislocator",
                false, false, false, true, List.of(), List.of("page.thaumictinkerer.dislocator.0", "thaumictinkerer:dislocator"),
                false, 0, List.of(), List.of(), List.of(), List.of("MIRROR")), thaumcraft.api.world.ContentRules::mirrorsEnabled);
        AddonRegistry.research(magnets());
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:levitator",CATEGORY,List.of("thaumictinkerer:magnets"),Map.of("motus",2,"ordo",2),3,false,false,-7,5,
                List.of("page.thaumictinkerer.levitator.0"),List.of("thaumictinkerer:levitational_locomotive","thaumictinkerer:levitational_locomotive_relay"),"thaumictinkerer:levitational_locomotive",
                false,false,false,true,List.of(),List.of("page.thaumictinkerer.levitator.0","thaumictinkerer:levitational_locomotive","thaumictinkerer:levitational_locomotive_relay"),false,0,List.of(),List.of(),List.of(),List.of("INFUSION")));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:funnel",CATEGORY,List.of("thaumictinkerer:bright_nitor"),Map.of("instrumentum",1,"iter",2),1,false,true,0,-7,
                List.of("page.thaumictinkerer.funnel.0"),List.of("thaumictinkerer:funnel"),"thaumictinkerer:funnel",false,false,false,true,List.of(),List.of("page.thaumictinkerer.funnel.0","thaumictinkerer:funnel"),false,0,List.of(),List.of(),List.of(),List.of("DISTILESSENTIA")));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:repairer",CATEGORY,List.of("thaumictinkerer:funnel"),Map.of("instrumentum",2,"fabrico",1,"ordo",1,"praecantatio",1),3,false,true,-1,-9,
                List.of("page.thaumictinkerer.repairer.0"),List.of("thaumictinkerer:repairer"),"thaumictinkerer:repairer",false,false,false,false,List.of(),List.of("page.thaumictinkerer.repairer.0","thaumictinkerer:repairer"),false,0,List.of(),List.of(),List.of(),List.of("INFUSION","THAUMIUM","ENCHFABRIC")));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:platform",CATEGORY,List.of("thaumictinkerer:cleansing_talisman"),
                Map.of("sensus",2,"arbor",1,"motus",1),3,false,true,-2,6,List.of("page.thaumictinkerer.platform.0"),List.of("thaumictinkerer:platform"),"thaumictinkerer:platform",
                false,false,false,true,List.of(),List.of("page.thaumictinkerer.platform.0","thaumictinkerer:platform"),false,0,List.of(),List.of(),List.of(),List.of()));
        if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) {
            AddonRegistry.research(new ResearchDefinition("thaumictinkerer:aspect_analyzer",CATEGORY,List.of("thaumictinkerer:peripherals"),Map.of("machina",2,"sensus",1,"cognitio",1),2,false,true,0,1,
                    List.of("page.thaumictinkerer.aspect_analyzer.0"),List.of("thaumictinkerer:aspect_analyzer"),"thaumictinkerer:aspect_analyzer",false,true,false,false,List.of(),List.of("page.thaumictinkerer.aspect_analyzer.0","thaumictinkerer:aspect_analyzer"),false,0,List.of(),List.of(),List.of(),List.of("GOGGLES","THAUMIUM")));

            AddonRegistry.research(new ResearchDefinition("thaumictinkerer:peripherals", CATEGORY, List.of(), Map.of(),
                    0, true, false, -1, 0, List.of("page.thaumictinkerer.peripherals.0"), List.of(), "minecraft:redstone",
                    false, true, false, false, List.of(), List.of("page.thaumictinkerer.peripherals.0"), false, 0,
                    List.of(), List.of(), List.of(), List.of()));
            AddonRegistry.research(new ResearchDefinition("thaumictinkerer:golem_connector", CATEGORY, List.of("thaumictinkerer:peripherals"),
                    Map.of("ordo", 1, "iter", 2, "instrumentum", 1), 0, false, true, 1, 0,
                    List.of("page.thaumictinkerer.golem_connector.0", "page.thaumictinkerer.golem_connector.1", "page.thaumictinkerer.golem_connector.2", "page.thaumictinkerer.golem_connector.3"),
                    List.of("thaumictinkerer:golem_connector", "thaumictinkerer:connector"), "thaumictinkerer:golem_connector",
                    false, true, false, false, List.of(),
                    List.of("page.thaumictinkerer.golem_connector.0", "thaumictinkerer:golem_connector", "page.thaumictinkerer.golem_connector.1", "thaumictinkerer:connector", "page.thaumictinkerer.golem_connector.2", "page.thaumictinkerer.golem_connector.3"),
                    false, 0, List.of(), List.of(), List.of(), List.of("GOLEMBELL")));
        }
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:xp_talisman", CATEGORY,
                List.of("JARBRAIN", SPELL_CLOTH), Map.of("lucrum", 1, "praecantatio", 1, "humanus", 1),
                2, false, true, 4, -1, List.of("page.thaumictinkerer.xp_talisman"),
                List.of("thaumictinkerer:xp_talisman"), "thaumictinkerer:textures/item/xp_talisman_1.png",
                false, false, false, true, List.of(), List.of("page.thaumictinkerer.xp_talisman", "thaumictinkerer:xp_talisman"),
                false, 0, List.of(), List.of(), List.of(), List.of("INFUSION")));
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:cleansing_talisman", CATEGORY,
                List.of("thaumictinkerer:dark_quartz"), Map.of("sano", 2, "ordo", 1, "venenum", 1),
                3, false, false, -3, 4, List.of("page.thaumictinkerer.cleansing_talisman"),
                List.of("thaumictinkerer:cleansing_talisman"), "thaumictinkerer:cleansing_talisman",
                false, false, false, true, List.of(), List.of("page.thaumictinkerer.cleansing_talisman", "thaumictinkerer:cleansing_talisman"),
                false, 0, List.of(), List.of(), List.of(), List.of("INFUSION")));
    }

    public static ResearchDefinition magnets() {
        return new ResearchDefinition("thaumictinkerer:magnets", CATEGORY, List.of("thaumictinkerer:interface"),
                Map.of("machina", 2, "motus", 1, "sensus", 1), 3, false, true, -6, 3,
                List.of("page.thaumictinkerer.magnets.0", "page.thaumictinkerer.magnets.1"),
                List.of("thaumictinkerer:magnet", "thaumictinkerer:mob_magnet", "thaumictinkerer:soul_mould"), "thaumictinkerer:magnet",
                false, false, false, false, List.of(), List.of("page.thaumictinkerer.magnets.0", "page.thaumictinkerer.magnets.1", "thaumictinkerer:magnet", "thaumictinkerer:mob_magnet", "thaumictinkerer:soul_mould"),
                false, 0, List.of(), List.of(), List.of(), List.of("thaumictinkerer:focus_telekinesis"));
    }

    private static void gasResearch(String path, List<String> parents, Map<String, Integer> aspects,
                                    int x, int y, int complexity, boolean secondary, boolean concealed, boolean round) {
        String id = ThaumicTinkerer.ID + ":" + path;
        String page = "page.thaumictinkerer." + path;
        AddonRegistry.research(new ResearchDefinition(id, CATEGORY, parents, aspects, complexity, false, concealed,
                x, y, List.of(page), List.of(id), id, false, round, false, secondary, List.of(), List.of(page, id),
                false, 0, List.of(), List.of(), List.of(), List.of()));
    }

    private TinkererResearch() {}
}
