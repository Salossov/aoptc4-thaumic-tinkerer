package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.client.ParticleApi;

public final class ForcefieldBlockEntity extends BlockEntity {
    public int ticks = 60;
    public ForcefieldBlockEntity(BlockPos pos, BlockState state) { super(ForcefieldContent.TILE.get(), pos, state); }
    public void tick() {
        if (level == null) return;
        if (ticks < 0) level.removeBlock(worldPosition, false);
        ticks--;
        ParticleApi.blockSparkle(level, worldPosition, 255, 1);
    }
}
