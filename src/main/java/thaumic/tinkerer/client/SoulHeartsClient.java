package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.SoulHearts;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT)
public final class SoulHeartsClient {
    private static final ResourceLocation TEXTURE=ThaumicTinkerer.id("textures/gui/soul_hearts.png");
    private static boolean visible() {
        var mc=Minecraft.getInstance();var player=mc.player;
        return SoulHearts.clientHealth()>0&&player!=null&&mc.getCameraEntity()==player&&!mc.options.hideGui&&mc.gameMode!=null&&mc.gameMode.canHurtPlayer()
                &&player.getVehicle()==null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void beforeFood(RenderGuiLayerEvent.Pre event) {
        if(event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)&&visible())
            // The regular food and later air rows move up ten pixels. Use the
            // engine's height budget instead of leaving a GL translation active.
            Minecraft.getInstance().gui.rightHeight+=10;
    }
    @SubscribeEvent public static void afterFood(RenderGuiLayerEvent.Post event) {
        if(!event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)||!visible())return;
        var graphics=event.getGuiGraphics();int hp=SoulHearts.clientHealth(),column=0;
        for(int i=0;i<hp;i++) {
            boolean half=i==hp-1&&hp%2!=0;
            if(half||i%2==0) {
                GuiTextures.blit(graphics,TEXTURE,graphics.guiWidth()/2+10+column*8,graphics.guiHeight()-39,
                        half?9:0,0,half?7:9,9,16,16);column++;
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){SoulHearts.receiveClient(0);}
    private SoulHeartsClient(){}
}
