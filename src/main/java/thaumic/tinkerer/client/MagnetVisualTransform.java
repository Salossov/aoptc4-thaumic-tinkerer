package thaumic.tinkerer.client;

import net.minecraft.resources.ResourceLocation;
import thaumic.tinkerer.ThaumicTinkerer;

/**
 * Exact source-extracted transformations, coordinates, and visual layout constants
 * from Thaumic Tinkerer 2.5-1.7.10-164 ModelMagnet, RenderTileMagnet, RenderMagnet,
 * GuiMobMagnet, GuiButtonMM, and GuiButtonMMRadio.
 */
public final class MagnetVisualTransform {
    private MagnetVisualTransform() {}

    // ---------------- Model Dimensions & Geometry (ModelMagnet) ----------------
    public static final int MODEL_TEXTURE_WIDTH = 64;
    public static final int MODEL_TEXTURE_HEIGHT = 64;

    // Panel: addBox(0, 0, 0, 14, 2, 14), setRotationPoint(-7, 22, -7), texOffs(0, 0)
    public static final float PANEL_ORIGIN_X = -7.0F;
    public static final float PANEL_ORIGIN_Y = 22.0F;
    public static final float PANEL_ORIGIN_Z = -7.0F;
    public static final float PANEL_SIZE_X = 14.0F;
    public static final float PANEL_SIZE_Y = 2.0F;
    public static final float PANEL_SIZE_Z = 14.0F;
    public static final int PANEL_TEX_U = 0;
    public static final int PANEL_TEX_V = 0;

    // Magnet core: addBox(0, 0, 0, 4, 13, 4), setRotationPoint(-2, 9, -2), texOffs(0, 16)
    public static final float CORE_ORIGIN_X = -2.0F;
    public static final float CORE_ORIGIN_Y = 9.0F;
    public static final float CORE_ORIGIN_Z = -2.0F;
    public static final float CORE_SIZE_X = 4.0F;
    public static final float CORE_SIZE_Y = 13.0F;
    public static final float CORE_SIZE_Z = 4.0F;
    public static final int CORE_TEX_U = 0;
    public static final int CORE_TEX_V = 16;

    // Box1: addBox(0, -2, 0, 6, 14, 0), setRotationPoint(3, 10, -3), rot(0, -1.570796F, 0), texOffs(28, 19)
    public static final float BOX1_ORIGIN_X = 3.0F;
    public static final float BOX1_ORIGIN_Y = 10.0F;
    public static final float BOX1_ORIGIN_Z = -3.0F;
    public static final float BOX1_OFFSET_Y = -2.0F;
    public static final float BOX1_SIZE_X = 6.0F;
    public static final float BOX1_SIZE_Y = 14.0F;
    public static final float BOX1_SIZE_Z = 0.0F;
    public static final float BOX1_ROT_Y = -1.570796F;
    public static final int BOX1_TEX_U = 28;
    public static final int BOX1_TEX_V = 19;

    // Box2: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(-3, 8, 3), rot(0, 1.570796F, 0), texOffs(28, 33)
    public static final float BOX2_ORIGIN_X = -3.0F;
    public static final float BOX2_ORIGIN_Y = 8.0F;
    public static final float BOX2_ORIGIN_Z = 3.0F;
    public static final float BOX2_SIZE_X = 6.0F;
    public static final float BOX2_SIZE_Y = 14.0F;
    public static final float BOX2_SIZE_Z = 0.0F;
    public static final float BOX2_ROT_Y = 1.570796F;
    public static final int BOX2_TEX_U = 28;
    public static final int BOX2_TEX_V = 33;

    // Box3: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(3, 8, 3), rot(0, 3.141593F, 0), texOffs(40, 19)
    public static final float BOX3_ORIGIN_X = 3.0F;
    public static final float BOX3_ORIGIN_Y = 8.0F;
    public static final float BOX3_ORIGIN_Z = 3.0F;
    public static final float BOX3_SIZE_X = 6.0F;
    public static final float BOX3_SIZE_Y = 14.0F;
    public static final float BOX3_SIZE_Z = 0.0F;
    public static final float BOX3_ROT_Y = 3.141593F;
    public static final int BOX3_TEX_U = 40;
    public static final int BOX3_TEX_V = 19;

    // Box4: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(-3, 8, -3), rot(0, 0, 0), texOffs(40, 33)
    public static final float BOX4_ORIGIN_X = -3.0F;
    public static final float BOX4_ORIGIN_Y = 8.0F;
    public static final float BOX4_ORIGIN_Z = -3.0F;
    public static final float BOX4_SIZE_X = 6.0F;
    public static final float BOX4_SIZE_Y = 14.0F;
    public static final float BOX4_SIZE_Z = 0.0F;
    public static final int BOX4_TEX_U = 40;
    public static final int BOX4_TEX_V = 33;

    // Box5: addBox(0, 0, 0, 6, 0, 6), setRotationPoint(-3, 8, -3), rot(0, 0, 0), texOffs(28, 49)
    public static final float BOX5_ORIGIN_X = -3.0F;
    public static final float BOX5_ORIGIN_Y = 8.0F;
    public static final float BOX5_ORIGIN_Z = -3.0F;
    public static final float BOX5_SIZE_X = 6.0F;
    public static final float BOX5_SIZE_Y = 0.0F;
    public static final float BOX5_SIZE_Z = 6.0F;
    public static final int BOX5_TEX_U = 28;
    public static final int BOX5_TEX_V = 49;

    // ---------------- World Block & Item Transformations ----------------
    // 1.7.10 RenderTileMagnet: GL11.glTranslatef(0.5F, 1.5F, 0.5F); GL11.glScalef(1.0F, -1.0F, -1.0F);
    public static final double TILE_TRANSLATE_X = 0.5;
    public static final double TILE_TRANSLATE_Y = 1.5;
    public static final double TILE_TRANSLATE_Z = 0.5;
    public static final float TILE_SCALE_X = 1.0F;
    public static final float TILE_SCALE_Y = -1.0F;
    public static final float TILE_SCALE_Z = -1.0F;

    // Ring Base: GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F); GL11.glTranslatef(0.0F, 0.0F, -0.6F);
    public static final float RING_ROT_X = 90.0F;
    public static final double RING_BASE_TRANS_Z = -0.6;
    public static final int RING_COUNT = 3;
    public static final int FACES_PER_RING = 2;

    /**
     * 1.7.10 RenderTileMagnet ring scaling formula:
     * redstone / 15.0F * 0.7F + (redstone == 0 ? 0.0F : 0.4F)
     */
    public static float ringScale(int redstone) {
        if (redstone <= 0) return 0.0F;
        return (redstone / 15.0F) * 0.7F + 0.4F;
    }

    /**
     * 1.7.10 RenderTileMagnet ring oscillation formula along Z:
     * -(Math.cos((float)timeMs / 500.0F) + 1.0) * 0.09 - 0.1
     */
    public static double ringZStep(long timeMs) {
        return -(Math.cos((float)timeMs / 500.0F) + 1.0) * 0.09 - 0.1;
    }

    public static float ringRed(boolean pulling) { return pulling ? 0.0F : 1.0F; }
    public static float ringBlue(boolean pulling) { return pulling ? 1.0F : 0.0F; }
    public static float ringGreen() { return 0.0F; }
    public static float ringAlpha() { return 0.9F; }
    public static int ringLight() { return 225; }

    public static ResourceLocation modelTexture(boolean mobs, boolean pulling) {
        return ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/model/" + (mobs ? "mob_magnet_" : "magnet_") + (pulling ? "s" : "n") + ".png");
    }

    public static final ResourceLocation RING_SPRITE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "item/magnet_ring");

    // ---------------- GUI Dimensions & Layout (GuiMobMagnet, GuiButtonMM) ----------------
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;
    public static final int GUI_TEX_WIDTH = 256;
    public static final int GUI_TEX_HEIGHT = 256;
    public static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/gui/mob_magnet.png");

    public static final int RADIO_ADULT_ID = 0;
    public static final int RADIO_ADULT_X = 100;
    public static final int RADIO_ADULT_Y = 28;

    public static final int RADIO_CHILD_ID = 1;
    public static final int RADIO_CHILD_X = 100;
    public static final int RADIO_CHILD_Y = 48;

    public static final int RADIO_WIDTH = 13;
    public static final int RADIO_HEIGHT = 13;

    public static final int RADIO_TEX_U = 176;
    public static final int RADIO_TEX_V_UNSELECTED = 0;
    public static final int RADIO_TEX_V_SELECTED = 13;

    public static final int LABEL_COLOR = 10066329; // 0x999999
    public static final int LABEL_FILTER_Y = 16;
    public static final int LABEL_ADULT_X = 120;
    public static final int LABEL_ADULT_Y = 30;
    public static final int LABEL_CHILD_X = 120;
    public static final int LABEL_CHILD_Y = 50;

    public static final int SLOT_FILTER_INDEX = 0;
    public static final int SLOT_FILTER_X = 53;
    public static final int SLOT_FILTER_Y = 37;

    public static int filterLabelX(int imageWidth, int textWidth) {
        return imageWidth / 2 - textWidth / 2 - 26;
    }
}
