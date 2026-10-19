package thaumic.tinkerer.kami.warpgate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumic.tinkerer.ThaumicTinkerer;

public record WarpGateButtonPayload(BlockPos pos, boolean locked) implements CustomPacketPayload {
    public static final Type<WarpGateButtonPayload> TYPE = new Type<>(ThaumicTinkerer.id("warp_gate_button"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpGateButtonPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarpGateButtonPayload::pos,
            ByteBufCodecs.BOOL, WarpGateButtonPayload::locked,
            WarpGateButtonPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WarpGateButtonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu instanceof WarpGateMenu menu) {
                if (menu.getBlockEntity() != null && menu.getBlockEntity().getBlockPos().equals(payload.pos())) {
                    if (menu.stillValid(player)) {
                        menu.getBlockEntity().setLocked(payload.locked());
                    }
                }
            }
        });
    }
}
