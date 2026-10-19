package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumic.tinkerer.ThaumicTinkerer;

/**
 * Modern 1.21.1 port of ModelSpinningCubes from Thaumic Tinkerer 2.5-1.7.10-164.
 * Renders 12 small cubes rotating in orbits above the Warp Gate with motion trail echoes.
 */
public final class WarpGateSpinningCubesModel {
    public static final ResourceLocation WHITE_TEXTURE = ThaumicTinkerer.id("textures/block/warp_gate_white.png");

    public void renderSpinningCubes(PoseStack poseStack, MultiBufferSource bufferSource, double playerTicks, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        int cubes = WarpGateVisualHelper.CUBES_COUNT;
        int origRepeat = WarpGateVisualHelper.REPEAT_COUNT;

        // In 1.7.10 RenderTileWarpGate:
        // GL11.glTranslated(x + 0.5, y + 2.5, z + 0.5);
        // GL11.glRotatef(180.0F, 1.0F, 0.0F, 1.0F);
        poseStack.pushPose();
        poseStack.translate(WarpGateVisualHelper.BASE_TRANSLATE_X, WarpGateVisualHelper.BASE_TRANSLATE_Y, WarpGateVisualHelper.BASE_TRANSLATE_Z);
        Vector3f baseAxis = new Vector3f(1.0F, 0.0F, 1.0F).normalize();
        poseStack.mulPose(Axis.of(baseAxis).rotationDegrees(WarpGateVisualHelper.ROTATION_ANGLE_DEG));

        for (int repeat = origRepeat; repeat >= 0; repeat--) {
            double ticks = WarpGateVisualHelper.ticksForRepeat(playerTicks, repeat, origRepeat);
            float alpha = WarpGateVisualHelper.alphaForRepeat(repeat, origRepeat);

            poseStack.pushPose();
            // GL11.glTranslatef(-0.025F, 0.85F, -0.025F);
            poseStack.translate(WarpGateVisualHelper.RING_OFFSET_X, WarpGateVisualHelper.RING_OFFSET_Y, WarpGateVisualHelper.RING_OFFSET_Z);

            for (int i = 0; i < cubes; i++) {
                int deg = WarpGateVisualHelper.degForCube(ticks, i, cubes);
                float radiusX = WarpGateVisualHelper.radiusX(ticks);
                float radiusZ = WarpGateVisualHelper.radiusZ(ticks);
                float x = WarpGateVisualHelper.cubeX(radiusX, deg);
                float z = WarpGateVisualHelper.cubeZ(radiusZ, deg);
                float y = WarpGateVisualHelper.cubeY(ticks, i);

                Vector3f rotAxis = WarpGateVisualHelper.cubeRotAxis(ticks);

                poseStack.pushPose();
                poseStack.translate(x, y, z);
                if (rotAxis.lengthSquared() > 1e-6F) {
                    rotAxis.normalize();
                    poseStack.mulPose(Axis.of(rotAxis).rotationDegrees(deg));
                }

                renderCubeBox(poseStack, consumer, alpha, packedOverlay);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void renderCubeBox(PoseStack poseStack, VertexConsumer consumer, float alpha, int overlay) {
        Matrix4f mat = poseStack.last().pose();
        float s = WarpGateVisualHelper.CUBE_SCALE; // 0.0625F (addBox(0,0,0, 1,1,1) * 0.0625F)

        // 6 faces from 0.0F to s:
        // Up (+Y)
        consumer.addVertex(mat, 0.0F, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        consumer.addVertex(mat, s, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        consumer.addVertex(mat, s, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        consumer.addVertex(mat, 0.0F, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);

        // Down (-Y)
        consumer.addVertex(mat, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, -1.0F, 0.0F);
        consumer.addVertex(mat, s, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, -1.0F, 0.0F);
        consumer.addVertex(mat, s, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, -1.0F, 0.0F);
        consumer.addVertex(mat, 0.0F, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, -1.0F, 0.0F);

        // North (-Z)
        consumer.addVertex(mat, 0.0F, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, -1.0F);
        consumer.addVertex(mat, s, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, -1.0F);
        consumer.addVertex(mat, s, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, -1.0F);
        consumer.addVertex(mat, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, -1.0F);

        // South (+Z)
        consumer.addVertex(mat, 0.0F, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(mat, s, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(mat, s, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);
        consumer.addVertex(mat, 0.0F, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);

        // West (-X)
        consumer.addVertex(mat, 0.0F, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), -1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, 0.0F, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), -1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, 0.0F, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), -1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, 0.0F, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), -1.0F, 0.0F, 0.0F);

        // East (+X)
        consumer.addVertex(mat, s, 0.0F, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, s, 0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, s, s, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 1.0F, 0.0F, 0.0F);
        consumer.addVertex(mat, s, s, s).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(poseStack.last(), 1.0F, 0.0F, 0.0F);
    }
}
