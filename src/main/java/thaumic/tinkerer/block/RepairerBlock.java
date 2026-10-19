package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;

public final class RepairerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final java.util.Random drops=new java.util.Random();
    public RepairerBlock(){super(Properties.of().mapColor(MapColor.METAL).strength(5,6).sound(SoundType.STONE).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<? extends RepairerBlock> codec(){return simpleCodec(properties->new RepairerBlock());}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override protected boolean skipRendering(BlockState state,BlockState neighbour,Direction face){return false;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override protected boolean isSignalSource(BlockState state){return true;}
    @Override protected int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return level.getBlockEntity(pos) instanceof RepairerBlockEntity tile&&!tile.isEmpty()&&tile.getItem(0).getDamageValue()!=0?15:0;}
    @Override protected int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){return 0;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RepairerBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,RepairerContent.TILE.get(),RepairerBlockEntity::tick);}
    private boolean interact(Level level,BlockPos pos,Player player,InteractionHand hand){
        if(!(level.getBlockEntity(pos) instanceof RepairerBlockEntity tile))return false;
        var stack=tile.getItem(0);var held=player.getItemInHand(hand);
        if(stack.isEmpty()&&!RepairerBlockEntity.accepts(held))return false;
        if(level.isClientSide())return true;
        if(stack.isEmpty())tile.setItem(0,held.split(1));
        else{if(!player.getInventory().add(stack))player.drop(stack,false);tile.setItem(0,ItemStack.EMPTY);}
        return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(level,pos,player,hand)?ItemInteractionResult.sidedSuccess(level.isClientSide()):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(level,pos,player,InteractionHand.MAIN_HAND)?InteractionResult.sidedSuccess(level.isClientSide()):InteractionResult.PASS;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof RepairerBlockEntity tile){
            var stack=tile.removeItemNoUpdate(0);
            if(!stack.isEmpty()){
                float x=drops.nextFloat()*.8f+.1f,y=drops.nextFloat()*.8f+.1f,z=drops.nextFloat()*.8f+.1f;
                while(!stack.isEmpty()){int count=Math.min(stack.getCount(),drops.nextInt(21)+10);var entity=new ItemEntity(level,pos.getX()+x,pos.getY()+y,pos.getZ()+z,stack.split(count));entity.setDeltaMovement((float)drops.nextGaussian()*.05f,(float)drops.nextGaussian()*.05f+.2f,(float)drops.nextGaussian()*.05f);level.addFreshEntity(entity);}
            }
            level.updateNeighborsAt(pos,state.getBlock());
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
