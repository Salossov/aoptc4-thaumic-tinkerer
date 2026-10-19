package thaumic.tinkerer.item.smelting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Source ten-block collider ray, with accepted modern eye and loaded-only shape reads. */
public final class SmeltingRay {
    public record Trace(Vec3 from,Vec3 to,BlockHitResult hit,boolean complete,ChunkPos missing){}
    private static final class ColdChunk extends RuntimeException {
        final ChunkPos pos;
        ColdChunk(BlockPos pos){super(null,null,false,false);this.pos=new ChunkPos(pos);}
    }
    public static Trace trace(ServerPlayer player){
        var from=player.getEyePosition();var to=endpoint(from,player.getYRot(),player.getXRot());
        if(!finite(from)||!finite(to))return new Trace(from,to,BlockHitResult.miss(Vec3.ZERO,Direction.UP,BlockPos.ZERO),true,null);
        try{return new Trace(from,to,new LoadedBlocks(player.serverLevel()).clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)),true,null);}
        catch(ColdChunk missing){return new Trace(from,to,null,false,missing.pos);}
    }
    public static Vec3 endpoint(Vec3 from,float yaw,float pitch){
        float cos=Mth.cos(-yaw*((float)Math.PI/180)-(float)Math.PI),sin=Mth.sin(-yaw*((float)Math.PI/180)-(float)Math.PI);
        float cp=-Mth.cos(-pitch*((float)Math.PI/180)),sp=Mth.sin(-pitch*((float)Math.PI/180));
        return from.add((double)(sin*cp)*10,(double)sp*10,(double)(cos*cp)*10);
    }
    private static boolean finite(Vec3 pos){return Double.isFinite(pos.x)&&Double.isFinite(pos.y)&&Double.isFinite(pos.z);}
    public static List<ChunkPos> requiredChunks(Trace ray){
        if(!finite(ray.from())||!finite(ray.to()))return List.of();
        var end=ray.complete()&&ray.hit()!=null&&ray.hit().getType()==HitResult.Type.BLOCK?ray.hit().getLocation():ray.to();
        var chunks=new ArrayList<ChunkPos>();
        // Include the native sponge/redstone notification radius for modern
        // block-output recipes, rather than just the selected target column.
        for(int x=Mth.floor(Math.min(ray.from().x,end.x)-18)>>4;x<=Mth.floor(Math.max(ray.from().x,end.x)+18)>>4;x++)
            for(int z=Mth.floor(Math.min(ray.from().z,end.z)-18)>>4;z<=Mth.floor(Math.max(ray.from().z,end.z)+18)>>4;z++)chunks.add(new ChunkPos(x,z));
        if(ray.missing()!=null&&!chunks.contains(ray.missing()))chunks.add(ray.missing());
        return List.copyOf(chunks);
    }
    public static boolean loaded(ServerLevel level,List<ChunkPos> columns){for(var column:columns)if(level.getChunkSource().getChunkNow(column.x,column.z)==null)return false;return true;}
    private record LoadedBlocks(ServerLevel level) implements BlockGetter {
        private net.minecraft.world.level.chunk.LevelChunk chunk(BlockPos pos){var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)throw new ColdChunk(pos);return chunk;}
        @Override public BlockState getBlockState(BlockPos pos){return level.isOutsideBuildHeight(pos)?Blocks.VOID_AIR.defaultBlockState():chunk(pos).getBlockState(pos);}
        @Override public FluidState getFluidState(BlockPos pos){return level.isOutsideBuildHeight(pos)?Fluids.EMPTY.defaultFluidState():chunk(pos).getFluidState(pos);}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return level.isOutsideBuildHeight(pos)?null:chunk(pos).getBlockEntity(pos);}
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    }
    private SmeltingRay(){}
}
