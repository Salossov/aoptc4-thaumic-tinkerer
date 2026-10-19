package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.ClientHooks;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

/** Release overlay sizes, heights, rotations and cumulative tool transforms. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AnimationTabletRenderer implements BlockEntityRenderer<AnimationTabletBlockEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(AnimationTabletContent.TILE.get(),context->new AnimationTabletRenderer());}
    @Override public void render(AnimationTabletBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int index=tile.rotation();float rotation=TabletVisualTransform.ROTATIONS[index];
        quad(pose,buffers,"at_center",TabletVisualTransform.OVERLAY_CENTER_SIZE,TabletVisualTransform.OVERLAY_CENTER_HEIGHT,(float)(-tile.ticksExisted%360));
        quad(pose,buffers,tile.leftClick?"at_left":"at_right",TabletVisualTransform.OVERLAY_MODE_SIZE,tile.leftClick?TabletVisualTransform.OVERLAY_LEFT_HEIGHT:TabletVisualTransform.OVERLAY_RIGHT_HEIGHT,(float)(tile.ticksExisted%360));
        quad(pose,buffers,"at_facing_indent",TabletVisualTransform.OVERLAY_INDENT_SIZE,TabletVisualTransform.OVERLAY_INDENT_HEIGHT,rotation+TabletVisualTransform.OVERLAY_INDENT_ANGLE_OFFSET);
        var stack=tile.getItem(0);if(stack.isEmpty())return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(TabletVisualTransform.ITEM_BASE_TRANS_X,TabletVisualTransform.ITEM_BASE_TRANS_Y+TabletVisualTransform.itemHoverY(System.currentTimeMillis()),TabletVisualTransform.ITEM_BASE_TRANS_Z);
        var translations=TabletVisualTransform.TRANSLATIONS[index];
        pose.translate(translations[0],translations[1],translations[2]);
        pose.scale(TabletVisualTransform.ITEM_SCALE,TabletVisualTransform.ITEM_SCALE,TabletVisualTransform.ITEM_SCALE);
        pose.translate(.5,0,.5);
        pose.mulPose(Axis.ZP.rotationDegrees(tile.swingProgress));
        pose.translate(-.5,0,-.5);
        pose.translate(-tile.swingProgress/250.0F,tile.swingProgress/1000.0F,0);
        pose.mulPose(Axis.of(new org.joml.Vector3f(TabletVisualTransform.WOBBLE_AXIS_X,TabletVisualTransform.WOBBLE_AXIS_Y,TabletVisualTransform.WOBBLE_AXIS_Z).normalize()).rotationDegrees(TabletVisualTransform.itemWobbleAngle(System.currentTimeMillis())));
        pose.translate(TabletVisualTransform.ITEM_FINAL_TRANS_X,TabletVisualTransform.ITEM_FINAL_TRANS_Y,TabletVisualTransform.ITEM_FINAL_TRANS_Z);
        float scale=stack.getItem() instanceof BlockItem?TabletVisualTransform.ITEM_SCALE_BLOCK:TabletVisualTransform.ITEM_SCALE_GENERIC;
        pose.scale(scale,scale,scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack.copyWithCount(1),ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);
        pose.popPose();
    }
    private static void quad(PoseStack pose,MultiBufferSource buffers,String texture,double size,float height,float degrees){
        pose.pushPose();pose.translate(.5,height,.5);pose.mulPose(Axis.YP.rotationDegrees(degrees));float half=(float)(size/2);var transform=pose.last();var vertices=buffers.getBuffer(OverlayTypes.get(texture));
        float[][] corners={{-half,half,0,1},{half,half,1,1},{half,-half,1,0},{-half,-half,0,0}};
        for(var q:corners)vertices.addVertex(transform,q[0],0,q[1]).setColor(1,1,1,1).setUv(q[2],q[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(transform,0,1,0);pose.popPose();
    }
    /** Source overlays disable both lighting and depth writes. */
    private static final class OverlayTypes extends RenderType {
        private static final java.util.Map<String,RenderType> TYPES=new java.util.HashMap<>();
        static RenderType get(String texture){return TYPES.computeIfAbsent(texture,key->create("thaumictinkerer_tablet_"+key,DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(ClientHooks.ClientEvents::getEntityTranslucentUnlitShader))
                        .setTextureState(new TextureStateShard(ThaumicTinkerer.id("textures/misc/"+key+".png"),false,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false)));}
        private OverlayTypes(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumble,boolean sorted,Runnable setup,Runnable clear){super(name,format,mode,size,crumble,sorted,setup,clear);}
    }
}
