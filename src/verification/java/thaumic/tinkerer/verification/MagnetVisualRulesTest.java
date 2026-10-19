package thaumic.tinkerer.verification;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import thaumic.tinkerer.client.MagnetVisualTransform;

/**
 * Independent verification test and oracle for Magnet client visuals,
 * 1.7.10 model geometry, ring animation wave equations, matrix transforms,
 * UV coordinates, and Mob Magnet GUI layout.
 */
public final class MagnetVisualRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testModelBoxGeometry();
        testWorldAndItemTransforms();
        testRingScalingAndWaveFormula();
        testRingColorsAndLight();
        testRingMatrixFiniteness();
        testGuiLayoutAndUv();
        System.out.println("TT_MAGNET_VISUAL_RULES_PASS checks=" + checks);
    }

    public static void testModelBoxGeometry() {
        // Texture dimensions
        check(MagnetVisualTransform.MODEL_TEXTURE_WIDTH == 64, "model texture width is 64");
        check(MagnetVisualTransform.MODEL_TEXTURE_HEIGHT == 64, "model texture height is 64");

        // Panel: addBox(0, 0, 0, 14, 2, 14), setRotationPoint(-7, 22, -7), texOffs(0, 0)
        check(MagnetVisualTransform.PANEL_ORIGIN_X == -7.0F && MagnetVisualTransform.PANEL_ORIGIN_Y == 22.0F && MagnetVisualTransform.PANEL_ORIGIN_Z == -7.0F, "panel rotation point matches (-7, 22, -7)");
        check(MagnetVisualTransform.PANEL_SIZE_X == 14.0F && MagnetVisualTransform.PANEL_SIZE_Y == 2.0F && MagnetVisualTransform.PANEL_SIZE_Z == 14.0F, "panel box dimensions match (14, 2, 14)");
        check(MagnetVisualTransform.PANEL_TEX_U == 0 && MagnetVisualTransform.PANEL_TEX_V == 0, "panel texture offset matches (0, 0)");

        // Magnet core: addBox(0, 0, 0, 4, 13, 4), setRotationPoint(-2, 9, -2), texOffs(0, 16)
        check(MagnetVisualTransform.CORE_ORIGIN_X == -2.0F && MagnetVisualTransform.CORE_ORIGIN_Y == 9.0F && MagnetVisualTransform.CORE_ORIGIN_Z == -2.0F, "core rotation point matches (-2, 9, -2)");
        check(MagnetVisualTransform.CORE_SIZE_X == 4.0F && MagnetVisualTransform.CORE_SIZE_Y == 13.0F && MagnetVisualTransform.CORE_SIZE_Z == 4.0F, "core box dimensions match (4, 13, 4)");
        check(MagnetVisualTransform.CORE_TEX_U == 0 && MagnetVisualTransform.CORE_TEX_V == 16, "core texture offset matches (0, 16)");

        // Box 1: addBox(0, -2, 0, 6, 14, 0), setRotationPoint(3, 10, -3), rot(0, -1.570796F, 0), texOffs(28, 19)
        check(MagnetVisualTransform.BOX1_ORIGIN_X == 3.0F && MagnetVisualTransform.BOX1_ORIGIN_Y == 10.0F && MagnetVisualTransform.BOX1_ORIGIN_Z == -3.0F, "box1 origin matches (3, 10, -3)");
        check(MagnetVisualTransform.BOX1_OFFSET_Y == -2.0F && MagnetVisualTransform.BOX1_SIZE_X == 6.0F && MagnetVisualTransform.BOX1_SIZE_Y == 14.0F && MagnetVisualTransform.BOX1_SIZE_Z == 0.0F, "box1 box size matches (6, 14, 0) with Y offset -2");
        check(Math.abs(MagnetVisualTransform.BOX1_ROT_Y - (-1.570796F)) < 1e-6, "box1 rotY matches -pi/2");
        check(MagnetVisualTransform.BOX1_TEX_U == 28 && MagnetVisualTransform.BOX1_TEX_V == 19, "box1 texOffs matches (28, 19)");

        // Box 2: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(-3, 8, 3), rot(0, 1.570796F, 0), texOffs(28, 33)
        check(MagnetVisualTransform.BOX2_ORIGIN_X == -3.0F && MagnetVisualTransform.BOX2_ORIGIN_Y == 8.0F && MagnetVisualTransform.BOX2_ORIGIN_Z == 3.0F, "box2 origin matches (-3, 8, 3)");
        check(MagnetVisualTransform.BOX2_SIZE_X == 6.0F && MagnetVisualTransform.BOX2_SIZE_Y == 14.0F && MagnetVisualTransform.BOX2_SIZE_Z == 0.0F, "box2 size matches (6, 14, 0)");
        check(Math.abs(MagnetVisualTransform.BOX2_ROT_Y - 1.570796F) < 1e-6, "box2 rotY matches +pi/2");
        check(MagnetVisualTransform.BOX2_TEX_U == 28 && MagnetVisualTransform.BOX2_TEX_V == 33, "box2 texOffs matches (28, 33)");

        // Box 3: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(3, 8, 3), rot(0, 3.141593F, 0), texOffs(40, 19)
        check(MagnetVisualTransform.BOX3_ORIGIN_X == 3.0F && MagnetVisualTransform.BOX3_ORIGIN_Y == 8.0F && MagnetVisualTransform.BOX3_ORIGIN_Z == 3.0F, "box3 origin matches (3, 8, 3)");
        check(MagnetVisualTransform.BOX3_SIZE_X == 6.0F && MagnetVisualTransform.BOX3_SIZE_Y == 14.0F && MagnetVisualTransform.BOX3_SIZE_Z == 0.0F, "box3 size matches (6, 14, 0)");
        check(Math.abs(MagnetVisualTransform.BOX3_ROT_Y - 3.141593F) < 1e-6, "box3 rotY matches pi");
        check(MagnetVisualTransform.BOX3_TEX_U == 40 && MagnetVisualTransform.BOX3_TEX_V == 19, "box3 texOffs matches (40, 19)");

        // Box 4: addBox(0, 0, 0, 6, 14, 0), setRotationPoint(-3, 8, -3), rot(0, 0, 0), texOffs(40, 33)
        check(MagnetVisualTransform.BOX4_ORIGIN_X == -3.0F && MagnetVisualTransform.BOX4_ORIGIN_Y == 8.0F && MagnetVisualTransform.BOX4_ORIGIN_Z == -3.0F, "box4 origin matches (-3, 8, -3)");
        check(MagnetVisualTransform.BOX4_SIZE_X == 6.0F && MagnetVisualTransform.BOX4_SIZE_Y == 14.0F && MagnetVisualTransform.BOX4_SIZE_Z == 0.0F, "box4 size matches (6, 14, 0)");
        check(MagnetVisualTransform.BOX4_TEX_U == 40 && MagnetVisualTransform.BOX4_TEX_V == 33, "box4 texOffs matches (40, 33)");

        // Box 5: addBox(0, 0, 0, 6, 0, 6), setRotationPoint(-3, 8, -3), rot(0, 0, 0), texOffs(28, 49)
        check(MagnetVisualTransform.BOX5_ORIGIN_X == -3.0F && MagnetVisualTransform.BOX5_ORIGIN_Y == 8.0F && MagnetVisualTransform.BOX5_ORIGIN_Z == -3.0F, "box5 origin matches (-3, 8, -3)");
        check(MagnetVisualTransform.BOX5_SIZE_X == 6.0F && MagnetVisualTransform.BOX5_SIZE_Y == 0.0F && MagnetVisualTransform.BOX5_SIZE_Z == 6.0F, "box5 size matches (6, 0, 6)");
        check(MagnetVisualTransform.BOX5_TEX_U == 28 && MagnetVisualTransform.BOX5_TEX_V == 49, "box5 texOffs matches (28, 49)");
    }

    public static void testWorldAndItemTransforms() {
        check(MagnetVisualTransform.TILE_TRANSLATE_X == 0.5 && MagnetVisualTransform.TILE_TRANSLATE_Y == 1.5 && MagnetVisualTransform.TILE_TRANSLATE_Z == 0.5, "tile translation is (0.5, 1.5, 0.5)");
        check(MagnetVisualTransform.TILE_SCALE_X == 1.0F && MagnetVisualTransform.TILE_SCALE_Y == -1.0F && MagnetVisualTransform.TILE_SCALE_Z == -1.0F, "tile scaling is (1.0, -1.0, -1.0)");
        check(MagnetVisualTransform.RING_ROT_X == 90.0F, "ring rotX is 90 deg");
        check(MagnetVisualTransform.RING_BASE_TRANS_Z == -0.6, "ring base transZ is -0.6");
        check(MagnetVisualTransform.RING_COUNT == 3, "ring count is 3");
        check(MagnetVisualTransform.FACES_PER_RING == 2, "faces per ring is 2");

        // Texture name mapping
        check(MagnetVisualTransform.modelTexture(false, true).getPath().equals("textures/model/magnet_s.png"), "kinetic pulling maps to magnet_s.png");
        check(MagnetVisualTransform.modelTexture(false, false).getPath().equals("textures/model/magnet_n.png"), "kinetic pushing maps to magnet_n.png");
        check(MagnetVisualTransform.modelTexture(true, true).getPath().equals("textures/model/mob_magnet_s.png"), "corporeal pulling maps to mob_magnet_s.png");
        check(MagnetVisualTransform.modelTexture(true, false).getPath().equals("textures/model/mob_magnet_n.png"), "corporeal pushing maps to mob_magnet_n.png");
    }

    public static void testRingScalingAndWaveFormula() {
        // Redstone 0
        check(MagnetVisualTransform.ringScale(0) == 0.0F, "signal 0 produces scale 0.0");

        // Redstone 1..15 scaling exactness
        // Formula: redstone / 15.0F * 0.7F + 0.4F
        float s1 = MagnetVisualTransform.ringScale(1);
        check(Math.abs(s1 - (1.0F / 15.0F * 0.7F + 0.4F)) < 1e-6, "signal 1 scale exact");

        float s7 = MagnetVisualTransform.ringScale(7);
        check(Math.abs(s7 - (7.0F / 15.0F * 0.7F + 0.4F)) < 1e-6, "signal 7 scale exact");

        float s14 = MagnetVisualTransform.ringScale(14);
        check(Math.abs(s14 - (14.0F / 15.0F * 0.7F + 0.4F)) < 1e-6, "signal 14 scale exact");

        float s15 = MagnetVisualTransform.ringScale(15);
        check(Math.abs(s15 - 1.1F) < 1e-6, "signal 15 produces max scale 1.1");

        // Test cumulative scales across faces (scale^1 through scale^6)
        for (int r = 0; r < 3; r++) {
            for (int f = 0; f < 2; f++) {
                float cum = MagnetVisualOracle.cumulativeScale(15, r, f);
                int expStep = r * 2 + f + 1;
                float expected = (float)Math.pow(1.1, expStep);
                check(Math.abs(cum - expected) < 1e-5, "cumulative scale for ring=" + r + " face=" + f + " matches 1.1^" + expStep);
            }
        }

        // Test wave formula: -(cos(timeMs / 500.0F) + 1.0) * 0.09 - 0.1
        // At t=0: cos(0)=1.0 -> -(2.0) * 0.09 - 0.1 = -0.18 - 0.1 = -0.28
        double step0 = MagnetVisualTransform.ringZStep(0);
        check(Math.abs(step0 - (-0.28)) < 1e-5, "wave step at t=0 is -0.28");

        // Check periodic bounds: cos in [-1, 1], so step in [-0.28, -0.10]
        for (long t = 0; t <= 10000; t += 200) {
            double step = MagnetVisualTransform.ringZStep(t);
            check(step >= -0.28001 && step <= -0.09999, "wave step bounded in [-0.28, -0.10] at t=" + t);
        }
    }

    public static void testRingColorsAndLight() {
        check(MagnetVisualTransform.ringRed(true) == 0.0F, "pulling (blue) red is 0.0");
        check(MagnetVisualTransform.ringBlue(true) == 1.0F, "pulling (blue) blue is 1.0");
        check(MagnetVisualTransform.ringRed(false) == 1.0F, "pushing (red) red is 1.0");
        check(MagnetVisualTransform.ringBlue(false) == 0.0F, "pushing (red) blue is 0.0");
        check(MagnetVisualTransform.ringGreen() == 0.0F, "green is always 0.0");
        check(MagnetVisualTransform.ringAlpha() == 0.9F, "alpha is always 0.9");
        check(MagnetVisualTransform.ringLight() == 225, "packed light matches 1.7.10 brightness 225");
    }

    public static void testRingMatrixFiniteness() {
        for (int signal = 0; signal <= 15; signal++) {
            for (int r = 0; r < 3; r++) {
                for (int f = 0; f < 2; f++) {
                    Matrix4f mat = MagnetVisualOracle.computeRingFaceMatrix(signal, r, f, 12345L);
                    check(mat.isFinite(), "ring face matrix is finite for signal=" + signal + " r=" + r + " f=" + f);
                    Vector4f corner = MagnetVisualOracle.transformQuadCorner(mat, -0.5F, 0.5F, 0.0F);
                    check(corner.isFinite(), "transformed corner is finite for signal=" + signal + " r=" + r + " f=" + f);
                }
            }
        }
    }

    public static void testGuiLayoutAndUv() {
        check(MagnetVisualTransform.GUI_WIDTH == 176, "GUI width is 176");
        check(MagnetVisualTransform.GUI_HEIGHT == 166, "GUI height is 166");
        check(MagnetVisualTransform.GUI_TEX_WIDTH == 256, "GUI texture width is 256");
        check(MagnetVisualTransform.GUI_TEX_HEIGHT == 256, "GUI texture height is 256");

        // Radio buttons
        check(MagnetVisualTransform.RADIO_ADULT_ID == 0, "adult radio id is 0");
        check(MagnetVisualTransform.RADIO_ADULT_X == 100 && MagnetVisualTransform.RADIO_ADULT_Y == 28, "adult radio at (100, 28)");
        check(MagnetVisualTransform.RADIO_CHILD_ID == 1, "child radio id is 1");
        check(MagnetVisualTransform.RADIO_CHILD_X == 100 && MagnetVisualTransform.RADIO_CHILD_Y == 48, "child radio at (100, 48)");
        check(MagnetVisualTransform.RADIO_WIDTH == 13 && MagnetVisualTransform.RADIO_HEIGHT == 13, "radio size is 13x13");

        // Radio UVs
        check(MagnetVisualTransform.RADIO_TEX_U == 176, "radio tex U is 176");
        check(MagnetVisualTransform.RADIO_TEX_V_SELECTED == 13, "radio selected tex V is 13");
        check(MagnetVisualTransform.RADIO_TEX_V_UNSELECTED == 0, "radio unselected tex V is 0");

        // Buttons do not overlap: adult Y=[28..41], child Y=[48..61], distance is 7 px
        check(MagnetVisualTransform.RADIO_ADULT_Y + MagnetVisualTransform.RADIO_HEIGHT <= MagnetVisualTransform.RADIO_CHILD_Y, "radio buttons have distinct vertical space");

        // Labels
        check(MagnetVisualTransform.LABEL_COLOR == 10066329, "label color matches 1.7.10 10066329 (0x999999)");
        check(MagnetVisualTransform.LABEL_FILTER_Y == 16, "filter label Y is 16");
        check(MagnetVisualTransform.LABEL_ADULT_X == 120 && MagnetVisualTransform.LABEL_ADULT_Y == 30, "adult label at (120, 30)");
        check(MagnetVisualTransform.LABEL_CHILD_X == 120 && MagnetVisualTransform.LABEL_CHILD_Y == 50, "child label at (120, 50)");

        // Filter slot
        check(MagnetVisualTransform.SLOT_FILTER_INDEX == 0, "filter slot index is 0");
        check(MagnetVisualTransform.SLOT_FILTER_X == 53 && MagnetVisualTransform.SLOT_FILTER_Y == 37, "filter slot at (53, 37)");

        // Label centering test
        int labelWidth = 30;
        int expectedX = 176 / 2 - 30 / 2 - 26; // 88 - 15 - 26 = 47
        check(MagnetVisualTransform.filterLabelX(176, labelWidth) == expectedX, "filter label centering formula matches 1.7.10");
    }

}
