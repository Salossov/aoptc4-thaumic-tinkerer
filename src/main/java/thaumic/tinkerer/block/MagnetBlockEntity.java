package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.client.ParticleApi;
import thaumic.tinkerer.util.LegacyMotion;

public class MagnetBlockEntity extends BlockEntity implements thaumcraft.api.world.IMovableBlockEntity {
    public MagnetBlockEntity(BlockPos pos, BlockState state) { this(MagnetContent.MAGNET_TILE.get(), pos, state); }
    protected MagnetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public boolean isPulling() { return getBlockState().getValue(MagnetBlock.PULLING); }
    public void setPulling(boolean pulling) { if (level != null) level.setBlock(worldPosition, getBlockState().setValue(MagnetBlock.PULLING, pulling), 3); }
    public int signal() {
        if (level == null) return 0;
        int signal = 0;
        for (var direction : Direction.values()) signal = Math.max(signal, level.getSignal(worldPosition.relative(direction), direction));
        return signal;
    }
    public AABB effectBox(int signal) {
        var centre = Vec3.atCenterOf(worldPosition); int range = signal / 2;
        return new AABB(centre.x - range, worldPosition.getY(), centre.z - range, centre.x + range, centre.y + range, centre.z + range);
    }
    public boolean accepts(Entity entity) { return entity instanceof ItemEntity; }
    public void tick() {
        if (level == null || isRemoved()) return;
        int signal = signal(); if (signal <= 0) return;
        var centre = Vec3.atCenterOf(worldPosition); boolean pulling = isPulling();
        for (var entity : level.getEntities((Entity)null, effectBox(signal), this::accepts)) {
            float distance = pulling ? (float) entity.position().distanceToSqr(centre) : 1.1f;
            if (distance > 1) {
                LegacyMotion.towards(entity, centre, pulling ? .25f : -.25f);
                ParticleApi.sparkle(level, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), pulling ? 2 : 4);
            }
        }
    }
}
