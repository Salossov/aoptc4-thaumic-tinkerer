package thaumic.tinkerer.kami.blocktalisman;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.block.TabletPlayer;

/** Defer the entire item action while FULL columns are prepared. Normal player
 * replay uses the native interaction/permission pipeline. Tablet replay resumes
 * its already-selected item phase with its stable block inventory as owner.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class TalismanActions {
    private static final Map<ServerPlayer,Job> PENDING=new IdentityHashMap<>();
    private static final Set<ServerPlayer> REPLAYING=Collections.newSetFromMap(new IdentityHashMap<>());
    /** null continues immediately; a result means deferred or rejected. */
    public static InteractionResult prepare(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel level))return null;
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
        if(REPLAYING.contains(player))return null;
        var previous=PENDING.get(player);
        if(previous!=null){
            if(previous.sameAction(context)&&previous.valid())return InteractionResult.CONSUME;
            previous.close();
        }
        var clicked=context.getClickedPos();
        if(level.isOutsideBuildHeight(clicked))return InteractionResult.FAIL;
        var target=clicked.relative(context.getClickedFace());
        // Ordinary placement/neighbor checks fit within two blocks. Modern
        // scaffolding searches up to seven horizontal cells before placement.
        int margin=BlockTalismanItem.getBlock(context.getItemInHand()).asItem() instanceof ScaffoldingBlockItem?8:2;
        var chunks=new ArrayList<ChunkPos>();boolean cold=false;
        for(int x=(Math.min(clicked.getX(),target.getX())-margin)>>4;x<=(Math.max(clicked.getX(),target.getX())+margin)>>4;x++)
            for(int z=(Math.min(clicked.getZ(),target.getZ())-margin)>>4;z<=(Math.max(clicked.getZ(),target.getZ())+margin)>>4;z++){
                var pos=new ChunkPos(x,z);chunks.add(pos);cold|=level.getChunkSource().getChunkNow(x,z)==null;
            }
        if(!cold)return null;
        // A foreign synthetic player has no stable lifetime owner to bind here.
        // Fail without spending rather than creating an unowned loading job.
        if(player instanceof FakePlayer&&!(player instanceof TabletPlayer))return InteractionResult.FAIL;
        if(!(player instanceof TabletPlayer)&&level.getServer().getPlayerList().getPlayer(player.getUUID())!=player)return InteractionResult.FAIL;
        var job=new Job(context,chunks);
        if(!job.valid()){job.close();return InteractionResult.FAIL;}
        PENDING.put(player,job);job.request.advance(2);sync(player);return InteractionResult.CONSUME;
    }
    private static final class Job implements AutoCloseable {
        final ServerPlayer player;final ServerLevel level;final InteractionHand hand;final ItemStack stack,snapshot;
        final AnimationTabletBlockEntity tablet;final BlockPos ownerPos;final int rotation;final boolean redstone;
        final GameType mode;final boolean sneaking;final AbstractContainerMenu menu;
        final double x,y,z;final float yaw,pitch;final BlockHitResult hit;
        final BlockState initial;final BlockEntity initialEntity;final boolean knownTarget;
        final ChunkPreparationApi.Request request;
        Job(UseOnContext context,Collection<ChunkPos> chunks){
            player=(ServerPlayer)context.getPlayer();level=(ServerLevel)context.getLevel();hand=context.getHand();stack=context.getItemInHand();snapshot=stack.copy();
            tablet=player instanceof TabletPlayer actor?actor.tablet():null;
            ownerPos=tablet==null?null:tablet.getBlockPos().immutable();rotation=tablet==null?0:tablet.rotation();redstone=tablet!=null&&tablet.redstone;
            mode=player.gameMode.getGameModeForPlayer();sneaking=player.isShiftKeyDown();menu=player.containerMenu;
            x=player.getX();y=player.getY();z=player.getZ();yaw=player.getYRot();pitch=player.getXRot();
            hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos().immutable(),context.isInside());
            var chunk=level.getChunkSource().getChunkNow(hit.getBlockPos().getX()>>4,hit.getBlockPos().getZ()>>4);
            knownTarget=chunk!=null;initial=knownTarget?chunk.getBlockState(hit.getBlockPos()):null;
            initialEntity=knownTarget?chunk.getBlockEntity(hit.getBlockPos()):null;
            request=tablet==null?ChunkPreparationApi.prepare(player,chunks):ChunkPreparationApi.prepare(tablet,chunks);
        }
        boolean sameAction(UseOnContext context){return stack==context.getItemInHand()&&hand==context.getHand()
                &&hit.getBlockPos().equals(context.getClickedPos())&&hit.getDirection()==context.getClickedFace()
                &&hit.getLocation().equals(context.getClickLocation())&&hit.isInside()==context.isInside();}
        boolean valid(){
            if(request.closed()||player.level()!=level||!player.isAlive()||player.isRemoved()||!ItemStack.matches(stack,snapshot))return false;
            if(tablet!=null){
                if(hand!=InteractionHand.MAIN_HAND||tablet.isRemoved()||tablet.getLevel()!=level||!tablet.getBlockPos().equals(ownerPos)
                        ||tablet.player()!=player||tablet.leftClick||tablet.rotation()!=rotation||tablet.redstone!=redstone||tablet.getItem(0)!=stack)return false;
                var ownerChunk=level.getChunkSource().getChunkNow(ownerPos.getX()>>4,ownerPos.getZ()>>4);
                if(ownerChunk==null||ownerChunk.getBlockEntities().get(ownerPos)!=tablet)return false;
            }else if(level.getServer().getPlayerList().getPlayer(player.getUUID())!=player||player.getItemInHand(hand)!=stack
                    ||player.gameMode.getGameModeForPlayer()!=mode||player.isShiftKeyDown()!=sneaking||player.containerMenu!=menu
                    ||player.getX()!=x||player.getY()!=y||player.getZ()!=z||player.getYRot()!=yaw||player.getXRot()!=pitch
                    ||!player.canInteractWithBlock(hit.getBlockPos(),1))return false;
            if(knownTarget){
                var chunk=level.getChunkSource().getChunkNow(hit.getBlockPos().getX()>>4,hit.getBlockPos().getZ()>>4);
                if(chunk==null||chunk.getBlockState(hit.getBlockPos())!=initial||chunk.getBlockEntity(hit.getBlockPos())!=initialEntity)return false;
            }
            return true;
        }
        void replay(){
            REPLAYING.add(player);
            try{
                if(tablet==null)player.gameMode.useItemOn(player,level,stack,hand,hit);
                else{
                    player.setItemInHand(hand,stack);
                    try{stack.useOn(new UseOnContext(level,player,hand,stack,hit));}
                    finally{tablet.setItem(0,player.getItemInHand(hand));player.setItemInHand(hand,ItemStack.EMPTY);}
                }
            }finally{REPLAYING.remove(player);}
        }
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);sync(player);}
    }
    private static void sync(ServerPlayer player){
        if(player instanceof FakePlayer||player.getServer().getPlayerList().getPlayer(player.getUUID())!=player)return;
        player.inventoryMenu.broadcastFullState();
        if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastFullState();
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:List.copyOf(PENDING.values()))if(job.level==level){
            if(!job.valid()){job.close();continue;}
            if(job.request.advance(2))try{if(job.valid())job.replay();}finally{job.close();}
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
    public static boolean hasPending(ServerPlayer player){return PENDING.containsKey(player);}
    private TalismanActions(){}
}
