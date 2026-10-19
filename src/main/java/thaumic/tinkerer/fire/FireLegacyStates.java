package thaumic.tinkerer.fire;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Extracted identities, rather than treating flattened variants as metadata zero. */
public final class FireLegacyStates {
    public record Key(String block,int meta,BlockState modern){
        public Key zero(){return modern==null?new Key(block,0,null):this;}
    }
    private record Pattern(String legacy,int meta,Map<String,String> properties){}
    private static final Map<String,List<Pattern>> VANILLA=new HashMap<>();
    private static final Map<String,Pattern> CORE=new HashMap<>();
    private static final Map<BlockState,Key> CACHE=Collections.synchronizedMap(new IdentityHashMap<>());
    static{
        try(var stream=Objects.requireNonNull(FireLegacyStates.class.getResourceAsStream("/assets/thaumictinkerer/fire/legacy_states.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            for(var element:root.getAsJsonArray("vanilla")){
                var item=element.getAsJsonObject();var properties=new HashMap<String,String>();item.getAsJsonObject("properties").entrySet().forEach(e->properties.put(e.getKey(),e.getValue().getAsString()));
                VANILLA.computeIfAbsent(item.get("block").getAsString(),unused->new ArrayList<>()).add(new Pattern(item.get("legacy").getAsString(),item.get("meta").getAsInt(),Map.copyOf(properties)));
            }
            root.getAsJsonObject("thaumcraft").entrySet().forEach(e->{var p=e.getValue().getAsJsonObject();CORE.put(e.getKey(),new Pattern(p.get("legacy").getAsString(),p.get("meta").getAsInt(),Map.of()));});
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    private static <T extends Comparable<T>> String value(BlockState state,net.minecraft.world.level.block.state.properties.Property<T> property){return property.getName(state.getValue(property));}
    private static String property(BlockState state,String name){var p=state.getBlock().getStateDefinition().getProperty(name);return p==null?"":value(state,p);}
    public static Key key(BlockState state){return CACHE.computeIfAbsent(state,FireLegacyStates::resolve);}
    private static Key resolve(BlockState state){
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(id.toString().equals("thaumictinkerer:legacy_dry_shrub"))return new Key("minecraft:tallgrass",0,null);
        if(id.getNamespace().equals("minecraft"))for(var pattern:VANILLA.getOrDefault(id.toString(),List.of())){
            if(pattern.properties.entrySet().stream().allMatch(e->property(state,e.getKey()).equals(e.getValue())))return new Key(pattern.legacy,pattern.meta,null);
        }
        if(id.getNamespace().equals("thaumcraft")){
            var pattern=CORE.get(id.getPath());
            if(pattern!=null){
                int meta=pattern.meta;
                if(id.getPath().equals("golem_fetter")&&property(state,"powered").equals("true"))meta=10;
                if(id.getPath().equals("taint_spore_stalk")&&property(state,"occupied").equals("true"))meta=4;
                if(id.getPath().equals("arcane_pressure_plate")&&property(state,"powered").equals("true"))meta=3;
                if(pattern.legacy.equals("thaumcraft:blockMagicalLog"))meta+=switch(property(state,"axis")){case "x"->4;case "z"->8;default->0;};
                if(pattern.legacy.contains("Slab")){if(property(state,"type").equals("top"))meta+=8;else if(property(state,"type").equals("double"))return new Key(pattern.legacy.replace("Slab","DoubleSlab"),meta,null);}
                if(pattern.legacy.equals("thaumcraft:blockCustomPlant")&&meta<2&&property(state,"stage").equals("1"))meta+=8;
                if(pattern.legacy.equals("thaumcraft:blockMagicalLeaves")&&property(state,"persistent").equals("true"))meta+=4;
                if(pattern.legacy.equals("thaumcraft:blockMirror")){int direction=switch(property(state,"facing")){case "down"->0;case "up"->1;case "north"->2;case "south"->3;case "west"->4;case "east"->5;default->0;};meta+=direction;}
                return new Key(pattern.legacy,meta,null);
            }
        }
        return new Key(id.toString(),0,state);
    }
    public static boolean isMetadataZero(BlockState state){return key(state).meta==0;}
    public static boolean sameLegacyBlock(BlockState first,BlockState second){return key(first).block.equals(key(second).block);}
    public static boolean matchesResult(BlockState result,BlockState query){var resultKey=key(result);return resultKey.modern!=null?result==query:resultKey.meta==0&&sameLegacyBlock(result,query);}
    private FireLegacyStates(){}
}
