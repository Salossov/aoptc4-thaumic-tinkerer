package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.EnchanterBlockEntity;
import thaumic.tinkerer.block.EnchanterContent;

/**
 * 1.7.10 RenderTileEnchanter port: renders slot 0 (item to enchant, laid flat on top of the block)
 * and slot 1 (hovering wand with bobbing and tilt animation).
 */
@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EnchanterRenderer implements BlockEntityRenderer<EnchanterBlockEntity> {
    public EnchanterRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EnchanterContent.TILE.get(), EnchanterRenderer::new);
    }

    @Override
    public void render(EnchanterBlockEntity tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // 1.7.10 RenderTileEnchanter: GL11.glTranslated(d0, d1 + 0.75, d2)
        pose.translate(0.0, EnchanterVisualTransform.BASE_HEIGHT, 0.0);

        // Slot 0: tool / item being enchanted
        var tool = tile.getItem(0);
        if (!tool.isEmpty()) {
            pose.pushPose();
            EnchanterVisualTransform.applyToolTransform(pose);
            var renderer=Minecraft.getInstance().getItemRenderer();
            var model=renderer.getModel(tool,tile.getLevel(),null,0);
            // Source ItemRenderer.renderItemIn2D uses x/y=0..1 and z=-1/16..0.
            // The native generated mesh is centered by ItemRenderer before drawing.
            if(!model.isGui3d()&&!model.isCustomRenderer())pose.translate(.5,.5,-1.0/32);
            renderer.render(tool,ItemDisplayContext.NONE,false,pose,buffers,light,overlay,model);
            pose.popPose();
        }

        // Slot 1: wand
        var wand = tile.getItem(1);
        if (!wand.isEmpty()) {
            pose.pushPose();
            long millis = System.currentTimeMillis();
            EnchanterVisualTransform.applyWandTransform(pose, millis);
            if(!thaumcraft.api.client.WandDeviceRendering.renderEntity(wand,pose,buffers,light,overlay,tile.getLevel())){
                Minecraft.getInstance().getItemRenderer().renderStatic(wand,ItemDisplayContext.NONE,light,overlay,pose,buffers,tile.getLevel(),0);
            }
            pose.popPose();
        }

        pose.popPose();
    }
}
