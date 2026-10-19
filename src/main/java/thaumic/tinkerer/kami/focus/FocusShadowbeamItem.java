package thaumic.tinkerer.kami.focus;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;

/** Immediate 300-step release ray, deferred as one cast only while its bounded area is cold. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class FocusShadowbeamItem extends KamiFocusItem {
    public static final int COLOR=4915283;
    public static final Map<Aspect,Integer> VIS_COST=Map.of(Aspects.ORDER,25,Aspects.ENTROPY,25,Aspects.AIR,15);
    private static final SoundEvent BRAIN_SOUND=SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:brain"));
    private static final Map<ServerPlayer,Waiting> WAITING=new WeakHashMap<>();

    private static final class Waiting implements AutoCloseable {
        final ServerLevel level;
        final ItemStack wand,wandSnapshot,focus;
        final InteractionHand hand;
        final Vec3 position;
        final float yaw,pitch,eyeHeight;
        final Pose pose;
        final List<ChunkPos> chunks;
        ChunkPreparationApi.Request request;

        Waiting(ServerPlayer player,ItemStack wand,ItemStack focus,List<ChunkPos> chunks) {
            this.level=player.serverLevel();this.wand=wand;this.wandSnapshot=wand.copy();this.focus=focus.copy();
            this.hand=player.getUsedItemHand();this.position=player.position();
            this.yaw=player.getYRot();this.pitch=player.getXRot();this.eyeHeight=player.getEyeHeight();this.pose=player.getPose();
            this.chunks=chunks;
        }
        boolean valid(ServerPlayer player) {
            return active(player,wand,focus,level)&&player.getUsedItemHand()==hand
                    &&player.position().equals(position)&&player.getYRot()==yaw&&player.getXRot()==pitch
                    &&player.getPose()==pose&&player.getEyeHeight()==eyeHeight
                    &&ItemStack.matches(wand,wandSnapshot)&&!request.closed();
        }
        @Override public void close(){if(request!=null)request.close();}
    }

    public FocusShadowbeamItem(){super(new Properties().durability(1).rarity(Rarity.EPIC));}
    @Override public Map<Aspect,Integer> getVisCost(ItemStack stack){return VIS_COST;}
    @Override public boolean isVisCostPerTick(ItemStack stack){return true;}
    @Override public boolean continuous(ItemStack stack){return true;}
    @Override public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack,int rank){return List.of();}
    @Override public int getFocusColor(ItemStack stack){return COLOR;}
    @Override public String getSortingHelper(ItemStack stack){return "SHADOWBEAM";}

    private static boolean active(ServerPlayer player,ItemStack wand,ItemStack focus,ServerLevel level) {
        return player.serverLevel()==level&&player.isAlive()&&!player.isRemoved()
                &&level.getServer().getPlayerList().getPlayer(player.getUUID())==player
                &&!player.hasDisconnected()
                &&player.isUsingItem()&&player.getUseItem()==wand
                &&player.getItemInHand(player.getUsedItemHand())==wand
                &&ItemStack.matches(focus,WandApi.focus(wand));
    }

    @Override public boolean begin(ServerPlayer player,ItemStack wand,ItemStack focus,InteractionHand hand) {
        cancel(player);return true;
    }
    @Override public void end(ServerPlayer player,ItemStack wand,ItemStack focus){cancel(player);}

    @Override public boolean cast(ServerPlayer player,ItemStack wand,ItemStack focus) {
        ServerLevel level=player.serverLevel();
        Waiting waiting=WAITING.get(player);
        if(waiting!=null&&(!waiting.valid(player)||waiting.wand!=wand)){cancel(player);waiting=null;}
        if(!active(player,wand,focus,level)||!WandApi.consume(wand,player,VIS_COST,false)){cancel(player);return false;}
        if(waiting==null) {
            var start=ShadowbeamGeometry.origin(player.getX(),player.getEyeY(),player.getZ(),player.getYRot());
            var motion=ShadowbeamGeometry.motion(player.getYRot(),player.getXRot());
            if(!ShadowbeamGeometry.valid(start,motion))return false;
            var chunks=ShadowbeamGeometry.requiredChunks(start,motion);
            if(loaded(level,chunks))return fire(player,wand,level);
            waiting=new Waiting(player,wand,focus,chunks);
            waiting.request=ChunkPreparationApi.prepare(player,chunks);
            WAITING.put(player,waiting);
        }
        try {
            if(!waiting.request.advance(2)) {
                if(waiting.request.closed())cancel(player);
                return false;
            }
            if(!waiting.valid(player)||!loaded(level,waiting.chunks)||!WandApi.consume(wand,player,VIS_COST,false))return false;
            return fire(player,wand,level);
        } catch(RuntimeException|Error failure) {
            cancel(player);throw failure;
        } finally {
            if(waiting.request.closed()||loaded(level,waiting.chunks))cancel(player);
        }
    }


    private static boolean loaded(ServerLevel level,List<ChunkPos> chunks) {
        for(var chunk:chunks)if(level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null)return false;
        return true;
    }

    private static boolean fire(ServerPlayer player,ItemStack wand,ServerLevel level) {
        if(!ShadowbeamGeometry.valid(ShadowbeamGeometry.origin(player.getX(),player.getEyeY(),player.getZ(),player.getYRot()),
                ShadowbeamGeometry.motion(player.getYRot(),player.getXRot())))return false;
        if(!WandApi.consume(wand,player,VIS_COST,true))return false;
        if(level.random.nextInt(10)==0)level.playSound(null,player.getX(),player.getY(),player.getZ(),BRAIN_SOUND,SoundSource.PLAYERS,.5f,1);
        executeBeam(player,level);
        return true;
    }

    static void executeBeam(ServerPlayer thrower,ServerLevel level) {
        Vec3 position=ShadowbeamGeometry.origin(thrower.getX(),thrower.getEyeY(),thrower.getZ(),thrower.getYRot());
        Vec3 motion=ShadowbeamGeometry.motion(thrower.getYRot(),thrower.getXRot());
        var blocks=new LoadedBlocks(level);
        for(int tick=0;tick<300;tick++) {
            boolean belowVoid=position.y<level.getMinBuildHeight()-64;
            Vec3 end=position.add(motion);
            var context=new ClipContext(position,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,thrower);
            BlockHitResult blockHit=blocks.clip(context);
            Vec3 rayEnd=blockHit.getType()==HitResult.Type.MISS?end:blockHit.getLocation();
            var search=new AABB(position.x-.125,position.y,position.z-.125,position.x+.125,position.y+.25,position.z+.125)
                    .expandTowards(motion).inflate(1);
            Entity hit=null;double nearest=0;
            for(Entity entity:level.getEntities((Entity)null,search)) {
                if(!entity.isPickable()||(entity==thrower&&tick+1<5))continue;
                Vec3 point=ShadowbeamGeometry.intercept(entity.getBoundingBox().inflate((double).3f),position,rayEnd);
                if(point==null)continue;
                double distance=ShadowbeamGeometry.legacyDistance(position,point);
                if(distance<nearest||nearest==0){hit=entity;nearest=distance;}
            }
            if(hit!=null) {
                if(hit!=thrower&&(!(hit instanceof Player)||level.getServer().isPvpAllowed()))
                    hit.hurt(level.damageSources().playerAttack(thrower),8);
            } else if(blockHit.getType()!=HitResult.Type.MISS) {
                if(blocks.getBlockState(blockHit.getBlockPos()).is(Blocks.NETHER_PORTAL))break;
                motion=ShadowbeamGeometry.reflect(motion,blockHit.getDirection());
            }
            position=position.add(motion);
            if(tick>2)ParticleApi.broadcastShadowSparkle(level,(float)position.x,(float)position.y,(float)position.z,4);
            if(belowVoid)break;
        }
    }

    /** Shape queries may inspect neighbours; every read stays loaded-only. */
    private record LoadedBlocks(ServerLevel level) implements BlockGetter {
        private net.minecraft.world.level.chunk.LevelChunk chunk(BlockPos pos) {
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null)throw new IllegalStateException("Shadowbeam area became unavailable: "+new ChunkPos(pos));
            return chunk;
        }
        @Override public BlockState getBlockState(BlockPos pos){return chunk(pos).getBlockState(pos);}
        @Override public FluidState getFluidState(BlockPos pos){return chunk(pos).getFluidState(pos);}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return chunk(pos).getBlockEntity(pos);}
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
        @Override public BlockHitResult clip(ClipContext context) {
            return BlockGetter.traverseBlocks(context.getFrom(),context.getTo(),context,(ray,pos)->{
                var state=getBlockState(pos);
                var shape=state.is(Blocks.NETHER_PORTAL)?Shapes.block():ray.getBlockShape(state,this,pos);
                return clipWithInteractionOverride(ray.getFrom(),ray.getTo(),pos,shape,state);
            },ray->BlockHitResult.miss(ray.getTo(),net.minecraft.core.Direction.getNearest(ray.getFrom().subtract(ray.getTo()).x,
                    ray.getFrom().subtract(ray.getTo()).y,ray.getFrom().subtract(ray.getTo()).z),BlockPos.containing(ray.getTo())));
        }
    }

    private static void cancel(ServerPlayer player){var waiting=WAITING.remove(player);if(waiting!=null)waiting.close();}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player){var waiting=WAITING.get(player);if(waiting!=null&&!waiting.valid(player))cancel(player);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)cancel(player);}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){for(var player:List.copyOf(WAITING.keySet()))if(WAITING.get(player).level==event.getLevel())cancel(player);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var player:List.copyOf(WAITING.keySet()))if(WAITING.get(player).level.getServer()==event.getServer())cancel(player);}
}
