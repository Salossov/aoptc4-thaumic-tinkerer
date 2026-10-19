package thaumic.tinkerer.fire;

import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.*;
import thaumic.tinkerer.TinkererConfig;

/** Release-164 fire tick, including its coordinate permutation and random-call order. */
public final class ImbuedFireBlock extends FireBlock {
    public final ImbuedFireKind kind;
    public ImbuedFireBlock(ImbuedFireKind kind){
        super(Properties.of().mapColor(MapColor.FIRE).replaceable().noCollission().instabreak().randomTicks().noLootTable());
        this.kind=kind;
    }
    public int tickDelay(){return kind==ImbuedFireKind.PERDITIO?1:200;}
    // Connection faces are read by the client model using ordinary vanilla fire,
    // as in RenderBlocks. Rendering must not populate the order-fire drop cache.
    @Override protected BlockState getStateForPlacement(BlockGetter level,BlockPos pos){return defaultBlockState();}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return true;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public boolean isPossibleToRespawnInThis(BlockState state){return false;}
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){return state;}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbour,BlockPos neighbourPos,boolean moving){
        if(!level.isClientSide()&&!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)&&!isNeighbourTarget(level,pos))level.removeBlock(pos,false);
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){
        if(previous.is(this))return;
        if(!level.isClientSide()){
            if(level.dimension()==Level.OVERWORLD||level.dimension()==Level.NETHER){
                var portal=net.neoforged.neoforge.event.EventHooks.onTrySpawnPortal(level,pos,PortalShape.findEmptyPortalShape(level,pos,Direction.Axis.X));
                if(portal.isPresent()){portal.get().createPortalBlocks();return;}
            }
            level.scheduleTick(pos,this,tickDelay()+level.random.nextInt(10));
        }
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){
        if(entity instanceof Player player)com.mojang.logging.LogUtils.getLogger().info("Player: {} placed TT Fire",player.getDisplayName().getString());
    }
    public BlockState transformation(Level level,BlockPos pos,BlockState query){return kind.transformation(level,pos,query);}
    public boolean isTarget(Level level,BlockPos pos,BlockState query){return transformation(level,pos,query)!=null;}
    public boolean isResult(Level level,BlockPos pos,BlockState query){return kind.isResult(level,pos,query);}
    public boolean isNeighbourTarget(Level level,BlockPos pos){
        for(var direction:Direction.values()){
            var adjacent=pos.relative(direction);
            if(level.hasChunkAt(adjacent)&&isTarget(level,adjacent,level.getBlockState(adjacent)))return true;
        }return false;
    }
    public int decayChance(ServerLevel level,BlockPos pos){return kind==ImbuedFireKind.ORDO?OrderFireRules.decayChance(level,pos):1;}
    public int flammability(BlockGetter level,BlockPos pos,Direction side){
        var state=level.getBlockState(pos);
        if(level instanceof Level world){if(isTarget(world,pos,state))return 100;if(isResult(world,pos,state))return 0;}
        return state.getFlammability(level,pos,side);
    }
    @Override public boolean canCatchFire(BlockGetter level,BlockPos pos,Direction face){return flammability(level,pos,face)>0;}
    /** Historical signature reads/writes (x,z,y), but asks the map and decay at (x,y,z). */
    public void setWithTransmutation(ServerLevel level,int x,int y,int z,int age,Block replacement){
        var destination=new BlockPos(x,z,y);var lookup=new BlockPos(x,y,z);var state=level.getBlockState(destination);
        if(isTarget(level,lookup,state)&&new Random().nextInt(decayChance(level,lookup))==0){
            var result=transformation(level,lookup,state);
            if(result!=null)level.setBlock(destination,result,3);
        }else level.setBlock(destination,replacement==this?defaultBlockState().setValue(AGE,age):replacement.defaultBlockState(),3);
    }
    private void catchFire(ServerLevel level,BlockPos pos,int strength,RandomSource random,int age,Direction side){
        if(random.nextInt(strength)<flammability(level,pos,side)){
            var previous=level.getBlockState(pos);boolean tnt=previous.is(Blocks.TNT);
            if(random.nextInt(age+10)<5&&!level.isRainingAt(pos))setWithTransmutation(level,pos.getX(),pos.getY(),pos.getZ(),Math.min(15,age+random.nextInt(5)/4),this);
            else setWithTransmutation(level,pos.getX(),pos.getY(),pos.getZ(),0,Blocks.AIR);
            if(tnt)previous.onCaughtFire(level,pos,side,null);
        }
    }
    private int encouragement(ServerLevel level,BlockPos pos){return level.getBlockState(pos).isAir()&&isNeighbourTarget(level,pos)?100:0;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)||!TinkererConfig.IMBUED_FIRE.get())return;
        if(!level.isRaining()||!level.isRainingAt(pos)&&!level.isRainingAt(pos.west())&&!level.isRainingAt(pos.east())&&!level.isRainingAt(pos.north())&&!level.isRainingAt(pos.south())){
            if(!isNeighbourTarget(level,pos))level.removeBlock(pos,false);
            if(random.nextInt(20)==0&&isNeighbourTarget(level,pos))for(var direction:Direction.values()){
                var adjacent=pos.relative(direction);var original=level.getBlockState(adjacent);
                if(isTarget(level,adjacent,original)){
                    if(new Random().nextInt(decayChance(level,adjacent))==0){var result=transformation(level,adjacent,original);if(result!=null)level.setBlock(adjacent,result,3);}
                    else level.removeBlock(adjacent,false);
                }
            }
        }else level.removeBlock(pos,false);
        var current=level.getBlockState(pos);int age=current.hasProperty(AGE)?current.getValue(AGE):0;
        if(age<15){int increment=random.nextInt(3)/2;if(current.hasProperty(AGE))level.setBlock(pos,current.setValue(AGE,age+increment),4);}
        level.scheduleTick(pos,this,tickDelay()+random.nextInt(3));
        boolean humid=level.getBiome(pos).value().getModifiedClimateSettings().downfall()>.85f;int modifier=humid?-50:0;
        for(var direction:Direction.values())catchFire(level,pos.relative(direction),300+modifier,random,age,direction);
        for(int x=pos.getX()-1;x<=pos.getX()+1;x++)for(int z=pos.getZ()-1;z<=pos.getZ()+1;z++)for(int y=pos.getY()-1;y<=pos.getY()+2;y++){
            if(x==pos.getX()&&y==pos.getY()&&z==pos.getZ())continue;
            var candidate=new BlockPos(x,y,z);int chance=encouragement(level,candidate);
            if(chance>0){int threshold=(chance+70)/(age+30)+70;if(humid)threshold/=2;
                if(threshold>0&&random.nextInt(100)<=threshold&&(!level.isRaining()||!level.isRainingAt(candidate))&&!level.isRainingAt(new BlockPos(x-1,y,pos.getZ()))&&!level.isRainingAt(candidate.east())&&!level.isRainingAt(candidate.north())&&!level.isRainingAt(candidate.south()))
                    setWithTransmutation(level,x,z,y,Math.min(15,age+random.nextInt(5)/4),this);
            }
        }
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(random.nextInt(24)==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.FIRE_AMBIENT,SoundSource.BLOCKS,1+random.nextFloat(),random.nextFloat()*.7f+.3f,false);
        var fire=(FireBlock)Blocks.FIRE;
        if(level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||fire.canCatchFire(level,pos.below(),Direction.UP)){
            for(int i=0;i<3;i++)level.addParticle(ParticleTypes.LARGE_SMOKE,pos.getX()+random.nextFloat(),pos.getY()+random.nextFloat()*.5f+.5f,pos.getZ()+random.nextFloat(),0,0,0);
        }else for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH,Direction.UP}){
            if(fire.canCatchFire(level,pos.relative(direction),direction.getOpposite()))for(int i=0;i<2;i++){
                float x=random.nextFloat(),y=random.nextFloat(),z=random.nextFloat();
                if(direction==Direction.WEST)x*=.1f;else if(direction==Direction.EAST)x=1-x*.1f;
                else if(direction==Direction.NORTH)z*=.1f;else if(direction==Direction.SOUTH)z=1-z*.1f;else y=1-y*.1f;
                level.addParticle(ParticleTypes.LARGE_SMOKE,pos.getX()+x,pos.getY()+y,pos.getZ()+z,0,0,0);
            }
        }
    }
}
