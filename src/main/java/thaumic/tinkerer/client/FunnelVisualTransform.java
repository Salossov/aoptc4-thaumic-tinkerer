package thaumic.tinkerer.client;

import net.minecraft.resources.ResourceLocation;

/**
 * Exact source-extracted transformations, coordinates, and visual constants
 * from Thaumic Tinkerer 2.5-1.7.10-164 BlockFunnel, TileFunnel, and RenderTileFunnel.
 */
public final class FunnelVisualTransform {
    private FunnelVisualTransform() {}

    // Block dimensions & collision
    public static final float BLOCK_HEIGHT = 0.125F; // 2 pixels
    public static final float[] BOUNDS = {0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F};

    // Textures
    public static final ResourceLocation TOP_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/block/funnel_top.png");
    public static final ResourceLocation SIDE_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumictinkerer", "textures/block/funnel_side.png");

    // Jar rendering in 1.7.10 RenderTileFunnel:
    // Translated: (d0 + 0.5, d1 + 0.365, d2 + 0.5)
    // ItemJarFilledRenderer entity shift: (-0.5, -0.25, -0.5)
    // Base Y: 0.365 - 0.25 = 0.115 (sunk 0.01 into funnel plate for stable seating)
    // Center Y: 0.115 + 0.5 = 0.615 (for standard 1x1x1 jar block model)
    public static final double RENDER_BASE_Y = 0.115;
    public static final double RENDER_CENTER_Y = 0.615;
    public static final double RENDER_CENTER_X = 0.5;
    public static final double RENDER_CENTER_Z = 0.5;

}
