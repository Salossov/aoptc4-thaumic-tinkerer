package thaumic.tinkerer.enchantment;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.*;
import thaumic.tinkerer.research.TinkererResearch;

/** Static release-164 manager declarations, extracted rather than retyped. */
public final class EnchanterCatalog {
    public record Entry(String id,int legacyId,String legacyType,int maxLevel,String texture,String research,Map<String,Integer> base) {
        public Holder.Reference<Enchantment> holder(HolderLookup.Provider registries){return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse(id)));}
        public AspectList cost(int level){var result=new AspectList();base.forEach((tag,n)->result.add(Aspect.getAspect(tag),(int)(n*(level*(1.0+level*.2)))));return result;}
        public boolean unlocked(Player player){return research.isEmpty()||(player instanceof ServerPlayer server?ResearchApi.knows(server,research):ResearchApi.clientKnowledge().completedResearch().contains(research));}
        public boolean canApply(ItemStack stack,HolderLookup.Provider registries,List<ResourceLocation> selected){
            if(stack.isEmpty()||!LegacyEnchantmentItems.accepts(stack.getItem(),legacyType)||selected.contains(ResourceLocation.parse(id)))return false;
            var enchant=holder(registries);
            if(id.equals("thaumcraft:repair")){if(!tool(stack)||stack.isEnchanted()||!(stack.getItem() instanceof thaumcraft.api.IRepairable))return false;}
            else if(id.equals("thaumcraft:haste")){if(!(stack.getItem() instanceof ArmorItem armor&&(armor.getType()==ArmorItem.Type.BOOTS||net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("thaumcraft:hover_harness"))))return false;}
            else if(id.startsWith("thaumictinkerer:")&&!enchant.value().isSupportedItem(stack))return false;
            for(var key:selected){var other=registries.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,key));if(other.isEmpty()||!Enchantment.areCompatible(enchant,other.get()))return false;}
            return true;
        }
    }
    private static final JsonObject SOURCE;
    public static final List<Entry> ENTRIES;
    static {
        try(var stream=Objects.requireNonNull(EnchanterCatalog.class.getResourceAsStream("/assets/thaumictinkerer/enchanter/catalog.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            SOURCE=JsonParser.parseReader(reader).getAsJsonObject();var entries=new ArrayList<Entry>();
            for(var element:SOURCE.getAsJsonArray("enchantments")){var e=element.getAsJsonObject();var aspects=new LinkedHashMap<String,Integer>();e.getAsJsonObject("base_aspects").entrySet().forEach(a->aspects.put(a.getKey(),a.getValue().getAsInt()));entries.add(new Entry(e.get("id").getAsString(),e.get("legacy_id").getAsInt(),e.get("legacy_type").getAsString(),e.get("max_level").getAsInt(),e.get("texture").getAsString(),e.get("research").getAsString(),Collections.unmodifiableMap(aspects)));}
            ENTRIES=List.copyOf(entries);
        }catch(IOException exception){throw new ExceptionInInitializerError(exception);}
    }
    public static Entry find(ResourceLocation id){return ENTRIES.stream().filter(e->e.id.equals(id.toString())).findFirst().orElse(null);}
    public static boolean tool(ItemStack stack){return !stack.isEmpty()&&!stack.is(Items.BOOK)&&stack.getMaxStackSize()==1&&stack.getMaxDamage()>0;}
    public static void research(){
        var pages=List.of("page.thaumictinkerer.enchanter.0","page.thaumictinkerer.enchanter.1","page.thaumictinkerer.enchanter.2");
        AddonRegistry.research(new ResearchDefinition("thaumictinkerer:enchanter",TinkererResearch.CATEGORY,List.of(TinkererResearch.SPELL_CLOTH),Map.of("praecantatio",2,"auram",1,"alienis",1,"tenebrae",1,"cognitio",1),5,false,false,5,4,pages,List.of("thaumictinkerer:enchanter"),"thaumictinkerer:enchanter",false,false,false,false,List.of(),List.of(pages.get(0),pages.get(1),pages.get(2),"thaumictinkerer:enchanter"),false,0,List.of(),List.of(),List.of(),List.of("INFUSION")));
        for(var element:SOURCE.getAsJsonArray("research")){var r=element.getAsJsonObject();var aspects=new LinkedHashMap<String,Integer>();r.getAsJsonObject("aspects").entrySet().forEach(e->aspects.put(e.getKey(),e.getValue().getAsInt()));var parents=new ArrayList<String>();r.getAsJsonArray("parents").forEach(e->parents.add(e.getAsString()));String page=r.get("page").getAsString();
            AddonRegistry.research(new ResearchDefinition(r.get("id").getAsString(),TinkererResearch.CATEGORY,parents,aspects,r.get("complexity").getAsInt(),false,false,r.get("x").getAsInt(),r.get("y").getAsInt(),List.of(page),List.of(),r.get("icon").getAsString(),false,false,false,true,List.of(),List.of(page),false,0,List.of(),List.of(),List.of(),List.of()));
        }
    }
    private EnchanterCatalog(){}
}
