package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

/** Release overlay sizes, heights, rotations and cumulative tool transforms. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AnimationTabletRenderer implements BlockEntityRenderer<AnimationTabletBlockEntity> {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(AnimationTabletContent.TILE.get(),context->new AnimationTabletRenderer());}
    @Override public void render(AnimationTabletBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int index=tile.rotation();float rotation=new float[]{270,90,0,180}[index];
        quad(pose,buffers,"at_center",.65,.13f,(float)(-tile.ticksExisted%360));
        quad(pose,buffers,tile.leftClick?"at_left":"at_right",1,tile.leftClick?.13f:.131f,(float)(tile.ticksExisted%360));
        quad(pose,buffers,"at_facing_indent",.5,.13f,rotation+90);
        var stack=tile.getItem(0);if(stack.isEmpty())return;pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(rotation));pose.translate(.1,.2+Math.cos(System.currentTimeMillis()/600.0)/18,.5);
        var translations=new float[][]{{0,0,-1},{-1,0,0},{0,0,0},{-1,0,-1}}[index];pose.translate(translations[0],translations[1],translations[2]);pose.scale(.8f,.8f,.8f);pose.translate(.5,0,.5);pose.mulPose(Axis.ZP.rotationDegrees(tile.swingProgress));pose.translate(-.5,0,-.5);pose.translate(-tile.swingProgress/250f,tile.swingProgress/1000f,0);
        pose.mulPose(Axis.of(new org.joml.Vector3f(1,0,1).normalize()).rotationDegrees((float)Math.cos((float)System.currentTimeMillis()/400f)*5));pose.translate(.5,.55,0);float scale=stack.getItem() instanceof BlockItem?2.5f:1.5f;pose.scale(scale,scale,scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack.copyWithCount(1),ItemDisplayContext.FIXED,light,overlay,pose,buffers,tile.getLevel(),0);pose.popPose();
    }
    private static void quad(PoseStack pose,MultiBufferSource buffers,String texture,double size,float height,float degrees){
        pose.pushPose();pose.translate(.5,height,.5);pose.mulPose(Axis.YP.rotationDegrees(degrees));float half=(float)(size/2);var transform=pose.last();var vertices=buffers.getBuffer(RenderType.entityTranslucent(ThaumicTinkerer.id("textures/misc/"+texture+".png")));
        float[][] corners={{-half,half,0,1},{half,half,1,1},{half,-half,1,0},{-half,-half,0,0}};
        for(var q:corners)vertices.addVertex(transform,q[0],0,q[1]).setColor(1,1,1,1).setUv(q[2],q[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xf000f0).setNormal(transform,0,1,0);pose.popPose();
    }
}
