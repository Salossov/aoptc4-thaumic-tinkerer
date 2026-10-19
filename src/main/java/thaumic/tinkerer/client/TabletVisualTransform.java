package thaumic.tinkerer.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Exact source-extracted transformations, coordinates, and visual layout constants
 * from Thaumic Tinkerer 2.5-1.7.10-164 RenderTileAnimationTablet, GuiAnimationTablet,
 * GuiButtonAT, and GuiButtonATRadio.
 */
public final class TabletVisualTransform {
    private TabletVisualTransform() {}

    // Overlays
    public static final ResourceLocation OVERLAY_CENTER = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/at_center.png");
    public static final ResourceLocation OVERLAY_LEFT = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/at_left.png");
    public static final ResourceLocation OVERLAY_RIGHT = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/at_right.png");
    public static final ResourceLocation OVERLAY_FACING_INDENT = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/at_facing_indent.png");

    public static final double OVERLAY_CENTER_SIZE = 0.65;
    public static final float OVERLAY_CENTER_HEIGHT = 0.13F;
    public static final int OVERLAY_CENTER_ROT_MOD = -1; // Counter-clockwise

    public static final double OVERLAY_MODE_SIZE = 1.0;
    public static final float OVERLAY_LEFT_HEIGHT = 0.13F;
    public static final float OVERLAY_RIGHT_HEIGHT = 0.131F; // 0.131 to prevent z-fighting
    public static final int OVERLAY_MODE_ROT_MOD = 1; // Clockwise

    public static final double OVERLAY_INDENT_SIZE = 0.5;
    public static final float OVERLAY_INDENT_HEIGHT = 0.13F;
    public static final float OVERLAY_INDENT_ANGLE_OFFSET = 90.0F;

    // Translations array for meta 2..5 (indices 0..3: North, South, West, East)
    public static final float[][] TRANSLATIONS = {
        {0.0F, 0.0F, -1.0F},   // NORTH (meta 2) -> rotation 270
        {-1.0F, 0.0F, 0.0F},   // SOUTH (meta 3) -> rotation 90
        {0.0F, 0.0F, 0.0F},    // WEST (meta 4) -> rotation 0
        {-1.0F, 0.0F, -1.0F}   // EAST (meta 5) -> rotation 180
    };

    public static final float[] ROTATIONS = {270.0F, 90.0F, 0.0F, 180.0F};

    // Item animation transforms
    public static final double ITEM_BASE_TRANS_X = 0.1;
    public static final double ITEM_BASE_TRANS_Y = 0.2;
    public static final double ITEM_BASE_TRANS_Z = 0.5;
    public static final double ITEM_HOVER_PERIOD_MS = 600.0;
    public static final double ITEM_HOVER_AMPLITUDE = 1.0 / 18.0;

    public static final float ITEM_SCALE = 0.8F;
    public static final float SWING_TRANS_X_FACTOR = -1.0F / 250.0F;
    public static final float SWING_TRANS_Y_FACTOR = 1.0F / 1000.0F;

    public static final float WOBBLE_AXIS_X = 1.0F;
    public static final float WOBBLE_AXIS_Y = 0.0F;
    public static final float WOBBLE_AXIS_Z = 1.0F;
    public static final float WOBBLE_PERIOD_MS = 400.0F;
    public static final float WOBBLE_AMPLITUDE_DEG = 5.0F;

    public static final float ITEM_FINAL_TRANS_X = 0.5F;
    public static final float ITEM_FINAL_TRANS_Y = 0.55F;
    public static final float ITEM_FINAL_TRANS_Z = 0.0F;
    public static final float ITEM_SCALE_BLOCK = 2.5F;
    public static final float ITEM_SCALE_GENERIC = 1.5F;

    // GUI constants (GuiAnimationTablet)
    public static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/gui/animation_tablet.png");
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;

    // Button coordinates relative to (leftPos, topPos)
    public static final int BTN_REDSTONE_REL_X = 81; // 176/2 - 7
    public static final int BTN_REDSTONE_REL_Y = 60;
    public static final int BTN_LEFT_CLICK_REL_X = 52;
    public static final int BTN_LEFT_CLICK_REL_Y = 15;
    public static final int BTN_RIGHT_CLICK_REL_X = 111;
    public static final int BTN_RIGHT_CLICK_REL_Y = 15;
    public static final int BTN_SIZE = 13;

    // Button texture atlas coordinates
    public static final int BTN_TEX_U = 176;
    public static final int BTN_TEX_V_OFF = 0;
    public static final int BTN_TEX_V_ON = 13;

    // Label text coordinates
    public static final int LABEL_COLOR = 0x999999; // 10066329
    public static final int LABEL_LEFT_ANCHOR_X = 48; // drawn at (48 - fontWidth, 18)
    public static final int LABEL_LEFT_Y = 18;
    public static final int LABEL_RIGHT_X = 128;
    public static final int LABEL_RIGHT_Y = 18;
    public static final int LABEL_REDSTONE_Y = 50;

    /** Computes item hover oscillation at timeMs */
    public static double itemHoverY(long timeMs) {
        return Math.cos(timeMs / ITEM_HOVER_PERIOD_MS) / 18.0;
    }

    /** Computes item wobble angle in degrees at timeMs */
    public static float itemWobbleAngle(long timeMs) {
        return (float) Math.cos((float) timeMs / WOBBLE_PERIOD_MS) * WOBBLE_AMPLITUDE_DEG;
    }
}
