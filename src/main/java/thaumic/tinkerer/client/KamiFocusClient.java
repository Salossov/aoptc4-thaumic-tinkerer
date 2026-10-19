package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.focus.KamiFocusContent;
import thaumic.tinkerer.kami.focus.XpDrainVisual;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class KamiFocusClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->XpDrainVisual.clock(()->{var player=Minecraft.getInstance().player;return player==null?null:player.tickCount;}));
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get())return;
        event.register((stack,tint)->0xFF000000|KamiFocusContent.FOCUS_XP_DRAIN.get().getFocusColor(stack),KamiFocusContent.FOCUS_XP_DRAIN.get());
    }
    private KamiFocusClient(){}
}
