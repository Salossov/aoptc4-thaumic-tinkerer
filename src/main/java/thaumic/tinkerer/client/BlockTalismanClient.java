package thaumic.tinkerer.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.blocktalisman.BlockTalismanContent;
import thaumic.tinkerer.kami.blocktalisman.BlockTalismanItem;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlockTalismanClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            event.enqueueWork(() -> ItemProperties.register(
                BlockTalismanContent.TALISMAN.get(),
                ThaumicTinkerer.id("enabled"),
                (stack, level, entity, seed) -> BlockTalismanItem.isEnabled(stack) ? 1.0F : 0.0F
            ));
        }
    }

    private BlockTalismanClient() {}
}
