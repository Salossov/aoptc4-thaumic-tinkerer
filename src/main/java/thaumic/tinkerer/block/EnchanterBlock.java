package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EnchanterBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=box(0,0,0,16,12,16);
    private final java.util.Random drops=new java.util.Random();
    public EnchanterBlock(){super(Properties.of().strength(5,1200).sound(SoundType.STONE).noOcclusion());}
    @Override protected MapCodec<? extends EnchanterBlock> codec(){return simpleCodec(p->new EnchanterBlock());}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EnchanterBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,EnchanterContent.TILE.get(),EnchanterBlockEntity::tick);}
    private void open(Level level,BlockPos pos,Player player){if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof EnchanterBlockEntity tile){tile.sync();player.openMenu(tile,buffer->buffer.writeBlockPos(pos));}}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){open(level,pos,player);return InteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof EnchanterBlockEntity tile){
            for(int slot=0;slot<2;slot++){var stack=tile.removeItemNoUpdate(slot);if(stack.isEmpty())continue;
                float x=drops.nextFloat()*.8f+.1f,y=drops.nextFloat()*.8f+.1f,z=drops.nextFloat()*.8f+.1f;
                while(!stack.isEmpty()){var entity=new ItemEntity(level,pos.getX()+x,pos.getY()+y,pos.getZ()+z,stack.split(Math.min(stack.getCount(),drops.nextInt(21)+10)));entity.setDeltaMovement((float)drops.nextGaussian()*.05f,(float)drops.nextGaussian()*.05f+.2f,(float)drops.nextGaussian()*.05f);level.addFreshEntity(entity);}
            }
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
