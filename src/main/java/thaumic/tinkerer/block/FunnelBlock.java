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
import thaumcraft.api.aspects.IEssentiaJarItem;

public final class FunnelBlock extends BaseEntityBlock {
    private final java.util.Random drops=new java.util.Random();
    public FunnelBlock(){super(Properties.of().mapColor(MapColor.STONE).strength(3,4.8f).sound(SoundType.STONE).noOcclusion());}
    @Override protected MapCodec<? extends FunnelBlock> codec(){return simpleCodec(properties->new FunnelBlock());}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(0,0,0,16,2,16);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.block();}
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override protected boolean skipRendering(BlockState state,BlockState neighbour,Direction face){return false;}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).is(Blocks.HOPPER);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return canSurvive(defaultBlockState(),context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide()&&!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FunnelBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,FunnelContent.TILE.get(),FunnelBlockEntity::tick);}
    private boolean interact(Level level,BlockPos pos,Player player,InteractionHand hand){
        if(!(level.getBlockEntity(pos) instanceof FunnelBlockEntity tile))return false;
        var stack=tile.getItem(0);var held=player.getItemInHand(hand);
        if(stack.isEmpty()&&!(held.getItem() instanceof IEssentiaJarItem))return false;
        if(level.isClientSide())return true;
        if(stack.isEmpty())tile.setItem(0,held.split(1));
        else{if(!player.getInventory().add(stack))player.drop(stack,false);tile.setItem(0,ItemStack.EMPTY);}
        return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return interact(level,pos,player,hand)?ItemInteractionResult.sidedSuccess(level.isClientSide()):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){return interact(level,pos,player,InteractionHand.MAIN_HAND)?InteractionResult.sidedSuccess(level.isClientSide()):InteractionResult.PASS;}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof FunnelBlockEntity tile){
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
