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


public final class AspectAnalyzerBlock extends BaseEntityBlock {
    private final java.util.Random drops=new java.util.Random();
    public AspectAnalyzerBlock(){super(Properties.of().mapColor(MapColor.WOOD).strength(1.7f,.6f).sound(SoundType.WOOD).ignitedByLava());}
    @Override protected MapCodec<? extends AspectAnalyzerBlock> codec(){return simpleCodec(properties->new AspectAnalyzerBlock());}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AspectAnalyzerBlockEntity(pos,state);}
    private void open(Level level,BlockPos pos,Player player){if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof AspectAnalyzerBlockEntity tile)player.openMenu(tile);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){open(level,pos,player);return ItemInteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){open(level,pos,player);return InteractionResult.sidedSuccess(level.isClientSide());}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof AspectAnalyzerBlockEntity tile){
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
