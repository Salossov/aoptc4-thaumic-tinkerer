package thaumic.tinkerer.kami.bedrock;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;

/** Ordinary tile route. Source search/cache/frame rules, modern exit and entity
 * lifecycle. Destination work is owned by the still-ticking source portal tile.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class BedrockEntityPortals {
    private static final Map<Entity,Trip> PENDING=new LinkedHashMap<>();
    private static final Map<ServerLevel,Manager> MANAGERS=new IdentityHashMap<>();
    private record Cached(LegacyPortalSearch.Pos pos,long lastUse){}
    private static final class Manager {
        final Random random;final Map<Long,Cached> cache=new HashMap<>();final List<Trip> queue=new ArrayList<>();
        Manager(ServerLevel level){random=new Random(level.getSeed());}
    }
    private static final class Trip implements AutoCloseable {
        final BedrockPortalBlockEntity owner;final Entity entity;final ServerLevel source,target;
        final ChunkPreparationApi.Request request;final Manager manager;final double x,y,z;final long key;final boolean fromEnd;
        final Cached capturedCache;
        final LegacyPortalSearch.Terrain terrain;LegacyPortalSearch.Existing existing;LegacyPortalSearch.Creation creation;
        LegacyPortalSearch.Site plannedSite;boolean committing;
        Trip(BedrockPortalBlockEntity owner,Entity entity,ServerLevel target){
            this.owner=owner;this.entity=entity;source=(ServerLevel)owner.getLevel();this.target=target;fromEnd=source.dimension()==Level.END;
            double scale=DimensionType.getTeleportationScale(source.dimensionType(),target.dimensionType());
            x=fromEnd?entity.getX():Mth.clamp((int)(entity.getX()*scale),-29999872,29999872);
            y=entity.getY();z=fromEnd?entity.getZ():Mth.clamp((int)(entity.getZ()*scale),-29999872,29999872);
            key=ChunkPos.asLong(Mth.floor(x),Mth.floor(z));
            manager=MANAGERS.computeIfAbsent(target,Manager::new);capturedCache=fromEnd?null:manager.cache.get(key);
            int maxWidth=Mth.ceil(entity.getDimensions(entity.getPose()).width());
            for(var passenger:entity.getIndirectPassengers())maxWidth=Math.max(maxWidth,Mth.ceil(passenger.getDimensions(passenger.getPose()).width()));
            int centerX=capturedCache==null?Mth.floor(x):capturedCache.pos().x(),centerZ=capturedCache==null?Mth.floor(z):capturedCache.pos().z();
            int radius=(fromEnd?0:capturedCache==null?150:22)+maxWidth+2;var columns=new ArrayList<ChunkPos>();
            for(int cx=(centerX-radius)>>4;cx<=(centerX+radius)>>4;cx++)for(int cz=(centerZ-radius)>>4;cz<=(centerZ+radius)>>4;cz++){
                var column=new ChunkPos(cx,cz);
                if(column.getMinBlockX()<30000000&&column.getMaxBlockX()>=-30000000&&column.getMinBlockZ()<30000000&&column.getMaxBlockZ()>=-30000000)columns.add(column);
            }
            request=ChunkPreparationApi.prepare(owner,target,columns);
            manager.queue.add(this);
            terrain=new LegacyPortalSearch.Terrain(){
                public int height(){return target.getMaxBuildHeight();}
                public boolean portal(int x,int y,int z){return state(x,y,z).is(Blocks.NETHER_PORTAL);}
                public boolean air(int x,int y,int z){return state(x,y,z).isAir();}
                @SuppressWarnings("deprecation") public boolean solid(int x,int y,int z){return state(x,y,z).isSolid();}
            };
        }
        BlockState state(int x,int y,int z){
            var pos=new BlockPos(x,y,z);if(!Level.isInSpawnableBounds(pos)||target.isOutsideBuildHeight(pos))return Blocks.AIR.defaultBlockState();
            var chunk=target.getChunkSource().getChunkNow(x>>4,z>>4);
            if(chunk==null)throw new IllegalStateException("Prepared portal terrain became unavailable: "+pos);
            return chunk.getBlockState(pos);
        }
        boolean valid(){
            var pos=owner.getBlockPos();
            return BedrockPortals.enabled()&&!request.closed()&&owner.getLevel()==source
                    &&(committing||!owner.isRemoved()&&source.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null
                    &&source.getBlockEntity(pos)==owner&&source.shouldTickBlocksAt(ChunkPos.asLong(pos)))
                    &&!entity.isRemoved()&&entity.level()==source&&entity.getBoundingBox().intersects(new AABB(pos));
        }
        boolean advance(){
            if(!request.advance(2)||manager.queue.getFirst()!=this)return false;
            if(fromEnd){move(null);return true;}
            if(capturedCache!=null){manager.cache.put(key,new Cached(capturedCache.pos(),target.getGameTime()));move(capturedCache.pos());return true;}
            if(existing==null&&creation==null){
                var cached=manager.cache.get(key);
                if(cached!=null){manager.cache.put(key,new Cached(cached.pos(),target.getGameTime()));move(cached.pos());return true;}
                existing=new LegacyPortalSearch.Existing(terrain,x,y,z);
            }
            if(existing!=null){
                if(!existing.advance(1024))return false;
                var found=existing.result();existing=null;
                if(plannedSite!=null){commit(found.orElse(null));return true;}
                if(found.isPresent()){
                    manager.cache.put(key,new Cached(found.get(),target.getGameTime()));move(found.get());return true;
                }
                creation=new LegacyPortalSearch.Creation(terrain,x,y,z,manager.random.nextInt(4));
            }
            if(!creation.advance(64))return false;
            plannedSite=creation.result();creation=null;
            // Run the source's second search against the planned blocks. Keep
            // the real portal unmodified until native transfer is ready, so it
            // cannot activate early or remove this task's owner during a yield.
            existing=new LegacyPortalSearch.Existing(LegacyPortalSearch.planned(terrain,plannedSite),x,y,z);return false;
        }
        void commit(LegacyPortalSearch.Pos anchor){
            if(!valid())return;committing=true;
            try{
            var site=plannedSite;
            for(var cell:LegacyPortalSearch.platform(site))target.setBlockAndUpdate(pos(cell.pos()),fill(cell.fill(),site));
            var frame=LegacyPortalSearch.frame(site);
            for(int pass=0;pass<4;pass++){
                for(var cell:frame)target.setBlock(pos(cell.pos()),fill(cell.fill(),site),2);
                for(var cell:frame){var pos=pos(cell.pos());target.updateNeighborsAt(pos,target.getBlockState(pos).getBlock());}
            }
            // Native neighbor callbacks can reject a planned frame (for example
            // above build height). Never treat a virtual portal as a real exit.
            if(anchor!=null&&!state(anchor.x(),anchor.y(),anchor.z()).is(Blocks.NETHER_PORTAL))anchor=null;
            if(anchor!=null)manager.cache.put(key,new Cached(anchor,target.getGameTime()));
            move(anchor);
            }finally{committing=false;}
        }
        void move(LegacyPortalSearch.Pos anchor){
            if(!valid())return;
            var transition=anchor==null?new DimensionTransition(target,new Vec3(x,y,z),entity.getDeltaMovement(),entity.getYRot(),entity.getXRot(),DimensionTransition.DO_NOTHING)
                    :BedrockPortalExit.transition(target,pos(anchor),entity);
            // Native lifecycle carries passengers and keeps same-level identity,
            // as explicitly chosen for this modern port.
            entity.changeDimension(transition);
        }
        @Override public void close(){request.close();manager.queue.remove(this);if(PENDING.get(entity)==this)PENDING.remove(entity);}
    }
    private static BlockPos pos(LegacyPortalSearch.Pos pos){return new BlockPos(pos.x(),pos.y(),pos.z());}
    private static BlockState fill(LegacyPortalSearch.Fill fill,LegacyPortalSearch.Site site){
        return switch(fill){
            case AIR->Blocks.AIR.defaultBlockState();case OBSIDIAN->Blocks.OBSIDIAN.defaultBlockState();
            case PORTAL->Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS,site.orientation()%2==0?Direction.Axis.Z:Direction.Axis.X);
        };
    }
    public static void touch(BedrockPortalBlockEntity owner,Entity entity){
        if(!BedrockPortals.enabled()||entity.isRemoved()||!(owner.getLevel() instanceof ServerLevel source))return;
        var vehicle=entity.getRootVehicle();
        if(vehicle!=entity&&vehicle.getBoundingBox().intersects(new AABB(owner.getBlockPos())))entity=vehicle;
        if(entity instanceof ServerPlayer player&&BedrockPortals.hasPending(player))return;
        for(var passenger:entity.getIndirectPassengers())if(passenger instanceof ServerPlayer player&&BedrockPortals.hasPending(player))return;
        var old=PENDING.get(entity);if(old!=null){if(old.valid())return;old.close();}
        var target=source.getServer().getLevel(BedrockContent.DIMENSION);if(target==null)return;
        PENDING.put(entity,new Trip(owner,entity,target));
    }
    public static boolean hasPending(Entity entity){return PENDING.containsKey(entity)||PENDING.containsKey(entity.getRootVehicle());}
    public static void removed(BedrockPortalBlockEntity owner){for(var trip:List.copyOf(PENDING.values()))if(trip.owner==owner&&!trip.committing)trip.close();}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(event.getLevel() instanceof ServerLevel level){
            var manager=MANAGERS.get(level);
            if(manager!=null&&level.getGameTime()%100==0)manager.cache.values().removeIf(cached->cached.lastUse()<level.getGameTime()-600);
        }
        for(var trip:List.copyOf(PENDING.values()))if(trip.source==event.getLevel()){
            if(!trip.valid()){trip.close();continue;}
            if(trip.advance())trip.close();
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(!(event.getLevel() instanceof ServerLevel))return;for(var trip:List.copyOf(PENDING.values()))if(trip.source==event.getLevel()||trip.target==event.getLevel())trip.close();MANAGERS.remove(event.getLevel());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){for(var trip:List.copyOf(PENDING.values()))if(trip.source.getServer()==event.getServer())trip.close();MANAGERS.keySet().removeIf(level->level.getServer()==event.getServer());}
    private BedrockEntityPortals(){}
}
