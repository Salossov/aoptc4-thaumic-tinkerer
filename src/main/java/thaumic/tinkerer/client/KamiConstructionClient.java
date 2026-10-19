package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.construction.PlacementMirrorItem;

/** Source textured alpha-.6 predictions, with the approved real block orientation. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.GAME,value=Dist.CLIENT)
public final class KamiConstructionClient {
    private static final RenderType GHOST=RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();
        VertexConsumer vertices=null;
        // Remote inventories have the same limited client-side knowledge as in
        // the source. No new inventory synchronization is inferred here.
        for(var player:mc.level.players()){
            var stack=player.getMainHandItem();
            if(!(stack.getItem() instanceof PlacementMirrorItem)){
                stack=player.getOffhandItem();if(!(stack.getItem() instanceof PlacementMirrorItem))continue;
            }
            var state=PlacementMirrorItem.getState(stack);if(state.isAir())continue;
            var plans=PlacementMirrorItem.plans(stack,player);
            if(!PlacementMirrorItem.hasBlocks(stack,player,PlacementMirrorItem.cost(plans)))continue;
            if(vertices==null)vertices=new GhostAlpha(buffers.getBuffer(GHOST));
            var dispatcher=mc.getBlockRenderer();
            for(var placement:plans)for(var part:placement.plan().parts()){
                state=part.state();var pos=part.pos();var model=dispatcher.getBlockModel(state);
                pose.pushPose();
                try{
                    pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
                    // Source disables inventory tint. Preserve native geometry,
                    // texture UVs and face normals without a biome color multiplier.
                    for(var layer:model.getRenderTypes(state,RandomSource.create(42),ModelData.EMPTY))
                        dispatcher.getModelRenderer().renderModel(pose.last(),vertices,state,model,1F,1F,1F,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,layer);
                }finally{pose.popPose();}
            }
        }
        if(vertices!=null)buffers.endBatch(GHOST);
    }
    private static final class GhostAlpha extends VertexConsumerWrapper {
        GhostAlpha(VertexConsumer parent){super(parent);}
        @Override public VertexConsumer setColor(int r,int g,int b,int alpha){parent.setColor(r,g,b,Math.round(alpha*.6F));return this;}
    }
    private KamiConstructionClient(){}
}
