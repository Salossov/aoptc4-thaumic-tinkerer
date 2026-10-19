package thaumic.tinkerer.kami;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import thaumic.tinkerer.item.TinkererFocusItem;

/** Source PlayerTracker login packet and focus-state cleanup; armor effects add their own cleanup. */
public final class TinkererPlayerLifecycle {
    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event)->event.registrar("1").playBidirectional(
                ToggleArmorPayload.TYPE,ToggleArmorPayload.CODEC,ToggleArmorPayload::handle));
        NeoForge.EVENT_BUS.addListener(TinkererPlayerLifecycle::login);
        NeoForge.EVENT_BUS.addListener(TinkererPlayerLifecycle::logout);
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            TinkererFocusItem.playerLogin(player.getGameProfile().getName());
            PacketDistributor.sendToPlayer(player,new ToggleArmorPayload(KamiArmorState.savedEnabled(player)));
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        TinkererFocusItem.playerLogout(event.getEntity().getGameProfile().getName());
    }
    private TinkererPlayerLifecycle() {}
}
