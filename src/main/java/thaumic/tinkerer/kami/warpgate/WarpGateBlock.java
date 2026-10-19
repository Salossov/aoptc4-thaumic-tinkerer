package thaumic.tinkerer.kami.warpgate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.Random;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WarpGateBlock extends BaseEntityBlock {
    private final Random dropRandom = new Random();
    public static final MapCodec<WarpGateBlock> CODEC = simpleCodec(WarpGateBlock::new);

    public WarpGateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public WarpGateBlock() {
        this(BlockBehaviour.Properties.of()
                .sound(SoundType.STONE)
                .strength(5.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpGateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, WarpGateContent.WARP_GATE_TILE.get(), WarpGateBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WarpGateBlockEntity gate && player instanceof ServerPlayer serverPlayer) {
                level.sendBlockUpdated(pos, state, state, 3);
                serverPlayer.openMenu(gate, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WarpGateBlockEntity gate) {
                if (!level.isClientSide()) dropInventory(level, pos, gate);
                gate.cancelChunkWork();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    private void dropInventory(Level level, BlockPos pos, WarpGateBlockEntity gate) {
        for (int slot = 0; slot < gate.getContainerSize(); slot++) {
            var stack = gate.getItem(slot);
            if (stack.isEmpty()) continue;
            float x = this.dropRandom.nextFloat() * .8F + .1F;
            float y = this.dropRandom.nextFloat() * .8F + .1F;
            float z = this.dropRandom.nextFloat() * .8F + .1F;
            while (!stack.isEmpty()) {
                int count = Math.min(this.dropRandom.nextInt(21) + 10, stack.getCount());
                var dropped = stack.copyWithCount(count);
                stack.shrink(count);
                var entity = new ItemEntity(level, pos.getX() + x, pos.getY() + y, pos.getZ() + z, dropped);
                entity.setDeltaMovement((float) this.dropRandom.nextGaussian() * .05F,
                        (float) this.dropRandom.nextGaussian() * .05F + .2F,
                        (float) this.dropRandom.nextGaussian() * .05F);
                level.addFreshEntity(entity);
            }
        }
        level.updateNeighbourForOutputSignal(pos, this);
    }

}
