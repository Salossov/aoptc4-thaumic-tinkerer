package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.*;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ChunkPreparationRulesTest {
    @GameTest(template="empty",batch="mobilizer",timeoutTicks=120000)
    public static void temporaryNativeChunks(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+7,Math.min(anchor.getY()+335,level.getMaxBuildHeight()-8),(anchor.getZ()>>4)*16+7);
        level.setBlock(pos,Blocks.CHEST.defaultBlockState(),2);var owner=(ChestBlockEntity)level.getBlockEntity(pos);
        var empty=ChunkPreparationApi.prepare(owner,List.of());helper.assertTrue(empty.advance(0)&&empty.total()==0,"empty request resolves without generation");empty.close();empty.close();helper.assertTrue(empty.closed()&&!empty.advance(1),"close is idempotent and closed requests cannot submit work");
        var target=new ChunkPos((pos.getX()>>4)+256,pos.getZ()>>4);var farPos=new BlockPos(target.getMinBlockX()+8,pos.getY(),target.getMinBlockZ()+8);
        helper.assertTrue(level.getChunkSource().getChunkNow(target.x,target.z)==null,"isolated target initially lacks a FULL chunk");
        var request=ChunkPreparationApi.prepare(owner,List.of(target,target));
        helper.assertTrue(request.total()==1&&request.submitted()==0,"exact columns are deduplicated before any ticket submission");
        helper.assertTrue(!request.advance(0)&&request.submitted()==0,"zero budget never requests a cold column");
        helper.assertTrue(!request.advance(1)&&request.submitted()==1,"cold submission returns without waiting on generation");
        ChunkPreparationApi.Request[] removal={null},selfRetention={null};ChestBlockEntity[] farOwner={null};
        int[] stage={0},ticks={0},total={0};boolean[] done={false},forced={false},unloaded={false};
        long started=System.nanoTime();long[] stageStarted={started};
        java.util.function.Consumer<ChunkEvent.Unload> listener=event->{if(event.getLevel()==level&&event.getChunk().getPos().equals(target))unloaded[0]=true;};
        NeoForge.EVENT_BUS.addListener(listener);
        Runnable cleanup=()->{
            NeoForge.EVENT_BUS.unregister(listener);request.close();if(removal[0]!=null)removal[0].close();if(selfRetention[0]!=null)selfRetention[0].close();
            if(forced[0]){level.setChunkForced(target.x,target.z,false);forced[0]=false;}
            if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        };
        // Register exactly once, before the native scheduler starts iterating.
        // Adding onEachTick callbacks from a running callback resizes its task map.
        helper.onEachTick(()->{if(done[0])return;try{
            // GameTestServer advances simulated ticks without the normal 50 ms pacing.
            // Native generation and chunk IO need a bounded wall-clock observation window.
            total[0]++;ticks[0]++;
            if(System.nanoTime()-started>45_000_000_000L)throw new GameTestAssertException("Chunk lifetime scenario exceeded 45 seconds: stage="+stage[0]+", ticks="+ticks[0]+", total="+total[0]+", requestClosed="+request.closed()+", selfRetentionClosed="+(selfRetention[0]!=null&&selfRetention[0].closed())+", unloaded="+unloaded[0]);
            switch(stage[0]){
                case 0->{
                    helper.assertTrue(!request.closed(),"owner remains valid while native chunk manager prepares target");if(!request.advance(1))return;
                    helper.assertTrue(level.getChunkSource().getChunkNow(target.x,target.z)!=null&&!level.getForcedChunks().contains(target.toLong()),"poll resolves to native FULL column with no prior permanent force");
                    helper.assertTrue(level.setChunkForced(target.x,target.z,true),"fixture installs its own native force to retain prepared column across setup");forced[0]=true;
                    request.close();helper.assertTrue(request.closed(),"completion releases preparation tickets");
                    removal[0]=ChunkPreparationApi.prepare(owner,List.of(new ChunkPos(target.x+1,target.z)));removal[0].advance(1);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);stage[0]=1;ticks[0]=0;stageStarted[0]=System.nanoTime();
                }
                case 1->{
                    if(ticks[0]<2)return;helper.assertTrue(removal[0].closed()&&!removal[0].advance(1),"registered owner's removal closes pending request");
                    helper.assertTrue(level.getChunkSource().getChunkNow(target.x,target.z)!=null&&level.getForcedChunks().contains(target.toLong()),"test-owned force retains prepared column across setup");
                    level.setBlock(farPos,Blocks.CHEST.defaultBlockState(),2);farOwner[0]=(ChestBlockEntity)level.getBlockEntity(farPos);stage[0]=2;ticks[0]=0;stageStarted[0]=System.nanoTime();
                }
                case 2->{
                    if(!level.shouldTickBlocksAt(target.toLong())){
                        if(System.nanoTime()-stageStarted[0]>15_000_000_000L){var chunk=level.getChunkSource().getChunkNow(target.x,target.z);throw new GameTestAssertException("Native force did not become block ticking: forced="+level.getForcedChunks().contains(target.toLong())+", chunk="+(chunk==null?"absent":chunk.getFullStatus()));}return;
                    }
                    helper.assertTrue(level.getForcedChunks().contains(target.toLong()),"fixture force remains installed when native block ticking starts");
                    selfRetention[0]=ChunkPreparationApi.prepare(farOwner[0],List.of(target));helper.assertTrue(selfRetention[0].advance(0)&&!selfRetention[0].closed(),"hot owner column gets FULL retention without cold budget");
                    unloaded[0]=false;level.setChunkForced(target.x,target.z,false);forced[0]=false;stage[0]=3;ticks[0]=0;stageStarted[0]=System.nanoTime();
                }
                case 3->{
                    if(System.nanoTime()-stageStarted[0]>15_000_000_000L){var chunk=level.getChunkSource().getChunkNow(target.x,target.z);throw new GameTestAssertException("Owner did not unload: closed="+selfRetention[0].closed()+", event="+unloaded[0]+", chunk="+(chunk==null?"absent":chunk.getFullStatus()));}
                    if(!selfRetention[0].closed()||level.getChunkSource().getChunkNow(target.x,target.z)!=null||!unloaded[0])return;
                    helper.assertTrue(!level.getForcedChunks().contains(target.toLong()),"self-retaining FULL request expires and real native ChunkEvent.Unload is observed");
                    done[0]=true;cleanup.run();System.out.println("TT_CHUNK_PREPARATION_PASS native_cold_full=true dedup=true budget=true removal_cleanup=true actual_owner_chunk_unload=true");helper.succeed();
                }
                default->throw new GameTestAssertException("Unexpected stage");
            }
        }catch(Throwable failure){done[0]=true;cleanup.run();throw failure;}});
    }
}
