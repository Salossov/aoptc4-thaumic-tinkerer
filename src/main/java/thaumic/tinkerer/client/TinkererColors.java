package thaumic.tinkerer.client;

import java.awt.Color;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumic.tinkerer.ThaumicTinkerer;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TinkererColors {
    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        // Original ItemSpellCloth.func_82790_a: hue .75, saturation fades from .5 to 0.
        event.register((stack, layer) -> Color.HSBtoRGB(.75f,
                (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage() * .5f, 1),
                ThaumicTinkerer.SPELL_CLOTH.get());
    }

    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.renderer.item.ItemProperties.register(thaumic.tinkerer.summon.SummonContent.BLOOD_SWORD.get(), ThaumicTinkerer.id("enabled"),
                    (stack, level, entity, seed) -> thaumic.tinkerer.summon.BloodSwordItem.active(stack) ? 1 : 0);
            net.minecraft.client.renderer.item.ItemProperties.register(thaumic.tinkerer.summon.SummonContent.MOB_ASPECT.get(), ThaumicTinkerer.id("variant"),
                    (stack, level, entity, seed) -> thaumic.tinkerer.summon.MobAspectItem.variant(stack));
            net.minecraft.client.renderer.item.ItemProperties.register(ThaumicTinkerer.XP_TALISMAN.get(), ThaumicTinkerer.id("enabled"),
                    (stack, level, entity, seed) -> thaumic.tinkerer.item.XpTalismanItem.absorbing(stack) ? 1 : 0);
            net.minecraft.client.renderer.item.ItemProperties.register(ThaumicTinkerer.CLEANSING_TALISMAN.get(), ThaumicTinkerer.id("enabled"),
                    (stack, level, entity, seed) -> thaumic.tinkerer.item.CleansingTalismanItem.enabled(stack) ? 1 : 0);
        });
    }

    private TinkererColors() {}
}
