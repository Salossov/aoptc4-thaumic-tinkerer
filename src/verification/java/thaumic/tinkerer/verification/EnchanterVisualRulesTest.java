package thaumic.tinkerer.verification;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import thaumcraft.api.aspects.Aspects;
import thaumic.tinkerer.block.EnchanterBlockEntity;
import thaumic.tinkerer.client.EnchanterVisualTransform;
import thaumic.tinkerer.enchantment.EnchanterCatalog;

/**
 * Independent verification test and oracle for Osmotic Enchanter client visuals,
 * transforms, GUI layout, aspect bars, UV coordinates, and catalog integrity.
 */
public final class EnchanterVisualRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testWorldTransforms();
        testGuiCoordinatesAndUv();
        testAspectBarsAndPrimals();
        testCatalogIntegrity();
        System.out.println("TT_ENCHANTER_VISUAL_RULES_PASS checks=" + checks);
    }

    public static void testWorldTransforms() {
        // Base translation on top of enchanter block (slab height 0.75)
        check(EnchanterVisualTransform.BASE_HEIGHT == 0.75, "base height matches 1.7.10 slab height 0.75");

        // Tool slot 0 transformation
        check(EnchanterVisualTransform.TOOL_ROT_X == 90.0F, "tool rotX is 90 deg");
        check(EnchanterVisualTransform.TOOL_SCALE == 0.7F, "tool scale is 0.7");
        check(EnchanterVisualTransform.TOOL_TRANS_X == 0.6F, "tool transX is 0.6");
        check(EnchanterVisualTransform.TOOL_TRANS_Y == -0.2F, "tool transY is -0.2");
        check(EnchanterVisualTransform.TOOL_TRANS_Z == 0.0F, "tool transZ is 0.0");
        check(EnchanterVisualTransform.TOOL_ROT_Z == 30.0F, "tool rotZ is 30 deg");

        Matrix4f toolMat = EnchanterVisualTransform.computeToolMatrix();
        check(toolMat.isFinite(), "tool matrix is valid finite matrix");
        // Verify tool origin translation transformed
        Vector4f toolOrigin = new Vector4f(0, 0, 0, 1).mul(toolMat);
        check(Math.abs(toolOrigin.y - 0.75F) < 1e-4, "tool lies on the enchanter top surface at Y=0.75");

        // Wand slot 1 transformation
        check(EnchanterVisualTransform.WAND_ROT_X == 90.0F, "wand rotX is 90 deg");
        check(EnchanterVisualTransform.WAND_SCALE == 0.5F, "wand scale is 0.5");
        check(EnchanterVisualTransform.WAND_TRANS_X == 0.6F, "wand transX is 0.6");
        check(EnchanterVisualTransform.WAND_TRANS_Y == 1.5F, "wand transY is 1.5");
        check(EnchanterVisualTransform.WAND_TRANS_Z == -0.1F, "wand transZ is -0.1");
        check(EnchanterVisualTransform.WAND_ROT_Z == -70.0F, "wand rotZ is -70 deg");
        check(EnchanterVisualTransform.WAND_PIVOT_Y == 0.325F, "wand pivotY is 0.325");

        // Test bobbing and tilting formulas at multiple sample timestamps
        // At millis = 0: cos(0) = 1.0 -> bobZ = (1.0 - 1.2) / 10.0 = -0.02
        float bob0 = EnchanterVisualTransform.wandBobZ(0);
        check(Math.abs(bob0 - (-0.02F)) < 1e-5, "bobZ at t=0 is -0.02");

        // At millis = 0: cos(0) * 5.0 = 5.0 deg
        float tilt0 = EnchanterVisualTransform.wandTiltX(0);
        check(Math.abs(tilt0 - 5.0F) < 1e-5, "tiltX at t=0 is +5.0 deg");

        // At millis = 500: cos(500 / 500) = cos(1.0)
        float tilt500 = EnchanterVisualTransform.wandTiltX(500);
        check(Math.abs(tilt500 - (float)(Math.cos(1.0) * 5.0)) < 1e-5, "tiltX at t=500 matches cos(1)*5");

        // Check periodic bounds:
        // bobZ in [(-1 - 1.2)/10, (1 - 1.2)/10] = [-0.22, -0.02]
        // tiltX in [-5.0, +5.0]
        for (long t = 0; t <= 10000; t += 250) {
            float b = EnchanterVisualTransform.wandBobZ(t);
            float tilt = EnchanterVisualTransform.wandTiltX(t);
            check(b >= -0.2201F && b <= -0.0199F, "wand bobZ bounded in [-0.22, -0.02] at t=" + t);
            check(tilt >= -5.001F && tilt <= 5.001F, "wand tiltX bounded in [-5.0, 5.0] at t=" + t);
            Matrix4f wandMat = EnchanterVisualTransform.computeWandMatrix(t);
            check(wandMat.isFinite(), "wand matrix finite at t=" + t);
        }
    }

    public static void testGuiCoordinatesAndUv() {
        check(EnchanterVisualTransform.GUI_WIDTH == 176, "GUI width is 176");
        check(EnchanterVisualTransform.GUI_HEIGHT == 166, "GUI height is 166");
        check(EnchanterVisualTransform.TEXTURE_WIDTH == 256, "texture width is 256");
        check(EnchanterVisualTransform.TEXTURE_HEIGHT == 256, "texture height is 256");

        // Tool slot and wand slot
        check(EnchanterVisualTransform.SLOT_TOOL_X == 6 && EnchanterVisualTransform.SLOT_TOOL_Y == 6, "slot tool at (6,6)");
        check(EnchanterVisualTransform.SLOT_WAND_X == 6 && EnchanterVisualTransform.SLOT_WAND_Y == 31, "slot wand at (6,31)");

        // Candidates layout
        check(EnchanterVisualTransform.CANDIDATE_MAX == 16, "candidates max is 16");
        check(EnchanterVisualTransform.CANDIDATE_COLS == 8, "candidates cols is 8");
        check(EnchanterVisualTransform.CANDIDATE_BASE_X == 34, "candidate base X is 34");
        check(EnchanterVisualTransform.CANDIDATE_Y_SINGLE_ROW == 54, "candidate single row Y is 54");
        check(EnchanterVisualTransform.CANDIDATE_Y_ROW0_TWO_ROWS == 30, "candidate row0 two rows Y is 30 (54 - 24)");
        check(EnchanterVisualTransform.CANDIDATE_Y_ROW1_TWO_ROWS == 54, "candidate row1 two rows Y is 54");
        check(EnchanterVisualTransform.CANDIDATE_STEP_X == 16, "candidate step X is 16");
        check(EnchanterVisualTransform.CANDIDATE_STEP_Y == 24, "candidate step Y is 24");

        // Background plates for candidates
        check(EnchanterVisualTransform.PLATE_X == 30, "plate X is 30");
        check(EnchanterVisualTransform.PLATE_UPPER_Y == 26, "plate upper Y is 26");
        check(EnchanterVisualTransform.PLATE_LOWER_Y == 50, "plate lower Y is 50");
        check(EnchanterVisualTransform.PLATE_U == 0 && EnchanterVisualTransform.PLATE_V == 166, "plate UV is (0, 166)");
        check(EnchanterVisualTransform.PLATE_WIDTH == 147 && EnchanterVisualTransform.PLATE_HEIGHT == 24, "plate size is 147x24");

        // Start button
        check(EnchanterVisualTransform.START_X == 151 && EnchanterVisualTransform.START_Y == 33, "start button at (151, 33)");
        check(EnchanterVisualTransform.START_SIZE == 15, "start button size is 15x15");
        check(EnchanterVisualTransform.START_U == 176, "start button U is 176");
        check(EnchanterVisualTransform.START_V_IDLE == 24, "start button V idle is 24");
        check(EnchanterVisualTransform.START_V_WORKING == 39, "start button V working is 39");

        // Selected enchantments frames and buttons
        check(EnchanterVisualTransform.FRAMED_BASE_X == 180, "framed base X is 180 (176 + 4)");
        check(EnchanterVisualTransform.FRAMED_ROW_HEIGHT == 26, "framed row height is 26");
        check(EnchanterVisualTransform.FRAMED_UV_X == 176 && EnchanterVisualTransform.FRAMED_UV_Y == 0, "framed UV is (176, 0)");
        check(EnchanterVisualTransform.FRAMED_UV_SIZE == 24, "framed frame size is 24x24");
        check(EnchanterVisualTransform.FRAMED_TEXT_OFFSET_X == 26 && EnchanterVisualTransform.FRAMED_TEXT_OFFSET_Y == 8, "level text offset is (+26, +8)");

        // Level plus and minus buttons
        check(EnchanterVisualTransform.LEVEL_MINUS_OFFSET_X == 24, "minus button X offset is 24 (X=200)");
        check(EnchanterVisualTransform.LEVEL_PLUS_OFFSET_X == 31, "plus button X offset is 31 (X=207)");
        check(EnchanterVisualTransform.LEVEL_BUTTON_OFFSET_Y == -4, "level buttons Y offset is -4");
        check(EnchanterVisualTransform.LEVEL_BUTTON_SIZE == 7, "level button size is 7x7");
        check(EnchanterVisualTransform.LEVEL_MINUS_U == 218 && EnchanterVisualTransform.LEVEL_PLUS_U == 225, "minus U is 218, plus U is 225");
        check(EnchanterVisualTransform.LEVEL_BUTTON_V == 0, "level buttons V is 0");
    }

    public static void testAspectBarsAndPrimals() {
        check(EnchanterVisualTransform.BAR_BASE_X == 55, "bar base X is 55 (40 + 15)");
        check(EnchanterVisualTransform.BAR_STEP_X == 15, "bar step X is 15");
        check(EnchanterVisualTransform.BAR_Y_SINGLE_ROW == 50, "bar Y single row is 50");
        check(EnchanterVisualTransform.BAR_Y_TWO_ROWS == 26, "bar Y two rows is 26");
        check(EnchanterVisualTransform.BAR_WIDTH == 10, "bar width is 10");
        check(EnchanterVisualTransform.BAR_EMPTY_SIZE == 11, "empty bar size is 11");
        check(EnchanterVisualTransform.BAR_ACTIVE_SIZE == 59, "active bar size is 59");
        check(EnchanterVisualTransform.BAR_FILL_WIDTH == 8, "bar fill width is 8");
        check(EnchanterVisualTransform.BAR_FILL_MAX_HEIGHT == 48, "bar fill max height is 48");
        check(EnchanterVisualTransform.BAR_FILL_OFFSET_X == 1, "bar fill offset X is 1");
        check(EnchanterVisualTransform.BAR_FILL_U == 210, "bar fill U is 210");
        check(EnchanterVisualTransform.BAR_FRAME_U == 200 && EnchanterVisualTransform.BAR_FRAME_V == 0, "bar frame UV is (200, 0)");
        check(EnchanterVisualTransform.BAR_CAP_BOTTOM_V == 52, "bar cap bottom V is 52");

        // Test fill pixels calculation
        check(EnchanterVisualTransform.aspectBarFillPixels(0, 100) == 0, "0/100 -> 0 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(25, 100) == 12, "25/100 -> 12 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(50, 100) == 24, "50/100 -> 24 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(75, 100) == 36, "75/100 -> 36 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(100, 100) == 48, "100/100 -> 48 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(0, 0) == 0, "0/0 -> 0 px");
        check(EnchanterVisualTransform.aspectBarFillPixels(150, 100) == 72, "original unclamped fill math");

        // Verify Primal Aspects order and colors match 1.7.10 LibFeatures.PRIMAL_ASPECTS
        check(EnchanterBlockEntity.PRIMALS.size() == 6, "exactly 6 primal aspects");
        check(EnchanterBlockEntity.PRIMALS.get(0) == Aspects.AIR, "primal 0 is AIR");
        check(EnchanterBlockEntity.PRIMALS.get(1) == Aspects.EARTH, "primal 1 is EARTH");
        check(EnchanterBlockEntity.PRIMALS.get(2) == Aspects.FIRE, "primal 2 is FIRE");
        check(EnchanterBlockEntity.PRIMALS.get(3) == Aspects.WATER, "primal 3 is WATER");
        check(EnchanterBlockEntity.PRIMALS.get(4) == Aspects.ORDER, "primal 4 is ORDER");
        check(EnchanterBlockEntity.PRIMALS.get(5) == Aspects.ENTROPY, "primal 5 is ENTROPY");

        // Canonical TC4 aspect colors
        check(Aspects.AIR.getColor() == 16777086, "AIR color is 16777086 (0xffff7e)");
        check(Aspects.EARTH.getColor() == 5685248, "EARTH color is 5685248 (0x56c000)");
        check(Aspects.FIRE.getColor() == 16734721, "FIRE color is 16734721 (0xff5a01)");
        check(Aspects.WATER.getColor() == 3986684, "WATER color is 3986684 (0x3cd4fc)");
        check(Aspects.ORDER.getColor() == 14013676, "ORDER color is 14013676 (0xd5d4ec)");
        check(Aspects.ENTROPY.getColor() == 4210752, "ENTROPY color is 4210752 (0x404040)");
    }

    public static void testCatalogIntegrity() {
        check(EnchanterCatalog.ENTRIES.size() == 40, "catalog has 40 entries matching 1.7.10 EnchantmentManager");

        var prot = EnchanterCatalog.find(net.minecraft.resources.ResourceLocation.parse("minecraft:protection"));
        check(prot != null, "protection entry exists");
        check(prot.cost(1).getAmount(Aspects.EARTH) == 12, "protection base earth cost is 12");
        check(prot.cost(1).getAmount(Aspects.ENTROPY) == 8, "protection base entropy cost is 8");

        var ascent = EnchanterCatalog.find(net.minecraft.resources.ResourceLocation.parse("thaumictinkerer:ascent_boost"));
        check(ascent != null, "ascent_boost entry exists");
        check(ascent.cost(1).getAmount(Aspects.AIR) == 12, "ascent_boost level 1 air cost is 12");
        check(ascent.cost(4).getAmount(Aspects.AIR) == 72, "ascent_boost level 4 air cost is 72");

        var haste = EnchanterCatalog.find(net.minecraft.resources.ResourceLocation.parse("thaumcraft:haste"));
        check(haste != null && haste.legacyId() == 150, "haste legacy id is 150");

        var repair = EnchanterCatalog.find(net.minecraft.resources.ResourceLocation.parse("thaumcraft:repair"));
        check(repair != null && repair.legacyId() == 151, "repair legacy id is 151");
    }
}
