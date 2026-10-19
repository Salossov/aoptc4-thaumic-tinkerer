package thaumic.tinkerer.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Exact source-extracted transformations, coordinates, and visual constants
 * from Thaumic Tinkerer 2.5-1.7.10-164 ModelRepairer, RenderTileRepairer, and RenderRepairer.
 */
public final class RepairerVisualTransform {
    private RepairerVisualTransform() {}

    // Model texture specification
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 64;
    public static final ResourceLocation MODEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/model/repairer.png");
    public static final ResourceLocation REPAIR_OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/repair.png");
    public static final ResourceLocation REPAIR_OFF_OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/misc/repair_off.png");

    // Model Base: addBox(0, 0, 0, 16, 1, 16), pivot(-8, 23, -8), uv(0, 0)
    public static final float BASE_ORIGIN_X = -8.0F;
    public static final float BASE_ORIGIN_Y = 23.0F;
    public static final float BASE_ORIGIN_Z = -8.0F;
    public static final float BASE_SIZE_X = 16.0F;
    public static final float BASE_SIZE_Y = 1.0F;
    public static final float BASE_SIZE_Z = 16.0F;
    public static final int BASE_TEX_U = 0;
    public static final int BASE_TEX_V = 0;

    // Supports 1..4: size(2, 14, 2), uv(0, 17)
    public static final float SUPPORT_SIZE_X = 2.0F;
    public static final float SUPPORT_SIZE_Y = 14.0F;
    public static final float SUPPORT_SIZE_Z = 2.0F;
    public static final int SUPPORT_TEX_U = 0;
    public static final int SUPPORT_TEX_V = 17;
    public static final float[][] SUPPORT_PIVOTS = {
        {-8.0F, 9.0F, 6.0F},   // Support1
        {6.0F, 9.0F, 6.0F},    // Support2
        {-8.0F, 9.0F, -8.0F},  // Support3
        {6.0F, 9.0F, -8.0F}    // Support4
    };

    // Top parts 1..4
    public static final int TOP12_TEX_U = 11;
    public static final int TOP12_TEX_V = 19;
    public static final float TOP12_SIZE_X = 16.0F;
    public static final float TOP12_SIZE_Y = 1.0F;
    public static final float TOP12_SIZE_Z = 2.0F;

    public static final int TOP34_TEX_U = 11;
    public static final int TOP34_TEX_V = 23;
    public static final float TOP34_SIZE_X = 2.0F;
    public static final float TOP34_SIZE_Y = 1.0F;
    public static final float TOP34_SIZE_Z = 12.0F;

    // Tube opening parts 1..4 on back wall (Z = 7)
    public static final int OPENING_H_TEX_U = 48;
    public static final int OPENING_H_TEX_V = 30;
    public static final float OPENING_H_SIZE_X = 5.0F;
    public static final float OPENING_H_SIZE_Y = 1.0F;
    public static final float OPENING_H_SIZE_Z = 1.0F;

    public static final int OPENING_V_TEX_U = 48;
    public static final int OPENING_V_TEX_V = 24;
    public static final float OPENING_V_SIZE_X = 1.0F;
    public static final float OPENING_V_SIZE_Y = 3.0F;
    public static final float OPENING_V_SIZE_Z = 1.0F;

    // Glass parts 1..5
    // Glass1: Top plate (12, 0, 12), pivot(-6, 8.5, -6), uv(-11, 37)
    public static final float GLASS1_ORIGIN_X = -6.0F;
    public static final float GLASS1_ORIGIN_Y = 8.5F;
    public static final float GLASS1_ORIGIN_Z = -6.0F;
    public static final float GLASS1_SIZE_X = 12.0F;
    public static final float GLASS1_SIZE_Y = 0.0F;
    public static final float GLASS1_SIZE_Z = 12.0F;

    // Glass4: Front pane (12, 14, 0), pivot(-6, 9, -7.5), uv(40, 34)
    public static final float GLASS4_ORIGIN_X = -6.0F;
    public static final float GLASS4_ORIGIN_Y = 9.0F;
    public static final float GLASS4_ORIGIN_Z = -7.5F;
    public static final float GLASS4_SIZE_X = 12.0F;
    public static final float GLASS4_SIZE_Y = 14.0F;
    public static final float GLASS4_SIZE_Z = 0.0F;

    // Glass5: Back pane with tube hole (12, 14, 0), pivot(6, 9, 7.5), rotY(pi), uv(33, 50)
    public static final float GLASS5_ORIGIN_X = 6.0F;
    public static final float GLASS5_ORIGIN_Y = 9.0F;
    public static final float GLASS5_ORIGIN_Z = 7.5F;
    public static final float GLASS5_SIZE_X = 12.0F;
    public static final float GLASS5_SIZE_Y = 14.0F;
    public static final float GLASS5_SIZE_Z = 0.0F;

    // BlockEntity Transforms
    public static final double TILE_TRANSLATE_X = 0.0;
    public static final double TILE_TRANSLATE_Y = 2.0;
    public static final double TILE_TRANSLATE_Z = 1.0;
    public static final float TILE_SCALE_X = 1.0F;
    public static final float TILE_SCALE_Y = -1.0F;
    public static final float TILE_SCALE_Z = -1.0F;
    public static final double TILE_CENTER_X = 0.5;
    public static final double TILE_CENTER_Y = 0.5;
    public static final double TILE_CENTER_Z = 0.5;

    // Item animation
    public static final float ITEM_SCALE = 0.5F;
    public static final float ITEM_ROTATION_RATE = 0.75F;
    public static final float ITEM_BOB_DIVISOR = 10.0F;
    public static final float ITEM_BOB_AMPLITUDE = 0.1F;

    // Overlay (rotating repair seal)
    public static final double OVERLAY_SIZE = 1.25;
    public static final float OVERLAY_HALF_SIZE = 0.625F;
    public static final double OVERLAY_TRANSLATE_Y = -0.525;
    public static final float OVERLAY_WORLD_Y = 0.975F;
    public static final float OVERLAY_ROTATION_RATE = 0.75F;

    /** Returns rotation angle in degrees for the given Direction metadata */
    public static float getFacingRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 90.0F;
        };
    }

    /** Computes item bobbing offset along Y */
    public static float itemBob(int ticks) {
        return (float) (Math.sin(ticks / ITEM_BOB_DIVISOR) * ITEM_BOB_AMPLITUDE);
    }

    /** Cast the entire original translation after the source float division. */
    public static float itemTranslationY(int ticks) {
        return (float) (-2.5 + Math.sin(ticks / ITEM_BOB_DIVISOR) * ITEM_BOB_AMPLITUDE);
    }

    /** Computes rotation angle in degrees at the given tick */
    public static float rotationDegrees(int ticks) {
        return (ticks * ITEM_ROTATION_RATE) % 360.0F;
    }
}
