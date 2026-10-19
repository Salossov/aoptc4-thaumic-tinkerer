package thaumic.tinkerer.crop;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.api.aspects.*;

/** Release harvest happens on removal, independently of vanilla loot tables. */
public final class InfusedCropBlock extends CropBlock implements EntityBlock {
    public InfusedCropBlock(){super(Properties.ofFullCopy(Blocks.WHEAT).noLootTable());}
    @Override public MapCodec<? extends CropBlock> codec(){return simpleCodec(properties->new InfusedCropBlock());}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(0,0,0,16,4,16);}
    @Override protected boolean isRandomlyTicking(BlockState state){return true;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var soil=level.getBlockState(pos.below());var support=soil.canSustainPlant(level,pos.below(),Direction.UP,state);
        return (level.getRawBrightness(pos,0)>=8||level.canSeeSky(pos))&&(support.isDefault()?soil.getBlock() instanceof FarmBlock:support.isTrue());
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InfusedCropBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==InfusedContent.CROP_TILE.get()?(world,pos,current,tile)->InfusedCropBlockEntity.tick(world,pos,current,(InfusedCropBlockEntity)tile):null;}
    public static int textureIndex(Aspect aspect){return aspect==Aspects.AIR?0:aspect==Aspects.FIRE?1:aspect==Aspects.WATER?2:aspect==Aspects.EARTH?3:aspect==Aspects.ORDER?4:aspect==Aspects.ENTROPY?5:6;}
    public static int textureAge(int age){return age<7?(age==6?5:age)>>1:3;}
    public static Aspect seedAspect(Level level,BlockPos pos,int age,InfusedCropBlockEntity tile){
        var current=tile.aspect;if(age>=7&&current!=null&&level.getBlockEntity(pos.below()) instanceof InfusedFarmlandBlockEntity soil){
            for(var aspect:soil.aspectList.getAspects()){
                if(new Random().nextInt(10)<(tile.primalTendencies.getAmount(Aspects.FIRE)+1)*soil.aspectList.getAmount(aspect)*soil.aspectList.getAmount(aspect)&&Aspect.combine(aspect,current)!=null)return Aspect.combine(aspect,current);
            }
        }return current;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide()&&level.getBlockEntity(pos) instanceof InfusedCropBlockEntity tile){
            var random=new Random();var seed=InfusedSeedsItem.of(seedAspect(level,pos,state.getValue(AGE),tile));InfusedSeedsItem.tendencies(seed,tile.primalTendencies);
            while(random.nextInt(10000)<Math.pow(tile.primalTendencies.getAmount(Aspects.ENTROPY),2))seed.grow(1);
            var output=new ArrayList<ItemStack>();output.add(seed);
            if(state.getValue(AGE)>=7&&tile.aspect!=null){
                do{if(level.getBlockEntity(pos.below()) instanceof InfusedFarmlandBlockEntity soil){soil.aspectList.add(tile.aspect,1);soil.reduceSaturatedAspects();soil.changed();}}
                while(level.random.nextInt(55)<tile.primalTendencies.getAmount(Aspects.EARTH));
                do{output.add(CropLoot.harvest((ServerLevel)level,tile.aspect));}while(level.random.nextInt(75)<tile.primalTendencies.getAmount(Aspects.ORDER));
            }
            for(var stack:output){
                double x=random.nextFloat()*.7f+(1f-.7f)*.5,y=random.nextFloat()*.7f+(1f-.7f)*.5,z=random.nextFloat()*.7f+(1f-.7f)*.5;
                if(stack.isEmpty())continue;var drop=new ItemEntity(level,pos.getX()+x,pos.getY()+y,pos.getZ()+z,stack);drop.setPickUpDelay(10);level.addFreshEntity(drop);
            }
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
