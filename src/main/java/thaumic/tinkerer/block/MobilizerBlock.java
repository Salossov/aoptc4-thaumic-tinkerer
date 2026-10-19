package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Release-164 defaults: full cube, stone sound, zero hardness and resistance. */
public final class MobilizerBlock extends BaseEntityBlock {
    public final boolean relay;
    public MobilizerBlock(boolean relay){super(Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(0,0).sound(SoundType.STONE));this.relay=relay;}
    @Override protected MapCodec<MobilizerBlock> codec(){return simpleCodec(p->new MobilizerBlock(relay));}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return relay?new MobilizerRelayBlockEntity(pos,state):new MobilizerBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(relay)return createTickerHelper(type,MobilizerContent.RELAY_TILE.get(),(world,pos,current,tile)->tile.tick());
        return level.isClientSide()?null:createTickerHelper(type,MobilizerContent.LOCOMOTIVE_TILE.get(),(world,pos,current,tile)->tile.tick());
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())){
            if(level.getBlockEntity(pos) instanceof MobilizerBlockEntity tile)tile.destroyed();
            if(level.getBlockEntity(pos) instanceof MobilizerRelayBlockEntity tile)tile.cancelWork();
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
