package thaumic.tinkerer.crop;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.common.util.TriState;

/** Full-height original farmland, no drying/trampling, immediate solid-cover decay. */
public final class InfusedFarmlandBlock extends FarmBlock implements EntityBlock {
    public InfusedFarmlandBlock(){super(Properties.ofFullCopy(Blocks.FARMLAND).strength(.6f).sound(SoundType.GRAVEL).noOcclusion());}
    @Override public MapCodec<FarmBlock> codec(){return simpleCodec(properties->new InfusedFarmlandBlock());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos next){return state;}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos next,boolean moving){if(!level.isClientSide()&&level.getBlockState(pos.above()).isSolid())level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,Entity entity,float distance){}
    @Override public TriState canSustainPlant(BlockState state,BlockGetter level,BlockPos pos,Direction side,BlockState plant){return plant.getBlock() instanceof InfusedCropBlock?TriState.TRUE:TriState.FALSE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfusedFarmlandBlockEntity(pos,state);}
}
