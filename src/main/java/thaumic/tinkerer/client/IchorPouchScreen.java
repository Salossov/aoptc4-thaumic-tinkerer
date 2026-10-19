package thaumic.tinkerer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.pouch.IchorPouchContent;
import thaumic.tinkerer.menu.IchorPouchMenu;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IchorPouchScreen extends AbstractContainerScreen<IchorPouchMenu> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/gui/ichor_pouch.png");

    public IchorPouchScreen(IchorPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        if(thaumic.tinkerer.TinkererConfig.KAMI_FORCE_ENABLED.get())event.register(IchorPouchContent.MENU.get(),IchorPouchScreen::new);
    }

    @Override
    protected void init() {
        this.imageWidth = 256;
        this.imageHeight = 256;
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Original 1.7.10 Ichor Pouch did not render labels; 117 slots occupy (12, 8) to (245, 169)
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiTextures.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (this.minecraft != null && this.minecraft.player != null) {
            int selected = this.menu.getPouchSlot();
            if (selected >= 0 && selected < 9) {
                if (this.minecraft.options.keyHotbarSlots[selected].matches(keyCode, scanCode)) {
                    return false;
                }
            }
        }
        return super.checkHotbarKeyPressed(keyCode, scanCode);
    }
}
