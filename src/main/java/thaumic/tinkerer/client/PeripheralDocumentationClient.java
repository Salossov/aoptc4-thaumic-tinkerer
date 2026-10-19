package thaumic.tinkerer.client;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import thaumcraft.api.client.BookResearchKeyEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** Release GuiResearchPeripheral behavior, using the public book hook and native link dialog. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PeripheralDocumentationClient {
    public static final String URL_KEY="thaumictinkerer.documentation.peripherals";
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event){BookResearchKeyEvent.BUS.addListener(PeripheralDocumentationClient::keyPressed);}
    private static void keyPressed(BookResearchKeyEvent event){
        if(!handles(event.research().key(),event.keyCode()))return;
        event.setCanceled(true);
        ConfirmLinkScreen.confirmLinkNow(event.screen(),I18n.get(URL_KEY),false);
    }
    public static boolean handles(String research,int key){
        return key==GLFW.GLFW_KEY_ENTER&&("thaumictinkerer:peripherals".equals(research)||"thaumictinkerer:golem_connector".equals(research));
    }
    private PeripheralDocumentationClient(){}
}
