package thaumic.tinkerer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.warpgate.WarpGateBlockEntity;
import thaumic.tinkerer.kami.warpgate.WarpGateButtonPayload;
import thaumic.tinkerer.kami.warpgate.WarpGateContent;
import thaumic.tinkerer.kami.warpgate.WarpGateMenu;

/**
 * Modern 1.21.1 port of GuiWarpGate from Thaumic Tinkerer 2.5-1.7.10-164.
 * Inventory GUI with locked toggle checkbox button and "Allow incoming teleports" label.
 */
@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WarpGateScreen extends AbstractContainerScreen<WarpGateMenu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/warp_gate.png");

    public WarpGateScreen(WarpGateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WarpGateVisualHelper.GUI_IMAGE_WIDTH;
        this.imageHeight = WarpGateVisualHelper.GUI_IMAGE_HEIGHT;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        if (TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            event.register(WarpGateContent.MENU.get(), WarpGateScreen::new);
        }
    }

    @Override
    protected void init() {
        this.imageWidth = WarpGateVisualHelper.GUI_IMAGE_WIDTH;
        this.imageHeight = WarpGateVisualHelper.GUI_IMAGE_HEIGHT;
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Label "Allow incoming teleports" next to the lock checkbox
        // In 1.7.10: drawStringWithShadow(StatCollector.translateToLocal("ttmisc.lockedGate"), this.x + 20, this.y + 7, 10066329);
        graphics.drawString(this.font, Component.translatable("ttmisc.lockedGate"),
                WarpGateVisualHelper.LABEL_X_OFFSET, WarpGateVisualHelper.LABEL_Y_OFFSET,
                WarpGateVisualHelper.LABEL_COLOR, true);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Checkbox button at (5, 5)
        // Original 1.7.10 inversion quirk: when locked == true, draws (176, 0, 13, 13);
        // when locked == false, button is disabled / empty.
        WarpGateBlockEntity gate = this.menu.getBlockEntity();
        if (gate != null && gate.isLocked()) {
            graphics.blit(TEXTURE, this.leftPos + WarpGateVisualHelper.BUTTON_X_OFFSET,
                    this.topPos + WarpGateVisualHelper.BUTTON_Y_OFFSET,
                    176, 0,
                    WarpGateVisualHelper.BUTTON_SIZE, WarpGateVisualHelper.BUTTON_SIZE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int btnX = this.leftPos + WarpGateVisualHelper.BUTTON_X_OFFSET;
            int btnY = this.topPos + WarpGateVisualHelper.BUTTON_Y_OFFSET;
            if (mouseX >= btnX && mouseX < btnX + WarpGateVisualHelper.BUTTON_SIZE &&
                    mouseY >= btnY && mouseY < btnY + WarpGateVisualHelper.BUTTON_SIZE) {
                WarpGateBlockEntity gate = this.menu.getBlockEntity();
                if (gate != null) {
                    boolean newLocked = !gate.isLocked();
                    gate.setLocked(newLocked);
                    PacketDistributor.sendToServer(new WarpGateButtonPayload(gate.getBlockPos(), newLocked));
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
