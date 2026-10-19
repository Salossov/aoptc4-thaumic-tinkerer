package thaumic.tinkerer.verification;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** A separate dev-only datapack, so replace:false is tested through the real pack merge. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD)
public final class MaterialVerificationPack {
    @SubscribeEvent public static void packs(AddPackFindersEvent event){
        event.addPackFinders(ThaumicTinkerer.id("tt_verification_materials"),PackType.SERVER_DATA,Component.literal("TT material verification"),PackSource.BUILT_IN,true,Pack.Position.TOP);
    }
    private MaterialVerificationPack(){}
}
