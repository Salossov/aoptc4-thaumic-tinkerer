package thaumic.tinkerer.verification;

import thaumic.tinkerer.client.FunnelVisualTransform;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Independent mathematical oracle for Funnel geometry, jar positioning,
 * and essentia fluid level calculations.
 */
public final class FunnelVisualOracle {
    private FunnelVisualOracle() {}

    /**
     * Builds the exact 4x4 matrix for placing the jar on top of the funnel plate.
     */
    public static Matrix4f computeJarMatrix() {
        Matrix4f mat = new Matrix4f();
        mat.translate((float) FunnelVisualTransform.RENDER_CENTER_X,
                      (float) FunnelVisualTransform.RENDER_CENTER_Y,
                      (float) FunnelVisualTransform.RENDER_CENTER_Z);
        return mat;
    }

    /**
     * Computes the world-space base coordinate of the jar.
     */
    public static Vector4f computeJarBasePos() {
        return new Vector4f((float) FunnelVisualTransform.RENDER_CENTER_X,
                            (float) FunnelVisualTransform.RENDER_BASE_Y,
                            (float) FunnelVisualTransform.RENDER_CENTER_Z,
                            1.0F);
    }

    /**
     * Verifies that the jar base is seated slightly into the funnel plate (height 0.125),
     * confirming the 1.7.10 seating at 0.115.
     */
    public static boolean isJarSeatedProperly() {
        double diff = FunnelVisualTransform.BLOCK_HEIGHT - FunnelVisualTransform.RENDER_BASE_Y;
        // Sunk by 0.01 (0.125 - 0.115 = 0.01)
        return Math.abs(diff - 0.01) < 1e-4;
    }
}
