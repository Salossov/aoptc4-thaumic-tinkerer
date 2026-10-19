package thaumic.tinkerer.client;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;

/**
 * Source-extracted geometry, UV mappings, aspect color rules, growth stages,
 * and 1.7.10 RenderGenericSeeds / RenderInfusedCrops visual constants.
 */
public final class CropVisualTransform {
    public static final double CROP_Y_OFFSET = -0.0625;
    public static final float CROP_Y_OFFSET_F = -0.0625F;

    public static final String[] TEXTURE_NAMES = {
        "aer", "ignis", "aqua", "terra", "ordo", "perditio", "generic"
    };

    public static final int PRIMAL_COUNT = 6;
    public static final int SEED_GENERIC_INDEX = 6;
    public static final String SEED_ASPECT_PROPERTY = "seed_aspect";

    public static final int[] AGE_INDICES = {0, 0, 1, 1, 2, 2, 2, 3};
    public static final int STAGE_COUNT = 4;
    public static final int MAX_AGE = 7;
    public static final int QUAD_COUNT = 8;

    /**
     * Exact 8 quads of the 1.7.10 RenderBlocks.renderBlockCropsImpl geometry.
     * Each quad has 4 vertices with [x, y, z, u, v].
     * Coordinates are defined relative to the block origin before Y translation.
     */
    public static final float[][][] CROP_QUADS = {
        // Quad 0: X = 0.25, facing -X
        {
            {0.25F, 1.0F, 0.0F, 0.0F, 0.0F},
            {0.25F, 0.0F, 0.0F, 0.0F, 1.0F},
            {0.25F, 0.0F, 1.0F, 1.0F, 1.0F},
            {0.25F, 1.0F, 1.0F, 1.0F, 0.0F}
        },
        // Quad 1: X = 0.25, facing +X
        {
            {0.25F, 1.0F, 1.0F, 0.0F, 0.0F},
            {0.25F, 0.0F, 1.0F, 0.0F, 1.0F},
            {0.25F, 0.0F, 0.0F, 1.0F, 1.0F},
            {0.25F, 1.0F, 0.0F, 1.0F, 0.0F}
        },
        // Quad 2: X = 0.75, facing -X
        {
            {0.75F, 1.0F, 1.0F, 0.0F, 0.0F},
            {0.75F, 0.0F, 1.0F, 0.0F, 1.0F},
            {0.75F, 0.0F, 0.0F, 1.0F, 1.0F},
            {0.75F, 1.0F, 0.0F, 1.0F, 0.0F}
        },
        // Quad 3: X = 0.75, facing +X
        {
            {0.75F, 1.0F, 0.0F, 0.0F, 0.0F},
            {0.75F, 0.0F, 0.0F, 0.0F, 1.0F},
            {0.75F, 0.0F, 1.0F, 1.0F, 1.0F},
            {0.75F, 1.0F, 1.0F, 1.0F, 0.0F}
        },
        // Quad 4: Z = 0.25, facing -Z
        {
            {0.0F, 1.0F, 0.25F, 0.0F, 0.0F},
            {0.0F, 0.0F, 0.25F, 0.0F, 1.0F},
            {1.0F, 0.0F, 0.25F, 1.0F, 1.0F},
            {1.0F, 1.0F, 0.25F, 1.0F, 0.0F}
        },
        // Quad 5: Z = 0.25, facing +Z
        {
            {1.0F, 1.0F, 0.25F, 0.0F, 0.0F},
            {1.0F, 0.0F, 0.25F, 0.0F, 1.0F},
            {0.0F, 0.0F, 0.25F, 1.0F, 1.0F},
            {0.0F, 1.0F, 0.25F, 1.0F, 0.0F}
        },
        // Quad 6: Z = 0.75, facing -Z
        {
            {1.0F, 1.0F, 0.75F, 0.0F, 0.0F},
            {1.0F, 0.0F, 0.75F, 0.0F, 1.0F},
            {0.0F, 0.0F, 0.75F, 1.0F, 1.0F},
            {0.0F, 1.0F, 0.75F, 1.0F, 0.0F}
        },
        // Quad 7: Z = 0.75, facing +Z
        {
            {0.0F, 1.0F, 0.75F, 0.0F, 0.0F},
            {0.0F, 0.0F, 0.75F, 0.0F, 1.0F},
            {1.0F, 0.0F, 0.75F, 1.0F, 1.0F},
            {1.0F, 1.0F, 0.75F, 1.0F, 0.0F}
        }
    };

    /**
     * Maps crop block growth age [0..7] to texture stage [0..3].
     * Matches 1.7.10 BlockInfusedGrain.getIcon meta handling:
     * meta < 7 ? (meta == 6 ? 5 : meta) >> 1 : 3.
     */
    public static int cropTextureAge(int age) {
        return age < 7 ? (age == 6 ? 5 : age) >> 1 : 3;
    }

    /**
     * Maps aspect to texture index [0..6].
     * 0: aer, 1: ignis, 2: aqua, 3: terra, 4: ordo, 5: perditio, 6: generic.
     * Missing or null aspect safely maps to 6 (generic), avoiding accidental primal rendering.
     */
    public static int aspectTextureIndex(Aspect aspect) {
        if (aspect == Aspects.AIR) return 0;
        if (aspect == Aspects.FIRE) return 1;
        if (aspect == Aspects.WATER) return 2;
        if (aspect == Aspects.EARTH) return 3;
        if (aspect == Aspects.ORDER) return 4;
        if (aspect == Aspects.ENTROPY) return 5;
        return SEED_GENERIC_INDEX;
    }

    /**
     * Determines the in-world tint color for a crop block.
     * In 1.7.10 RenderInfusedCrops:
     * if (aspect != null && !aspect.isPrimal()) -> aspect.getColor(); else -> 0xFFFFFF.
     */
    public static int cropColor(Aspect aspect) {
        return aspect != null && !aspect.isPrimal() ? (aspect.getColor() & 0xFFFFFF) : 0xFFFFFF;
    }

    public static float cropRed(int color) {
        return ((color >> 16) & 0xFF) / 255.0F;
    }

    public static float cropGreen(int color) {
        return ((color >> 8) & 0xFF) / 255.0F;
    }

    public static float cropBlue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    /**
     * Property value for ItemProperties "seed_aspect".
     * Null aspect safely returns 6.0F (generic).
     */
    public static float seedAspectProperty(Aspect aspect) {
        return (float) aspectTextureIndex(aspect);
    }

    public static ResourceLocation cropTextureLocation(int textureIndex, int stage) {
        int safeTex = (textureIndex >= 0 && textureIndex < TEXTURE_NAMES.length) ? textureIndex : SEED_GENERIC_INDEX;
        int safeStage = Math.max(0, Math.min(stage, STAGE_COUNT - 1));
        return ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/block/crop_" + TEXTURE_NAMES[safeTex] + "_" + safeStage + ".png");
    }

    public static ResourceLocation seedTextureLocation(int textureIndex) {
        int safeTex = (textureIndex >= 0 && textureIndex < TEXTURE_NAMES.length) ? textureIndex : SEED_GENERIC_INDEX;
        return ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/item/seed_" + TEXTURE_NAMES[safeTex] + ".png");
    }



    private CropVisualTransform() {}
}
