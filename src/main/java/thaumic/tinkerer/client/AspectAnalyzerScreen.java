package thaumic.tinkerer.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.AspectAnalyzerContent;
import thaumic.tinkerer.menu.AspectAnalyzerMenu;

/**
 * Original 176x166 screen and scanned-item display; computer queries intentionally do not require scanning.
 */
@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AspectAnalyzerScreen extends AbstractContainerScreen<AspectAnalyzerMenu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id(AnalyzerVisualTransform.TEXTURE_PATH);
    private Aspect hovered;

    public AspectAnalyzerScreen(AspectAnalyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = AnalyzerVisualTransform.GUI_WIDTH;
        this.imageHeight = AnalyzerVisualTransform.GUI_HEIGHT;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) {
            event.register(AspectAnalyzerContent.MENU.get(), AspectAnalyzerScreen::new);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int x, int y) {}

    @Override
    protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        hovered = null;
        GuiTextures.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (minecraft == null || minecraft.level == null) return;
        var stack = menu.getSlot(0).getItem();
        if (stack.isEmpty() || !AspectApi.scanned(stack)) return;

        int index = 0;
        for (var entry : AspectAnalyzerMenu.sortedAmounts(AspectApi.aspects(minecraft.level, stack))) {
            var aspect = Aspect.getAspect(entry.getKey());
            if (aspect == null) continue;
            int x = leftPos + AnalyzerVisualTransform.tagX(index++);
            int y = topPos + AnalyzerVisualTransform.tagY();
            drawTag(graphics, aspect, entry.getValue(), x, y);
            if (AnalyzerVisualTransform.isHovered(x, y, mouseX, mouseY)) {
                hovered = aspect;
            }
        }
    }

    private void drawTag(GuiGraphics graphics, Aspect aspect, int amount, int x, int y) {
        GuiTextures.tinted(
            graphics,
            ResourceLocation.parse(aspect.texture()),
            x,
            y,
            AnalyzerVisualTransform.ASPECT_SIZE,
            AnalyzerVisualTransform.ASPECT_SIZE,
            0,
            0,
            32,
            32,
            32,
            32,
            aspect.getColor()
        );
        if (amount <= 0) return;

        graphics.pose().pushPose();
        graphics.pose().scale(AnalyzerVisualTransform.TEXT_SCALE, AnalyzerVisualTransform.TEXT_SCALE, AnalyzerVisualTransform.TEXT_SCALE);
        String text = AnalyzerVisualTransform.amountText(amount);
        int tx = AnalyzerVisualTransform.textX(x, font.width(text));
        int ty = AnalyzerVisualTransform.textY(y, font.lineHeight);
        for (var shift : AnalyzerVisualTransform.OUTLINE_SHIFTS) {
            graphics.drawString(font, text, tx + shift[0], ty + shift[1], AnalyzerVisualTransform.OUTLINE_COLOR, false);
        }
        graphics.drawString(font, text, tx, ty, AnalyzerVisualTransform.TEXT_COLOR, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float partial) {
        super.render(graphics, x, y, partial);
        renderTooltip(graphics, x, y);
        if (hovered != null) {
            var name = hovered.translationKey() == null
                ? Component.literal(hovered.getName())
                : Component.translatable(hovered.translationKey());
            graphics.renderComponentTooltip(
                font,
                List.of(
                    name.withStyle(ChatFormatting.AQUA),
                    Component.translatable("tc.aspect." + hovered.getTag()).withStyle(ChatFormatting.GRAY)
                ),
                x,
                y
            );
        }
    }
}
