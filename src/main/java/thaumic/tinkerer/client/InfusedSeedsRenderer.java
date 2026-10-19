package thaumic.tinkerer.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.crop.InfusedContent;
import thaumic.tinkerer.crop.InfusedSeedsItem;

/**
 * Client registration and item visual properties for Infused Seeds.
 * Port of 1.7.10 RenderGenericSeeds item renderer.
 */
@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InfusedSeedsRenderer {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
            InfusedContent.SEEDS.get(),
            ThaumicTinkerer.id(CropVisualTransform.SEED_ASPECT_PROPERTY),
            (stack, level, entity, seed) -> {
                var aspect = InfusedSeedsItem.aspect(stack);
                return CropVisualTransform.seedAspectProperty(aspect);
            }
        ));
    }

    private InfusedSeedsRenderer() {}
}
