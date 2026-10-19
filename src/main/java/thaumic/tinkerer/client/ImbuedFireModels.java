package thaumic.tinkerer.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.fire.*;

/** Native chunk quads, with geometry/UVs extracted from the original fire renderer. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ImbuedFireModels {
    private static final ModelProperty<Integer> GEOMETRY=new ModelProperty<>();
    private static final Map<String,List<List<MeshQuad>>> MESHES=load();
    private record MeshQuad(int layer,List<float[]> vertices){}
    private static Map<String,List<List<MeshQuad>>> load(){
        var result=new HashMap<String,List<List<MeshQuad>>>();
        try(var stream=Objects.requireNonNull(ImbuedFireModels.class.getResourceAsStream("/assets/thaumictinkerer/fire/meshes.json"));var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            var root=JsonParser.parseReader(reader).getAsJsonObject();
            for(var shape:root.entrySet()){
                var variants=new ArrayList<List<MeshQuad>>();for(int variant=0;variant<4;variant++){
                    var quads=new ArrayList<MeshQuad>();for(var value:shape.getValue().getAsJsonObject().getAsJsonArray(Integer.toString(variant))){
                        var quad=value.getAsJsonObject();var vertices=new ArrayList<float[]>();for(var vertex:quad.getAsJsonArray("vertices")){var a=vertex.getAsJsonArray();vertices.add(new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat(),a.get(3).getAsFloat(),a.get(4).getAsFloat()});}
                        quads.add(new MeshQuad(quad.get("layer").getAsInt(),List.copyOf(vertices)));
                    }variants.add(List.copyOf(quads));
                }result.put(shape.getKey(),List.copyOf(variants));
            }
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
        return Map.copyOf(result);
    }
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        for(var kind:ImbuedFireKind.values()){
            var quads=new ConcurrentHashMap<Integer,List<BakedQuad>>();
            for(var state:ImbuedFireContent.BLOCKS_BY_KIND.get(kind).get().getStateDefinition().getPossibleStates()){
                var key=BlockModelShaper.stateToModelLocation(state);var base=event.getModels().get(key);
                if(base!=null)event.getModels().put(key,new Fire(base,kind,quads));
            }
        }
    }
    private static int geometry(BlockAndTintGetter level,BlockPos pos){
        int parity=(pos.getX()+pos.getY()+pos.getZ())&1;int flip=(pos.getX()/2+pos.getY()/2+pos.getZ()/2)&1;int mask=0;var fire=(FireBlock)Blocks.FIRE;
        if(level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||fire.canCatchFire(level,pos.below(),Direction.UP))mask=1;
        else{
            var directions=new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH,Direction.UP};
            for(int i=0;i<directions.length;i++)if(fire.canCatchFire(level,pos.relative(directions[i]),directions[i].getOpposite()))mask|=1<<(i+1);
        }
        return (mask<<2)|(parity*2+flip);
    }
    private static final class Fire extends BakedModelWrapper<BakedModel> {
        private final ImbuedFireKind kind;private final Map<Integer,List<BakedQuad>> quads;
        Fire(BakedModel base,ImbuedFireKind kind,Map<Integer,List<BakedQuad>> quads){super(base);this.kind=kind;this.quads=quads;}
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return data.derive().with(GEOMETRY,geometry(level,pos)).build();}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType renderType){
            if(side!=null||renderType!=null&&renderType!=RenderType.cutout())return List.of();
            var geometry=data.get(GEOMETRY);return quads.computeIfAbsent(geometry==null?4:geometry,this::bake);
        }
        private List<BakedQuad> bake(int key){
            var output=new ArrayList<BakedQuad>();int mask=key>>2,variant=key&3;var names=new String[]{"floor","west","east","north","south","up"};
            for(int bit=0;bit<names.length;bit++)if((mask&(1<<bit))!=0)for(var quad:MESHES.get(names[bit]).get(variant)){
                var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ThaumicTinkerer.id("block/"+kind.path()+"_"+quad.layer));var vertices=new int[32];
                for(int i=0;i<4;i++){
                    var vertex=quad.vertices.get(i);int offset=i*8;vertices[offset]=Float.floatToRawIntBits(vertex[0]);vertices[offset+1]=Float.floatToRawIntBits(vertex[1]);vertices[offset+2]=Float.floatToRawIntBits(vertex[2]);vertices[offset+3]=-1;
                    vertices[offset+4]=Float.floatToRawIntBits(sprite.getU(vertex[3]));vertices[offset+5]=Float.floatToRawIntBits(sprite.getV(vertex[4]));
                }
                output.add(new BakedQuad(vertices,-1,Direction.UP,sprite,false,false));
            }return List.copyOf(output);
        }
        @Override public boolean useAmbientOcclusion(){return false;}
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){return ChunkRenderTypeSet.of(RenderType.cutout());}
    }
    private ImbuedFireModels(){}
}
