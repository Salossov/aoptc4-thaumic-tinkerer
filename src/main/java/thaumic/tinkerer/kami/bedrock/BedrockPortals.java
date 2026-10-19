package thaumic.tinkerer.kami.bedrock;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Special player collision route, separate from the original tile's generic
 * entity route. Hold destination columns before any native dimension transfer.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class BedrockPortals {
    private static final Map<ServerPlayer,Trip> PENDING=new IdentityHashMap<>();
    public static boolean enabled(){return TinkererConfig.KAMI_FORCE_ENABLED.get()&&TinkererConfig.KAMI_BEDROCK_ENABLED.get();}
    private static final class Trip implements AutoCloseable {
        final ServerPlayer player;final ServerLevel source,target;final BlockPos portal;
        final int x,z;final boolean entering;final ChunkPreparationApi.Request request;
        Trip(ServerPlayer player,BlockPos portal,ServerLevel target,boolean entering){
            this.player=player;source=player.serverLevel();this.portal=portal.immutable();this.target=target;this.entering=entering;
            var random=new Random();x=entering?portal.getX():(int)player.getX()+random.nextInt(100)-50;
            z=entering?portal.getZ():(int)player.getZ()+random.nextInt(100)-50;
            var chunks=new ArrayList<ChunkPos>();
            for(int cx=(x-1)>>4;cx<=(x+1)>>4;cx++)for(int cz=(z-1)>>4;cz<=(z+1)>>4;cz++)chunks.add(new ChunkPos(cx,cz));
            request=ChunkPreparationApi.prepare(player,target,chunks);
        }
        boolean valid(){
            return enabled()&&!request.closed()&&player.isAlive()&&!player.isRemoved()&&player.level()==source
                    &&source.getServer().getPlayerList().getPlayer(player.getUUID())==player
                    &&source.getChunkSource().getChunkNow(portal.getX()>>4,portal.getZ()>>4)!=null
                    &&source.getBlockState(portal).is(BedrockContent.PORTAL.get())&&player.getBoundingBox().intersects(new AABB(portal))
                    &&(entering?BedrockPortalWorlds.isSurfaceWorld(source):source.dimension()==BedrockContent.DIMENSION);
        }
        void perform(){
            if(!valid())return;
            int y=251;
            if(!entering){
                var height=BedrockPortalGeometry.returnHeight(target.getMinBuildHeight(),target.getMaxBuildHeight(),
                        scanY->target.getBlockState(new BlockPos(x,scanY,z)).isAir());
                if(height.isEmpty())return;
                y=height.getAsInt();
            }
            var moved=player.changeDimension(new DimensionTransition(target,new Vec3(x+.5,y,z+.5),player.getDeltaMovement(),0,0,DimensionTransition.DO_NOTHING));
            if(moved==null||player.level()!=target)return;
            if(entering){
                for(int clearY=250;clearY<=253;clearY++){
                    var pos=new BlockPos(x,clearY,z);if(target.getBlockState(pos).is(Blocks.BEDROCK))target.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                }
                var back=new BlockPos(x,254,z);if(target.getBlockState(back).is(Blocks.BEDROCK))target.setBlockAndUpdate(back,BedrockContent.PORTAL.get().defaultBlockState());
            }
        }
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);}
    }
    public static void touch(ServerPlayer player,BlockPos portal){
        if(!enabled()||player.isRemoved()||!player.isAlive()||BedrockEntityPortals.hasPending(player))return;
        var old=PENDING.get(player);if(old!=null){if(old.valid())return;old.close();}
        var source=player.serverLevel();boolean entering=BedrockPortalWorlds.isSurfaceWorld(source);
        if(!entering&&source.dimension()!=BedrockContent.DIMENSION)return;
        var target=source.getServer().getLevel(entering?BedrockContent.DIMENSION:Level.OVERWORLD);if(target==null)return;
        var trip=new Trip(player,portal,target,entering);PENDING.put(player,trip);
        if(trip.request.advance(2))try{trip.perform();}finally{trip.close();}
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        for(var trip:List.copyOf(PENDING.values()))if(trip.source==event.getLevel()){
            if(!trip.valid()){trip.close();continue;}
            if(trip.request.advance(2))try{trip.perform();}finally{trip.close();}
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(!(event.getLevel() instanceof ServerLevel))return;for(var trip:List.copyOf(PENDING.values()))if(trip.source==event.getLevel()||trip.target==event.getLevel())trip.close();}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(var trip:List.copyOf(PENDING.values()))if(trip.source.getServer()==event.getServer())trip.close();}
    public static boolean hasPending(ServerPlayer player){return PENDING.containsKey(player);}
    private BedrockPortals(){}
}
