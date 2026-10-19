package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/**
 * Exact source-extracted transformations, coordinates, and visual layout constants
 * from Thaumic Tinkerer 2.5-1.7.10-164 RenderTileEnchanter, GuiEnchanting, and button classes.
 */
public final class EnchanterVisualTransform {
    // ---------------- Tile Entity World Geometry & Animation ----------------
    // 1.7.10 RenderTileEnchanter: GL11.glTranslated(d0, d1 + 0.75, d2)
    public static final double BASE_HEIGHT = 0.75;

    // Slot 0 (Tool being enchanted):
    // GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
    // float scale = 0.7F; GL11.glScalef(0.7F, 0.7F, 0.7F);
    // GL11.glTranslatef(0.6F, -0.2F, 0.0F);
    // GL11.glRotatef(30.0F, 0.0F, 0.0F, 1.0F);
    public static final float TOOL_ROT_X = 90.0F;
    public static final float TOOL_SCALE = 0.7F;
    public static final float TOOL_TRANS_X = 0.6F;
    public static final float TOOL_TRANS_Y = -0.2F;
    public static final float TOOL_TRANS_Z = 0.0F;
    public static final float TOOL_ROT_Z = 30.0F;

    // Slot 1 (Hovering casting wand):
    // GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
    // float scale = 0.5F; GL11.glScalef(0.5F, 0.5F, 0.5F);
    // GL11.glTranslatef(0.6F, 1.5F, -0.1F);
    // GL11.glRotatef(-70.0F, 0.0F, 0.0F, 1.0F);
    // long millis = System.currentTimeMillis();
    // GL11.glTranslatef(0.0F, 0.0F, (float)(Math.cos(millis / 1000.0) - 1.2F) / 10.0F);
    // GL11.glTranslatef(0.0F, 0.325F, 0.0F);
    // GL11.glRotatef((float)Math.cos(millis / 500.0) * 5.0F, 1.0F, 0.0F, 0.0F);
    // GL11.glTranslatef(0.0F, -0.325F, 0.0F);
    public static final float WAND_ROT_X = 90.0F;
    public static final float WAND_SCALE = 0.5F;
    public static final float WAND_TRANS_X = 0.6F;
    public static final float WAND_TRANS_Y = 1.5F;
    public static final float WAND_TRANS_Z = -0.1F;
    public static final float WAND_ROT_Z = -70.0F;
    public static final float WAND_PIVOT_Y = 0.325F;

    // ---------------- GUI Dimensions & Layout ----------------
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    // Slot 0 (tool): x=6, y=6; Slot 1 (wand): x=6, y=31
    public static final int SLOT_TOOL_X = 6;
    public static final int SLOT_TOOL_Y = 6;
    public static final int SLOT_WAND_X = 6;
    public static final int SLOT_WAND_Y = 31;

    // Candidate buttons (up to 16, 8 columns)
    public static final int CANDIDATE_MAX = 16;
    public static final int CANDIDATE_COLS = 8;
    public static final int CANDIDATE_BUTTON_SIZE = 16;
    public static final int CANDIDATE_BASE_X = 34;
    public static final int CANDIDATE_Y_SINGLE_ROW = 54;
    public static final int CANDIDATE_Y_ROW0_TWO_ROWS = 30; // 54 - 24
    public static final int CANDIDATE_Y_ROW1_TWO_ROWS = 54;
    public static final int CANDIDATE_STEP_X = 16;
    public static final int CANDIDATE_STEP_Y = 24;

    // Background plates for candidates: UV (0, 166), size (147, 24)
    public static final int PLATE_X = 30;
    public static final int PLATE_UPPER_Y = 26;
    public static final int PLATE_LOWER_Y = 50;
    public static final int PLATE_U = 0;
    public static final int PLATE_V = 166;
    public static final int PLATE_WIDTH = 147;
    public static final int PLATE_HEIGHT = 24;

    // Start button: 15x15 at (151, 33)
    public static final int START_X = 151;
    public static final int START_Y = 33;
    public static final int START_SIZE = 15;
    public static final int START_U = 176;
    public static final int START_V_IDLE = 24;
    public static final int START_V_WORKING = 39;

    // Selected enchantment frame: 24x24 at (180 - 4, y - 4), icon at (180, y)
    public static final int FRAMED_BASE_X = 180; // leftPos + 176 + 4
    public static final int FRAMED_ROW_HEIGHT = 26;
    public static final int FRAMED_UV_X = 176;
    public static final int FRAMED_UV_Y = 0;
    public static final int FRAMED_UV_SIZE = 24;
    public static final int FRAMED_TEXT_OFFSET_X = 26;
    public static final int FRAMED_TEXT_OFFSET_Y = 8;

    // Level buttons: 7x7 at (200, y - 4) and (207, y - 4)
    public static final int LEVEL_MINUS_OFFSET_X = 24; // 176 + 24 = 200
    public static final int LEVEL_PLUS_OFFSET_X = 31;  // 176 + 31 = 207
    public static final int LEVEL_BUTTON_OFFSET_Y = -4;
    public static final int LEVEL_BUTTON_SIZE = 7;
    public static final int LEVEL_MINUS_U = 218;
    public static final int LEVEL_PLUS_U = 225;
    public static final int LEVEL_BUTTON_V = 0;

    // Vis indicator bars (6 tubes: Air, Earth, Fire, Water, Order, Entropy)
    public static final int BAR_BASE_X = 55; // 40 + 15
    public static final int BAR_STEP_X = 15;
    public static final int BAR_Y_SINGLE_ROW = 50;
    public static final int BAR_Y_TWO_ROWS = 26;
    public static final int BAR_WIDTH = 10;
    public static final int BAR_EMPTY_SIZE = 11;
    public static final int BAR_ACTIVE_SIZE = 59;
    public static final int BAR_FILL_WIDTH = 8;
    public static final int BAR_FILL_MAX_HEIGHT = 48;
    public static final int BAR_FILL_OFFSET_X = 1;
    public static final int BAR_FILL_U = 210;
    public static final int BAR_FRAME_U = 200;
    public static final int BAR_FRAME_V = 0;
    public static final int BAR_CAP_BOTTOM_V = 52;

    public static float wandBobZ(long millis) {
        return (float) (Math.cos(millis / 1000.0) - 1.2F) / 10.0F;
    }

    public static float wandTiltX(long millis) {
        return (float) Math.cos(millis / 500.0) * 5.0F;
    }

    public static void applyToolTransform(PoseStack pose) {
        pose.mulPose(Axis.XP.rotationDegrees(TOOL_ROT_X));
        pose.scale(TOOL_SCALE, TOOL_SCALE, TOOL_SCALE);
        pose.translate(TOOL_TRANS_X, TOOL_TRANS_Y, TOOL_TRANS_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(TOOL_ROT_Z));
    }

    public static void applyWandTransform(PoseStack pose, long millis) {
        pose.mulPose(Axis.XP.rotationDegrees(WAND_ROT_X));
        pose.scale(WAND_SCALE, WAND_SCALE, WAND_SCALE);
        pose.translate(WAND_TRANS_X, WAND_TRANS_Y, WAND_TRANS_Z);
        pose.mulPose(Axis.ZP.rotationDegrees(WAND_ROT_Z));

        pose.translate(0.0F, 0.0F, wandBobZ(millis));
        pose.translate(0.0F, WAND_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(wandTiltX(millis)));
        pose.translate(0.0F, -WAND_PIVOT_Y, 0.0F);
    }

    public static Matrix4f computeToolMatrix() {
        Matrix4f m = new Matrix4f();
        m.translate(0.0F, (float) BASE_HEIGHT, 0.0F);
        m.rotate((float) Math.toRadians(TOOL_ROT_X), 1.0F, 0.0F, 0.0F);
        m.scale(TOOL_SCALE, TOOL_SCALE, TOOL_SCALE);
        m.translate(TOOL_TRANS_X, TOOL_TRANS_Y, TOOL_TRANS_Z);
        m.rotate((float) Math.toRadians(TOOL_ROT_Z), 0.0F, 0.0F, 1.0F);
        return m;
    }

    public static Matrix4f computeWandMatrix(long millis) {
        Matrix4f m = new Matrix4f();
        m.translate(0.0F, (float) BASE_HEIGHT, 0.0F);
        m.rotate((float) Math.toRadians(WAND_ROT_X), 1.0F, 0.0F, 0.0F);
        m.scale(WAND_SCALE, WAND_SCALE, WAND_SCALE);
        m.translate(WAND_TRANS_X, WAND_TRANS_Y, WAND_TRANS_Z);
        m.rotate((float) Math.toRadians(WAND_ROT_Z), 0.0F, 0.0F, 1.0F);
        m.translate(0.0F, 0.0F, wandBobZ(millis));
        m.translate(0.0F, WAND_PIVOT_Y, 0.0F);
        m.rotate((float) Math.toRadians(wandTiltX(millis)), 1.0F, 0.0F, 0.0F);
        m.translate(0.0F, -WAND_PIVOT_Y, 0.0F);
        return m;
    }

    public static int aspectBarFillPixels(int current, int total) {
        if (total == 0) return 0;
        return (int) (48.0 * ((double) current / total));
    }

    private EnchanterVisualTransform() {}
}
