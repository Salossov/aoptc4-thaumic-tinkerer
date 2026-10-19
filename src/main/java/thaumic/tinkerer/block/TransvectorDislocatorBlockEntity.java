package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.world.ChunkPreparationApi;

/** The release dislocator swaps NBT copies, unlike the locomotive's identity-preserving move. */
public final class TransvectorDislocatorBlockEntity extends TransvectorBlockEntity {
    private int orientation, cooldown;
    private boolean pulseStored;
    private Pending pending;
    private ChunkPreparationApi.Request chunks;
    private java.util.Set<ChunkPos> requested = java.util.Set.of();
    private static final class Pending {
        final BlockPos end, target;
        final boolean exchangeAllowed;
        boolean exchanged;
        Pending(BlockPos end, BlockPos target, boolean exchangeAllowed) { this.end = end; this.target = target; this.exchangeAllowed = exchangeAllowed; }
    }
    public TransvectorDislocatorBlockEntity(BlockPos pos, BlockState state) { super(TransvectorContent.DISLOCATOR_TILE.get(), pos, state); }
    @Override public int maxDistance() { return 16; }
    @Override protected boolean tileRequiredAtLink() { return false; }
    public int orientation() { return orientation; }
    public void orientation(int value) { orientation = value; setChanged(); requestModelDataUpdate(); sync(); }
    public int cooldown() { return cooldown; }
    public boolean pulseStored() { return pulseStored; }
    public boolean preparing() { return pending != null; }
    public BlockPos workingPosition() { return orientation >= 0 && orientation < 6 ? worldPosition.relative(Direction.from3DDataValue(orientation)) : worldPosition; }
    @Override public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return super.getModelData().derive().with(CamoModelData.ORIENTATION, orientation).build();
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TransvectorDislocatorBlockEntity tile) {
        tile.cooldown = Math.max(0, tile.cooldown - 1);
        if (tile.pending != null) { tile.advancePulse(); return; }
        if (tile.cooldown == 0 && tile.pulseStored) { tile.pulseStored = false; tile.receiveRedstonePulse(); }
    }
    public void receiveRedstonePulse() {
        if (!(level instanceof ServerLevel server)) return;
        if (pending != null) return; // Approved: pulses coalesce while chunks are being prepared.
        resolve();
        if (!hasBinding()) return;
        if (cooldown > 0) { pulseStored = true; return; }
        var end = targetPosition(); var target = workingPosition();
        pending = new Pending(end, target, server.isInWorldBounds(end) && server.isInWorldBounds(target) && server.hasChunkAt(end) && server.hasChunkAt(target));
        advancePulse();
    }
    private void advancePulse() {
        if (!(level instanceof ServerLevel server) || pending == null) return;
        var operation = pending; var end = operation.end; var target = operation.target;
        if (isRemoved() || !hasBinding() || !end.equals(targetPosition()) || !target.equals(workingPosition()) || chunks != null && chunks.closed()) { cancelWork(); return; }
        var atEnd = server.getEntitiesOfClass(Entity.class, new AABB(end));
        var atTarget = server.getEntitiesOfClass(Entity.class, new AABB(target));
        var columns = destinations(atEnd, atTarget, end, target);
        // FULL columns are needed even outside build height: Entity.setPosRaw loads by X/Z.
        columns.add(new ChunkPos(end)); columns.add(new ChunkPos(target));
        if (!ready(server, columns)) return;
        // Approved fix: never clear either source when a placement would be outside the world.
        // Unloaded links still skip the block exchange, as in World.blockExists in the release.
        if (!operation.exchanged && operation.exchangeAllowed) {
            var endData = new Snapshot(server, end); var targetData = new Snapshot(server, target);
            if (checkBlock(end, endData.state()) && checkBlock(target, targetData.state())) {
                server.removeBlockEntity(end); server.removeBlockEntity(target);
                endData.place(server, target); targetData.place(server, end);
                server.neighborChanged(target, TransvectorContent.DISLOCATOR.get(), worldPosition);
                server.neighborChanged(end, TransvectorContent.DISLOCATOR.get(), worldPosition);
            }
        }
        operation.exchanged = true;
        // Query both lists before moving anything; overlapping boxes deliberately retain two moves.
        atEnd = server.getEntitiesOfClass(Entity.class, new AABB(end));
        atTarget = server.getEntitiesOfClass(Entity.class, new AABB(target));
        // onPlace/neighborChanged may have created entities. Never let their native setPos force a cold column.
        if (!ready(server, destinations(atEnd, atTarget, end, target))) return;
        for (var entity : atEnd) move(entity, end, target);
        for (var entity : atTarget) move(entity, target, end);
        cooldown = 10;
        cancelWork();
    }
    private java.util.LinkedHashSet<ChunkPos> destinations(java.util.List<Entity> atEnd, java.util.List<Entity> atTarget, BlockPos end, BlockPos target) {
        var columns = new java.util.LinkedHashSet<ChunkPos>(); var positions = new java.util.IdentityHashMap<Entity, net.minecraft.world.phys.Vec3>();
        collectDestinations(columns, positions, atEnd, end, target); collectDestinations(columns, positions, atTarget, target, end); return columns;
    }
    private static void collectDestinations(java.util.Set<ChunkPos> columns, java.util.Map<Entity, net.minecraft.world.phys.Vec3> positions, java.util.List<Entity> entities, BlockPos from, BlockPos to) {
        for (var entity : entities) {
            var old = positions.getOrDefault(entity, entity.position());
            var next = old.add((double)to.getX() - from.getX(), (double)to.getY() - from.getY(), (double)to.getZ() - from.getZ());
            if (entity instanceof ServerPlayer) next = new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.clamp(next.x, -3e7, 3e7), next.y, net.minecraft.util.Mth.clamp(next.z, -3e7, 3e7));
            positions.put(entity, next); columns.add(new ChunkPos(BlockPos.containing(next)));
        }
    }
    private boolean ready(ServerLevel level, java.util.Set<ChunkPos> columns) {
        boolean loaded = true;
        for (var column : columns) if (level.getChunkSource().getChunkNow(column.x, column.z) == null) { loaded = false; break; }
        if (loaded) return true;
        if (chunks == null || !requested.containsAll(columns)) {
            if (chunks != null) chunks.close();
            requested = java.util.Set.copyOf(columns); chunks = ChunkPreparationApi.prepare(this, columns);
        }
        return chunks.advance(2);
    }
    public void cancelWork() { pending = null; if (chunks != null) { chunks.close(); chunks = null; } requested = java.util.Set.of(); }
    @Override public void setRemoved() { cancelWork(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { cancelWork(); super.onChunkUnloaded(); }
    private boolean checkBlock(BlockPos pos, BlockState state) {
        var block = state.getBlock();
        // Preserve the original OR, which permits non-air blocks even when blacklisted/unbreakable.
        return (!BuiltInRegistries.BLOCK.getKey(block).toString().equals("thaumcraft:aura_node")
                && !ThaumcraftApi.portableHoleBlackList.contains(block) && state.getDestroySpeed(level, pos) != -1) || block != Blocks.AIR;
    }
    private static void move(Entity entity, BlockPos from, BlockPos to) {
        double x = entity.getX() + ((double)to.getX() - from.getX()), y = entity.getY() + ((double)to.getY() - from.getY()), z = entity.getZ() + ((double)to.getZ() - from.getZ());
        if (entity instanceof ServerPlayer player) player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        else entity.setPos(x, y, z);
    }
    private record Snapshot(BlockState state, CompoundTag data) {
        Snapshot(ServerLevel level, BlockPos pos) { this(level.getBlockState(pos), level.getBlockEntity(pos) == null ? null : level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess())); }
        void place(ServerLevel level, BlockPos pos) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.removeBlockEntity(pos);
            if (data != null) {
                var restored = BlockEntity.loadStatic(pos, state, data, level.registryAccess());
                if (restored == null) throw new IllegalStateException("Could not restore dislocator payload at " + pos);
                level.setBlockEntity(restored); restored.setChanged();
            }
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putInt("orientation", orientation); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); orientation = tag.getInt("orientation"); requestModelDataUpdate(); }
}
