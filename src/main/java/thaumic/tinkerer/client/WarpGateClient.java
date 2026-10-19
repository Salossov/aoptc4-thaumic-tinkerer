package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.warpgate.WarpGateBlockEntity;

/**
 * Client tick handler for detecting when the local player sneaks on top of a Warp Gate,
 * opening the destination selection screen (GuiWarpGateDestinations).
 * Matches 1.7.10 TileWarpGate.updateEntity() sneaking local player check with exact AABB.
 */
@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WarpGateClient {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!TinkererConfig.KAMI_FORCE_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // In 1.7.10: player == clientPlayer && player.isSneaking()
        if (!player.isCrouching() && !player.isShiftKeyDown()) return;

        AABB playerBox = player.getBoundingBox();
        int minX = (int) Math.floor(playerBox.minX);
        int maxX = (int) Math.floor(playerBox.maxX);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxZ = (int) Math.floor(playerBox.maxZ);
        int minY = (int) Math.floor(playerBox.minY - 1.5);
        int maxY = (int) Math.floor(playerBox.minY);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.level.getBlockEntity(pos) instanceof WarpGateBlockEntity gate) {
                        AABB triggerAabb = WarpGateVisualHelper.getSneakTriggerAABB(pos);
                        if (playerBox.intersects(triggerAabb)) {
                            mc.setScreen(new WarpGateDestinationsScreen(gate));
                            return;
                        }
                    }
                }
            }
        }
    }

    private WarpGateClient() {}
}
