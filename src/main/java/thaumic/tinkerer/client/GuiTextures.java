package thaumic.tinkerer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Immediate GUI textures must restore blending after pending font and item batches. */
final class GuiTextures {
    private GuiTextures() {}

    static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                     int u, int v, int width, int height) {
        tinted(graphics, texture, x, y, width, height, u, v, width, height, 256, 256, 0xffffff);
    }

    static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                     float u, float v, int width, int height, int textureWidth, int textureHeight) {
        tinted(graphics, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight, 0xffffff);
    }

    static void tinted(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                       int width, int height, float u, float v, int regionWidth, int regionHeight,
                       int textureWidth, int textureHeight, int color) {
        if (width <= 0 || height <= 0) return;
        // Flush first: a pending text batch may disable blending during endBatch().
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
        try {
            graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }
}
