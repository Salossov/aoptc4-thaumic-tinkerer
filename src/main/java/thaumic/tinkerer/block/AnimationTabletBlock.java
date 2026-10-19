package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.api.wands.ICastingWand;

public final class AnimationTabletBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public static final Direction[] DIRECTIONS={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final VoxelShape SHAPE=box(0,0,0,16,2,16);
    private final java.util.Random drops=new java.util.Random();
    public AnimationTabletBlock(){super(Properties.of().strength(3,30).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,POWERED);}
    @Override protected MapCodec<? extends AnimationTabletBlock> codec(){return simpleCodec(p->new AnimationTabletBlock());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AnimationTabletBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,AnimationTabletContent.TILE.get(),AnimationTabletBlockEntity::tick);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block from,BlockPos neighbor,boolean moving){
        if(level.isClientSide())return;boolean power=level.hasNeighborSignal(pos)||level.hasNeighborSignal(pos.above()),on=state.getValue(POWERED);
        if(power&&!on){level.scheduleTick(pos,this,1);level.setBlock(pos,state.setValue(POWERED,true),4);}else if(!power&&on)level.setBlock(pos,state.setValue(POWERED,false),4);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof AnimationTabletBlockEntity tile&&tile.redstone&&tile.swingProgress==0)tile.trigger();}
    private void use(Level level,BlockPos pos,Player player){
        if(level.isClientSide()||!(level.getBlockEntity(pos) instanceof AnimationTabletBlockEntity tile))return;
        if(player.getMainHandItem().getItem() instanceof ICastingWand){
            if(!tile.getBlockState().getValue(POWERED)&&!tile.isBreaking()&&tile.swingProgress==0){tile.rotation((tile.rotation()+1)%4);level.playSound(null,pos,net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:tool")),net.minecraft.sounds.SoundSource.BLOCKS,.6f,1);}
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmisc.animationTablet.notRotatable"),false);
        }else player.openMenu(tile);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){use(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){use(level,pos,player);return InteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof AnimationTabletBlockEntity tile){
            var stack=tile.removeItemNoUpdate(0);if(!stack.isEmpty()){float x=drops.nextFloat()*.8f+.1f,y=drops.nextFloat()*.8f+.1f,z=drops.nextFloat()*.8f+.1f;
                while(!stack.isEmpty()){var entity=new ItemEntity(level,pos.getX()+x,pos.getY()+y,pos.getZ()+z,stack.split(Math.min(stack.getCount(),drops.nextInt(21)+10)));entity.setDeltaMovement((float)drops.nextGaussian()*.05f,(float)drops.nextGaussian()*.05f+.2f,(float)drops.nextGaussian()*.05f);level.addFreshEntity(entity);}
            }level.updateNeighborsAt(pos,state.getBlock());
        }super.onRemove(state,level,pos,next,moving);
    }
}
