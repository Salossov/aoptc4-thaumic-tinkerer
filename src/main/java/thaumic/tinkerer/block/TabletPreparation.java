package thaumic.tinkerer.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.world.ChunkPreparationApi;

/** One accepted tablet action, owned by its live block entity rather than its fake player. */
final class TabletPreparation implements AutoCloseable {
    private final AnimationTabletBlockEntity owner;
    private final ServerLevel level;
    private final BlockPos origin, target;
    private final BlockState ownerState, targetState;
    private final BlockEntity targetEntity;
    private final ItemStack stack, snapshot;
    private final boolean leftClick, redstone;
    private final List<ChunkPos> columns;
    private final ChunkPreparationApi.Request request;
    private boolean ready;

    TabletPreparation(AnimationTabletBlockEntity owner, List<ChunkPos> columns) {
        this.owner = owner;
        level = (ServerLevel) owner.getLevel();
        origin = owner.getBlockPos().immutable();
        target = owner.target().immutable();
        ownerState = owner.getBlockState();
        stack = owner.getItem(0);
        snapshot = stack.copy();
        leftClick = owner.leftClick;
        redstone = owner.redstone;
        var chunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
        targetState = chunk == null ? null : chunk.getBlockState(target);
        targetEntity = chunk == null ? null : chunk.getBlockEntities().get(target);
        this.columns = List.copyOf(columns);
        request = ChunkPreparationApi.prepare(owner, columns);
    }

    boolean valid() {
        if (request.closed() || owner.isRemoved() || owner.getLevel() != level || !owner.getBlockPos().equals(origin)
                || owner.getBlockState() != ownerState || !owner.target().equals(target)
                || owner.leftClick != leftClick || owner.redstone != redstone
                || owner.getItem(0) != stack || !ItemStack.matches(stack, snapshot)) return false;
        var chunk = level.getChunkSource().getChunkNow(origin.getX() >> 4, origin.getZ() >> 4);
        if (chunk == null || chunk.getBlockEntities().get(origin) != owner) return false;
        if (targetState != null) {
            chunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
            if (chunk == null || chunk.getBlockState(target) != targetState || chunk.getBlockEntities().get(target) != targetEntity) return false;
        }
        return true;
    }

    boolean covers(List<ChunkPos> needed) { return columns.containsAll(needed); }
    boolean ready() { return ready; }
    boolean advance() {
        if (!valid()) return false;
        ready = request.advance(2) && loaded(level, columns);
        return ready;
    }
    @Override public void close() { request.close(); }

    static List<ChunkPos> columns(BlockPos origin, BlockPos target, int radius) {
        var result = new ArrayList<ChunkPos>();
        for (int x = (Math.min(origin.getX(), target.getX()) - radius) >> 4; x <= (Math.max(origin.getX(), target.getX()) + radius) >> 4; x++)
            for (int z = (Math.min(origin.getZ(), target.getZ()) - radius) >> 4; z <= (Math.max(origin.getZ(), target.getZ()) + radius) >> 4; z++) result.add(new ChunkPos(x, z));
        return List.copyOf(result);
    }
    static boolean loaded(ServerLevel level, List<ChunkPos> columns) {
        for (var column : columns) if (level.getChunkSource().getChunkNow(column.x, column.z) == null) return false;
        return true;
    }
}
