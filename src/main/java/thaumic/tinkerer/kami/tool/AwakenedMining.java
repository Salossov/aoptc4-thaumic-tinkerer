package thaumic.tinkerer.kami.tool;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.BlockMaterials;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import static thaumic.tinkerer.kami.tool.AwakenedMiningShape.*;

/** Original extra-block order, with player-owned asynchronous FULL preparation.
 * All mutation and polling stay on the server thread; no chunk future is joined.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class AwakenedMining {
    private static final Map<ServerPlayer,Job> PENDING=new IdentityHashMap<>();
    private static final Set<ServerPlayer> BREAKING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<TagKey<Block>> TREE_BLOCK_FAMILIES=List.of("log","log2","magical_log").stream()
            .map(name->TagKey.create(Registries.BLOCK,ThaumicTinkerer.id("ichor_tools/tree_families/"+name))).toList();
    /** Several old metadata variants shared one Block object. Preserve that
     * identity across modern oak/spruce/etc and greatwood/silverwood splits. */
    public static boolean sameTreeBlock(BlockState state,Block reference){
        if(state.is(reference))return true;
        for(var family:TREE_BLOCK_FAMILIES)if(reference.defaultBlockState().is(family)&&state.is(family))return true;
        return false;
    }
    private record Plan(Kind kind,Direction face,BlockPos aim,Bounds bounds,boolean tree){}
    private static final class Job implements AutoCloseable {
        final ServerPlayer player;final ServerLevel level;final ItemStack stack,snapshot;
        final int slot,mode;final BlockPos origin;final BlockState initial;final Kind kind;final Plan plan;
        final ChunkPreparationApi.Request request;boolean resuming;
        Job(ServerPlayer player,ItemStack stack,BlockPos origin,Kind kind,Plan plan,Collection<ChunkPos> chunks){
            this.player=player;level=player.serverLevel();this.stack=stack;snapshot=stack.copy();slot=player.getInventory().selected;
            mode=AdvancedToolMode.get(stack);this.origin=origin.immutable();initial=level.getBlockState(origin);this.kind=kind;this.plan=plan;
            request=ChunkPreparationApi.prepare(player,chunks);
        }
        boolean valid(){
            return !request.closed()&&player.isAlive()&&!player.isRemoved()&&player.level()==level
                    &&level.getServer().getPlayerList().getPlayer(player.getUUID())==player
                    &&player.getInventory().selected==slot&&player.getMainHandItem()==stack&&ItemStack.matches(stack,snapshot)
                    &&AdvancedToolMode.get(stack)==mode&&level.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4)!=null
                    &&level.getBlockState(origin)==initial&&player.canInteractWithBlock(origin,1.0);
        }
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);}
    }
    public static boolean rightMaterial(BlockState state,Kind kind){
        if(state.is(BlockMaterials.TOOL_NOT_REQUIRED))return true;
        return switch(kind){
            case PICK -> state.is(BlockMaterials.ROCK)||state.is(BlockMaterials.IRON)||state.is(BlockMaterials.ICE)
                    ||state.is(BlockMaterials.GLASS)||state.is(BlockMaterials.PISTON)||state.is(BlockMaterials.ANVIL);
            case SHOVEL -> state.is(BlockMaterials.GRASS)||state.is(BlockMaterials.GROUND)||state.is(BlockMaterials.SAND)
                    ||state.is(BlockMaterials.SNOW)||state.is(BlockMaterials.CRAFTED_SNOW)||state.is(BlockMaterials.CLAY);
            case AXE -> state.is(BlockMaterials.CORAL)||state.is(BlockMaterials.LEAVES)||state.is(BlockMaterials.PLANTS)||state.is(BlockMaterials.WOOD);
        };
    }
    public static boolean start(ItemStack stack,BlockPos origin,ServerPlayer player,Kind kind){
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()||!player.isAlive()||player.isRemoved()||BREAKING.contains(player))return false;
        var previous=PENDING.get(player);
        if(previous!=null){
            if(previous.resuming){
                if(!previous.valid())return true;
                if(previous.plan!=null){
                    var view=new LoadedView(previous.level);var ray=raytrace(view,player,true);
                    if(!view.missing.isEmpty()||ray.getType()!=HitResult.Type.BLOCK||ray.getDirection()!=previous.plan.face
                            ||!ray.getBlockPos().equals(previous.plan.aim))return true;
                    return perform(previous);
                }
                // The first phase only prepared the sight ray. Plan the actual
                // mining region now while those sight columns are still held.
            } else if(previous.origin.equals(origin)&&previous.stack==stack&&previous.valid())return true;
            else previous.close();
        }
        int mode=AdvancedToolMode.get(stack);
        var level=player.serverLevel();var state=level.getBlockState(origin);
        if((mode!=1&&mode!=2)||!rightMaterial(state,kind))return false;
        var view=new LoadedView(level);var ray=raytrace(view,player,true);
        if(!view.missing.isEmpty())return defer(new Job(player,stack,origin,kind,null,view.visited));
        if(ray.getType()!=HitResult.Type.BLOCK)return false;
        boolean tree=mode==2&&kind==Kind.AXE;
        if(tree&&!state.is(BlockTags.LOGS))return false;
        if(!tree){
            var second=raytrace(view,player,false);
            if(!view.missing.isEmpty())return defer(new Job(player,stack,origin,kind,null,view.visited));
            if(second.getType()!=HitResult.Type.BLOCK)return false;
        }
        var face=ray.getDirection();var bounds=AwakenedMiningShape.bounds(kind,mode,face.getStepX(),face.getStepY(),face.getStepZ());
        var plan=new Plan(kind,face,ray.getBlockPos().immutable(),bounds,tree);var chunks=new LinkedHashSet<>(view.visited);
        // Leaf-update callbacks inspect +/-3 around a log selected within +/-24.
        int minX=tree?-27:bounds.xs()-1,maxX=tree?27:bounds.xe();
        int minZ=tree?-27:bounds.zs()-1,maxZ=tree?27:bounds.ze();
        for(int x=(origin.getX()+minX)>>4;x<=(origin.getX()+maxX)>>4;x++)
            for(int z=(origin.getZ()+minZ)>>4;z<=(origin.getZ()+maxZ)>>4;z++)chunks.add(new ChunkPos(x,z));
        var job=new Job(player,stack,origin,kind,plan,chunks);
        if(job.request.advance(2)){
            try{return perform(job);}finally{job.close();}
        }
        return defer(job);
    }
    private static boolean defer(Job job){PENDING.put(job.player,job);return true;}
    private static boolean perform(Job job){
        if(!job.valid())return true;
        BREAKING.add(job.player);
        try{
            if(job.plan.tree){if(!fellTree(job))return true;}
            else {
                // The source repeats a non-fluid ray before enumerating extras.
                var view=new LoadedView(job.level);var second=raytrace(view,job.player,false);
                if(second.getType()!=HitResult.Type.BLOCK||!view.missing.isEmpty())return false;
                var b=job.plan.bounds;
                for(var offset:b.extraBlocks()){
                    if(!job.valid())return true;
                    var pos=job.origin.offset(offset.x(),offset.y(),offset.z());
                    if(job.level.isOutsideBuildHeight(pos))continue;
                    var state=job.level.getBlockState(pos);
                    if(!state.isAir()&&state.canHarvestBlock(job.level,pos,job.player)&&rightMaterial(state,job.kind)
                            &&allowsStrength(job.level.getBlockState(job.origin).getDestroyProgress(job.player,job.level,job.origin),state.getDestroyProgress(job.player,job.level,pos)))
                        job.player.gameMode.destroyBlock(pos);
                }
                var o=job.origin;
                var box=new AABB(o.getX()+b.xs(),o.getY()+b.ys(),o.getZ()+b.zs(),o.getX()+b.xe(),o.getY()+b.ye(),o.getZ()+b.ze());
                for(var item:job.level.getEntitiesOfClass(ItemEntity.class,box))item.setPos(job.player.getX(),job.player.getY()+1,job.player.getZ());
            }
            // A completed tree already removed its root. Prevent a second native
            // harvest using the state captured before the item hook.
            return job.level.getBlockState(job.origin).isAir();
        }finally{BREAKING.remove(job.player);}
    }
    private static boolean fellTree(Job job){
        boolean stopped=false;
        while(!job.level.getBlockState(job.origin).isAir()){
            if(!job.valid()){stopped=true;break;}
            Block block=job.level.getBlockState(job.origin).getBlock();
            var selected=furthest(job.level,job.origin,block);var before=job.level.getBlockState(selected);
            if(before.getDestroySpeed(job.level,selected)<0){stopped=true;break;}
            job.level.levelEvent(2001,selected,Block.getId(before));
            job.player.gameMode.destroyBlock(selected);
            if(sameTreeBlock(job.level.getBlockState(selected),before.getBlock())){stopped=true;break;}
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){
                var pos=selected.offset(x,y,z);
                job.level.scheduleTick(pos,job.level.getBlockState(pos).getBlock(),150+job.level.random.nextInt(150));
            }
        }
        var o=job.origin;var box=new AABB(o.getX()-5,o.getY()-1,o.getZ()-5,o.getX()+5,o.getY()+64,o.getZ()+5);
        for(var item:job.level.getEntitiesOfClass(ItemEntity.class,box)){
            item.setPos(o.getX()+.5,o.getY()+.5,o.getZ()+.5);item.tickCount+=20;
        }
        return !stopped;
    }
    /** Iterative equivalent of the source's first-outward-neighbor recursion,
     * including early return (rather than continue) at the asymmetric boundary. */
    public static BlockPos furthest(ServerLevel level,BlockPos origin,Block block){
        var last=furthestLog((x,y,z)->{
            var pos=origin.offset(x,y,z);var state=level.getBlockState(pos);
            return sameTreeBlock(state,block)&&state.is(BlockTags.LOGS)&&state.getDestroySpeed(level,pos)>=0;
        });
        return origin.offset(last.x(),last.y(),last.z());
    }
    /** Original server eye offset and float trigonometry; modern player reach
     * supplies the former server interaction-manager reach value. */
    private static BlockHitResult raytrace(LoadedView view,ServerPlayer player,boolean fluids){
        float yaw=player.getYRot(),pitch=player.getXRot();
        float cos=Mth.cos(-yaw*((float)Math.PI/180)-(float)Math.PI),sin=Mth.sin(-yaw*((float)Math.PI/180)-(float)Math.PI);
        float cp=-Mth.cos(-pitch*((float)Math.PI/180)),sp=Mth.sin(-pitch*((float)Math.PI/180));
        var start=new Vec3(player.getX(),player.getY()+1.62,player.getZ());
        double reach=player.blockInteractionRange()+(player.isCreative()?0:.5);
        var end=start.add(sin*cp*reach,sp*reach,cos*cp*reach);
        return view.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,fluids?ClipContext.Fluid.SOURCE_ONLY:ClipContext.Fluid.NONE,player){
            @Override public VoxelShape getFluidShape(FluidState state,BlockGetter reader,BlockPos pos){
                // Old BlockLiquid accepted metadata0 only and ray-traced its
                // full block bounds, rather than the modern 8/9 fluid surface.
                if(state.is(Fluids.WATER)||state.is(Fluids.LAVA))return fluids&&state.isSource()?Shapes.block():Shapes.empty();
                return super.getFluidShape(state,reader,pos);
            }
        });
    }
    private static final class LoadedView implements BlockGetter {
        final ServerLevel level;final Set<ChunkPos> visited=new LinkedHashSet<>(),missing=new LinkedHashSet<>();
        LoadedView(ServerLevel level){this.level=level;}
        @Override public BlockState getBlockState(BlockPos pos){
            if(level.isOutsideBuildHeight(pos))return Blocks.VOID_AIR.defaultBlockState();
            var cp=new ChunkPos(pos);visited.add(cp);var chunk=level.getChunkSource().getChunkNow(cp.x,cp.z);
            if(chunk==null){missing.add(cp);return Blocks.AIR.defaultBlockState();}
            return chunk.getBlockState(pos);
        }
        @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        @Override public BlockEntity getBlockEntity(BlockPos pos){
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?null:chunk.getBlockEntity(pos);
        }
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:List.copyOf(PENDING.values()))if(job.level==level){
            if(!job.valid()){job.close();continue;}
            if(!job.request.advance(2))continue;
            job.resuming=true;
            try{job.player.gameMode.destroyBlock(job.origin);}finally{job.resuming=false;job.close();}
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
    public static boolean hasPending(ServerPlayer player){return PENDING.containsKey(player);}
    private AwakenedMining(){}
}
