package thaumic.tinkerer.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import thaumic.tinkerer.ThaumicTinkerer;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class KamiArmorKeyRegistration {
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {event.register(KamiArmorClient.ARMOR_KEY);}
    private KamiArmorKeyRegistration() {}
}
