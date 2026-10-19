package thaumic.tinkerer.verification;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumic.tinkerer.client.CropVisualTransform;

/**
 * Independent verification test for Infused Crops & Seeds client visual rules,
 * 1.7.10 bytecode geometry, growth stages, aspect colors, and missing aspect safety.
 */
public final class CropVisualRulesTest {
    private static int checks = 0;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check failed: " + label);
        }
    }

    public static void main(String[] args) {
        checks = 0;
        testStageAgeIndices();
        testAspectTextureMapping();
        testMissingAspectSafety();
        testCropColorTinting();
        testCropWorldGeometry();
        testCropQuadWindingOrder();
        testSeedItemPropertyMapping();
        testTexturePaths();
        System.out.println("TT_CROP_VISUAL_RULES_PASS checks=" + checks);
    }

    public static void testStageAgeIndices() {
        int[] expectedStages = {0, 0, 1, 1, 2, 2, 2, 3};
        for (int age = 0; age <= 7; age++) {
            int stage = CropVisualTransform.cropTextureAge(age);
            check(stage == expectedStages[age], "age " + age + " maps to stage " + expectedStages[age] + ", got " + stage);
        }
        // Age 6 quirk verification: in 1.7.10 meta 6 was rewritten to meta 5 before >> 1
        check(CropVisualTransform.cropTextureAge(6) == 2, "age 6 quirk maps to stage 2");
        check(CropVisualTransform.cropTextureAge(7) == 3, "mature age 7 maps to stage 3");
        check(CropVisualTransform.cropTextureAge(8) == 3, "out-of-bounds age >= 7 clamps to stage 3");
    }

    public static void testAspectTextureMapping() {
        check(CropVisualTransform.aspectTextureIndex(Aspects.AIR) == 0, "AIR maps to texture 0 (aer)");
        check(CropVisualTransform.aspectTextureIndex(Aspects.FIRE) == 1, "FIRE maps to texture 1 (ignis)");
        check(CropVisualTransform.aspectTextureIndex(Aspects.WATER) == 2, "WATER maps to texture 2 (aqua)");
        check(CropVisualTransform.aspectTextureIndex(Aspects.EARTH) == 3, "EARTH maps to texture 3 (terra)");
        check(CropVisualTransform.aspectTextureIndex(Aspects.ORDER) == 4, "ORDER maps to texture 4 (ordo)");
        check(CropVisualTransform.aspectTextureIndex(Aspects.ENTROPY) == 5, "ENTROPY maps to texture 5 (perditio)");

        // Non-primal compound aspects
        Aspect[] compounds = {Aspects.LIGHT, Aspects.MOTION, Aspects.HARVEST, Aspects.PLANT, Aspects.ENERGY};
        for (Aspect compound : compounds) {
            check(CropVisualTransform.aspectTextureIndex(compound) == 6, compound.getTag() + " maps to texture 6 (generic)");
        }
    }

    public static void testMissingAspectSafety() {
        // Strict requirement: null aspect must NEVER map to 0 (AIR)
        int nullIndex = CropVisualTransform.aspectTextureIndex(null);
        check(nullIndex == 6, "null aspect safely maps to texture 6 (generic), not primal 0");
        check(nullIndex != 0, "null aspect does NOT map to air");

        float nullProperty = CropVisualTransform.seedAspectProperty(null);
        check(nullProperty == 6.0F, "seed property for null aspect returns 6.0F (generic model override)");
        check(nullProperty != 0.0F, "seed property for null aspect is not 0.0F");
    }

    public static void testCropColorTinting() {
        // 1.7.10 RenderInfusedCrops: only non-primal aspects receive color tint
        check(CropVisualTransform.cropColor(null) == 0xFFFFFF, "null aspect crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.AIR) == 0xFFFFFF, "AIR crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.FIRE) == 0xFFFFFF, "FIRE crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.WATER) == 0xFFFFFF, "WATER crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.EARTH) == 0xFFFFFF, "EARTH crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.ORDER) == 0xFFFFFF, "ORDER crop color is white (no tint)");
        check(CropVisualTransform.cropColor(Aspects.ENTROPY) == 0xFFFFFF, "ENTROPY crop color is white (no tint)");

        // Compound aspects: must retain canonical aspect color
        Aspect[] compounds = {Aspects.LIGHT, Aspects.MOTION, Aspects.HARVEST, Aspects.ENERGY, Aspects.VOID};
        for (Aspect compound : compounds) {
            int expectedColor = compound.getColor() & 0xFFFFFF;
            int actualColor = CropVisualTransform.cropColor(compound);
            check(actualColor == expectedColor, compound.getTag() + " crop tint matches Aspect.getColor(): 0x" + Integer.toHexString(expectedColor));

            float r = CropVisualTransform.cropRed(actualColor);
            float g = CropVisualTransform.cropGreen(actualColor);
            float b = CropVisualTransform.cropBlue(actualColor);
            check(r >= 0.0F && r <= 1.0F, compound.getTag() + " red component in [0..1]");
            check(g >= 0.0F && g <= 1.0F, compound.getTag() + " green component in [0..1]");
            check(b >= 0.0F && b <= 1.0F, compound.getTag() + " blue component in [0..1]");
        }
    }

    public static void testCropWorldGeometry() {
        check(Math.abs(CropVisualTransform.CROP_Y_OFFSET - (-0.0625)) < 1e-6, "Y offset matches -1/16 block (-0.0625)");
        check(Math.abs(CropVisualTransform.CROP_Y_OFFSET_F - (-0.0625F)) < 1e-6, "float Y offset matches -0.0625F");
        check(CropVisualTransform.CROP_QUADS.length == 8, "geometry contains exactly 8 crop quads");

        for (int q = 0; q < CropVisualTransform.CROP_QUADS.length; q++) {
            float[][] quad = CropVisualTransform.CROP_QUADS[q];
            check(quad.length == 4, "quad " + q + " has 4 vertices");
            for (int v = 0; v < 4; v++) {
                float[] pt = quad[v];
                check(pt.length == 5, "vertex " + v + " in quad " + q + " has 5 floats [x,y,z,u,v]");
                check(pt[0] >= 0.0F && pt[0] <= 1.0F, "X in [0..1]");
                check(pt[1] >= 0.0F && pt[1] <= 1.0F, "Y in [0..1]");
                check(pt[2] >= 0.0F && pt[2] <= 1.0F, "Z in [0..1]");
                check(pt[3] == 0.0F || pt[3] == 1.0F, "U is 0 or 1");
                check(pt[4] == 0.0F || pt[4] == 1.0F, "V is 0 or 1");
            }
        }

        // Verify plane positions: 2 planes at X=0.25, 2 at X=0.75, 2 at Z=0.25, 2 at Z=0.75
        check(CropVisualTransform.CROP_QUADS[0][0][0] == 0.25F && CropVisualTransform.CROP_QUADS[1][0][0] == 0.25F, "quads 0 & 1 at X=0.25");
        check(CropVisualTransform.CROP_QUADS[2][0][0] == 0.75F && CropVisualTransform.CROP_QUADS[3][0][0] == 0.75F, "quads 2 & 3 at X=0.75");
        check(CropVisualTransform.CROP_QUADS[4][0][2] == 0.25F && CropVisualTransform.CROP_QUADS[5][0][2] == 0.25F, "quads 4 & 5 at Z=0.25");
        check(CropVisualTransform.CROP_QUADS[6][0][2] == 0.75F && CropVisualTransform.CROP_QUADS[7][0][2] == 0.75F, "quads 6 & 7 at Z=0.75");
    }

    public static void testCropQuadWindingOrder() {
        // Each quad must have CCW winding order when viewed from its outside face
        // Normal vector via cross product (P2 - P1) x (P3 - P2)
        for (int q = 0; q < CropVisualTransform.CROP_QUADS.length; q++) {
            float[][] quad = CropVisualTransform.CROP_QUADS[q];
            float[] p1 = quad[0];
            float[] p2 = quad[1];
            float[] p3 = quad[2];

            float v1x = p2[0] - p1[0], v1y = p2[1] - p1[1], v1z = p2[2] - p1[2];
            float v2x = p3[0] - p2[0], v2y = p3[1] - p2[1], v2z = p3[2] - p2[2];

            // cross product
            float nx = v1y * v2z - v1z * v2y;
            float ny = v1z * v2x - v1x * v2z;
            float nz = v1x * v2y - v1y * v2x;

            if (q == 0) check(nx < -0.9F, "quad 0 normal faces -X");
            if (q == 1) check(nx > 0.9F, "quad 1 normal faces +X");
            if (q == 2) check(nx > 0.9F, "quad 2 normal faces +X");
            if (q == 3) check(nx < -0.9F, "quad 3 normal faces -X");
            if (q == 4) check(nz > 0.9F, "quad 4 normal faces +Z");
            if (q == 5) check(nz < -0.9F, "quad 5 normal faces -Z");
            if (q == 6) check(nz < -0.9F, "quad 6 normal faces -Z");
            if (q == 7) check(nz > 0.9F, "quad 7 normal faces +Z");
        }
    }

    public static void testSeedItemPropertyMapping() {
        check(CropVisualTransform.seedAspectProperty(Aspects.AIR) == 0.0F, "AIR seeds -> 0.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.FIRE) == 1.0F, "FIRE seeds -> 1.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.WATER) == 2.0F, "WATER seeds -> 2.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.EARTH) == 3.0F, "EARTH seeds -> 3.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.ORDER) == 4.0F, "ORDER seeds -> 4.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.ENTROPY) == 5.0F, "ENTROPY seeds -> 5.0F");
        check(CropVisualTransform.seedAspectProperty(Aspects.LIGHT) == 6.0F, "compound seeds -> 6.0F");
        check(CropVisualTransform.seedAspectProperty(null) == 6.0F, "null aspect seeds -> 6.0F");
    }



    public static void testTexturePaths() {
        check(CropVisualTransform.TEXTURE_NAMES.length == 7, "exactly 7 texture categories");
        String[] expectedNames = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "generic"};
        for (int i = 0; i < 7; i++) {
            check(CropVisualTransform.TEXTURE_NAMES[i].equals(expectedNames[i]), "texture name " + i + " is " + expectedNames[i]);
            var seedLoc = CropVisualTransform.seedTextureLocation(i);
            check(seedLoc.getPath().equals("textures/item/seed_" + expectedNames[i] + ".png"), "seed texture path: " + seedLoc);
            for (int s = 0; s < 4; s++) {
                var cropLoc = CropVisualTransform.cropTextureLocation(i, s);
                check(cropLoc.getPath().equals("textures/block/crop_" + expectedNames[i] + "_" + s + ".png"), "crop texture path: " + cropLoc);
            }
        }
    }

    private CropVisualRulesTest() {}
}
