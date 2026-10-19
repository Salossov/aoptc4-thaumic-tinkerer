package thaumic.tinkerer.kami;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumic.tinkerer.ThaumicTinkerer;

/** The original packet supplies a boolean, rather than requesting a blind toggle. */
public record ToggleArmorPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleArmorPayload> TYPE=new Type<>(ThaumicTinkerer.id("toggle_armor"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ToggleArmorPayload> CODEC=StreamCodec.of(
            (buffer,payload)->buffer.writeBoolean(payload.enabled()),buffer->new ToggleArmorPayload(buffer.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
    public static void handle(ToggleArmorPayload payload,IPayloadContext context) {
        context.enqueueWork(()->{
            var player=context.player();if(player==null)return;
            if(context.flow().isClientbound())KamiArmorState.receiveClient(player,payload.enabled());
            else KamiArmorState.save(player,payload.enabled());
        });
    }
}
