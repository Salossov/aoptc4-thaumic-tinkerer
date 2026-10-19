package thaumic.tinkerer.summon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;

public final class SummonBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=box(0,0,0,16,2,16);
    public SummonBlock() {super(Properties.of().mapColor(MapColor.METAL).strength(3,30).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());}
    @Override protected MapCodec<SummonBlock> codec() {return simpleCodec(properties->new SummonBlock());}
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new SummonBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {return level.isClientSide()?null:createTickerHelper(type,SummonContent.TILE.get(),(world,pos,current,tile)->tile.tick());}
}
