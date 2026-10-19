package thaumic.tinkerer.kami.bedrock;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Ordered release groups; one resolved block per weighted group, not one
 * weight per modern tag member. Resolve tags after reload, on worldgen access.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class BedrockOres {
    public record Group(String name,int weight,TagKey<Block> tag,ResourceLocation preferred){}
    public record Ore(BlockState state,int weight){}
    public record Pool(List<Ore> entries,int totalWeight){
        public Pool{entries=List.copyOf(entries);}
        public BlockState draw(Random random){
            int roll=random.nextInt(totalWeight);
            for(var entry:entries)if((roll-=entry.weight())<0)return entry.state();
            throw new IllegalStateException("Inconsistent bedrock ore weights");
        }
    }
    private static volatile List<Group> groups=List.of();
    public static List<Group> groups(){return groups;}
    public static Pool resolve(WorldGenLevel level){
        var registry=level.registryAccess().registryOrThrow(Registries.BLOCK);var result=new ArrayList<Ore>();int total=0;
        var blacklist=TinkererConfig.KAMI_BEDROCK_BLACKLIST.get();
        for(var group:groups){
            if(blacklist.contains(group.name()))continue;
            var members=registry.getTag(group.tag());if(members.isEmpty())continue;
            Block first=null,preferred=null;
            for(var holder:members.get()){
                var block=holder.value();if(!(block.asItem() instanceof BlockItem))continue;
                if(first==null)first=block;
                if(group.preferred()!=null&&registry.getKey(block).equals(group.preferred()))preferred=block;
            }
            var selected=preferred==null?first:preferred;if(selected==null)continue;
            total=Math.addExact(total,group.weight());result.add(new Ore(selected.defaultBlockState(),group.weight()));
        }
        return new Pool(result,total);
    }
    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader(){super(new Gson(),"bedrock_ores");}
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){
            var result=new LinkedHashMap<String,Group>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->{
                for(var element:file.getValue().getAsJsonObject().getAsJsonArray("groups")){
                    var object=element.getAsJsonObject();String name=object.get("name").getAsString();int weight=object.get("weight").getAsInt();
                    if(weight<=0)throw new IllegalArgumentException("Nonpositive bedrock ore weight: "+file.getKey()+"/"+name);
                    var tag=TagKey.create(Registries.BLOCK,ResourceLocation.parse(object.get("tag").getAsString()));
                    var preferred=object.has("preferred")?ResourceLocation.parse(object.get("preferred").getAsString()):null;
                    result.put(name,new Group(name,weight,tag,preferred));
                }
            });
            int total=0;for(var group:result.values())total=Math.addExact(total,group.weight());
            groups=List.copyOf(result.values());
        }
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event){event.addListener(new Loader());}
    private BedrockOres(){}
}
