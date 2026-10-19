package thaumic.tinkerer.verification;

import thaumic.tinkerer.client.MagnetVisualTransform;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Independent mathematical oracle for Magnet geometry, animation transforms,
 * UV coordinates, matrix bounds, and GUI layout.
 */
public final class MagnetVisualOracle {
    private MagnetVisualOracle() {}

    /**
     * Computes the cumulative ring scale for face index [0..5] (2 faces per each of 3 rings)
     * as executed by the cumulative glScalef transforms.
     */
    public static float cumulativeScale(int redstone, int ringIndex, int faceIndex) {
        float baseScale = MagnetVisualTransform.ringScale(redstone);
        if (baseScale == 0.0F) return 0.0F;
        int step = ringIndex * MagnetVisualTransform.FACES_PER_RING + faceIndex + 1;
        float result = 1.0F;
        for (int i = 0; i < step; i++) {
            result *= baseScale;
        }
        return result;
    }

    /**
     * Builds the exact 4x4 transform matrix for a given ring face at timeMs and redstone signal.
     */
    public static Matrix4f computeRingFaceMatrix(int redstone, int ringIndex, int faceIndex, long timeMs) {
        Matrix4f mat = new Matrix4f();
        // Model base: translate(0.5, 1.5, 0.5), scale(1, -1, -1)
        mat.translate((float)MagnetVisualTransform.TILE_TRANSLATE_X, (float)MagnetVisualTransform.TILE_TRANSLATE_Y, (float)MagnetVisualTransform.TILE_TRANSLATE_Z);
        mat.scale(MagnetVisualTransform.TILE_SCALE_X, MagnetVisualTransform.TILE_SCALE_Y, MagnetVisualTransform.TILE_SCALE_Z);

        if (redstone <= 0) {
            return mat; // Zero signal early return
        }

        // Ring base: rotX(90), translate(0, 0, -0.6)
        mat.rotate((float)Math.toRadians(MagnetVisualTransform.RING_ROT_X), 1.0F, 0.0F, 0.0F);
        mat.translate(0.0F, 0.0F, (float)MagnetVisualTransform.RING_BASE_TRANS_Z);

        float baseScale = MagnetVisualTransform.ringScale(redstone);
        double stepZ = MagnetVisualTransform.ringZStep(timeMs);

        for (int r = 0; r <= ringIndex; r++) {
            int maxF = (r == ringIndex) ? faceIndex : (MagnetVisualTransform.FACES_PER_RING - 1);
            for (int f = 0; f <= maxF; f++) {
                mat.scale(1.0F, -1.0F, 1.0F);
                mat.scale(baseScale, baseScale, baseScale);
            }
            if (r < ringIndex) {
                mat.translate(0.0F, 0.0F, (float)stepZ);
            }
        }
        return mat;
    }

    /**
     * Transforms standard unit centered quad corner (-0.5, 0.5, 0.0) by the matrix.
     */
    public static Vector4f transformQuadCorner(Matrix4f matrix, float x, float y, float z) {
        return new Vector4f(x, y, z, 1.0F).mul(matrix);
    }

}
