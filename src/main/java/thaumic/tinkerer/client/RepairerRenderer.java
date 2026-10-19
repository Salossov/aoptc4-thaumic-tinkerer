package thaumic.tinkerer.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

/** Original ModelRepairer, transforms and animation; native item rendering supports modern components. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class RepairerRenderer implements BlockEntityRenderer<RepairerBlockEntity> {
    private static final ResourceLocation TEXTURE=RepairerVisualTransform.MODEL_TEXTURE;
    private final ModelPart model=bake();
    public RepairerRenderer(){}
    private static float f(JsonObject object,String key,int i){return object.getAsJsonArray(key).get(i).getAsFloat();}
    private static ModelPart bake(){
        try(var input=RepairerRenderer.class.getResourceAsStream("/assets/thaumictinkerer/entity_models/repairer.json")){
            if(input==null)throw new IOException("Missing original repairer geometry");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();var mesh=new MeshDefinition();
            for(var entry:json.getAsJsonArray("parts")){
                var part=entry.getAsJsonObject();var uv=part.getAsJsonArray("uv");
                mesh.getRoot().addOrReplaceChild(part.get("name").getAsString(),CubeListBuilder.create().texOffs(uv.get(0).getAsInt(),uv.get(1).getAsInt()).addBox(f(part,"box",0),f(part,"box",1),f(part,"box",2),f(part,"box",3),f(part,"box",4),f(part,"box",5)),PartPose.offsetAndRotation(f(part,"pivot",0),f(part,"pivot",1),f(part,"pivot",2),f(part,"rotation",0),f(part,"rotation",1),f(part,"rotation",2)));
            }
            return LayerDefinition.create(mesh,json.get("texture_width").getAsInt(),json.get("texture_height").getAsInt()).bakeRoot();
        }catch(IOException error){throw new IllegalStateException(error);}
    }
    @Override public void render(RepairerBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float rotation=RepairerVisualTransform.getFacingRotation(tile.orientation());
        draw(tile.getItem(0),tile.ticksExisted,tile.tookLastTick,rotation,pose,buffers,light,overlay,tile.getLevel());
    }
    private void parts(boolean glass,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var output=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        var names=glass?new String[]{"Glass1","Glass2","Glass3","Glass4","Glass5"}:new String[]{"Base","Support1","Support2","Support3","Support4","Top1","Top2","Top3","Top4","Opening1","Opening2","Opening3","Opening4"};
        for(var name:names)model.getChild(name).render(pose,output,light,overlay);
    }
    void draw(ItemStack stack,int ticks,boolean active,float rotation,PoseStack pose,MultiBufferSource buffers,int light,int overlay,Level level){
        float degrees=RepairerVisualTransform.rotationDegrees(ticks);
        pose.pushPose();
        pose.translate(RepairerVisualTransform.TILE_TRANSLATE_X,RepairerVisualTransform.TILE_TRANSLATE_Y,RepairerVisualTransform.TILE_TRANSLATE_Z);
        pose.scale(RepairerVisualTransform.TILE_SCALE_X,RepairerVisualTransform.TILE_SCALE_Y,RepairerVisualTransform.TILE_SCALE_Z);
        pose.translate(RepairerVisualTransform.TILE_CENTER_X,RepairerVisualTransform.TILE_CENTER_Y,RepairerVisualTransform.TILE_CENTER_Z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        parts(false,pose,buffers,light,overlay);
        flush(buffers);
        pose.scale(1,-1,-1);

        if(!stack.isEmpty()){
            pose.pushPose();
            pose.scale(RepairerVisualTransform.ITEM_SCALE,RepairerVisualTransform.ITEM_SCALE,RepairerVisualTransform.ITEM_SCALE);
            pose.translate(-.5,RepairerVisualTransform.itemTranslationY(ticks),0);
            pose.translate(.5,.5,.03125);
            pose.mulPose(Axis.YP.rotationDegrees(degrees));
            pose.translate(-.5,-.5,-.03125);
            // The source sprite center is (.5,.5,-1/32), while native models
            // render around zero. Keep that center for the modern item model.
            pose.translate(.5,.5,-.03125);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,level,0);
            pose.popPose();
            flush(buffers);
        }

        // Source glass is rendered after the item and receives another facing
        // rotation. The paired reflections cancel before that extra rotation.
        pose.scale(1,-1,-1);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        parts(true,pose,buffers,light,overlay);
        flush(buffers);
        pose.scale(1,-1,-1);
        pose.translate(0,RepairerVisualTransform.OVERLAY_TRANSLATE_Y,0);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        var output=buffers.getBuffer(OverlayTypes.overlay(active));
        var last=pose.last();
        float size=RepairerVisualTransform.OVERLAY_HALF_SIZE;
        output.addVertex(last,-size,0,size).setColor(-1).setUv(0,1).setOverlay(overlay).setLight(light).setNormal(last,0,1,0);
        output.addVertex(last,size,0,size).setColor(-1).setUv(1,1).setOverlay(overlay).setLight(light).setNormal(last,0,1,0);
        output.addVertex(last,size,0,-size).setColor(-1).setUv(1,0).setOverlay(overlay).setLight(light).setNormal(last,0,1,0);
        output.addVertex(last,-size,0,-size).setColor(-1).setUv(0,0).setOverlay(overlay).setLight(light).setNormal(last,0,1,0);
        pose.popPose();
    }
    private static void flush(MultiBufferSource buffers){if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch();}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(RepairerContent.TILE.get(),context->new RepairerRenderer());}
    private static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final RepairerRenderer renderer=new RepairerRenderer();
        ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){renderer.draw(ItemStack.EMPTY,0,true,180,pose,buffers,light,overlay,null);}
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){event.registerItem(new IClientItemExtensions(){private ItemRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ItemRenderer();return renderer;}},RepairerContent.BLOCK.get().asItem());}
    private static final class OverlayTypes extends RenderType {
        private static final RenderType ON=createOverlay(true),OFF=createOverlay(false);
        private static RenderType createOverlay(boolean active){return create("thaumictinkerer_repairer_overlay",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(active?RepairerVisualTransform.REPAIR_OVERLAY:RepairerVisualTransform.REPAIR_OFF_OVERLAY,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));}
        static RenderType overlay(boolean active){return active?ON:OFF;}
        private OverlayTypes(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumble,boolean sorted,Runnable setup,Runnable clear){super(name,format,mode,size,crumble,sorted,setup,clear);}
    }
}
