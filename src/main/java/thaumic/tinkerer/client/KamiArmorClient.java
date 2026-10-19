package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.KamiArmorState;
import thaumic.tinkerer.kami.ToggleArmorPayload;
import thaumic.tinkerer.kami.ToolModeMessage;

/** Source client mode change and HUD. The equipment key gate is connected with advanced armor. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT)
public final class KamiArmorClient {
    private static final ToolModeMessage MESSAGE=new ToolModeMessage();

    public static void setStatus(boolean enabled) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.screen!=null)return;
        MESSAGE.set(Component.translatable(enabled?"ttmisc.enableAllArmor":"ttmisc.disableAllArmor").getString());
        KamiArmorState.setLocal(enabled);
        PacketDistributor.sendToServer(new ToggleArmorPayload(enabled));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {MESSAGE.clientTick();}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        if(!MESSAGE.visible())return;
        var minecraft=Minecraft.getInstance();var graphics=event.getGuiGraphics();
        graphics.drawString(minecraft.font,MESSAGE.text(),MESSAGE.x(graphics.guiWidth(),minecraft.font.width(MESSAGE.text())),
                MESSAGE.y(graphics.guiHeight()),MESSAGE.color(),true);
    }
    private KamiArmorClient() {}
}
