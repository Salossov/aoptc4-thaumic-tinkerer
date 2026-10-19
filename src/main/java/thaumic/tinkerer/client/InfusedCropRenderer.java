package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.crop.*;

/** Original crop planes, -1/16 render offset and compound-aspect world tint. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusedCropRenderer implements BlockEntityRenderer<InfusedCropBlockEntity> {
    private static final String[] TEXTURES={"aer","ignis","aqua","terra","ordo","perditio","generic"};
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(InfusedContent.CROP_TILE.get(),context->new InfusedCropRenderer());}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->ItemProperties.register(InfusedContent.SEEDS.get(),ThaumicTinkerer.id("seed_aspect"),(stack,level,entity,seed)->{var aspect=InfusedSeedsItem.aspect(stack);return aspect==null?0:InfusedCropBlock.textureIndex(aspect);}));}
    @Override public void render(InfusedCropBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var aspect=tile.aspect;int color=aspect!=null&&!aspect.isPrimal()?aspect.getColor():0xffffff;
        var texture=ThaumicTinkerer.id("textures/block/crop_"+TEXTURES[InfusedCropBlock.textureIndex(aspect)]+"_"+InfusedCropBlock.textureAge(tile.getBlockState().getValue(InfusedCropBlock.AGE))+".png");
        var vertices=buffers.getBuffer(RenderType.entityCutout(texture));pose.pushPose();pose.translate(0,-.0625,0);var transform=pose.last();
        for(boolean axis:new boolean[]{true,false})for(float offset:new float[]{.25f,.75f})for(int face=0;face<2;face++){
            float[][] corners=axis?new float[][]{{offset,1,0,0,0},{offset,0,0,0,1},{offset,0,1,1,1},{offset,1,1,1,0}}:new float[][]{{0,1,offset,0,0},{0,0,offset,0,1},{1,0,offset,1,1},{1,1,offset,1,0}};
            boolean reverse=(offset==.75f)^(face==1);
            // The bytecode emits eight faces: back faces have their own U
            // direction. One uncullable quad would mirror their source art.
            for(int i=0;i<4;i++){var point=corners[reverse?3-i:i];vertices.addVertex(transform,point[0],point[1],point[2]).setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1).setUv(corners[i][3],corners[i][4]).setOverlay(overlay).setLight(light).setNormal(transform,0,1,0);}
        }
        pose.popPose();
    }
}
