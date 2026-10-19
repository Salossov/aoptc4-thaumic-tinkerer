package thaumic.tinkerer.item.dislocation;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.block.TabletPlayer;

/** One deferred click, bound to a real player's hand or a tablet's stable slot. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class DislocationActions {
    @FunctionalInterface public interface Action {
        boolean run(ServerPlayer player,ItemStack wand,ItemStack focus,DislocationRay.Trace readyRay);
    }
    private record Observed(BlockState state,BlockEntity entity){}
    private static final Map<ServerPlayer,Job> PENDING=new IdentityHashMap<>();

    /** True means the caller must leave the current click without mutating or paying. */
    public static boolean defer(ServerPlayer player,ItemStack wand,ItemStack focus,DislocationRay.Trace ray,Action action) {
        var previous=PENDING.get(player);
        if(previous!=null){if(previous.sameClick(wand,focus,ray)&&previous.valid())return true;previous.close();}
        var chunks=DislocationRay.requiredChunks(ray);
        if(ray.complete()&&DislocationRay.loaded(player.serverLevel(),chunks))return false;
        if(player instanceof FakePlayer&&!(player instanceof TabletPlayer))return true;
        if(!(player instanceof TabletPlayer)&&player.server.getPlayerList().getPlayer(player.getUUID())!=player)return true;
        var job=new Job(player,wand,focus,ray,chunks,action);
        try{
            if(!job.valid()){job.close();return true;}
            PENDING.put(player,job);job.request.advance(2);return true;
        }catch(RuntimeException|Error failure){job.close();throw failure;}
    }

    private static final class Job implements AutoCloseable {
        final ServerPlayer player;final ServerLevel level;final ItemStack wand,wandSnapshot,focus;
        final InteractionHand hand;final Vec3 position;final float yaw,pitch;final Pose pose;
        final GameType mode;final boolean sneaking;final AbstractContainerMenu menu;final double reach;
        final AnimationTabletBlockEntity tablet;final BlockPos ownerPos;final int rotation;final boolean redstone;
        final Vec3 from,to;final BlockHitResult originalHit;final Action action;
        final Map<BlockPos,Observed> observed=new LinkedHashMap<>();final Set<ChunkPos> columns=new LinkedHashSet<>();
        ChunkPreparationApi.Request request;

        Job(ServerPlayer player,ItemStack wand,ItemStack focus,DislocationRay.Trace ray,List<ChunkPos> chunks,Action action) {
            this.player=player;level=player.serverLevel();this.wand=wand;wandSnapshot=wand.copy();this.focus=focus.copy();
            hand=player.getMainHandItem()==wand?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            position=player.position();yaw=player.getYRot();pitch=player.getXRot();pose=player.getPose();
            mode=player.gameMode.getGameModeForPlayer();sneaking=player.isShiftKeyDown();menu=player.containerMenu;reach=player.blockInteractionRange();
            tablet=player instanceof TabletPlayer actor?actor.tablet():null;
            ownerPos=tablet==null?null:tablet.getBlockPos().immutable();rotation=tablet==null?0:tablet.rotation();redstone=tablet!=null&&tablet.redstone;
            from=ray.from();to=ray.to();originalHit=ray.complete()&&ray.hit()!=null&&ray.hit().getType()==HitResult.Type.BLOCK?ray.hit():null;
            this.action=action;columns.addAll(chunks);
            if(originalHit!=null){
                observe(originalHit.getBlockPos());
                var capture=DislocationCapture.read(focus);
                if(capture.isPresent())for(var part:capture.get().parts())observe(originalHit.getBlockPos().relative(originalHit.getDirection()).offset(part.offset()));
                else{
                    var source=observed.get(originalHit.getBlockPos());
                    if(source!=null){var offset=DislocationCapture.partnerOffset(source.state());if(offset!=null)observe(originalHit.getBlockPos().offset(offset));}
                }
            }
            request=prepare();
        }
        private ChunkPreparationApi.Request prepare(){return tablet==null?ChunkPreparationApi.prepare(player,columns):ChunkPreparationApi.prepare(tablet,columns);}
        private void observe(BlockPos pos){
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk!=null)observed.put(pos.immutable(),new Observed(chunk.getBlockState(pos),chunk.getBlockEntity(pos)));
        }
        boolean sameClick(ItemStack wand,ItemStack focus,DislocationRay.Trace ray){return this.wand==wand&&ItemStack.matches(this.focus,focus)&&from.equals(ray.from())&&to.equals(ray.to());}
        boolean valid(){
            if(request.closed()||player.level()!=level||!player.isAlive()||player.isRemoved()||!ItemStack.matches(wand,wandSnapshot)
                    ||!ItemStack.matches(focus,WandApi.focus(wand)))return false;
            if(tablet!=null){
                if(hand!=InteractionHand.MAIN_HAND||tablet.isRemoved()||tablet.getLevel()!=level||!tablet.getBlockPos().equals(ownerPos)
                        ||tablet.player()!=player||tablet.leftClick||tablet.rotation()!=rotation||tablet.redstone!=redstone||tablet.getItem(0)!=wand)return false;
                var chunk=level.getChunkSource().getChunkNow(ownerPos.getX()>>4,ownerPos.getZ()>>4);
                if(chunk==null||chunk.getBlockEntities().get(ownerPos)!=tablet)return false;
            }else if(level.getServer().getPlayerList().getPlayer(player.getUUID())!=player||player.hasDisconnected()||player.getItemInHand(hand)!=wand
                    ||!player.position().equals(position)||player.getYRot()!=yaw||player.getXRot()!=pitch||player.getPose()!=pose
                    ||player.gameMode.getGameModeForPlayer()!=mode||player.isShiftKeyDown()!=sneaking||player.containerMenu!=menu||player.blockInteractionRange()!=reach)return false;
            for(var entry:observed.entrySet()){
                var pos=entry.getKey();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                if(chunk==null||chunk.getBlockState(pos)!=entry.getValue().state()||chunk.getBlockEntity(pos)!=entry.getValue().entity())return false;
            }
            return true;
        }
        /** Repeating a cold shape read may reveal another dependency; extend the same click. */
        boolean ready(DislocationRay.Trace ray){
            var needed=DislocationRay.requiredChunks(ray);
            if(ray.complete()&&DislocationRay.loaded(level,needed))return true;
            if(columns.addAll(needed)){request.close();request=prepare();}
            return false;
        }
        void execute(DislocationRay.Trace ray){
            if(!ray.complete()||ray.hit()==null||ray.hit().getType()!=HitResult.Type.BLOCK)return;
            if(originalHit!=null&&(!originalHit.getBlockPos().equals(ray.hit().getBlockPos())||originalHit.getDirection()!=ray.hit().getDirection()))return;
            if(tablet==null){if(action.run(player,wand,focus.copy(),ray))player.swing(hand,true);}
            else {
                player.setItemInHand(hand,wand);
                try{action.run(player,wand,focus.copy(),ray);}
                finally{tablet.setItem(0,player.getItemInHand(hand));player.setItemInHand(hand,ItemStack.EMPTY);}
            }
        }
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);sync(player);}
    }
    private static void sync(ServerPlayer player){
        if(player instanceof FakePlayer||player.server.getPlayerList().getPlayer(player.getUUID())!=player)return;
        player.inventoryMenu.broadcastFullState();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastFullState();
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:List.copyOf(PENDING.values()))if(job.level==level){
            try{
                if(!job.valid()){job.close();continue;}
                if(!job.request.advance(2))continue;
                var ray=DislocationRay.trace(job.player);
                if(!job.ready(ray))continue;
                try{if(job.valid())job.execute(ray);}finally{job.close();}
            }catch(RuntimeException|Error failure){job.close();throw failure;}
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
    private DislocationActions(){}
}
