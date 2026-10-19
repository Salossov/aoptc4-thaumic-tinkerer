package thaumic.tinkerer.item.smelting;

import java.util.IdentityHashMap;
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
import thaumic.tinkerer.item.TinkererFocusItem;

/** A cold use tick waits without advancing source progress or paying vis. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class SmeltingActions {
    private static final Map<ServerPlayer,Job> PENDING=new IdentityHashMap<>();
    public static boolean defer(TinkererFocusItem item,ServerPlayer player,ItemStack wand,ItemStack focus,SmeltingRay.Trace ray){
        var previous=PENDING.get(player);
        if(previous!=null){if(previous.wand==wand&&previous.valid())return true;previous.close();}
        var columns=SmeltingRay.requiredChunks(ray);
        if(ray.complete()&&SmeltingRay.loaded(player.serverLevel(),columns))return false;
        if(player instanceof FakePlayer&&!(player instanceof TabletPlayer))return true;
        if(!(player instanceof TabletPlayer)&&player.server.getPlayerList().getPlayer(player.getUUID())!=player)return true;
        var job=new Job(item,player,wand,focus,ray,columns);
        try{if(!job.valid()){job.close();return true;}PENDING.put(player,job);job.request.advance(2);return true;}
        catch(RuntimeException|Error failure){job.close();throw failure;}
    }
    private static final class Job implements AutoCloseable {
        final TinkererFocusItem item;final ServerPlayer player;final ServerLevel level;final ItemStack wand,wandSnapshot,focus;
        final InteractionHand hand;final Vec3 position,eye;final float yaw,pitch;final Pose pose;final GameType mode;final AbstractContainerMenu menu;
        final boolean sneaking;final AnimationTabletBlockEntity tablet;final BlockPos ownerPos;final int rotation;final boolean redstone;
        final BlockPos target;final net.minecraft.core.Direction face;final BlockState state;final BlockEntity entity;
        final Set<ChunkPos> columns=new LinkedHashSet<>();ChunkPreparationApi.Request request;
        Job(TinkererFocusItem item,ServerPlayer player,ItemStack wand,ItemStack focus,SmeltingRay.Trace ray,List<ChunkPos> columns){
            this.item=item;this.player=player;level=player.serverLevel();this.wand=wand;wandSnapshot=wand.copy();this.focus=focus.copy();
            hand=player.getMainHandItem()==wand?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            position=player.position();eye=player.getEyePosition();yaw=player.getYRot();pitch=player.getXRot();pose=player.getPose();mode=player.gameMode.getGameModeForPlayer();menu=player.containerMenu;sneaking=player.isShiftKeyDown();
            tablet=player instanceof TabletPlayer actor?actor.tablet():null;ownerPos=tablet==null?null:tablet.getBlockPos().immutable();rotation=tablet==null?0:tablet.rotation();redstone=tablet!=null&&tablet.redstone;
            var hit=ray.complete()&&ray.hit()!=null&&ray.hit().getType()==HitResult.Type.BLOCK?ray.hit():null;
            target=hit==null?null:hit.getBlockPos().immutable();face=hit==null?null:hit.getDirection();state=target==null?null:level.getBlockState(target);entity=target==null?null:level.getBlockEntity(target);
            this.columns.addAll(columns);request=prepare();
        }
        private ChunkPreparationApi.Request prepare(){return tablet==null?ChunkPreparationApi.prepare(player,columns):ChunkPreparationApi.prepare(tablet,columns);}
        boolean valid(){
            if(request.closed()||player.level()!=level||!player.isAlive()||player.isRemoved()||!ItemStack.matches(wand,wandSnapshot)
                    ||!ItemStack.matches(focus,WandApi.focus(wand))||!player.isUsingItem()||player.getUseItem()!=wand||player.getUsedItemHand()!=hand)return false;
            if(tablet!=null){
                if(hand!=InteractionHand.MAIN_HAND||tablet.isRemoved()||tablet.getLevel()!=level||!tablet.getBlockPos().equals(ownerPos)||tablet.player()!=player
                        ||tablet.leftClick||tablet.rotation()!=rotation||tablet.redstone!=redstone||tablet.getItem(0)!=wand)return false;
                var chunk=level.getChunkSource().getChunkNow(ownerPos.getX()>>4,ownerPos.getZ()>>4);if(chunk==null||chunk.getBlockEntities().get(ownerPos)!=tablet)return false;
            }else if(player.server.getPlayerList().getPlayer(player.getUUID())!=player||player.hasDisconnected()||player.getItemInHand(hand)!=wand
                    ||!player.position().equals(position)||!player.getEyePosition().equals(eye)||player.getYRot()!=yaw||player.getXRot()!=pitch||player.getPose()!=pose
                    ||player.gameMode.getGameModeForPlayer()!=mode||player.containerMenu!=menu||player.isShiftKeyDown()!=sneaking)return false;
            if(target!=null){var chunk=level.getChunkSource().getChunkNow(target.getX()>>4,target.getZ()>>4);if(chunk==null||chunk.getBlockState(target)!=state||chunk.getBlockEntity(target)!=entity)return false;}
            return true;
        }
        boolean ready(SmeltingRay.Trace ray){
            var needed=SmeltingRay.requiredChunks(ray);if(ray.complete()&&SmeltingRay.loaded(level,needed))return true;
            if(columns.addAll(needed)){request.close();request=prepare();}return false;
        }
        void execute(SmeltingRay.Trace ray){
            if(!ray.complete()||ray.hit()==null||ray.hit().getType()!=HitResult.Type.BLOCK)return;
            if(target!=null&&(!target.equals(ray.hit().getBlockPos())||face!=ray.hit().getDirection()))return;
            if(tablet==null){
                try{SmeltingFocus.ready(item,player,wand,focus.copy(),ray);}
                finally{player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();}
            }
            else{player.setItemInHand(hand,wand);try{SmeltingFocus.ready(item,player,wand,focus.copy(),ray);}finally{tablet.setItem(0,player.getItemInHand(hand));player.setItemInHand(hand,ItemStack.EMPTY);}}
        }
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);}
    }
    public static void cancel(ServerPlayer player){var job=PENDING.get(player);if(job!=null)job.close();}
    public static void logout(String name){for(var job:List.copyOf(PENDING.values()))if(job.player.getGameProfile().getName().equals(name))job.close();}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:List.copyOf(PENDING.values()))if(job.level==level){
            try{if(!job.valid()){job.close();continue;}if(!job.request.advance(2))continue;var ray=SmeltingRay.trace(job.player);if(!job.ready(ray))continue;
                try{if(job.valid())job.execute(ray);}finally{job.close();}
            }catch(RuntimeException|Error failure){job.close();throw failure;}
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
    private SmeltingActions(){}
}
