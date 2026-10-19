package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.MagnetContent;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MagnetClient {
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent event) { event.register(MagnetContent.MENU.get(), MobMagnetScreen::new); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MagnetContent.MAGNET_TILE.get(), context -> new MagnetRenderer());
        event.registerBlockEntityRenderer(MagnetContent.MOB_TILE.get(), context -> new MagnetRenderer());
    }
    private static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final MagnetRenderer renderer = new MagnetRenderer();
        ItemRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()); }
        @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderer.draw(stack.is(MagnetContent.MOB_MAGNET.get().asItem()), true, 15, pose, buffers, light, overlay);
        }
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private ItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { if (renderer == null) renderer = new ItemRenderer(); return renderer; }
        }, MagnetContent.MAGNET.get().asItem(), MagnetContent.MOB_MAGNET.get().asItem());
    }
    private MagnetClient() { }
}
