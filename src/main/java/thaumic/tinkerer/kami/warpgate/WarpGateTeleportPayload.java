package thaumic.tinkerer.kami.warpgate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumic.tinkerer.ThaumicTinkerer;

public record WarpGateTeleportPayload(BlockPos pos, int index) implements CustomPacketPayload {
    public static final Type<WarpGateTeleportPayload> TYPE = new Type<>(ThaumicTinkerer.id("warp_gate_teleport"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpGateTeleportPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarpGateTeleportPayload::pos,
            ByteBufCodecs.VAR_INT, WarpGateTeleportPayload::index,
            WarpGateTeleportPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WarpGateTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            if (payload.index() < 0 || payload.index() >= WarpGateBlockEntity.INVENTORY_SIZE) return;
            var chunk = player.serverLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) instanceof WarpGateBlockEntity gate) {
                gate.teleportPlayer(player, payload.index());
            }
        });
    }
}
