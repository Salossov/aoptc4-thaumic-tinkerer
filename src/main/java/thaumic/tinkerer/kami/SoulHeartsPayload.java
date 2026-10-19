package thaumic.tinkerer.kami;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumic.tinkerer.ThaumicTinkerer;

public record SoulHeartsPayload(int health) implements CustomPacketPayload {
    public static final Type<SoulHeartsPayload> TYPE=new Type<>(ThaumicTinkerer.id("soul_hearts"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SoulHeartsPayload> CODEC=StreamCodec.of(
            (buffer,payload)->buffer.writeInt(payload.health()),buffer->new SoulHeartsPayload(buffer.readInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(SoulHeartsPayload payload,IPayloadContext context){context.enqueueWork(()->SoulHearts.receiveClient(payload.health()));}
}
