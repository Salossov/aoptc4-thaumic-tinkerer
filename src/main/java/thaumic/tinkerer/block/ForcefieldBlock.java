package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Original invisible, air-material block still has the inherited full collision cube. */
public final class ForcefieldBlock extends BaseEntityBlock implements thaumcraft.api.world.IPersistentAir {
    public ForcefieldBlock() { this(Properties.of().air().noOcclusion().noLootTable().forceSolidOff().isSuffocating((state, level, pos) -> false)); }
    private ForcefieldBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<ForcefieldBlock> codec() { return simpleCodec(ForcefieldBlock::new); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ForcefieldBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world, pos, current, tile) -> { if (tile instanceof ForcefieldBlockEntity field) field.tick(); };
    }
}
