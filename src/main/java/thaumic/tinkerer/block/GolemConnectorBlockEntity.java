package thaumic.tinkerer.block;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IControllableGolem;

public final class GolemConnectorBlockEntity extends CamoBlockEntity {
    private UUID connected;
    private LivingEntity golem;
    public GolemConnectorBlockEntity(BlockPos pos, BlockState state) { super(GolemConnectorContent.TILE.get(), pos, state); }
    public UUID connectedId() { return connected; }
    public LivingEntity golem() { return golem; }
    public IControllableGolem control() { return golem == null ? null : (IControllableGolem)golem; }
    public void verifyGolem() {
        if (connected == null || level == null) return;
        var box = new AABB(worldPosition.getX() - 30, worldPosition.getY() - 30, worldPosition.getZ() - 30,
                worldPosition.getX() + 30, worldPosition.getY() + 30, worldPosition.getZ() + 30);
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate instanceof IControllableGolem && connected.equals(candidate.getUUID()))) {
            golem = entity; break;
        }
        // TileGolemConnector deliberately retains its previous cached entity when
        // the UUID is not found, including a newly selected UUID outside the box.
    }
    public void connect(UUID id) {
        connected = id;
        if (id == null) golem = null; else verifyGolem();
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (connected != null) { tag.putLong("UUIDMost", connected.getMostSignificantBits()); tag.putLong("UUIDLeast", connected.getLeastSignificantBits()); }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("UUIDMost") && tag.contains("UUIDLeast")) connected = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
    }
}
