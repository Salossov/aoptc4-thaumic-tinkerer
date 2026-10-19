package thaumic.tinkerer.kami.tool;

import java.util.List;
import java.util.Map;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.kami.KamiResearch;
import thaumic.tinkerer.research.TinkererResearch;

public final class IchorToolsResearch {
    public static void register() {
        String key="thaumictinkerer:ichor_tools",page="page.thaumictinkerer.kami.ichor_tools.0";
        var recipes=List.of("thaumictinkerer:ichor_pick","thaumictinkerer:ichor_shovel","thaumictinkerer:ichor_axe","thaumictinkerer:ichor_sword");
        KamiResearch.register(new ResearchDefinition(key,TinkererResearch.CATEGORY,List.of("thaumictinkerer:ichorium"),
                Map.of("instrumentum",2,"telum",1,"metallum",1,"fabrico",1),5,false,true,13,12,List.of(page),recipes,
                "thaumictinkerer:ichor_pick",false,false,false,false,List.of(),
                List.of(page,recipes.get(0),recipes.get(1),recipes.get(2),recipes.get(3)),false,2,
                List.of(),List.of(),List.of(),List.of("thaumictinkerer:rod_ichorcloth")));
        String sword="thaumictinkerer:ichor_sword_gem",swordPage="page.thaumictinkerer.kami.ichor_sword_gem.";
        KamiResearch.register(new ResearchDefinition(sword,TinkererResearch.CATEGORY,List.of(key),
                Map.of("aer",2,"telum",1,"spiritus",1,"fames",1),5,false,true,16,12,
                List.of(swordPage+"0",swordPage+"1"),List.of(sword),sword,false,false,false,false,List.of(),
                List.of(swordPage+"0",sword,swordPage+"1"),false,0,List.of(),List.of(),List.of(),List.of()));
        digger("pick",13,15,Map.of("ignis",2,"instrumentum",1,"perfodio",1,"terra",1),true);
        digger("shovel",15,15,Map.of("terra",3,"instrumentum",1,"perfodio",1),false);
        digger("axe",16,14,Map.of("aqua",2,"instrumentum",1,"arbor",1,"messis",1),false);
    }
    private static void digger(String kind,int column,int row,Map<String,Integer> aspects,boolean secondText){
        String key="thaumictinkerer:ichor_"+kind+"_gem",page="page.thaumictinkerer.kami.ichor_"+kind+"_gem.";
        var texts=secondText?List.of(page+"0",page+"1"):List.of(page+"0");
        var order=secondText?List.of(page+"0",key,page+"1"):List.of(page+"0",key);
        KamiResearch.register(new ResearchDefinition(key,TinkererResearch.CATEGORY,List.of("thaumictinkerer:ichor_tools"),
                aspects,5,false,true,column,row,texts,List.of(key),key,false,false,false,false,List.of(),order,false,0,List.of(),List.of(),List.of(),List.of()));
    }
    private IchorToolsResearch(){}
}
