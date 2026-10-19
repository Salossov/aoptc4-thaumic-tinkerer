package thaumic.tinkerer.verification;

import thaumic.tinkerer.client.TabletVisualTransform;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Independent mathematical oracle for Animation Tablet geometry, animation transforms,
 * swing matrices, overlay positions, and GUI layout bounds.
 */
public final class TabletVisualOracle {
    private TabletVisualOracle() {}

    /**
     * Builds the exact 4x4 matrix for an overlay quad.
     */
    public static Matrix4f computeOverlayMatrix(float height, float rotationDegrees) {
        Matrix4f mat = new Matrix4f();
        mat.translate(0.5F, height, 0.5F);
        mat.rotate((float) Math.toRadians(rotationDegrees), 0.0F, 1.0F, 0.0F);
        return mat;
    }

    /**
     * Computes the corners of an overlay quad of given size.
     */
    public static float[][] computeOverlayCorners(double size) {
        float half = (float) (size / 2.0);
        return new float[][] {
            {-half, half,  0.0F, 1.0F},
            {half,  half,  1.0F, 1.0F},
            {half,  -half, 1.0F, 0.0F},
            {-half, -half, 0.0F, 0.0F}
        };
    }

    /**
     * Builds the complete item transformation matrix for the given parameters.
     */
    public static Matrix4f computeItemMatrix(int rotationIndex, int swingProgress, long timeMs, boolean isBlock) {
        Matrix4f mat = new Matrix4f();
        float rotation = TabletVisualTransform.ROTATIONS[rotationIndex];
        mat.rotate((float) Math.toRadians(rotation), 0.0F, 1.0F, 0.0F);

        double hover = TabletVisualTransform.itemHoverY(timeMs);
        mat.translate((float) TabletVisualTransform.ITEM_BASE_TRANS_X,
                      (float) (TabletVisualTransform.ITEM_BASE_TRANS_Y + hover),
                      (float) TabletVisualTransform.ITEM_BASE_TRANS_Z);

        float[] trans = TabletVisualTransform.TRANSLATIONS[rotationIndex];
        mat.translate(trans[0], trans[1], trans[2]);
        mat.scale(TabletVisualTransform.ITEM_SCALE, TabletVisualTransform.ITEM_SCALE, TabletVisualTransform.ITEM_SCALE);

        // Swing rotation around (0.5, 0.0, 0.5)
        mat.translate(0.5F, 0.0F, 0.5F);
        mat.rotate((float) Math.toRadians(swingProgress), 0.0F, 0.0F, 1.0F);
        mat.translate(-0.5F, 0.0F, -0.5F);

        // Swing translation
        mat.translate(-swingProgress / 250.0F,
                      swingProgress / 1000.0F,
                      0.0F);

        // Wobble around (1, 0, 1) normalized
        Vector3f axis = new Vector3f(TabletVisualTransform.WOBBLE_AXIS_X,
                                     TabletVisualTransform.WOBBLE_AXIS_Y,
                                     TabletVisualTransform.WOBBLE_AXIS_Z).normalize();
        float wobble = TabletVisualTransform.itemWobbleAngle(timeMs);
        mat.rotate((float) Math.toRadians(wobble), axis.x, axis.y, axis.z);

        mat.translate(TabletVisualTransform.ITEM_FINAL_TRANS_X,
                      TabletVisualTransform.ITEM_FINAL_TRANS_Y,
                      TabletVisualTransform.ITEM_FINAL_TRANS_Z);

        float scale = isBlock ? TabletVisualTransform.ITEM_SCALE_BLOCK : TabletVisualTransform.ITEM_SCALE_GENERIC;
        mat.scale(scale, scale, scale);
        return mat;
    }

    /**
     * Checks if a point (mouseX, mouseY) falls within button (relX, relY, 13, 13).
     */
    public static boolean isMouseOverButton(int mouseX, int mouseY, int leftPos, int topPos, int relX, int relY) {
        int x = leftPos + relX;
        int y = topPos + relY;
        return mouseX >= x && mouseX < x + TabletVisualTransform.BTN_SIZE &&
               mouseY >= y && mouseY < y + TabletVisualTransform.BTN_SIZE;
    }
}
