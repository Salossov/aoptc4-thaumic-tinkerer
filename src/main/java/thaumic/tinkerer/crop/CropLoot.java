package thaumic.tinkerer.crop;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.ThaumicTinkerer;

/** Release identity-keyed pools and inclusive weighted boundary. Tag variants
 * keep the weight of one original wildcard entry, regardless of group size.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class CropLoot {
    private static final JsonArray SOURCE;
    private static final Map<MinecraftServer,Map<Aspect,Pool>> CACHE=new WeakHashMap<>();
    static{
        try(var stream=Objects.requireNonNull(CropLoot.class.getResourceAsStream("/assets/thaumictinkerer/crop/loot.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){SOURCE=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries");}
        catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    public static final class Pool {
        private final HashMap<ItemStack,Integer> entries=new HashMap<>();
        private final IdentityHashMap<ItemStack,List<ItemStack>> variants=new IdentityHashMap<>();
        private void add(ItemStack stack,int weight){entries.put(stack,weight);}
        private void addVariants(List<ItemStack> stacks,int weight){if(stacks.isEmpty())return;var key=stacks.getFirst().copy();entries.put(key,weight);variants.put(key,List.copyOf(stacks));}
        public int entries(){return entries.size();}
        public int totalWeight(){return entries.values().stream().mapToInt(Integer::intValue).sum();}
        public List<ItemStack> outputs(){return entries.keySet().stream().map(ItemStack::copy).toList();}
        public ItemStack draw(Random random){
            int sum=totalWeight();if(sum==0)return ItemStack.EMPTY;
            int roll=random.nextInt(sum);
            for(var entry:entries.entrySet())if((roll-=entry.getValue())<=0){var possible=variants.get(entry.getKey());return possible==null?entry.getKey().copy():possible.get(random.nextInt(possible.size())).copy();}
            return ItemStack.EMPTY;
        }
    }
    public static Pool pool(ServerLevel level,Aspect aspect){synchronized(CACHE){return CACHE.computeIfAbsent(level.getServer(),server->build(level)).get(aspect);}}
    public static ItemStack harvest(ServerLevel level,Aspect aspect){var pool=pool(level,aspect);return pool==null?ItemStack.EMPTY:pool.draw(new Random());}
    private static Map<Aspect,Pool> build(ServerLevel level){
        var result=new LinkedHashMap<Aspect,Pool>();Aspect.all().values().forEach(a->result.put(a,new Pool()));
        for(var element:SOURCE){var record=element.getAsJsonObject();var aspect=Aspect.getAspect(record.get("aspect").getAsString());var pool=result.computeIfAbsent(aspect,a->new Pool());int weight=record.get("weight").getAsInt(),count=record.has("count")?record.get("count").getAsInt():1;
            if(record.has("all_wisp_essences")){for(var essenceAspect:Aspect.all().values())pool.add(AspectApi.wispEssence(essenceAspect),weight);}
            else if(record.has("enchantment")){
                var key=ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse(record.get("enchantment").getAsString()));var holder=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);pool.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder,1)),weight);
            }else if(record.has("expand_tag")){for(var stack:tagged(level,record.get("expand_tag").getAsString(),count))pool.add(stack,weight);}
            else if(record.has("variant_tag"))pool.addVariants(tagged(level,record.get("variant_tag").getAsString(),count),weight);
            else if(record.has("legacy_entity_id"))pool.add(LegacySpawnEggItem.of(record.get("legacy_entity_id").getAsInt()),weight);
            else{var identifier=ResourceLocation.parse(record.get("item").getAsString());var item=BuiltInRegistries.ITEM.getOptional(identifier).orElseThrow(()->new IllegalStateException("Missing original crop loot item "+identifier));pool.add(new ItemStack(item,count),weight);}
        }
        return result;
    }
    private static List<ItemStack> tagged(ServerLevel level,String identifier,int count){
        var result=new ArrayList<ItemStack>();var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse(identifier));var holders=level.registryAccess().lookupOrThrow(Registries.ITEM).get(tag);
        if(holders.isPresent())for(var holder:holders.get())result.add(new ItemStack(holder.value(),count));return result;
    }
    @SubscribeEvent public static void tags(TagsUpdatedEvent event){if(event.getUpdateCause()==TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)synchronized(CACHE){CACHE.clear();}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){synchronized(CACHE){CACHE.remove(event.getServer());}}
    private CropLoot(){}
}
