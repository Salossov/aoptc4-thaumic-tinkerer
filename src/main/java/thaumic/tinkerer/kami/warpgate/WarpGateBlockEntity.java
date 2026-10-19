package thaumic.tinkerer.kami.warpgate;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.world.ChunkPreparationApi;

public class WarpGateBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INVENTORY_SIZE = 10;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean locked = false;
    private boolean teleportedThisTick = false;

    private ChunkPreparationApi.Request chunkRequest;
    private PendingTeleport pendingTeleport;

    private record PendingTeleport(ServerPlayer player, int index, ItemStack pearl,
            ItemStack snapshot, ServerLevel level, BlockPos targetPos) {}

    public WarpGateBlockEntity(BlockPos pos, BlockState state) {
        super(WarpGateContent.WARP_GATE_TILE.get(), pos, state);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WarpGateBlockEntity gate) {
        gate.teleportedThisTick = false;
        PendingTeleport pending = gate.pendingTeleport;
        if (pending == null) return;
        if (!gate.valid(pending) || gate.chunkRequest == null || gate.chunkRequest.closed()) {
            gate.cancelChunkWork();
            return;
        }
        final boolean ready;
        try {
            ready = gate.chunkRequest.advance(2);
        } catch (RuntimeException | Error failure) {
            gate.cancelChunkWork();
            throw failure;
        }
        if (!ready) {
            if (gate.chunkRequest.closed()) gate.cancelChunkWork();
            return;
        }
        try {
            // Chunk preparation can span ticks: quote the original slot again at commit.
            if (gate.valid(pending)) {
                gate.teleportedThisTick = performTeleport(pending.player(), pending.level(), pending.targetPos());
            }
        } finally {
            gate.cancelChunkWork();
        }
    }

    private boolean valid(PendingTeleport pending) {
        if (!(this.level instanceof ServerLevel source) || pending.level() != source
                || !connected(pending.player(), source) || !stillValid(pending.player())) return false;
        ItemStack current = this.items.get(pending.index());
        return current == pending.pearl() && ItemStack.matches(current, pending.snapshot())
                && current.is(WarpGateContent.SKY_PEARL.get()) && SkyPearlItem.isAttuned(current)
                && source.dimension().equals(SkyPearlItem.getTargetDim(current))
                && pending.targetPos().equals(SkyPearlItem.getTargetPos(current));
    }

    static boolean connected(ServerPlayer player, ServerLevel level) {
        return player.level() == level && player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player;
    }

    static boolean destinationLoaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos)
                && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Warm-only operation: callers retain their owner-bound request until this returns. */
    public static boolean teleportPlayer(ServerPlayer player, ServerLevel targetLevel, BlockPos targetPos) {
        return performTeleport(player, targetLevel, targetPos);
    }

    public static boolean performTeleport(ServerPlayer player, ServerLevel targetLevel, BlockPos targetPos) {
        if (!connected(player, targetLevel) || !destinationLoaded(targetLevel, targetPos)) return false;
        var chunk = targetLevel.getChunkSource().getChunkNow(targetPos.getX() >> 4, targetPos.getZ() >> 4);
        BlockEntity tile = chunk.getBlockEntity(targetPos);
        if (!(tile instanceof WarpGateBlockEntity destGate) || destGate.isRemoved()) {
            player.sendSystemMessage(Component.translatable("ttmisc.noDest"));
            return false;
        }
        if (destGate.isLocked()) {
            player.sendSystemMessage(Component.translatable("ttmisc.noTeleport"));
            return false;
        }
        SoundEvent wandSound = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wand"));
        targetLevel.playSound(null, player.getX(), player.getY(), player.getZ(), wandSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        sparkles(player, targetLevel);
        boolean moved = player.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY() + 1.6,
                targetPos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
        if (!moved) return false;
        sparkles(player, targetLevel);
        targetLevel.playSound(null, player.getX(), player.getY(), player.getZ(), wandSound, SoundSource.PLAYERS, 1.0F, 0.1F);
        return true;
    }

    private static void sparkles(ServerPlayer player, ServerLevel level) {
        for (int i = 0; i < 20; i++) {
            ParticleApi.sparkle(level,
                    (float) player.getX() + level.random.nextFloat() - 0.5F,
                    (float) player.getY() + level.random.nextFloat(),
                    (float) player.getZ() + level.random.nextFloat() - 0.5F, 6);
        }
    }

    public void teleportPlayer(ServerPlayer player, int index) {
        if (this.teleportedThisTick || index < 0 || index >= this.items.size()
                || !(this.level instanceof ServerLevel source) || !connected(player, source) || !stillValid(player)) return;
        if (this.pendingTeleport != null) {
            if (!valid(this.pendingTeleport)) cancelChunkWork();
            else if (this.pendingTeleport.player() != player || this.pendingTeleport.index() == index) return;
            else cancelChunkWork();
        }
        ItemStack pearl = this.items.get(index);
        if (!pearl.is(WarpGateContent.SKY_PEARL.get()) || !SkyPearlItem.isAttuned(pearl)
                || !source.dimension().equals(SkyPearlItem.getTargetDim(pearl))) return;
        BlockPos target = SkyPearlItem.getTargetPos(pearl);
        if (source.isOutsideBuildHeight(target)) {
            player.sendSystemMessage(Component.translatable("ttmisc.noDest"));
            return;
        }
        if (destinationLoaded(source, target)) {
            this.teleportedThisTick = performTeleport(player, source, target);
            return;
        }
        this.pendingTeleport = new PendingTeleport(player, index, pearl, pearl.copy(), source, target);
        try {
            this.chunkRequest = ChunkPreparationApi.prepare(this, source, List.of(new ChunkPos(target)));
            this.chunkRequest.advance(2);
        } catch (RuntimeException | Error failure) {
            cancelChunkWork();
            throw failure;
        }
    }

    public void cancelChunkWork() {
        this.pendingTeleport = null;
        if (this.chunkRequest != null) {
            this.chunkRequest.close();
            this.chunkRequest = null;
        }
    }

    @Override
    public void setRemoved() {
        cancelChunkWork();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        cancelChunkWork();
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("locked", this.locked);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.locked = tag.getBoolean("locked");
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("locked", this.locked);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved() || this.level == null || player.level() != this.level) return false;
        if (this.level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(this.worldPosition.getX() >> 4, this.worldPosition.getZ() >> 4);
            if (chunk == null || chunk.getBlockEntities().get(this.worldPosition) != this) return false;
        } else if (this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(this.worldPosition.getX() + .5, this.worldPosition.getY() + .5,
                this.worldPosition.getZ() + .5) <= 64;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(WarpGateContent.SKY_PEARL.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    // MenuProvider
    @Override
    public Component getDisplayName() {
        return Component.translatable("tile.warpGate.name");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WarpGateMenu(containerId, playerInventory, this);
    }
}
