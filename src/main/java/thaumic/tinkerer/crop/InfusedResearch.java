package thaumic.tinkerer.crop;

import java.util.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.research.TinkererResearch;

public final class InfusedResearch {
    public static void register(){
        var definition=definition();
        // Reject missing real parents rather than corrupting the core catalog.
        if(definition.parents().stream().anyMatch(parent->!AddonRegistry.research().containsKey(parent)))return;
        AddonRegistry.research(definition);
    }
    public static ResearchDefinition definition(){
        var text=java.util.stream.IntStream.range(0,6).mapToObj(i->"page.thaumictinkerer.infused_potions."+i).toList();
        var seeds=java.util.stream.IntStream.range(0,4).mapToObj(i->"thaumictinkerer:infused_seeds_"+i).toList();
        var recipes=new ArrayList<String>(seeds);for(var aspect:InfusedContent.FRUIT_ASPECTS)recipes.add("thaumictinkerer:infused_potion_"+aspect.getTag());
        var order=new ArrayList<String>(text);order.add("thaumictinkerer:infused_seed_recipes");order.addAll(recipes.subList(4,8));
        var parents=TinkererConfig.IMBUED_FIRE.get()?List.of("thaumictinkerer:fire_perditio","thaumictinkerer:fire_ordo","thaumictinkerer:fire_ignis","thaumictinkerer:fire_terra","thaumictinkerer:fire_aer","thaumictinkerer:fire_aqua"):List.<String>of();
        return new ResearchDefinition("thaumictinkerer:infused_potions",TinkererResearch.CATEGORY,parents,Map.of("aqua",5,"perditio",5),2,false,true,7,-5,text,recipes,"thaumictinkerer:textures/item/seed_ignis.png",false,false,false,false,List.of(),order,false,0,List.of(),List.of(),List.of(),List.of("INFUSION"),Map.of(),false,Map.of("thaumictinkerer:infused_seed_recipes",seeds));
    }
    private InfusedResearch(){}
}
