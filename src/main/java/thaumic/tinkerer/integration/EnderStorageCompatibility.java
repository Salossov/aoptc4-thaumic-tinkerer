package thaumic.tinkerer.integration;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.manager.EnderStorageManager;
import codechicken.enderstorage.storage.EnderItemStorage;
import codechicken.enderstorage.tile.TileEnderChest;
import java.util.Map;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.util.ItemData;

/** Loaded only behind the optional EnderStorage mod guard. */
public final class EnderStorageCompatibility {
    public static final String FREQUENCY_KEY = "enderstorage_frequency";

    public static boolean bind(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if (!player.isShiftKeyDown()) return false;
        var level = player.serverLevel();
        var from = player.position().add(0, 1.62, 0);
        var hit = level.clip(new ClipContext(from, from.add(player.getLookAngle().scale(5)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        var pos = hit.getBlockPos();
        if (level.getBlockEntity(pos) instanceof TileEnderChest chest) {
            var frequency = chest.getFrequency();
            var encoded = Frequency.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), frequency).getOrThrow();
            // The original nibble values follow wool metadata, not dye metadata.
            int legacy = frequency.left().getWoolMeta() << 8 | frequency.middle().getWoolMeta() << 4 | frequency.right().getWoolMeta();
            String owner = frequency.ownerName().map(name -> name.getString()).orElse("global");
            ItemData.update(focus, tag -> {
                tag.put(FREQUENCY_KEY, encoded);
                tag.putInt("freq", legacy);
                tag.putString("owner", owner);
                tag.putBoolean("ender", true);
            });
            WandApi.focus(wand, focus);
            return true;
        }
        if (level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
            ItemData.update(focus, tag -> {
                tag.putInt("freq", -1);
                tag.putString("owner", player.getGameProfile().getName());
                tag.putBoolean("ender", false);
                tag.remove(FREQUENCY_KEY);
            });
            WandApi.focus(wand, focus);
            return true;
        }
        return false;
    }

    public static boolean open(ServerPlayer player, ItemStack wand, ItemStack focus, Map<Aspect, Integer> cost) {
        var encoded = ItemData.read(focus).get(FREQUENCY_KEY);
        if (encoded == null) return false;
        var frequency = Frequency.CODEC.parse(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), encoded).result();
        if (frequency.isEmpty() || !WandApi.consume(wand, player, cost, true)) return false;
        EnderStorageManager.instance(false).getStorage(frequency.get(), EnderItemStorage.TYPE).openContainer(player, focus.getHoverName());
        return true;
    }

    private EnderStorageCompatibility() { }
}
