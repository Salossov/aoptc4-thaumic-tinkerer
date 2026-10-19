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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.BlockMaterials;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.bedrock.*;
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
    private enum Outcome { PASS, CONSUMED, DEFERRED, DENIED }
    private record Plan(Kind kind,Direction face,BlockPos aim,Bounds bounds,boolean tree){}
    private static final class Job implements AutoCloseable {
        final ServerPlayer player;final ServerLevel level;final ItemStack stack,snapshot;
        final int slot,mode;final BlockPos origin;final BlockState initial;BlockState expected;final Kind kind;final Plan plan;final boolean early;boolean denied;
        final ChunkPreparationApi.Request request;boolean resuming;
        Job(ServerPlayer player,ItemStack stack,BlockPos origin,Kind kind,Plan plan,Collection<ChunkPos> chunks,boolean early){
            this.player=player;level=player.serverLevel();this.stack=stack;snapshot=stack.copy();slot=player.getInventory().selected;
            mode=AdvancedToolMode.get(stack);this.origin=origin.immutable();initial=level.getBlockState(origin);this.kind=kind;this.plan=plan;this.early=early;expected=initial;
            request=ChunkPreparationApi.prepare(player,chunks);
        }
        boolean valid(){
            return !request.closed()&&player.isAlive()&&!player.isRemoved()&&player.level()==level
                    &&level.getServer().getPlayerList().getPlayer(player.getUUID())==player
                    &&player.getInventory().selected==slot&&player.getMainHandItem()==stack&&ItemStack.matches(stack,snapshot)
                    &&AdvancedToolMode.get(stack)==mode&&level.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4)!=null
                    &&level.getBlockState(origin)==expected&&player.canInteractWithBlock(origin,1.0);
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
        return begin(stack,origin,player,kind,false)!=Outcome.PASS;
    }
    private static Outcome result(Job job){boolean consumed=perform(job);return job.denied?Outcome.DENIED:consumed?Outcome.CONSUMED:Outcome.PASS;}
    private static Outcome begin(ItemStack stack,BlockPos origin,ServerPlayer player,Kind kind,boolean early){
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()||!player.isAlive()||player.isRemoved()||BREAKING.contains(player))return Outcome.PASS;
        var previous=PENDING.get(player);
        if(previous!=null){
            if(previous.resuming){
                if(!previous.valid())return Outcome.CONSUMED;
                if(previous.plan!=null){
                    var view=new LoadedView(previous.level);var ray=raytrace(view,player,true);
                    if(!view.missing.isEmpty()||ray.getType()!=HitResult.Type.BLOCK||ray.getDirection()!=previous.plan.face
                            ||!ray.getBlockPos().equals(previous.plan.aim))return Outcome.CONSUMED;
                    return result(previous);
                }
                // First phase prepared the sight ray. Plan the mining region
                // while those sight columns are still held.
            } else if(previous.origin.equals(origin)&&previous.stack==stack&&previous.valid())return Outcome.DEFERRED;
            else previous.close();
        }
        int mode=AdvancedToolMode.get(stack);
        var level=player.serverLevel();var state=level.getBlockState(origin);
        var change=bedrockChange(level,origin,state,kind);
        boolean extra=mode==1||mode==2;
        if((!extra&&change==BedrockMiningRules.Change.NONE)||!rightMaterial(state,kind))return Outcome.PASS;
        var view=new LoadedView(level);var ray=raytrace(view,player,true);
        if(!view.missing.isEmpty())return defer(new Job(player,stack,origin,kind,null,view.visited,early));
        if(ray.getType()!=HitResult.Type.BLOCK)return Outcome.PASS;
        boolean tree=mode==2&&kind==Kind.AXE;
        if(tree&&!state.is(BlockTags.LOGS))return Outcome.PASS;
        // Bedrock's non-fluid repeat must happen AFTER its central replacement.
        if(extra&&!tree&&change==BedrockMiningRules.Change.NONE){
            var second=raytrace(view,player,false);
            if(!view.missing.isEmpty())return defer(new Job(player,stack,origin,kind,null,view.visited,early));
            if(second.getType()!=HitResult.Type.BLOCK)return Outcome.PASS;
        }
        var face=ray.getDirection();var bounds=extra?AwakenedMiningShape.bounds(kind,mode,face.getStepX(),face.getStepY(),face.getStepZ()):null;
        var plan=new Plan(kind,face,ray.getBlockPos().immutable(),bounds,tree);var chunks=new LinkedHashSet<>(view.visited);
        // Leaf-update callbacks inspect +/-3 around a log selected within +/-24.
        int minX=tree?-27:bounds==null?-1:bounds.xs()-1,maxX=tree?27:bounds==null?1:bounds.xe();
        int minZ=tree?-27:bounds==null?-1:bounds.zs()-1,maxZ=tree?27:bounds==null?1:bounds.ze();
        for(int x=(origin.getX()+minX)>>4;x<=(origin.getX()+maxX)>>4;x++)
            for(int z=(origin.getZ()+minZ)>>4;z<=(origin.getZ()+maxZ)>>4;z++)chunks.add(new ChunkPos(x,z));
        if(change!=BedrockMiningRules.Change.NONE){
            // The second ray can see through the removed center. Prepare its
            // entire reach before mutation, including the player drop location.
            int reach=Mth.ceil(player.blockInteractionRange()+1.5);
            for(int x=Mth.floor(player.getX()-reach)>>4;x<=Mth.floor(player.getX()+reach)>>4;x++)
                for(int z=Mth.floor(player.getZ()-reach)>>4;z<=Mth.floor(player.getZ()+reach)>>4;z++)chunks.add(new ChunkPos(x,z));
        }
        var job=new Job(player,stack,origin,kind,plan,chunks,early);
        if(job.request.advance(2)){
            try{return result(job);}finally{job.close();}
        }
        return defer(job);
    }
    private static BedrockMiningRules.Change bedrockChange(ServerLevel level,BlockPos pos,BlockState state,Kind kind){
        return kind==Kind.PICK&&state.is(Blocks.BEDROCK)?BedrockMiningRules.change(BedrockPortals.enabled(),
                BedrockPortalWorlds.isSurfaceWorld(level),level.dimension()==BedrockContent.DIMENSION,pos.getY()):BedrockMiningRules.Change.NONE;
    }
    private static Outcome defer(Job job){PENDING.put(job.player,job);return Outcome.DEFERRED;}
    /** Survival never reaches native destroyBlock on hardness -1. The release
     * calls its item hook directly from LEFT_CLICK_BLOCK, before native mining.
     */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event){
        if(event.isCanceled()||event.getUseItem()==TriState.FALSE||event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START
                ||!(event.getEntity() instanceof ServerPlayer player)||BREAKING.contains(player)
                ||!(player.getMainHandItem().getItem() instanceof AwakenedDiggers.Pick))return;
        var level=player.serverLevel();var pos=event.getPos();
        // This event precedes vanilla's validation. Never read a cold packet
        // coordinate before validating its reach and currently loaded column.
        if(level.isOutsideBuildHeight(pos)||!player.canInteractWithBlock(pos,1)||player.isSpectator()
                ||player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer())||!level.mayInteract(player,pos)
                ||level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.getBlockState(pos).is(Blocks.BEDROCK))return;
        var outcome=begin(player.getMainHandItem(),pos,player,Kind.PICK,true);
        if(outcome==Outcome.DEFERRED||outcome==Outcome.DENIED)event.setCanceled(true);
        // Warm success falls through: creative removes the newly made portal
        // with this same click; survival sees its unbreakable hardness.
    }
    private static boolean perform(Job job){
        if(!job.valid())return true;
        if(job.early){
            if(job.player.blockActionRestricted(job.level,job.origin,job.player.gameMode.getGameModeForPlayer())
                    ||!job.level.mayInteract(job.player,job.origin)
                    ||CommonHooks.fireBlockBreak(job.level,job.player.gameMode.getGameModeForPlayer(),job.player,job.origin,job.expected).isCanceled()
                    ||!job.valid()){job.denied=true;return true;}
        }
        BREAKING.add(job.player);
        try{
            var change=bedrockChange(job.level,job.origin,job.expected,job.kind);
            if(change!=BedrockMiningRules.Change.NONE){
                var replacement=change==BedrockMiningRules.Change.PORTAL?BedrockContent.PORTAL.get().defaultBlockState():Blocks.AIR.defaultBlockState();
                job.level.setBlockAndUpdate(job.origin,replacement);job.expected=job.level.getBlockState(job.origin);
                if(job.expected!=replacement)return true;
            }
            if(job.plan.bounds==null)return job.level.getBlockState(job.origin).isAir();
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
            try{
                if(job.early){
                    var outcome=begin(job.stack,job.origin,job.player,job.kind,true);
                    if(outcome!=Outcome.DENIED&&outcome!=Outcome.DEFERRED&&job.player.isCreative()
                            &&job.level.getBlockState(job.origin).is(BedrockContent.PORTAL.get())){
                        BREAKING.add(job.player);
                        try{job.player.gameMode.destroyBlock(job.origin);}finally{BREAKING.remove(job.player);}
                    }
                }else job.player.gameMode.destroyBlock(job.origin);
            }finally{job.resuming=false;job.close();}
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
    public static boolean hasPending(ServerPlayer player){return PENDING.containsKey(player);}
    private AwakenedMining(){}
}
