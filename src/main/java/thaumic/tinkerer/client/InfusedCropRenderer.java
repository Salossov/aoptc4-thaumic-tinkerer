package thaumic.tinkerer.client;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.crop.*;

/** Source crop mesh in the ordinary chunk pipeline, without entity diffuse shading. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusedCropRenderer {
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        var sprites=new TextureAtlasSprite[7][4];
        for(int aspect=0;aspect<7;aspect++)for(int stage=0;stage<4;stage++){
            var path=ThaumicTinkerer.id("block/crop_"+CropVisualTransform.TEXTURE_NAMES[aspect]+"_"+stage);
            sprites[aspect][stage]=event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,path));
        }
        event.getModels().replaceAll((key,model)->key.id().equals(ThaumicTinkerer.id("infused_grain_block"))&&key.variant().startsWith("age=")?new CropModel(model,sprites):model);
    }
    public static final class CropModel extends BakedModelWrapper<BakedModel> {
        private final TextureAtlasSprite[][] sprites;
        private final Map<CropRenderData,List<BakedQuad>> cache=new ConcurrentHashMap<>();
        public CropModel(BakedModel original,TextureAtlasSprite[][] sprites){super(original);this.sprites=sprites;}
        @Override public boolean useAmbientOcclusion(){return false;}
        @Override public TextureAtlasSprite getParticleIcon(){return sprites[6][0];}
        @Override public TextureAtlasSprite getParticleIcon(ModelData data){var value=appearance(null,data);return sprites[value.texture()][value.stage()];}
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state,RandomSource random,ModelData data){return ChunkRenderTypeSet.of(RenderType.cutout());}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
            if(side!=null||type!=null&&type!=RenderType.cutout())return List.of();
            return cache.computeIfAbsent(appearance(state,data),this::mesh);
        }
        private static CropRenderData appearance(BlockState state,ModelData data){
            var value=data.get(CropRenderData.PROPERTY);if(value==null)value=CropRenderData.EMPTY;
            int stage=state!=null&&state.hasProperty(InfusedCropBlock.AGE)?InfusedCropBlock.textureAge(state.getValue(InfusedCropBlock.AGE)):value.stage();
            return new CropRenderData(value.texture(),stage,value.color());
        }
        private List<BakedQuad> mesh(CropRenderData appearance){
            var result=new ArrayList<BakedQuad>();var sprite=sprites[appearance.texture()][appearance.stage()];
            int rgb=appearance.color(),abgr=0xff000000|(rgb&255)<<16|rgb&0xff00|rgb>>16&255;
            for(var quad:CropVisualTransform.CROP_QUADS){
                int[] vertices=new int[32];int offset=0;
                for(var point:quad){
                    vertices[offset++]=Float.floatToRawIntBits(point[0]);
                    vertices[offset++]=Float.floatToRawIntBits(point[1]+CropVisualTransform.CROP_Y_OFFSET_F);
                    vertices[offset++]=Float.floatToRawIntBits(point[2]);vertices[offset++]=abgr;
                    vertices[offset++]=Float.floatToRawIntBits(sprite.getU(point[3]));vertices[offset++]=Float.floatToRawIntBits(sprite.getV(point[4]));
                    vertices[offset++]=0;vertices[offset++]=0x00007f00;
                }
                // World tessellator: block light, no directional shade or AO.
                result.add(new BakedQuad(vertices,-1,Direction.UP,sprite,false,false));
            }
            return List.copyOf(result);
        }
    }
    private InfusedCropRenderer(){}
}
