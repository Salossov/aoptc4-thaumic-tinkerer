package thaumic.tinkerer.kami.warpgate;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.focus.KamiFocusItem;

@EventBusSubscriber(modid = ThaumicTinkerer.ID)
public class FocusRecallItem extends KamiFocusItem {
    private static final Map<Aspect, Integer> VIS_COST = Map.of(
            Aspects.AIR, 4000, Aspects.EARTH, 4000, Aspects.ORDER, 4000);
    private static final Map<ServerPlayer, Casting> CASTING = new WeakHashMap<>();

    private static final class Casting implements AutoCloseable {
        final ServerLevel level;
        final InteractionHand hand;
        final ItemStack wand, focus;
        ChunkPreparationApi.Request request;
        ItemStack pearl, pearlSnapshot, wandSnapshot;
        int pearlSlot;
        BlockPos target;
        Vec3 origin;

        Casting(ServerPlayer player, ItemStack wand, ItemStack focus, InteractionHand hand) {
            this.level = player.serverLevel();
            this.wand = wand;
            this.focus = focus.copy();
            this.hand = hand;
        }

        boolean valid(ServerPlayer player) {
            if (!WarpGateBlockEntity.connected(player, this.level) || !player.isUsingItem()
                    || player.getUsedItemHand() != this.hand || player.getUseItem() != this.wand
                    || player.getItemInHand(this.hand) != this.wand
                    || !ItemStack.matches(this.focus, WandApi.focus(this.wand))) return false;
            if (this.request == null) return true;
            return !this.request.closed() && player.position().equals(this.origin)
                    && firstPearl(player) == this.pearlSlot
                    && player.getInventory().getItem(this.pearlSlot) == this.pearl
                    && ItemStack.matches(this.pearl, this.pearlSnapshot)
                    && ItemStack.matches(this.wand, this.wandSnapshot)
                    && this.level.dimension().equals(SkyPearlItem.getTargetDim(this.pearl))
                    && this.target.equals(SkyPearlItem.getTargetPos(this.pearl));
        }

        @Override public void close() {
            if (this.request != null) {
                this.request.close();
                this.request = null;
            }
        }
    }

    @Override public String getSortingHelper(ItemStack stack) { return "RECALL"; }
    @Override public int getFocusColor(ItemStack stack) { return 10287359; }
    @Override public Map<Aspect, Integer> getVisCost(ItemStack stack) { return VIS_COST; }
    @Override public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack, int rank) { return List.of(); }
    @Override public boolean continuous(ItemStack focus) { return true; }

    @Override
    public boolean begin(ServerPlayer player, ItemStack wand, ItemStack focus, InteractionHand hand) {
        end(player, wand, focus);
        CASTING.put(player, new Casting(player, wand, focus, hand));
        return true;
    }

    private static int firstPearl(ServerPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(WarpGateContent.SKY_PEARL.get()) && SkyPearlItem.isAttuned(stack)) return slot;
        }
        return -1;
    }

    @Override
    public boolean cast(ServerPlayer player, ItemStack wand, ItemStack focus) {
        Casting casting = CASTING.get(player);
        if (casting == null) return false;
        if (!casting.valid(player) || casting.wand != wand) {
            finish(player, casting);
            return false;
        }
        // The first source tick sees elapsed zero; the attempt occurs strictly after 60.
        if (player.getTicksUsingItem() <= 60) return false;
        if (casting.request == null) {
            int slot = firstPearl(player);
            if (slot < 0) { finish(player, casting); return false; }
            ItemStack pearl = player.getInventory().getItem(slot);
            BlockPos target = SkyPearlItem.getTargetPos(pearl);
            if (!casting.level.dimension().equals(SkyPearlItem.getTargetDim(pearl))
                    || casting.level.isOutsideBuildHeight(target)
                    || !WandApi.consume(wand, player, VIS_COST, false)) {
                finish(player, casting);
                return false;
            }
            if (WarpGateBlockEntity.destinationLoaded(casting.level, target)) {
                try { return teleport(player, casting, target); }
                finally { finish(player, casting); }
            }
            casting.pearlSlot = slot;
            casting.pearl = pearl;
            casting.pearlSnapshot = pearl.copy();
            casting.wandSnapshot = wand.copy();
            casting.target = target;
            casting.origin = player.position();
            try {
                casting.request = ChunkPreparationApi.prepare(player, casting.level, List.of(new ChunkPos(target)));
            } catch (RuntimeException | Error failure) {
                finish(player, casting);
                throw failure;
            }
        }
        final boolean ready;
        try {
            ready = casting.request.advance(2);
        } catch (RuntimeException | Error failure) {
            finish(player, casting);
            throw failure;
        }
        if (!ready) {
            if (casting.request.closed()) finish(player, casting);
            return false;
        }
        try {
            return casting.valid(player) && teleport(player, casting, casting.target);
        } finally {
            finish(player, casting);
        }
    }

    private static boolean teleport(ServerPlayer player, Casting casting, BlockPos target) {
        if (!WandApi.consume(casting.wand, player, VIS_COST, false)
                || !WarpGateBlockEntity.performTeleport(player, casting.level, target)) return false;
        WandApi.consume(casting.wand, player, VIS_COST, true);
        return true;
    }

    private static void finish(ServerPlayer player, Casting casting) {
        if (CASTING.get(player) == casting) CASTING.remove(player);
        casting.close();
        if (player.isUsingItem() && player.getUseItem() == casting.wand) player.stopUsingItem();
    }

    @Override
    public void end(ServerPlayer player, ItemStack wand, ItemStack focus) {
        Casting casting = CASTING.remove(player);
        if (casting != null) casting.close();
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Casting casting = CASTING.get(player);
            if (casting != null && !casting.valid(player)) finish(player, casting);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Casting casting = CASTING.get(player);
            if (casting != null) finish(player, casting);
        }
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload event) {
        for (var player : List.copyOf(CASTING.keySet())) {
            Casting casting = CASTING.get(player);
            if (casting.level == event.getLevel()) finish(player, casting);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        for (var player : List.copyOf(CASTING.keySet())) {
            Casting casting = CASTING.get(player);
            if (casting.level.getServer() == event.getServer()) finish(player, casting);
        }
    }
}
