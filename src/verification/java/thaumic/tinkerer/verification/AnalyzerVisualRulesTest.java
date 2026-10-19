package thaumic.tinkerer.verification;

import thaumic.tinkerer.client.AnalyzerVisualTransform;

/**
 * Independent verification test for Aspect Analyzer GUI geometry, layout math,
 * hit-testing, text scaling, amount formatting and outline offsets. No rendered client is started.
 */
public final class AnalyzerVisualRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testGuiGeometryAndConstants();
        testTagPositions();
        testHoverHitTesting();
        testScaledTextCoordinates();
        testAmountFormatting();
        testDocumentationKeys();
        System.out.println("TT_ANALYZER_VISUAL_RULES_PASS checks=" + checks);
    }

    public static void testGuiGeometryAndConstants() {
        check(AnalyzerVisualTransform.GUI_WIDTH == 176, "GUI width is 176");
        check(AnalyzerVisualTransform.GUI_HEIGHT == 166, "GUI height is 166");
        check(AnalyzerVisualTransform.TEXTURE_WIDTH == 256, "Texture width is 256");
        check(AnalyzerVisualTransform.TEXTURE_HEIGHT == 256, "Texture height is 256");
        check(AnalyzerVisualTransform.TEXTURE_PATH.equals("textures/gui/aspect_analyzer.png"), "Texture path is correct");

        check(AnalyzerVisualTransform.SLOT_X == 20, "Slot X is 20");
        check(AnalyzerVisualTransform.SLOT_Y == 30, "Slot Y is 30");

        check(AnalyzerVisualTransform.ASPECT_START_X == 20, "Aspect start X is 20");
        check(AnalyzerVisualTransform.ASPECT_Y == 58, "Aspect Y is 58");
        check(AnalyzerVisualTransform.ASPECT_STEP_X == 18, "Aspect step X is 18");
        check(AnalyzerVisualTransform.ASPECT_SIZE == 16, "Aspect size is 16x16");
        check(AnalyzerVisualTransform.MAX_ROW_ASPECTS == 8, "Max visible aspects in row is 8");

        check(AnalyzerVisualTransform.PLAYER_INV_X == 8, "Player inventory X is 8");
        check(AnalyzerVisualTransform.PLAYER_INV_Y == 84, "Player inventory Y is 84");
        check(AnalyzerVisualTransform.HOTBAR_X == 8, "Hotbar X is 8");
        check(AnalyzerVisualTransform.HOTBAR_Y == 142, "Hotbar Y is 142");
    }

    public static void testTagPositions() {
        int[] expectedX = new int[]{20, 38, 56, 74, 92, 110, 128, 146};
        for (int i = 0; i < expectedX.length; i++) {
            int computedX = AnalyzerVisualTransform.tagX(i);
            check(computedX == expectedX[i], "Tag " + i + " X coordinate is " + expectedX[i]);
            check(AnalyzerVisualTransform.tagY() == 58, "Tag " + i + " Y coordinate is 58");
            check(computedX + AnalyzerVisualTransform.ASPECT_SIZE <= AnalyzerVisualTransform.GUI_WIDTH,
                "Tag " + i + " fits within GUI width (176)");
        }
    }

    public static void testHoverHitTesting() {
        int x = 20;
        int y = 58;

        // Strict boundary conditions matching 1.7.10 (mx > x && mx < x + 16)
        check(!AnalyzerVisualTransform.isHovered(x, y, x, y), "Exact top-left boundary is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x + 16, y), "Exact top-right boundary is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x, y + 16), "Exact bottom-left boundary is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x + 16, y + 16), "Exact bottom-right boundary is not hovered");

        check(!AnalyzerVisualTransform.isHovered(x, y, x - 1, y + 8), "Left of tag is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x + 17, y + 8), "Right of tag is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x + 8, y - 1), "Above tag is not hovered");
        check(!AnalyzerVisualTransform.isHovered(x, y, x + 8, y + 17), "Below tag is not hovered");

        // Interior points
        check(AnalyzerVisualTransform.isHovered(x, y, x + 1, y + 1), "Interior (x+1, y+1) is hovered");
        check(AnalyzerVisualTransform.isHovered(x, y, x + 8, y + 8), "Center (x+8, y+8) is hovered");
        check(AnalyzerVisualTransform.isHovered(x, y, x + 15, y + 15), "Interior (x+15, y+15) is hovered");

        // Multi-tag discrimination
        int x0 = AnalyzerVisualTransform.tagX(0); // 20
        int x1 = AnalyzerVisualTransform.tagX(1); // 38
        check(AnalyzerVisualTransform.isHovered(x0, y, 25, 65), "Point (25, 65) hovers tag 0");
        check(!AnalyzerVisualTransform.isHovered(x1, y, 25, 65), "Point (25, 65) does not hover tag 1");
        check(!AnalyzerVisualTransform.isHovered(x0, y, 42, 65), "Point (42, 65) does not hover tag 0");
        check(AnalyzerVisualTransform.isHovered(x1, y, 42, 65), "Point (42, 65) hovers tag 1");
        // Gap between tag 0 and tag 1 (X: 36..37)
        check(!AnalyzerVisualTransform.isHovered(x0, y, 36, 65), "Gap (36, 65) does not hover tag 0");
        check(!AnalyzerVisualTransform.isHovered(x1, y, 36, 65), "Gap (36, 65) does not hover tag 1");
    }

    public static void testScaledTextCoordinates() {
        check(Math.abs(AnalyzerVisualTransform.TEXT_SCALE - 0.5f) < 1e-6, "Text scale is exactly 0.5f");
        check(AnalyzerVisualTransform.FONT_LINE_HEIGHT == 9, "Font line height is 9");

        // Formula: tx = 32 - width + x * 2, ty = 32 - lineHeight + y * 2
        int tagX = 20;
        int tagY = 58;
        int stringWidth = 6; // single digit width
        int tx = AnalyzerVisualTransform.textX(tagX, stringWidth);
        int ty = AnalyzerVisualTransform.textY(tagY, 9);
        check(tx == 32 - 6 + 40, "tx for tagX=20, width=6 is 66");
        check(ty == 32 - 9 + 116, "ty for tagY=58, lineHeight=9 is 139");

        // Outline shifts
        int[][] shifts = AnalyzerVisualTransform.OUTLINE_SHIFTS;
        check(shifts.length == 4, "4 outline shifts");
        check(shifts[0][0] == -1 && shifts[0][1] == 0, "Shift 0 is (-1, 0)");
        check(shifts[1][0] == 1 && shifts[1][1] == 0, "Shift 1 is (1, 0)");
        check(shifts[2][0] == 0 && shifts[2][1] == -1, "Shift 2 is (0, -1)");
        check(shifts[3][0] == 0 && shifts[3][1] == 1, "Shift 3 is (0, 1)");

        check(AnalyzerVisualTransform.OUTLINE_COLOR == 0xFF000000, "Outline color is opaque black");
        check(AnalyzerVisualTransform.TEXT_COLOR == 0xFFFFFFFF, "Text color is opaque white");
    }






    private static void testAmountFormatting() {
        check(AnalyzerVisualTransform.amountText(1).equals("1"), "source amount 1");
        check(AnalyzerVisualTransform.amountText(1024).equals("1024"), "source no grouping");
        check(AnalyzerVisualTransform.amountText(16777217).equals("16777216"), "original float rounding retained");
        check(AnalyzerVisualTransform.amountText(Integer.MAX_VALUE).equals("2147483648"), "original maximum-int float widening");
    }
    private static void testDocumentationKeys() {
        check(thaumic.tinkerer.client.PeripheralDocumentationClient.handles("thaumictinkerer:peripherals",257), "Enter on peripherals");
        check(thaumic.tinkerer.client.PeripheralDocumentationClient.handles("thaumictinkerer:golem_connector",257), "Enter on golem connector");
        check(!thaumic.tinkerer.client.PeripheralDocumentationClient.handles("thaumictinkerer:aspect_analyzer",257), "other research untouched");
        check(!thaumic.tinkerer.client.PeripheralDocumentationClient.handles("thaumictinkerer:peripherals",256), "Escape is ordinary book input");
        check(!thaumic.tinkerer.client.PeripheralDocumentationClient.handles("thaumictinkerer:peripherals",335), "original main Enter only");
    }
}
