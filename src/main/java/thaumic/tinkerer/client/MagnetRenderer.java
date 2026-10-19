package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import thaumic.tinkerer.block.MagnetBlock;
import thaumic.tinkerer.block.MagnetBlockEntity;

/** Original ModelMagnet and RenderTileMagnet transforms, including cumulative ring scales. */
public final class MagnetRenderer implements BlockEntityRenderer<MagnetBlockEntity> {
    private final ModelPart model = layer().bakeRoot();
    private static LayerDefinition layer() {
        var mesh = new MeshDefinition(); var root = mesh.getRoot();
        root.addOrReplaceChild("panel", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 14, 2, 14), PartPose.offset(-7, 22, -7));
        root.addOrReplaceChild("magnet", CubeListBuilder.create().texOffs(0, 16).addBox(0, 0, 0, 4, 13, 4), PartPose.offset(-2, 9, -2));
        root.addOrReplaceChild("box1", CubeListBuilder.create().texOffs(28, 19).addBox(0, -2, 0, 6, 14, 0), PartPose.offsetAndRotation(3, 10, -3, 0, -1.570796f, 0));
        root.addOrReplaceChild("box2", CubeListBuilder.create().texOffs(28, 33).addBox(0, 0, 0, 6, 14, 0), PartPose.offsetAndRotation(-3, 8, 3, 0, 1.570796f, 0));
        root.addOrReplaceChild("box3", CubeListBuilder.create().texOffs(40, 19).addBox(0, 0, 0, 6, 14, 0), PartPose.offsetAndRotation(3, 8, 3, 0, 3.141593f, 0));
        root.addOrReplaceChild("box4", CubeListBuilder.create().texOffs(40, 33).addBox(0, 0, 0, 6, 14, 0), PartPose.offset(-3, 8, -3));
        root.addOrReplaceChild("box5", CubeListBuilder.create().texOffs(28, 49).addBox(0, 0, 0, 6, 0, 6), PartPose.offset(-3, 8, -3));
        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override public void render(MagnetBlockEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(((MagnetBlock)tile.getBlockState().getBlock()).mobs, tile.isPulling(), tile.signal(), pose, buffers, light, overlay);
    }
    public void draw(boolean mobs, boolean pulling, int signal, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var texture = MagnetVisualTransform.modelTexture(mobs, pulling);
        pose.pushPose(); pose.translate(MagnetVisualTransform.TILE_TRANSLATE_X, MagnetVisualTransform.TILE_TRANSLATE_Y, MagnetVisualTransform.TILE_TRANSLATE_Z);
        pose.scale(MagnetVisualTransform.TILE_SCALE_X, MagnetVisualTransform.TILE_SCALE_Y, MagnetVisualTransform.TILE_SCALE_Z);
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        // The original zero-scale rings have no visible area. Avoid a singular modern normal matrix.
        if (signal <= 0) { pose.popPose(); return; }
        pose.mulPose(Axis.XP.rotationDegrees(MagnetVisualTransform.RING_ROT_X));
        pose.translate(0, 0, MagnetVisualTransform.RING_BASE_TRANS_Z);
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(MagnetVisualTransform.RING_SPRITE);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        var vertices = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float scale = MagnetVisualTransform.ringScale(signal);
        float red = MagnetVisualTransform.ringRed(pulling), blue = MagnetVisualTransform.ringBlue(pulling), alpha = MagnetVisualTransform.ringAlpha();
        int ringLight = MagnetVisualTransform.ringLight();
        for (int ring = 0; ring < MagnetVisualTransform.RING_COUNT; ring++) {
            for (int face = 0; face < MagnetVisualTransform.FACES_PER_RING; face++) {
                pose.scale(1, -1, 1); pose.scale(scale, scale, scale);
                var last = pose.last();
                vertices.addVertex(last, -.5f, .5f, 0).setColor(red, 0, blue, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(ringLight).setNormal(last, 0, 0, 1);
                vertices.addVertex(last, .5f, .5f, 0).setColor(red, 0, blue, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(ringLight).setNormal(last, 0, 0, 1);
                vertices.addVertex(last, .5f, -.5f, 0).setColor(red, 0, blue, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(ringLight).setNormal(last, 0, 0, 1);
                vertices.addVertex(last, -.5f, -.5f, 0).setColor(red, 0, blue, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(ringLight).setNormal(last, 0, 0, 1);
            }
            pose.translate(0, 0, MagnetVisualTransform.ringZStep(System.currentTimeMillis()));
        }
        pose.popPose();
    }
}
