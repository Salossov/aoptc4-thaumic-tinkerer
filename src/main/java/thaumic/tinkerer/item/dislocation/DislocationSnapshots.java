package thaumic.tinkerer.item.dislocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.neoforged.neoforge.common.util.BlockSnapshot;

/** A focus owns its protection/rollback; preserve an enclosing native snapshot scope. */
final class DislocationSnapshots implements AutoCloseable {
    final ServerLevel level;
    private final boolean outerCapture;
    private final List<BlockSnapshot> outer,initial=new ArrayList<>();
    private boolean committed;

    DislocationSnapshots(ServerLevel level,List<BlockPos> positions) {
        this.level=level;outerCapture=level.captureBlockSnapshots;outer=new ArrayList<>(level.capturedBlockSnapshots);
        // Capture tile data BEFORE detaching it, so a failed removal can restore inventories.
        for(var pos:positions)initial.add(BlockSnapshot.create(level.dimension(),level,pos,Block.UPDATE_ALL));
        level.capturedBlockSnapshots.clear();level.captureBlockSnapshots=true;
    }
    List<BlockSnapshot> snapshots(){
        var first=new LinkedHashMap<BlockPos,BlockSnapshot>();
        for(var snapshot:initial)first.putIfAbsent(snapshot.getPos(),snapshot);
        for(var snapshot:level.capturedBlockSnapshots)first.putIfAbsent(snapshot.getPos(),snapshot);
        return List.copyOf(first.values());
    }
    void finishMutation(){level.captureBlockSnapshots=false;}
    void commit(){
        committed=true;level.captureBlockSnapshots=false;
        for(var snapshot:snapshots()) {
            var pos=snapshot.getPos();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null)continue;
            var current=chunk.getBlockState(pos);
            current.onPlace(level,pos,snapshot.getState(),false);
            level.markAndNotifyBlock(pos,chunk,snapshot.getState(),current,snapshot.getFlags(),512);
        }
    }
    @Override public void close(){
        level.captureBlockSnapshots=false;
        try {
            if(!committed)rollback();
        }finally{
            level.capturedBlockSnapshots.clear();level.capturedBlockSnapshots.addAll(outer);level.captureBlockSnapshots=outerCapture;
        }
    }
    private void rollback(){
        var snapshots=snapshots();boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;
        RuntimeException problem=null;
        try {
            // A rejected filled container must not drop its copied inventory during rollback.
            for(var snapshot:snapshots)detach(snapshot.getPos());
            for(int index=snapshots.size()-1;index>=0;index--) {
                var snapshot=snapshots.get(index);var pos=snapshot.getPos();
                try {
                    if(level.getBlockState(pos)==snapshot.getState()){
                        // A failed state write can leave the old state after its
                        // entity was detached. Recreate before restoring its NBT.
                        if(snapshot.getTag()!=null&&snapshot.getState().getBlock() instanceof EntityBlock factory){
                            var entity=factory.newBlockEntity(pos,snapshot.getState());
                            if(entity==null)throw new IllegalStateException("Cannot recreate captured block entity at "+pos);
                            level.setBlockEntity(entity);
                        }
                        snapshot.restoreBlockEntity(level,pos);
                    }
                    else {
                        int flags=(snapshot.getFlags()&~Block.UPDATE_NEIGHBORS)|Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
                        snapshot.restore(flags);
                    }
                    if(level.getBlockState(pos)!=snapshot.getState())throw new IllegalStateException("Cannot restore captured block at "+pos);
                    if(snapshot.getTag()!=null&&level.getBlockEntity(pos)==null)throw new IllegalStateException("Cannot restore captured inventory at "+pos);
                }catch(RuntimeException failure){if(problem==null)problem=failure;else problem.addSuppressed(failure);}
            }
        }finally{level.restoringBlockSnapshots=restoring;}
        // Restore the entire assembly before allowing neighbour shape changes.
        for(var snapshot:snapshots){var pos=snapshot.getPos();var state=level.getBlockState(pos);level.sendBlockUpdated(pos,state,state,Block.UPDATE_ALL);level.updateNeighborsAt(pos,state.getBlock());}
        if(problem!=null)throw problem;
    }
    void detach(BlockPos pos){
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null)throw new IllegalStateException("Prepared chunk disappeared at "+pos);
        // Level.removeBlockEntity also sends comparator notifications before
        // the assembly is complete. Commit sends the normal notifications.
        chunk.removeBlockEntity(pos);
    }
}
