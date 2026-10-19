package thaumic.tinkerer.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Visual oracle and mathematical constants for Warp Gate client visuals
 * matching Thaumic Tinkerer 2.5-1.7.10-164:
 * - ModelSpinningCubes orbital math, time dilation, and rotation axes
 * - GuiWarpGate dimensions, coordinates, and inverted locked checkmark
 * - GuiWarpGateDestinations background hue, line alpha pulse, 2px line clipping, and pearl hover
 * - Player sneak detection AABB
 */
public final class WarpGateVisualHelper {

    // --- Spinning Cubes (ModelSpinningCubes) ---
    public static final int CUBES_COUNT = 12;
    public static final int REPEAT_COUNT = 5; // 5 down to 0 = 6 passes
    public static final float CUBE_SCALE = 0.0625F; // 1 / 16 block (1x1x1 pixel cube)
    public static final float OFFSET_PER_CUBE = 360.0F / CUBES_COUNT; // 30 degrees

    public static final float BASE_TRANSLATE_X = 0.5F;
    public static final float BASE_TRANSLATE_Y = 2.5F;
    public static final float BASE_TRANSLATE_Z = 0.5F;

    public static final float ROTATION_AXIS_X = (float) (1.0 / Math.sqrt(2.0));
    public static final float ROTATION_AXIS_Y = 0.0F;
    public static final float ROTATION_AXIS_Z = (float) (1.0 / Math.sqrt(2.0));
    public static final float ROTATION_ANGLE_DEG = 180.0F;

    public static final float RING_OFFSET_X = -0.025F;
    public static final float RING_OFFSET_Y = 0.85F;
    public static final float RING_OFFSET_Z = -0.025F;

    public static double ticksForRepeat(double baseTicks, int repeat, int origRepeat) {
        return baseTicks - 0.75 * (origRepeat - repeat);
    }

    public static float alphaForRepeat(int repeat, int origRepeat) {
        return (repeat < origRepeat) ? 0.2F : 1.0F;
    }

    public static int degForCube(double ticks, int cubeIndex, int totalCubes) {
        float offset = (360.0F / totalCubes) * cubeIndex;
        return (int) ((ticks / 0.25) % 360.0 + offset);
    }

    public static float radiusX(double ticks) {
        return (float) (0.7F + 0.1F * Math.sin(ticks / 6.0));
    }

    public static float radiusZ(double ticks) {
        return (float) (0.7F + 0.1F * Math.cos(ticks / 6.0));
    }

    public static float cubeX(float radiusX, float deg) {
        float rad = deg * (float) Math.PI / 180.0F;
        return (float) (radiusX * Math.cos(rad));
    }

    public static float cubeZ(float radiusZ, float deg) {
        float rad = deg * (float) Math.PI / 180.0F;
        return (float) (radiusZ * Math.sin(rad));
    }

    public static float cubeY(double ticks, int cubeIndex) {
        return (float) Math.cos((ticks + 50.0 * cubeIndex) / 5.0) / 10.0F;
    }

    public static Vector3f cubeRotAxis(double ticks) {
        float xRotate = (float) Math.sin(ticks * 0.25) / 2.0F;
        float yRotate = (float) Math.max(0.6F, Math.sin(ticks * 0.1F) / 2.0 + 0.5);
        float zRotate = (float) Math.cos(ticks * 0.25) / 2.0F;
        return new Vector3f(xRotate, yRotate, zRotate);
    }

    // --- GUI Inventory (GuiWarpGate) ---
    public static final int GUI_IMAGE_WIDTH = 176;
    public static final int GUI_IMAGE_HEIGHT = 166;
    public static final int BUTTON_X_OFFSET = 5;
    public static final int BUTTON_Y_OFFSET = 5;
    public static final int BUTTON_SIZE = 13;
    public static final int LABEL_X_OFFSET = 20;
    public static final int LABEL_Y_OFFSET = 7;
    public static final int LABEL_COLOR = 10066329; // 0x999999

    // --- GUI Destinations (GuiWarpGateDestinations) ---
    public static final float PEARL_RENDER_SCALE = 0.5F;
    public static final int HOVER_RADIUS = 4; // 8x8 box centered at (xp, yp)
    public static final float LINE_WIDTH = 2.0F;

    /** glLineWidth(2) measures framebuffer pixels, independently of GUI scale. */
    public static float lineHalfWidth(double guiScale) {
        return (float) (LINE_WIDTH / (2.0 * guiScale));
    }

    public static float backgroundHue(int ticks) {
        return (float) (Math.sin(ticks / 150.0) + 0.5);
    }

    public static int backgroundRgb(int ticks) {
        return java.awt.Color.HSBtoRGB(backgroundHue(ticks), 0.5F, 0.4F);
    }

    public static float lineAlpha(int ticks) {
        return (float) ((Math.sin(ticks / 10.0) + 1.0) / 4.0 + 0.25);
    }

    public static int lineColor(int ticks) {
        int alphaInt = Math.max(0, Math.min(255, (int) (lineAlpha(ticks) * 255.0F)));
        return (alphaInt << 24) | 0xFFFFFF;
    }

    public static boolean isPearlHovered(int mouseX, int mouseY, int pearlScreenX, int pearlScreenY) {
        return mouseX >= pearlScreenX - HOVER_RADIUS && mouseX <= pearlScreenX + HOVER_RADIUS
                && mouseY >= pearlScreenY - HOVER_RADIUS && mouseY < pearlScreenY + HOVER_RADIUS;
    }

    // --- Sneak Trigger (TileWarpGate) ---
    public static AABB getSneakTriggerAABB(BlockPos gatePos) {
        return new AABB(
                gatePos.getX(), gatePos.getY() + 1.0, gatePos.getZ(),
                gatePos.getX() + 1.0, gatePos.getY() + 1.5, gatePos.getZ() + 1.0
        );
    }

    // --- Cohen-Sutherland 2D Line Clipping for Safe Large-Coordinate Rendering ---
    private static final int INSIDE = 0; // 0000
    private static final int LEFT = 1;   // 0001
    private static final int RIGHT = 2;  // 0010
    private static final int BOTTOM = 4; // 0100
    private static final int TOP = 8;    // 1000

    private static int computeOutCode(double x, double y, double xmin, double ymin, double xmax, double ymax) {
        int code = INSIDE;
        if (x < xmin) code |= LEFT;
        else if (x > xmax) code |= RIGHT;
        if (y < ymin) code |= BOTTOM;
        else if (y > ymax) code |= TOP;
        return code;
    }

    public record ClippedLine(double x0, double y0, double x1, double y1, boolean visible) {}

    public static ClippedLine clipLine(double x0, double y0, double x1, double y1,
                                       double xmin, double ymin, double xmax, double ymax) {
        int code0 = computeOutCode(x0, y0, xmin, ymin, xmax, ymax);
        int code1 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
        boolean accept = false;

        while (true) {
            if ((code0 | code1) == 0) {
                // Both inside
                accept = true;
                break;
            } else if ((code0 & code1) != 0) {
                // Both outside on same side
                break;
            } else {
                double x = 0;
                double y = 0;
                int outCodeOut = (code0 != 0) ? code0 : code1;

                if ((outCodeOut & TOP) != 0) {
                    x = x0 + (x1 - x0) * (ymax - y0) / (y1 - y0);
                    y = ymax;
                } else if ((outCodeOut & BOTTOM) != 0) {
                    x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
                    y = ymin;
                } else if ((outCodeOut & RIGHT) != 0) {
                    y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
                    x = xmax;
                } else if ((outCodeOut & LEFT) != 0) {
                    y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
                    x = xmin;
                }

                if (outCodeOut == code0) {
                    x0 = x;
                    y0 = y;
                    code0 = computeOutCode(x0, y0, xmin, ymin, xmax, ymax);
                } else {
                    x1 = x;
                    y1 = y;
                    code1 = computeOutCode(x1, y1, xmin, ymin, xmax, ymax);
                }
            }
        }
        return new ClippedLine(x0, y0, x1, y1, accept);
    }

    private WarpGateVisualHelper() {}
}
