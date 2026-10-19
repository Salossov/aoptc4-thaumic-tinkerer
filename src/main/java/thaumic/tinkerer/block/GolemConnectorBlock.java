package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** The original wood material retains Block's default stone sound and zero strength. */
public final class GolemConnectorBlock extends CamoBlock {
    public GolemConnectorBlock() { this(Properties.of().mapColor(MapColor.WOOD).strength(0).ignitedByLava()); }
    private GolemConnectorBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<GolemConnectorBlock> codec() { return simpleCodec(GolemConnectorBlock::new); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GolemConnectorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world, pos, current, tile) -> {
            if (world.getGameTime() % 100 == 1 && tile instanceof GolemConnectorBlockEntity connector) connector.verifyGolem();
        };
    }
}
