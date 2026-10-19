package thaumic.tinkerer.verification;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import thaumic.tinkerer.client.*;

/**
 * Independent verification test suite and oracle for Packet C client visuals:
 * Repairer, Funnel, and Animation Tablet (Восстановитель, Магическая воронка, Планшет динамизма).
 */
public final class MachinesVisualRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testRepairerModelGeometry();
        testRepairerWorldTransforms();
        testFunnelGeometryAndTransforms();
        testAnimationTabletOverlays();
        testAnimationTabletItemAnimation();
        testAnimationTabletGui();
        testMatrixFiniteness();
        System.out.println("TT_MACHINES_VISUAL_RULES_PASS checks=" + checks);
    }

    public static void testRepairerModelGeometry() {
        // Texture dimensions
        check(RepairerVisualTransform.TEXTURE_WIDTH == 64, "repairer texture width is 64");
        check(RepairerVisualTransform.TEXTURE_HEIGHT == 64, "repairer texture height is 64");

        // Base: addBox(0, 0, 0, 16, 1, 16), pivot(-8, 23, -8), uv(0, 0)
        check(RepairerVisualTransform.BASE_ORIGIN_X == -8.0F &&
              RepairerVisualTransform.BASE_ORIGIN_Y == 23.0F &&
              RepairerVisualTransform.BASE_ORIGIN_Z == -8.0F, "repairer base pivot matches (-8, 23, -8)");
        check(RepairerVisualTransform.BASE_SIZE_X == 16.0F &&
              RepairerVisualTransform.BASE_SIZE_Y == 1.0F &&
              RepairerVisualTransform.BASE_SIZE_Z == 16.0F, "repairer base dimensions match (16, 1, 16)");
        check(RepairerVisualTransform.BASE_TEX_U == 0 && RepairerVisualTransform.BASE_TEX_V == 0, "repairer base UV matches (0, 0)");

        // 4 corner supports: (2, 14, 2), uv(0, 17)
        check(RepairerVisualTransform.SUPPORT_SIZE_X == 2.0F &&
              RepairerVisualTransform.SUPPORT_SIZE_Y == 14.0F &&
              RepairerVisualTransform.SUPPORT_SIZE_Z == 2.0F, "repairer support dimensions match (2, 14, 2)");
        check(RepairerVisualTransform.SUPPORT_TEX_U == 0 && RepairerVisualTransform.SUPPORT_TEX_V == 17, "repairer support UV matches (0, 17)");
        check(RepairerVisualTransform.SUPPORT_PIVOTS.length == 4, "repairer has 4 supports");
        check(RepairerVisualTransform.SUPPORT_PIVOTS[0][0] == -8.0F && RepairerVisualTransform.SUPPORT_PIVOTS[0][2] == 6.0F, "support1 pivot matches (-8, 9, 6)");
        check(RepairerVisualTransform.SUPPORT_PIVOTS[1][0] == 6.0F && RepairerVisualTransform.SUPPORT_PIVOTS[1][2] == 6.0F, "support2 pivot matches (6, 9, 6)");
        check(RepairerVisualTransform.SUPPORT_PIVOTS[2][0] == -8.0F && RepairerVisualTransform.SUPPORT_PIVOTS[2][2] == -8.0F, "support3 pivot matches (-8, 9, -8)");
        check(RepairerVisualTransform.SUPPORT_PIVOTS[3][0] == 6.0F && RepairerVisualTransform.SUPPORT_PIVOTS[3][2] == -8.0F, "support4 pivot matches (6, 9, -8)");

        // Top horizontal beams
        check(RepairerVisualTransform.TOP12_SIZE_X == 16.0F && RepairerVisualTransform.TOP12_SIZE_Y == 1.0F && RepairerVisualTransform.TOP12_SIZE_Z == 2.0F, "top1/2 size (16, 1, 2)");
        check(RepairerVisualTransform.TOP12_TEX_U == 11 && RepairerVisualTransform.TOP12_TEX_V == 19, "top1/2 UV (11, 19)");
        check(RepairerVisualTransform.TOP34_SIZE_X == 2.0F && RepairerVisualTransform.TOP34_SIZE_Y == 1.0F && RepairerVisualTransform.TOP34_SIZE_Z == 12.0F, "top3/4 size (2, 1, 12)");
        check(RepairerVisualTransform.TOP34_TEX_U == 11 && RepairerVisualTransform.TOP34_TEX_V == 23, "top3/4 UV (11, 23)");

        // Tube opening on back wall (Z = 7)
        check(RepairerVisualTransform.OPENING_H_TEX_U == 48 && RepairerVisualTransform.OPENING_H_TEX_V == 30, "opening horizontal UV (48, 30)");
        check(RepairerVisualTransform.OPENING_V_TEX_U == 48 && RepairerVisualTransform.OPENING_V_TEX_V == 24, "opening vertical UV (48, 24)");

        // Glass parts
        check(RepairerVisualTransform.GLASS1_SIZE_X == 12.0F && RepairerVisualTransform.GLASS1_SIZE_Z == 12.0F, "glass1 top plate is 12x12");
        check(RepairerVisualTransform.GLASS4_SIZE_X == 12.0F && RepairerVisualTransform.GLASS4_SIZE_Y == 14.0F, "glass4 front pane is 12x14");
        check(RepairerVisualTransform.GLASS5_SIZE_X == 12.0F && RepairerVisualTransform.GLASS5_SIZE_Y == 14.0F, "glass5 back pane is 12x14");
    }

    public static void testRepairerWorldTransforms() {
        // Floor plate bounds in world space
        float[] floor = RepairerVisualOracle.computeBaseFloorBounds();
        check(Math.abs(floor[0] - 0.0F) < 1e-4 && Math.abs(floor[3] - 1.0F) < 1e-4, "base floor spans X [0..1]");
        check(Math.abs(floor[1] - 0.0F) < 1e-4 && Math.abs(floor[4] - 0.0625F) < 1e-4, "base floor spans Y [0..1/16]");
        check(Math.abs(floor[2] - 0.0F) < 1e-4 && Math.abs(floor[5] - 1.0F) < 1e-4, "base floor spans Z [0..1]");

        // Facing rotations: NORTH=0, SOUTH=180, WEST=270, EAST=90
        check(RepairerVisualTransform.getFacingRotation(net.minecraft.core.Direction.NORTH) == 0.0F, "north rot 0");
        check(RepairerVisualTransform.getFacingRotation(net.minecraft.core.Direction.SOUTH) == 180.0F, "south rot 180");
        check(RepairerVisualTransform.getFacingRotation(net.minecraft.core.Direction.WEST) == 270.0F, "west rot 270");
        check(RepairerVisualTransform.getFacingRotation(net.minecraft.core.Direction.EAST) == 90.0F, "east rot 90");

        // Overlay properties
        check(RepairerVisualTransform.OVERLAY_SIZE == 1.25, "overlay size is 1.25");
        check(RepairerVisualTransform.OVERLAY_HALF_SIZE == 0.625F, "overlay half size is 0.625");
        check(Math.abs(RepairerVisualOracle.computeOverlayWorldY() - 0.975F) < 1e-4, "overlay world Y is 0.975");

        float[][] quad = RepairerVisualOracle.computeOverlayQuadVertices();
        check(quad.length == 4, "overlay quad has 4 vertices");
        check(quad[0][0] == -0.625F && quad[0][2] == 0.625F, "vertex 0 (-0.625, 0, 0.625)");
        check(quad[1][0] == 0.625F && quad[1][2] == 0.625F, "vertex 1 (0.625, 0, 0.625)");
        check(quad[2][0] == 0.625F && quad[2][2] == -0.625F, "vertex 2 (0.625, 0, -0.625)");
        check(quad[3][0] == -0.625F && quad[3][2] == -0.625F, "vertex 3 (-0.625, 0, -0.625)");
    }

    public static void testFunnelGeometryAndTransforms() {
        check(FunnelVisualTransform.BLOCK_HEIGHT == 0.125F, "funnel height is 0.125 (2 pixels)");
        check(FunnelVisualTransform.BOUNDS[1] == 0.0F && FunnelVisualTransform.BOUNDS[4] == 0.125F, "funnel bounds Y is 0..0.125");

        // Jar seating: 1.7.10 base Y = 0.365 - 0.25 = 0.115
        check(FunnelVisualTransform.RENDER_BASE_Y == 0.115, "jar base Y is 0.115");
        check(FunnelVisualTransform.RENDER_CENTER_Y == 0.615, "jar center Y is 0.615");
        check(FunnelVisualTransform.RENDER_CENTER_X == 0.5 && FunnelVisualTransform.RENDER_CENTER_Z == 0.5, "jar centered at (0.5, 0.5)");
        check(FunnelVisualOracle.isJarSeatedProperly(), "jar is seated 0.01 into funnel plate");


    }

    public static void testAnimationTabletOverlays() {
        check(TabletVisualTransform.OVERLAY_CENTER_SIZE == 0.65, "tablet center size 0.65");
        check(TabletVisualTransform.OVERLAY_CENTER_HEIGHT == 0.13F, "tablet center height 0.13");
        check(TabletVisualTransform.OVERLAY_CENTER_ROT_MOD == -1, "tablet center rotates counter-clockwise");

        check(TabletVisualTransform.OVERLAY_MODE_SIZE == 1.0, "tablet mode overlay size 1.0");
        check(TabletVisualTransform.OVERLAY_LEFT_HEIGHT == 0.13F, "tablet left overlay height 0.13");
        check(TabletVisualTransform.OVERLAY_RIGHT_HEIGHT == 0.131F, "tablet right overlay height 0.131 (z-fighting prevention)");
        check(TabletVisualTransform.OVERLAY_MODE_ROT_MOD == 1, "tablet mode overlay rotates clockwise");

        check(TabletVisualTransform.OVERLAY_INDENT_SIZE == 0.5, "tablet indent overlay size 0.5");
        check(TabletVisualTransform.OVERLAY_INDENT_HEIGHT == 0.13F, "tablet indent height 0.13");
        check(TabletVisualTransform.OVERLAY_INDENT_ANGLE_OFFSET == 90.0F, "tablet indent angle offset 90");

        // Direction mapping & translations
        check(TabletVisualTransform.ROTATIONS[0] == 270.0F, "north rotation 270");
        check(TabletVisualTransform.ROTATIONS[1] == 90.0F, "south rotation 90");
        check(TabletVisualTransform.ROTATIONS[2] == 0.0F, "west rotation 0");
        check(TabletVisualTransform.ROTATIONS[3] == 180.0F, "east rotation 180");

        check(TabletVisualTransform.TRANSLATIONS[0][2] == -1.0F, "north translation (0, 0, -1)");
        check(TabletVisualTransform.TRANSLATIONS[1][0] == -1.0F, "south translation (-1, 0, 0)");
        check(TabletVisualTransform.TRANSLATIONS[2][0] == 0.0F, "west translation (0, 0, 0)");
        check(TabletVisualTransform.TRANSLATIONS[3][0] == -1.0F && TabletVisualTransform.TRANSLATIONS[3][2] == -1.0F, "east translation (-1, 0, -1)");
    }

    public static void testAnimationTabletItemAnimation() {
        check(TabletVisualTransform.ITEM_SCALE == 0.8F, "tablet item scale 0.8");
        check(TabletVisualTransform.ITEM_SCALE_BLOCK == 2.5F, "tablet item block scale 2.5");
        check(TabletVisualTransform.ITEM_SCALE_GENERIC == 1.5F, "tablet item generic scale 1.5");

        // Hover function
        for (long t = 0; t <= 3600; t += 100) {
            double h = TabletVisualTransform.itemHoverY(t);
            check(h >= -TabletVisualTransform.ITEM_HOVER_AMPLITUDE - 1e-5 &&
                  h <= TabletVisualTransform.ITEM_HOVER_AMPLITUDE + 1e-5, "hover stays within bounds");
        }

        // Wobble function
        for (long t = 0; t <= 2400; t += 100) {
            float w = TabletVisualTransform.itemWobbleAngle(t);
            check(w >= -TabletVisualTransform.WOBBLE_AMPLITUDE_DEG - 1e-4 &&
                  w <= TabletVisualTransform.WOBBLE_AMPLITUDE_DEG + 1e-4, "wobble stays within bounds");
        }

        // Swing factors
        check(Math.abs(TabletVisualTransform.SWING_TRANS_X_FACTOR - (-0.004F)) < 1e-5, "swing X factor -1/250");
        check(Math.abs(TabletVisualTransform.SWING_TRANS_Y_FACTOR - 0.001F) < 1e-5, "swing Y factor 1/1000");
    }

    public static void testAnimationTabletGui() {
        check(TabletVisualTransform.GUI_WIDTH == 176, "GUI width 176");
        check(TabletVisualTransform.GUI_HEIGHT == 166, "GUI height 166");
        check(TabletVisualTransform.BTN_SIZE == 13, "button size 13");

        // Button relative coords
        check(TabletVisualTransform.BTN_REDSTONE_REL_X == 81, "redstone button relX 81");
        check(TabletVisualTransform.BTN_REDSTONE_REL_Y == 60, "redstone button relY 60");
        check(TabletVisualTransform.BTN_LEFT_CLICK_REL_X == 52, "left click button relX 52");
        check(TabletVisualTransform.BTN_LEFT_CLICK_REL_Y == 15, "left click button relY 15");
        check(TabletVisualTransform.BTN_RIGHT_CLICK_REL_X == 111, "right click button relX 111");
        check(TabletVisualTransform.BTN_RIGHT_CLICK_REL_Y == 15, "right click button relY 15");

        // Button UVs
        check(TabletVisualTransform.BTN_TEX_U == 176, "button tex U 176");
        check(TabletVisualTransform.BTN_TEX_V_OFF == 0, "button tex V unselected 0");
        check(TabletVisualTransform.BTN_TEX_V_ON == 13, "button tex V selected 13");

        // Label properties
        check(TabletVisualTransform.LABEL_COLOR == 0x999999, "label color 0x999999 (10066329)");
        check(TabletVisualTransform.LABEL_LEFT_ANCHOR_X == 48, "left label anchor X 48");
        check(TabletVisualTransform.LABEL_LEFT_Y == 18, "left label Y 18");
        check(TabletVisualTransform.LABEL_RIGHT_X == 128, "right label X 128");
        check(TabletVisualTransform.LABEL_RIGHT_Y == 18, "right label Y 18");
        check(TabletVisualTransform.LABEL_REDSTONE_Y == 50, "redstone label Y 50");

        // Mouse-over hit test
        int leftPos = 100;
        int topPos = 50;
        check(TabletVisualOracle.isMouseOverButton(100 + 81 + 5, 50 + 60 + 5, leftPos, topPos, 81, 60), "mouse inside redstone button");
        check(!TabletVisualOracle.isMouseOverButton(100 + 81 + 20, 50 + 60 + 5, leftPos, topPos, 81, 60), "mouse outside redstone button");
        check(TabletVisualOracle.isMouseOverButton(100 + 52 + 2, 50 + 15 + 2, leftPos, topPos, 52, 15), "mouse inside left click button");
        check(TabletVisualOracle.isMouseOverButton(100 + 111 + 2, 50 + 15 + 2, leftPos, topPos, 111, 15), "mouse inside right click button");
    }

    public static void testMatrixFiniteness() {
        // Model matrices
        for (float rot : new float[]{0, 90, 180, 270}) {
            Matrix4f m = RepairerVisualOracle.computeModelMatrix(rot);
            check(m.isFinite(), "repairer model matrix is finite for rot " + rot);
        }

        // Funnel jar matrix
        Matrix4f jarM = FunnelVisualOracle.computeJarMatrix();
        check(jarM.isFinite(), "funnel jar matrix is finite");

        // Tablet matrices
        for (int dir = 0; dir < 4; dir++) {
            for (int swing = 0; swing <= 45; swing += 15) {
                for (long t = 0; t <= 1000; t += 200) {
                    Matrix4f itemM = TabletVisualOracle.computeItemMatrix(dir, swing, t, false);
                    check(itemM.isFinite(), "tablet item matrix generic is finite");
                    Matrix4f itemBlockM = TabletVisualOracle.computeItemMatrix(dir, swing, t, true);
                    check(itemBlockM.isFinite(), "tablet item matrix block is finite");
                }
            }
        }

        // Overlay matrices
        Matrix4f overlayCenter = TabletVisualOracle.computeOverlayMatrix(TabletVisualTransform.OVERLAY_CENTER_HEIGHT, 45.0F);
        check(overlayCenter.isFinite(), "tablet overlay matrix is finite");
    }
}
