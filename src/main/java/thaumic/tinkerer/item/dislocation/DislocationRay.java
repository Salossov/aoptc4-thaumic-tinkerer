package thaumic.tinkerer.item.dislocation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Source block-selection direction with accepted modern eye/pose and reach modifiers. */
public final class DislocationRay {
    public record Trace(Vec3 from,Vec3 to,BlockHitResult hit,boolean complete,ChunkPos missing){}
    private static final class ColdChunk extends RuntimeException {
        final ChunkPos pos;
        ColdChunk(BlockPos pos){super(null,null,false,false);this.pos=new ChunkPos(pos);}
    }
    public static Trace trace(ServerPlayer player) {
        float yaw=player.getYRot(),pitch=player.getXRot();
        float cos=Mth.cos(-yaw*((float)Math.PI/180)-(float)Math.PI),sin=Mth.sin(-yaw*((float)Math.PI/180)-(float)Math.PI);
        float cp=-Mth.cos(-pitch*((float)Math.PI/180)),sp=Mth.sin(-pitch*((float)Math.PI/180));
        Vec3 from=player.getEyePosition();double reach=player.blockInteractionRange()+(player.isCreative()?0:.5);
        Vec3 to=from.add(sin*cp*reach,sp*reach,cos*cp*reach);
        var blocks=new LoadedBlocks(player.serverLevel());
        var context=new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player){
            @Override public VoxelShape getFluidShape(FluidState state,BlockGetter reader,BlockPos pos){
                // The original item ray accepts source liquids with full block bounds.
                if(state.is(Fluids.WATER)||state.is(Fluids.LAVA))return state.isSource()?Shapes.block():Shapes.empty();
                return super.getFluidShape(state,reader,pos);
            }
        };
        try{return new Trace(from,to,blocks.clip(context),true,null);}
        catch(ColdChunk unavailable){return new Trace(from,to,null,false,unavailable.pos);}
    }
    public static List<ChunkPos> requiredChunks(Trace trace) {
        Vec3 end=trace.complete()&&trace.hit()!=null&&trace.hit().getType()==HitResult.Type.BLOCK?trace.hit().getLocation():trace.to();
        var result=new ArrayList<ChunkPos>();
        // A selected cell, its adjacent destination, a second assembly part and support checks.
        for(int x=Mth.floor(Math.min(trace.from().x,end.x)-3)>>4;x<=Mth.floor(Math.max(trace.from().x,end.x)+3)>>4;x++)
            for(int z=Mth.floor(Math.min(trace.from().z,end.z)-3)>>4;z<=Mth.floor(Math.max(trace.from().z,end.z)+3)>>4;z++)result.add(new ChunkPos(x,z));
        if(trace.missing()!=null&&!result.contains(trace.missing()))result.add(trace.missing());
        return List.copyOf(result);
    }
    public static boolean loaded(ServerLevel level,List<ChunkPos> chunks) {
        for(var pos:chunks)if(level.getChunkSource().getChunkNow(pos.x,pos.z)==null)return false;
        return true;
    }
    private record LoadedBlocks(ServerLevel level) implements BlockGetter {
        private net.minecraft.world.level.chunk.LevelChunk chunk(BlockPos pos) {
            var result=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(result==null)throw new ColdChunk(pos);return result;
        }
        @Override public BlockState getBlockState(BlockPos pos){return level.isOutsideBuildHeight(pos)?Blocks.VOID_AIR.defaultBlockState():chunk(pos).getBlockState(pos);}
        @Override public FluidState getFluidState(BlockPos pos){return level.isOutsideBuildHeight(pos)?Fluids.EMPTY.defaultFluidState():chunk(pos).getFluidState(pos);}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return level.isOutsideBuildHeight(pos)?null:chunk(pos).getBlockEntity(pos);}
        @Override public int getHeight(){return level.getHeight();}
        @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
    }
    private DislocationRay(){}
}
