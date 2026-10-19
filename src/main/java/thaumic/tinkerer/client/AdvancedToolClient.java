package thaumic.tinkerer.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.tool.AdvancedToolMode;
import thaumic.tinkerer.kami.tool.IchorTools;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AdvancedToolClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        if(TinkererConfig.KAMI_FORCE_ENABLED.get())event.enqueueWork(()->{
            for(var item:IchorTools.awakened())ItemProperties.register(
                    item.get(),ThaumicTinkerer.id("variant"),(stack,level,entity,seed)->AdvancedToolMode.get(stack));
        });
    }
    private AdvancedToolClient(){}
}
