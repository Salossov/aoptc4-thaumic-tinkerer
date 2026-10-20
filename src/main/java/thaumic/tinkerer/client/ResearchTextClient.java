package thaumic.tinkerer.client;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.api.client.ResearchTextFormatters;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Release TTResearchItem.getText and KamiResearchItem.getPrefix. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ResearchTextClient {
    private static final Set<String> KAMI = Set.of(
            "dimension_shards", "ichor", "ichor_cloth", "ichorium", "cap_ichor", "rod_ichorcloth",
            "ichorcloth_armor", "ichorcloth_helm_gem", "ichorcloth_chest_gem", "ichorcloth_legs_gem",
            "ichorcloth_boots_gem", "cat_amulet", "ichor_tools", "ichor_sword_gem", "ichor_pouch",
            "ichor_pick_gem", "ichor_shovel_gem", "ichor_axe_gem", "block_talisman", "protoclay",
            "placement_mirror", "focus_xp_drain", "warp_gate", "focus_recall", "focus_shadowbeam");

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ResearchTextFormatters.register(ThaumicTinkerer.ID, research -> {
            var description = Component.translatable("tc.research_text." + research.key());
            if (!TinkererConfig.TOOLTIP_INDICATORS.get()) return description;
            String path = research.key().substring(ThaumicTinkerer.ID.length() + 1);
            return Component.translatable(KAMI.contains(path) ? "thaumictinkerer.research.prefix.kami"
                    : "thaumictinkerer.research.prefix").append(description);
        });
    }

    private ResearchTextClient() {}
}
