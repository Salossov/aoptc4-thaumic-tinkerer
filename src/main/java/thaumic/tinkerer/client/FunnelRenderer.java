package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.api.aspects.IEssentiaJarItem;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FunnelRenderer implements BlockEntityRenderer<FunnelBlockEntity> {
    public FunnelRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(FunnelContent.TILE.get(),FunnelRenderer::new);}
    @Override public void render(FunnelBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var stack=tile.getItem(0);if(!(stack.getItem() instanceof IEssentiaJarItem))return;
        pose.pushPose();
        // Legacy .365 centre minus ENTITY's .25 shift = .115 base. Native NONE
        // shifts its item by .5, so its centre is .615 for the same world geometry.
        pose.translate(.5,.615,.5);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,tile.getLevel(),0);
        pose.popPose();
    }
}
