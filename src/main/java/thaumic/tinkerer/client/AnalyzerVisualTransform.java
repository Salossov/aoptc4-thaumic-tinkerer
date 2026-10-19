package thaumic.tinkerer.client;

import java.text.DecimalFormat;

/**
 * Geometric, layout, and rendering constants for the Aspect Analyzer screen,
 * mathematically extracted from 1.7.10 GuiAspectAnalyzer and UtilsFX.drawTag.
 */
public final class AnalyzerVisualTransform {
    private static final DecimalFormat AMOUNTS = new DecimalFormat("#######.##");
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    public static final String TEXTURE_PATH = "textures/gui/aspect_analyzer.png";

    public static final int SLOT_X = 20;
    public static final int SLOT_Y = 30;

    public static final int ASPECT_START_X = 20;
    public static final int ASPECT_Y = 58;
    public static final int ASPECT_STEP_X = 18;
    public static final int ASPECT_SIZE = 16;
    public static final int MAX_ROW_ASPECTS = 8;

    public static final float TEXT_SCALE = 0.5f;
    public static final int FONT_LINE_HEIGHT = 9;

    public static final int[][] OUTLINE_SHIFTS = new int[][]{
        {-1, 0},
        {1, 0},
        {0, -1},
        {0, 1}
    };

    public static final int OUTLINE_COLOR = 0xFF000000;
    public static final int TEXT_COLOR = 0xFFFFFFFF;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = 142;

    private AnalyzerVisualTransform() {}

    /** Computes the GUI-relative X coordinate of the aspect icon at the given index. */
    public static int tagX(int index) {
        return ASPECT_START_X + index * ASPECT_STEP_X;
    }

    /** Computes the GUI-relative Y coordinate of the aspect icons. */
    public static int tagY() {
        return ASPECT_Y;
    }

    /**
     * Checks if the mouse coordinates (mx, my) are strictly within the aspect tag bounds,
     * matching 1.7.10 `mx > x && mx < x + 16 && my > y && my < y + 16`.
     */
    public static boolean isHovered(int tagX, int tagY, int mouseX, int mouseY) {
        return mouseX > tagX && mouseX < tagX + ASPECT_SIZE && mouseY > tagY && mouseY < tagY + ASPECT_SIZE;
    }

    /**
     * Computes the scaled text X coordinate for the amount label at scale 0.5f:
     * `32 - stringWidth + tagX * 2`.
     */
    public static int textX(int tagX, int stringWidth) {
        return 32 - stringWidth + tagX * 2;
    }

    /**
     * Computes the scaled text Y coordinate for the amount label at scale 0.5f:
     * `32 - lineHeight + tagY * 2`.
     */
    public static int textY(int tagY, int lineHeight) {
        return 32 - lineHeight + tagY * 2;
    }

    /** UtilsFX.drawTag receives a float, including the original large-int rounding. */
    public static String amountText(int amount) {
        return AMOUNTS.format((float) amount);
    }
}
