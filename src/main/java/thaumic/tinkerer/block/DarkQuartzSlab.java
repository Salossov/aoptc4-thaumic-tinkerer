package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public final class DarkQuartzSlab extends SlabBlock {
    private final boolean legacyFull;
    public DarkQuartzSlab(Properties properties, boolean legacyFull) {
        super(properties);
        this.legacyFull = legacyFull;
        if (legacyFull) registerDefaultState(defaultBlockState().setValue(TYPE, SlabType.DOUBLE));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return legacyFull ? defaultBlockState() : super.getStateForPlacement(context).setValue(WATERLOGGED, false);
    }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !legacyFull && super.canBeReplaced(state, context);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(DarkQuartzContent.SLAB.get());
    }
    @Override public boolean canPlaceLiquid(Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { return false; }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) { return false; }
}
