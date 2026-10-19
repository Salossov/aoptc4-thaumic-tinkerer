package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Release TileTransvector binding, with an explicit flag for modern negative heights. */
public abstract class TransvectorBlockEntity extends CamoBlockEntity {
    private BlockPos target = new BlockPos(0, -1, 0);
    private boolean bound, cheaty;
    protected TransvectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public abstract int maxDistance();
    public final boolean hasBinding() { return bound; }
    public final BlockPos targetPosition() { return target; }
    public final void bind(BlockPos pos) { target = pos.immutable(); bound = true; super.setChanged(); sync(); }
    public final void clearBinding() { bound = false; super.setChanged(); sync(); }
    protected final void sync() { if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    protected boolean tileRequiredAtLink() { return !cheaty; }
    public final BlockEntity resolve() {
        if (!bound || level == null || level.isOutsideBuildHeight(target) || !level.hasChunkAt(target)) return null;
        var tile = level.getBlockEntity(target);
        boolean inRange = Math.abs((long)target.getX() - worldPosition.getX()) <= maxDistance()
                && Math.abs((long)target.getY() - worldPosition.getY()) <= maxDistance()
                && Math.abs((long)target.getZ() - worldPosition.getZ()) <= maxDistance();
        if ((tile != null || !tileRequiredAtLink()) && (inRange || cheaty)) return tile;
        clearBinding(); return null;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putInt("xt", target.getX()); tag.putInt("yt", target.getY()); tag.putInt("zt", target.getZ());
        tag.putBoolean("bound", bound); tag.putBoolean("cheatyMode", cheaty);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); target = new BlockPos(tag.getInt("xt"), tag.getInt("yt"), tag.getInt("zt"));
        bound = tag.contains("bound") ? tag.getBoolean("bound") : tag.contains("yt") && target.getY() != -1;
        cheaty = tag.getBoolean("cheatyMode");
    }
}
