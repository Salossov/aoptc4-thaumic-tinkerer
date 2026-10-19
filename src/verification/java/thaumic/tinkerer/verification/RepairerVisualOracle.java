package thaumic.tinkerer.verification;

import thaumic.tinkerer.client.RepairerVisualTransform;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Independent mathematical oracle for Repairer geometry, animation transforms,
 * UV coordinates, matrix bounds, and item positioning.
 */
public final class RepairerVisualOracle {
    private RepairerVisualOracle() {}

    /**
     * Builds the exact 4x4 matrix for the model frame at the given facing rotation.
     */
    public static Matrix4f computeModelMatrix(float rotationDegrees) {
        Matrix4f mat = new Matrix4f();
        mat.translate((float) RepairerVisualTransform.TILE_TRANSLATE_X,
                      (float) RepairerVisualTransform.TILE_TRANSLATE_Y,
                      (float) RepairerVisualTransform.TILE_TRANSLATE_Z);
        mat.scale(RepairerVisualTransform.TILE_SCALE_X,
                  RepairerVisualTransform.TILE_SCALE_Y,
                  RepairerVisualTransform.TILE_SCALE_Z);
        mat.translate((float) RepairerVisualTransform.TILE_CENTER_X,
                      (float) RepairerVisualTransform.TILE_CENTER_Y,
                      (float) RepairerVisualTransform.TILE_CENTER_Z);
        mat.rotate((float) Math.toRadians(rotationDegrees), 0.0F, 1.0F, 0.0F);
        return mat;
    }

    /**
     * Computes the world-space bounding box of the base floor plate.
     */
    public static float[] computeBaseFloorBounds() {
        // Base box is 16x1x16, pivot (-8, 23, -8).
        // Model matrix converts this to world coordinates [0..1] x [0..1/16] x [0..1].
        Matrix4f mat = computeModelMatrix(0.0F);
        Vector4f minCorner = new Vector4f(-8.0F * 0.0625F, 24.0F * 0.0625F, -8.0F * 0.0625F, 1.0F);
        Vector4f maxCorner = new Vector4f(8.0F * 0.0625F, 23.0F * 0.0625F, 8.0F * 0.0625F, 1.0F);
        minCorner = mat.transform(minCorner);
        maxCorner = mat.transform(maxCorner);
        return new float[] {
            Math.min(minCorner.x, maxCorner.x), Math.min(minCorner.y, maxCorner.y), Math.min(minCorner.z, maxCorner.z),
            Math.max(minCorner.x, maxCorner.x), Math.max(minCorner.y, maxCorner.y), Math.max(minCorner.z, maxCorner.z)
        };
    }

    /**
     * Computes the overlay seal world Y height.
     */
    public static float computeOverlayWorldY() {
        return RepairerVisualTransform.OVERLAY_WORLD_Y;
    }

    /**
     * Computes overlay quad corners in local coordinates before rotation.
     */
    public static float[][] computeOverlayQuadVertices() {
        float half = RepairerVisualTransform.OVERLAY_HALF_SIZE;
        return new float[][] {
            {-half, 0.0F, half,  0.0F, 1.0F},
            {half,  0.0F, half,  1.0F, 1.0F},
            {half,  0.0F, -half, 1.0F, 0.0F},
            {-half, 0.0F, -half, 0.0F, 0.0F}
        };
    }
}
