package thaumic.tinkerer.kami;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.research.TinkererResearch;

public final class KamiEquipmentResearch {
    private static String id(String key) {return "thaumictinkerer:"+key;}
    private static void node(String key,String icon,String parent,int x,int y,Map<String,Integer> aspects,int warp,List<String> recipeKeys,boolean extraPage) {
        String page="page.thaumictinkerer.kami."+key+".0";
        var recipes=recipeKeys.stream().map(KamiEquipmentResearch::id).toList();
        var texts=new ArrayList<String>();texts.add(page);
        var order=new ArrayList<String>();order.add(page);order.addAll(recipes);
        if(extraPage){String tail="page.thaumictinkerer.kami."+key+".1";texts.add(tail);order.add(tail);}
        KamiResearch.register(new ResearchDefinition(id(key),TinkererResearch.CATEGORY,List.of(id(parent)),aspects,5,false,true,x,y,texts,recipes,id(icon),false,false,false,false,List.of(),order,false,warp,List.of(),List.of(),List.of(),List.of()));
    }
    public static void register() {
        thaumic.tinkerer.kami.warpgate.WarpGateResearch.register();
        node("focus_xp_drain","focus_xp_drain","rod_ichorcloth",12,3,Map.of("cognitio",2,"praecantatio",1,"auram",1,"humanus",1),0,List.of("focus_xp_drain"),false);
        node("focus_shadowbeam","focus_shadowbeam","rod_ichorcloth",14,4,Map.of("tenebrae",2,"praecantatio",1,"alienis",1,"vitium",1),0,List.of("focus_shadowbeam"),false);
        node("ichorcloth_armor","ichorcloth_helm","ichor_cloth",17,5,Map.of("tutamen",2,"pannus",1,"lux",1,"fabrico",1),4,
                List.of("ichorcloth_helm","ichorcloth_chest","ichorcloth_legs","ichorcloth_boots"),false);
        node("ichorcloth_helm_gem","ichorcloth_helm_gem","ichorcloth_armor",18,3,Map.of("aqua",2,"sano",1,"fames",1,"auram",1),0,List.of("ichorcloth_helm_gem"),false);
        node("ichorcloth_chest_gem","ichorcloth_chest_gem","ichorcloth_armor",17,7,Map.of("aer",2,"motus",1,"volatus",1,"alienis",1),0,List.of("ichorcloth_chest_gem"),false);
        node("ichorcloth_legs_gem","ichorcloth_legs_gem","ichorcloth_armor",17,9,Map.of("ignis",2,"sano",1,"lucrum",1,"potentia",1),0,List.of("ichorcloth_legs_gem"),true);
        node("ichorcloth_boots_gem","ichorcloth_boots_gem","ichorcloth_armor",15,10,Map.of("terra",2,"iter",1,"perfodio",1,"herba",1),0,List.of("ichorcloth_boots_gem"),false);
        node("cat_amulet","cat_amulet","ichorium",13,10,Map.of("cognitio",2,"ordo",1,"tenebrae",1,"mortuus",1),0,List.of("cat_amulet"),false);
        node("ichor_pouch","ichor_pouch","ichor_cloth",13,6,Map.of("vacuos",2,"pannus",1,"alienis",1,"humanus",1),0,List.of("ichor_pouch"),false);
        String talisman=id("block_talisman"),page="page.thaumictinkerer.kami.block_talisman.0";
        KamiResearch.register(new ResearchDefinition(talisman,TinkererResearch.CATEGORY,List.of(id("ichor_pick_gem"),id("ichor_shovel_gem")),
                Map.of("vacuos",2,"tenebrae",1,"alienis",1,"praecantatio",1),5,false,true,14,17,List.of(page),List.of(talisman),talisman,
                false,false,false,false,List.of(),List.of(page,talisman),false,0,List.of(),List.of(),List.of(),List.of()));
        node("placement_mirror","placement_mirror","block_talisman",17,16,Map.of("fabrico",2,"vitreus",1,"alienis",1,"cognitio",1),0,List.of("placement_mirror"),false);
        String proto=id("protoclay"),protoPage="page.thaumictinkerer.kami.protoclay.0";
        KamiResearch.register(new ResearchDefinition(proto,TinkererResearch.CATEGORY,List.of(id("ichor_pick_gem")),
                Map.of("instrumentum",2,"perfodio",1,"humanus",1,"machina",1),5,false,true,12,17,List.of(protoPage),List.of(proto),proto,
                false,false,false,false,List.of(),List.of(protoPage,proto),false,0,List.of(),List.of(),List.of(),List.of(id("ichor_shovel_gem"))));

    }
    private KamiEquipmentResearch() {}
}
