package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class TransvectorInterfaceBlock extends CamoBlock {
    public TransvectorInterfaceBlock() { this(Properties.of().mapColor(MapColor.METAL).strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()); }
    private TransvectorInterfaceBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<TransvectorInterfaceBlock> codec() { return simpleCodec(TransvectorInterfaceBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TransvectorInterfaceBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world, pos, current, tile) -> {
            if (world.getGameTime() % 100 == 0) world.updateNeighborsAt(pos, current.getBlock());
        };
    }
}
