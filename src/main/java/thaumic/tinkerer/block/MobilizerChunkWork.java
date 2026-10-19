package thaumic.tinkerer.block;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.world.ChunkPreparationApi;

/** One cancellable operation; every world read happens after the required FULL columns exist. */
final class MobilizerChunkWork implements AutoCloseable {
    private final BlockEntity owner;
    private ChunkPreparationApi.Request request;
    private Set<ChunkPos> requested=Set.of();
    MobilizerChunkWork(BlockEntity owner){this.owner=owner;}
    boolean ready(Collection<BlockPos> positions){
        if(!(owner.getLevel() instanceof ServerLevel level)||owner.isRemoved())return false;
        var columns=new LinkedHashSet<ChunkPos>();for(var pos:positions)columns.add(new ChunkPos(pos));
        boolean loaded=true;for(var pos:columns)if(level.getChunkSource().getChunkNow(pos.x,pos.z)==null){loaded=false;break;}
        if(loaded)return true;
        if(request==null||request.closed()||!requested.containsAll(columns)){
            close();requested=Set.copyOf(columns);request=ChunkPreparationApi.prepare(owner,columns);
        }
        return request.advance(2);
    }
    static boolean loaded(net.minecraft.world.level.Level level,BlockPos pos){
        return level instanceof ServerLevel server?server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null:level.hasChunkAt(pos);
    }
    @Override public void close(){if(request!=null){request.close();request=null;}requested=Set.of();}
}
