package thaumic.tinkerer.fire;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.TinkererConfig;

public enum ImbuedFireKind {
    AER("aer",3,-7,2),TERRA("terra",4,-6,2),IGNIS("ignis",4,-4,2),AQUA("aqua",2,-2,2),ORDO("ordo",3,-3,2),PERDITIO("perditio",2,-8,3);
    public final String tag;public final int x,y,complexity;
    ImbuedFireKind(String tag,int x,int y,int complexity){this.tag=tag;this.x=x;this.y=y;this.complexity=complexity;}
    public String path(){return "fire_"+tag;}
    /** Query ignores source metadata, exactly like new BlockTuple(block) in the release. */
    public BlockState transformation(Level level,BlockPos pos,BlockState query){
        if(this==ORDO)return OrderFireRules.transformation(level,pos,query);
        if((this==AER||this==IGNIS)&&query.is(Blocks.WATER)&&!query.getFluidState().isSource())return null;
        return transformations().get(query.getBlock());
    }
    public boolean isResult(Level level,BlockPos pos,BlockState query){
        if(this==ORDO)return OrderFireRules.isResult(level,pos,query);
        return transformations().values().stream().anyMatch(result->FireLegacyStates.matchesResult(result,query));
    }
    private static final JsonObject DECLARATIONS=load();
    private static JsonObject load(){
        try(var stream=Objects.requireNonNull(ImbuedFireKind.class.getResourceAsStream("/assets/thaumictinkerer/fire/transformations.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}
        catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    private static Block block(String id){return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow(()->new IllegalStateException("Missing extracted fire block "+id));}
    private static <T extends Comparable<T>> BlockState property(BlockState state,net.minecraft.world.level.block.state.properties.Property<T> property,String value){return state.setValue(property,property.getValue(value).orElseThrow());}
    public Map<Block,BlockState> transformations(){
        var result=new LinkedHashMap<Block,BlockState>();
        if(this==PERDITIO){for(var kind:values())if(kind!=PERDITIO)result.put(ImbuedFireContent.BLOCKS_BY_KIND.get(kind).get(),Blocks.FIRE.defaultBlockState());return result;}
        for(var element:DECLARATIONS.getAsJsonArray(tag)){
            var declaration=element.getAsJsonObject();if(declaration.get("cake").getAsBoolean()&&!TinkererConfig.IMBUED_FIRE_CAKE.get())continue;
            var target=declaration.getAsJsonObject("result");var state=block(target.get("block").getAsString()).defaultBlockState();
            if(target.has("properties"))for(var entry:target.getAsJsonObject("properties").entrySet())state=property(state,Objects.requireNonNull(state.getBlock().getStateDefinition().getProperty(entry.getKey())),entry.getValue().getAsString());
            for(var input:declaration.getAsJsonArray("inputs"))result.put(block(input.getAsString()),state);
        }
        return result;
    }
}
