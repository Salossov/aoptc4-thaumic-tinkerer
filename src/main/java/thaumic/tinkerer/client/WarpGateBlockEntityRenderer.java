package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.warpgate.WarpGateBlockEntity;
import thaumic.tinkerer.kami.warpgate.WarpGateContent;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WarpGateBlockEntityRenderer implements BlockEntityRenderer<WarpGateBlockEntity> {
    private final WarpGateSpinningCubesModel cubesModel = new WarpGateSpinningCubesModel();

    public WarpGateBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        if (TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            event.registerBlockEntityRenderer(WarpGateContent.WARP_GATE_TILE.get(), WarpGateBlockEntityRenderer::new);
        }
    }

    @Override
    public void render(WarpGateBlockEntity tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!TinkererConfig.KAMI_FORCE_ENABLED.get() || tile == null) return;
        Level level = tile.getLevel();
        if (level == null) return;

        // The source rune belongs to the chunk model; only the cube trail is a TESR.
        Minecraft mc = Minecraft.getInstance();
        double playerTicks = (mc.player != null ? mc.player.tickCount : level.getGameTime());
        this.cubesModel.renderSpinningCubes(poseStack, bufferSource, playerTicks, packedOverlay);
    }
}
