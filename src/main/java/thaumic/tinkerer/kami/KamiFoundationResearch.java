package thaumic.tinkerer.kami;

import java.util.List;
import java.util.Map;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.research.TinkererResearch;

public final class KamiFoundationResearch {
    // TC4 ResearchItem caps constructor complexity at 3, including the source's literal 5.
    private static String id(String key) {return ThaumicTinkerer.ID+":"+key;}
    private static void node(String key,String item,int x,int y,Map<String,Integer> aspects,List<String> parents,List<String> hidden,int warp,boolean auto,boolean stub) {
        String research=id(key),page="page.thaumictinkerer.kami."+key+".0";
        List<String> recipes=auto?List.of():List.of(research);
        KamiResearch.register(new ResearchDefinition(research,TinkererResearch.CATEGORY,parents,aspects,auto?0:3,auto,true,x,y,List.of(page),recipes,id(item),false,auto,stub,false,List.of(),auto?List.of(page):List.of(page,research),false,warp,List.of(),List.of(),List.of(),hidden));
    }
    public static void register() {
        KamiResearch.catalogRule();
        KamiWandContent.register();
        node("dimension_shards","ender_shard",7,8,Map.of(),List.of(),List.of(),0,true,true);
        node("ichor","ichor",9,8,Map.of("humanus",1,"lux",2,"spiritus",1,"vitium",1),List.of(),List.of(),5,false,false);
        node("ichor_cloth","ichor_cloth",11,7,Map.of("pannus",2,"lux",1,"fabrico",1,"sensus",1),List.of(id("ichor")),List.of(),0,false,false);
        node("ichorium","ichorium",11,9,Map.of("metallum",2,"lux",1,"fabrico",1,"instrumentum",1),List.of(id("ichor")),List.of(id("ichor_cloth")),0,false,false);
        node("cap_ichor","ichor_cap",11,11,Map.of("instrumentum",2,"metallum",1,"lux",1,"praecantatio",1),List.of(id("ichorium")),List.of(),2,false,false);
        node("rod_ichorcloth","ichorcloth_rod",14,2,Map.of("instrumentum",2,"pannus",1,"lux",1,"praecantatio",1),List.of(id("ichor_cloth")),List.of(id("cap_ichor")),1,false,false);
        KamiEquipmentResearch.register();
        thaumic.tinkerer.kami.tool.IchorToolsResearch.register();
    }
    private KamiFoundationResearch() {}
}
