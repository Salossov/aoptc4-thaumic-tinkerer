package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumic.tinkerer.ThaumicTinkerer;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT)
public final class BedrockFlickerClient {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var minecraft=Minecraft.getInstance();
        if(minecraft.level!=null&&!minecraft.isPaused())BedrockDimensionClient.FLICKER.tick(Math::random);
    }
    private BedrockFlickerClient(){}
}
