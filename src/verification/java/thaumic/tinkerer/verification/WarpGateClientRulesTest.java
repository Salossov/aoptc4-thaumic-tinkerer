package thaumic.tinkerer.verification;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import thaumic.tinkerer.client.WarpGateVisualHelper;
import thaumic.tinkerer.client.WarpGateSpinningCubesModel;

/**
 * Standalone verification test suite and oracle for Packet 3: Warp Gate Client visuals,
 * screens, matrix transformations, coordinate bounds, and parity with 1.7.10.
 */
public final class WarpGateClientRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testTextureIntegrity();
        testSpinningCubesMathematicalConstants();
        testSpinningCubesOrbitalMath();
        testSpinningCubesMatrixStack();
        testGuiInventoryCoordinatesAndInversion();
        testDestinationsBackgroundHueAndPulseAlpha();
        testLineClippingAndLargeCoordinates();
        testHoverDetectionAndCoincidentPoints();
        testSneakTriggerAABB();
        System.out.println("TT_WARPGATE_CLIENT_RULES_PASS checks=" + checks);
    }

    public static void testTextureIntegrity() {
        // SHA-256 of original textures extracted from ThaumicTinkerer-2.5-1.7.10-164.jar
        checkHash("/assets/thaumictinkerer/textures/block/warp_gate_0.png",
                "5709AAF9F6476871F4DF8BA0CA7D0C0C0FACF3BD693D4F638618BD708AF99225");
        checkHash("/assets/thaumictinkerer/textures/block/warp_gate_1.png",
                "8F052F1311CD1103DA60EEFCF7B9A329CB53113C87A32F094191F9715BFB29C4");
        checkHash("/assets/thaumictinkerer/textures/block/warp_gate_2.png",
                "0CDB1FE3E2D077DF6D0ECF37290037A408362045093DA90C00C452787F84761D");
        checkHash("/assets/thaumictinkerer/textures/gui/warp_gate.png",
                "D4168CEF0E91F76D5A117D6C87706F1CECAD2529AE38AF4728D938D034E1A3E6");
    }

    private static void checkHash(String resourcePath, String expectedHex) {
        try (InputStream stream = WarpGateClientRulesTest.class.getResourceAsStream(resourcePath)) {
            check(stream != null, "resource exists: " + resourcePath);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[4096];
            int read;
            while ((read = stream.read(buf)) != -1) {
                sha256.update(buf, 0, read);
            }
            byte[] hashBytes = sha256.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02X", b));
            }
            check(sb.toString().equalsIgnoreCase(expectedHex), "SHA-256 match for " + resourcePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testSpinningCubesMathematicalConstants() {
        check(WarpGateVisualHelper.CUBES_COUNT == 12, "12 spinning cubes in ring");
        check(WarpGateVisualHelper.REPEAT_COUNT == 5, "5 repeat echoes (6 passes total)");
        check(Math.abs(WarpGateVisualHelper.OFFSET_PER_CUBE - 30.0F) < 1e-4, "offset per cube is 30 degrees (360/12)");
        check(Math.abs(WarpGateVisualHelper.CUBE_SCALE - 0.0625F) < 1e-6, "cube scale is 1/16 block");

        check(WarpGateVisualHelper.BASE_TRANSLATE_X == 0.5F, "base translateX = 0.5");
        check(WarpGateVisualHelper.BASE_TRANSLATE_Y == 2.5F, "base translateY = 2.5");
        check(WarpGateVisualHelper.BASE_TRANSLATE_Z == 0.5F, "base translateZ = 0.5");

        check(WarpGateVisualHelper.RING_OFFSET_X == -0.025F, "ring offsetX = -0.025");
        check(WarpGateVisualHelper.RING_OFFSET_Y == 0.85F, "ring offsetY = 0.85");
        check(WarpGateVisualHelper.RING_OFFSET_Z == -0.025F, "ring offsetZ = -0.025");
    }

    public static void testSpinningCubesOrbitalMath() {
        // Test time dilation per repeat
        double baseTicks = 100.0;
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 5, 5) - 100.0) < 1e-6, "repeat 5 ticks = 100");
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 4, 5) - 99.25) < 1e-6, "repeat 4 ticks = 99.25");
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 3, 5) - 98.5) < 1e-6, "repeat 3 ticks = 98.5");
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 2, 5) - 97.75) < 1e-6, "repeat 2 ticks = 97.75");
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 1, 5) - 97.0) < 1e-6, "repeat 1 ticks = 97.0");
        check(Math.abs(WarpGateVisualHelper.ticksForRepeat(baseTicks, 0, 5) - 96.25) < 1e-6, "repeat 0 ticks = 96.25");

        // Test alpha
        check(WarpGateVisualHelper.alphaForRepeat(5, 5) == 1.0F, "repeat 5 (leading) alpha is 1.0");
        check(WarpGateVisualHelper.alphaForRepeat(4, 5) == 0.2F, "repeat 4 (trail) alpha is 0.2");
        check(WarpGateVisualHelper.alphaForRepeat(0, 5) == 0.2F, "repeat 0 (trail) alpha is 0.2");

        // Test degrees cast to int
        for (int i = 0; i < 12; i++) {
            int deg = WarpGateVisualHelper.degForCube(10.5, i, 12);
            int expected = (int) ((10.5 / 0.25) % 360.0 + 30.0F * i);
            check(deg == expected, "degForCube matches integer truncation for cube " + i);
        }

        // Test radius bounds [0.6, 0.8]
        for (double t = 0; t < 100; t += 1.0) {
            float rx = WarpGateVisualHelper.radiusX(t);
            float rz = WarpGateVisualHelper.radiusZ(t);
            check(rx >= 0.599F && rx <= 0.801F, "radiusX in [0.6, 0.8]");
            check(rz >= 0.599F && rz <= 0.801F, "radiusZ in [0.6, 0.8]");
        }

        // Test rotation axis yRotate clamped to >= 0.6
        for (double t = 0; t < 100; t += 1.0) {
            Vector3f axis = WarpGateVisualHelper.cubeRotAxis(t);
            check(axis.y() >= 0.6F, "yRotate >= 0.6F");
            check(Math.abs(axis.x()) <= 0.501F, "xRotate in [-0.5, 0.5]");
            check(Math.abs(axis.z()) <= 0.501F, "zRotate in [-0.5, 0.5]");
        }
    }

    public static void testSpinningCubesMatrixStack() {
        // Check the 180-deg rotation around (1, 0, 1) mapping:
        // (1, 0, 0) -> (0, 0, 1)
        // (0, 1, 0) -> (0, -1, 0)
        // (0, 0, 1) -> (1, 0, 0)
        Matrix4f rotMat = new Matrix4f().rotate((float) Math.PI, 1.0F / (float) Math.sqrt(2.0), 0.0F, 1.0F / (float) Math.sqrt(2.0));

        Vector4f posX = rotMat.transform(new Vector4f(1.0F, 0.0F, 0.0F, 0.0F));
        check(Math.abs(posX.x()) < 1e-5 && Math.abs(posX.y()) < 1e-5 && Math.abs(posX.z() - 1.0F) < 1e-5, "rot maps X to Z");

        Vector4f posY = rotMat.transform(new Vector4f(0.0F, 1.0F, 0.0F, 0.0F));
        check(Math.abs(posY.x()) < 1e-5 && Math.abs(posY.y() + 1.0F) < 1e-5 && Math.abs(posY.z()) < 1e-5, "rot maps Y to -Y");

        Vector4f posZ = rotMat.transform(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        check(Math.abs(posZ.x() - 1.0F) < 1e-5 && Math.abs(posZ.y()) < 1e-5 && Math.abs(posZ.z()) < 1e-5, "rot maps Z to X");

        // Composite world center calculation:
        // Translate(0.5, 2.5, 0.5) * Rot(180, 1,0,1) * Translate(-0.025, 0.85, -0.025)
        Matrix4f fullBase = new Matrix4f()
                .translate(0.5F, 2.5F, 0.5F)
                .rotate((float) Math.PI, 1.0F / (float) Math.sqrt(2.0), 0.0F, 1.0F / (float) Math.sqrt(2.0))
                .translate(-0.025F, 0.85F, -0.025F);

        Vector4f origin = fullBase.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        check(Math.abs(origin.x() - 0.475F) < 1e-4, "composite ring center X = 0.475");
        check(Math.abs(origin.y() - 1.650F) < 1e-4, "composite ring center Y = 1.650 (2.5 - 0.85)");
        check(Math.abs(origin.z() - 0.475F) < 1e-4, "composite ring center Z = 0.475");
    }

    public static void testGuiInventoryCoordinatesAndInversion() {
        check(WarpGateVisualHelper.GUI_IMAGE_WIDTH == 176, "Gui image width = 176");
        check(WarpGateVisualHelper.GUI_IMAGE_HEIGHT == 166, "Gui image height = 166");
        check(WarpGateVisualHelper.BUTTON_X_OFFSET == 5, "Button X = 5");
        check(WarpGateVisualHelper.BUTTON_Y_OFFSET == 5, "Button Y = 5");
        check(WarpGateVisualHelper.BUTTON_SIZE == 13, "Button size = 13x13");
        check(WarpGateVisualHelper.LABEL_X_OFFSET == 20, "Label X = 20");
        check(WarpGateVisualHelper.LABEL_Y_OFFSET == 7, "Label Y = 7");
        check(WarpGateVisualHelper.LABEL_COLOR == 10066329, "Label color = 10066329 (0x999999)");
    }

    public static void testDestinationsBackgroundHueAndPulseAlpha() {
        // Test background hue animation: sin(ticks / 150.0) + 0.5
        for (int t = 0; t < 600; t += 10) {
            float hue = WarpGateVisualHelper.backgroundHue(t);
            check(hue >= -0.501F && hue <= 1.501F, "hue bounds");
            int rgb = WarpGateVisualHelper.backgroundRgb(t);
            check((rgb & 0xFF000000) != 0, "RGB has opaque color");
        }

        // Test line pulsing alpha: ((sin(ticks / 10.0) + 1.0) / 4.0 + 0.25) -> range [0.25, 0.75]
        for (int t = 0; t < 200; t++) {
            float alpha = WarpGateVisualHelper.lineAlpha(t);
            check(alpha >= 0.249F && alpha <= 0.751F, "alpha in [0.25, 0.75]");
            int color = WarpGateVisualHelper.lineColor(t);
            int a = (color >>> 24);
            check(a >= 63 && a <= 192, "alpha byte in [63, 192]");
            check((color & 0xFFFFFF) == 0xFFFFFF, "line color base is white");
        }
    }

    public static void testLineClippingAndLargeCoordinates() {
        double w = 800;
        double h = 600;

        // 1. Line completely inside
        var clip1 = WarpGateVisualHelper.clipLine(100, 100, 200, 200, 0, 0, w, h);
        check(clip1.visible(), "inside line is visible");
        check(clip1.x0() == 100 && clip1.y0() == 100 && clip1.x1() == 200 && clip1.y1() == 200, "endpoints untouched");

        // 2. Line completely outside
        var clip2 = WarpGateVisualHelper.clipLine(-500, -500, -200, -200, 0, 0, w, h);
        check(!clip2.visible(), "outside line is invisible");

        // 3. Huge coordinates (e.g. 10,000,000 blocks away): must clip in O(1) without stalls or overflow
        long start = System.nanoTime();
        var clip3 = WarpGateVisualHelper.clipLine(400, 300, 10_000_000, 10_000_000, 0, 0, w, h);
        long elapsed = System.nanoTime() - start;
        check(clip3.visible(), "huge coordinate line crossing screen is visible");
        check(clip3.x0() >= -1e-4 && clip3.x0() <= w + 1e-4, "clipped x0 on screen");
        check(clip3.x1() >= -1e-4 && clip3.x1() <= w + 1e-4, "clipped x1 on screen");
        check(elapsed < 1_000_000L, "clipping huge coordinate took less than 1ms (" + elapsed + " ns)");

        // 4. Coincident X (vertical line)
        var clip4 = WarpGateVisualHelper.clipLine(100, -100, 100, 700, 0, 0, w, h);
        check(clip4.visible(), "vertical crossing line visible");
        check(clip4.x0() == 100 && clip4.x1() == 100, "vertical line maintains x");
        check(clip4.y0() == 0 && clip4.y1() == h, "vertical line clipped to [0, h]");

        // 5. Coincident Z (horizontal line)
        var clip5 = WarpGateVisualHelper.clipLine(-100, 200, 900, 200, 0, 0, w, h);
        check(clip5.visible(), "horizontal crossing line visible");
        check(clip5.y0() == 200 && clip5.y1() == 200, "horizontal line maintains y");
        check(clip5.x0() == 0 && clip5.x1() == w, "horizontal line clipped to [0, w]");
    }

    public static void testHoverDetectionAndCoincidentPoints() {
        int xp = 100;
        int yp = 150;

        // Hover box: mx in [xp - 4, xp + 4], my in [yp - 4, yp + 3] (my < yp + 4)
        check(WarpGateVisualHelper.isPearlHovered(100, 150, xp, yp), "center is hovered");
        check(WarpGateVisualHelper.isPearlHovered(96, 150, xp, yp), "xp - 4 is hovered");
        check(WarpGateVisualHelper.isPearlHovered(104, 150, xp, yp), "xp + 4 is hovered");
        check(WarpGateVisualHelper.isPearlHovered(100, 146, xp, yp), "yp - 4 is hovered");
        check(WarpGateVisualHelper.isPearlHovered(100, 153, xp, yp), "yp + 3 is hovered");

        check(!WarpGateVisualHelper.isPearlHovered(95, 150, xp, yp), "xp - 5 not hovered");
        check(!WarpGateVisualHelper.isPearlHovered(105, 150, xp, yp), "xp + 5 not hovered");
        check(!WarpGateVisualHelper.isPearlHovered(100, 145, xp, yp), "yp - 5 not hovered");
        check(!WarpGateVisualHelper.isPearlHovered(100, 154, xp, yp), "yp + 4 not hovered (my < yp + 4 strictly)");
    }

    public static void testSneakTriggerAABB() {
        BlockPos gatePos = new BlockPos(10, 64, 20);
        AABB aabb = WarpGateVisualHelper.getSneakTriggerAABB(gatePos);

        check(aabb.minX == 10.0 && aabb.maxX == 11.0, "AABB X bounds [10, 11]");
        check(aabb.minY == 65.0 && aabb.maxY == 65.5, "AABB Y bounds [64+1, 64+1.5] = [65.0, 65.5]");
        check(aabb.minZ == 20.0 && aabb.maxZ == 21.0, "AABB Z bounds [20, 21]");

        // Player standing on top of gate at Y = 65.0: player box minY = 65.0, maxY = 66.8
        AABB playerOnTop = new AABB(10.2, 65.0, 20.2, 10.8, 66.8, 20.8);
        check(playerOnTop.intersects(aabb), "player on top intersects sneak trigger AABB");

        // Player below gate:
        AABB playerBelow = new AABB(10.2, 63.0, 20.2, 10.8, 64.8, 20.8);
        check(!playerBelow.intersects(aabb), "player below does not intersect");

        // Player to the side:
        AABB playerSide = new AABB(12.0, 65.0, 20.2, 12.6, 66.8, 20.8);
        check(!playerSide.intersects(aabb), "player to the side does not intersect");
    }
}
