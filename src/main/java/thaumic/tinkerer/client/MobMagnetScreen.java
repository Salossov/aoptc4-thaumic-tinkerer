package thaumic.tinkerer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.SoulMouldItem;
import thaumic.tinkerer.menu.MobMagnetMenu;

public final class MobMagnetScreen extends AbstractContainerScreen<MobMagnetMenu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/mob_magnet.png");
    public MobMagnetScreen(MobMagnetMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void init() {
        super.init();
        addRenderableWidget(new Radio(0, leftPos + 100, topPos + 28));
        addRenderableWidget(new Radio(1, leftPos + 100, topPos + 48));
    }
    private final class Radio extends AbstractButton {
        private final int button;
        Radio(int button, int x, int y) { super(x, y, 13, 13, Component.translatable("gui.thaumictinkerer." + (button == 0 ? "adult" : "child"))); this.button = button; }
        @Override public void onPress() {
            menu.setData(0, button == 0 ? 1 : 0);
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int x, int y, float partial) {
            int v = (menu.searchesAdults() == (button == 0)) ? MagnetVisualTransform.RADIO_TEX_V_SELECTED : MagnetVisualTransform.RADIO_TEX_V_UNSELECTED;
            GuiTextures.blit(graphics, TEXTURE, getX(), getY(), MagnetVisualTransform.RADIO_TEX_U, v, MagnetVisualTransform.RADIO_WIDTH, MagnetVisualTransform.RADIO_HEIGHT);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) { }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int x, int y) {
        GuiTextures.blit(graphics,TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        var filter = menu.getSlot(0).getItem(); Component label;
        if (filter.isEmpty()) label = Component.translatable("gui.thaumictinkerer.all");
        else {
            String pattern = SoulMouldItem.getPatternName(filter); var id = ResourceLocation.tryParse(pattern);
            label = pattern.isEmpty() ? Component.translatable("gui.thaumictinkerer.none") : id == null ? Component.translatable("entity." + pattern + ".name")
                    : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type -> type.getDescription()).orElse(Component.translatable("entity." + pattern + ".name"));
        }
        graphics.drawString(font, label, leftPos + MagnetVisualTransform.filterLabelX(imageWidth, font.width(label)), topPos + MagnetVisualTransform.LABEL_FILTER_Y, MagnetVisualTransform.LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.thaumictinkerer.adult"), leftPos + MagnetVisualTransform.LABEL_ADULT_X, topPos + MagnetVisualTransform.LABEL_ADULT_Y, MagnetVisualTransform.LABEL_COLOR, false);
        graphics.drawString(font, Component.translatable("gui.thaumictinkerer.child"), leftPos + MagnetVisualTransform.LABEL_CHILD_X, topPos + MagnetVisualTransform.LABEL_CHILD_Y, MagnetVisualTransform.LABEL_COLOR, false);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) { super.render(graphics, x, y, partial); renderTooltip(graphics, x, y); }
}
