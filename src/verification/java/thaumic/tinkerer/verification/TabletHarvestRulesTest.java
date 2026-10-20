package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.kami.tool.*;

/** Real tablet and source hand-clearing context; compiled until native acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class TabletHarvestRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static AnimationTabletBlockEntity tablet(GameTestHelper helper,BlockPos pos,Direction facing){
        helper.getLevel().setBlock(pos,AnimationTabletContent.BLOCK.get().defaultBlockState().setValue(AnimationTabletBlock.FACING,facing),18);
        var tablet=(AnimationTabletBlockEntity)helper.getLevel().getBlockEntity(pos);tablet.leftClick=true;tablet.redstone=true;tablet.player().prepare();return tablet;
    }
    private static void prepared(GameTestHelper helper,String name,Consumer<AnimationTabletBlockEntity> scenario)throws Exception{
        var pos=helper.absolutePos(new BlockPos(4,4,8));var columns=new ArrayList<ChunkPos>();
        for(int x=(pos.getX()-20)>>4;x<=(pos.getX()+20)>>4;x++)for(int z=(pos.getZ()-20)>>4;z<=(pos.getZ()+20)>>4;z++)columns.add(new ChunkPos(x,z));
        ReplacementApiRulesTest.prepared(helper,name,columns,unused->scenario.accept(tablet(helper,pos,Direction.SOUTH)));
    }
    @GameTest(template="empty",batch="tablet_harvest",timeoutTicks=1200)
    public static void breakCancellationPrecedesStoredToolHookAndWear(GameTestHelper helper)throws Exception{
        prepared(helper,"TTTabletProtect",tablet->{
            var pos=tablet.target();var level=helper.getLevel();level.setBlock(pos,Blocks.DIRT.defaultBlockState(),18);var tool=new ItemStack(ClothTestMod.TABLET_BREAK_PROBE);tablet.setItem(0,tool);
            var probe=new ClothTestMod.TabletBreakProbe.State();ClothTestMod.TabletBreakProbe.states.put(tool,probe);
            Consumer<BlockEvent.BreakEvent> deny=event->{if(event.getPlayer()==tablet.player()&&event.getPos().equals(pos))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(deny);
            try{check(!tablet.tryHarvestBlock(),"final protection rejects tablet harvest");check(probe.calls==0&&tool.getDamageValue()==0&&level.getBlockState(pos).is(Blocks.DIRT),"rejection precedes hook, wear and removal");}
            finally{NeoForge.EVENT_BUS.unregister(deny);ClothTestMod.TabletBreakProbe.states.remove(tool);tablet.clearContent();}
        });
    }
    @GameTest(template="empty",batch="tablet_harvest",timeoutTicks=1200)
    public static void storedToolHookRetainsEmptyHandAndCanDeferRemoval(GameTestHelper helper)throws Exception{
        prepared(helper,"TTTabletHook",tablet->{
            var pos=tablet.target();var level=helper.getLevel();level.setBlock(pos,Blocks.DIRT.defaultBlockState(),18);var tool=new ItemStack(ClothTestMod.TABLET_BREAK_PROBE);tablet.setItem(0,tool);tablet.player().prepare();
            var probe=new ClothTestMod.TabletBreakProbe.State();probe.defer=true;ClothTestMod.TabletBreakProbe.states.put(tool,probe);
            try{
                check(!tablet.tryHarvestBlock()&&probe.calls==1&&probe.emptyHand,"stored hook runs once while original transient hand stays empty");
                check(level.getBlockState(pos).is(Blocks.DIRT)&&tool.getDamageValue()==0&&tablet.player().getMainHandItem().isEmpty(),"deferred hook leaves block/tool/hand intact");
                probe.defer=false;check(tablet.tryHarvestBlock()&&probe.calls==2,"passing hook continues source harvest");
                check(level.getBlockState(pos).isAir()&&tool.getDamageValue()==1&&tablet.player().getMainHandItem().isEmpty(),"accepted harvest wears stored tool once without installing it in hand");
            }finally{ClothTestMod.TabletBreakProbe.states.remove(tool);tablet.clearContent();}
        });
    }
    @GameTest(template="empty",batch="tablet_awakened",timeoutTicks=1200)
    public static void warmAwakenedToolUsesTabletOwnerWithoutOnlinePlayer(GameTestHelper helper)throws Exception{
        prepared(helper,"TTTabletAwakened",tablet->{
            boolean enabled=TinkererConfig.KAMI_FORCE_ENABLED.get();TinkererConfig.KAMI_FORCE_ENABLED.set(true);var level=helper.getLevel();var target=tablet.target();var tool=IchorTools.AWAKENED_SHOVEL.toStack();AdvancedToolMode.set(tool,1);tablet.setItem(0,tool);tablet.player().prepare();var changes=new ArrayList<BlockPos>();
            try{
                var bounds=AwakenedMiningShape.bounds(AwakenedMiningShape.Kind.SHOVEL,1,0,0,-1);
                for(var offset:bounds.extraBlocks()){var pos=target.offset(offset.x(),offset.y(),offset.z());changes.add(pos);level.setBlock(pos,Blocks.DIRT.defaultBlockState(),18);}
                level.setBlock(target,Blocks.DIRT.defaultBlockState(),18);changes.add(target);
                // The source eye is about tabletY+1.6, rather than +3.22.
                // Its upward ray intersects this plane and selects NORTH.
                check(Math.abs(tablet.player().getEyeY()-(tablet.getBlockPos().getY()+1.6))<1e-12,"actual tablet eye uses translated legacy coordinates");
                check(tablet.tryHarvestBlock(),"real tablet hook passes to final original block after warm extra mining");
                check(!AwakenedMining.hasPending(tablet.player()),"warm owner is validated without a PlayerList entry");
                check(changes.stream().allMatch(pos->level.getBlockState(pos).isAir()),"all eligible source-plane dirt cells harvested");
                check(tablet.player().getMainHandItem().isEmpty()&&tablet.getItem(0)==tool,"source empty hand and stored awakened tool retained");
            }finally{tablet.clearContent();for(var pos:changes)level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);TinkererConfig.KAMI_FORCE_ENABLED.set(enabled);}
        });
    }
    @SuppressWarnings("unchecked") private static Map<ServerPlayer,?> waiting()throws ReflectiveOperationException{var field=AwakenedMining.class.getDeclaredField("PENDING");field.setAccessible(true);return (Map<ServerPlayer,?>)field.get(null);}
    private static ChunkPreparationApi.Request request(Object job)throws ReflectiveOperationException{var field=job.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(job);}
    @GameTest(template="empty",batch="tablet_awakened_cold",timeoutTicks=6000)
    public static void coldAwakenedActionClosesWhenTabletModeChanges(GameTestHelper helper)throws Exception{
        VerificationReports.install();var peer=new DislocationServerRulesTest.Peer(helper,"TTTabletColdLoader");var level=helper.getLevel();var column=new ChunkPos(1_220_000,-1_220_000);
        check(level.getChunkSource().getChunkNow(column.x,column.z)==null,"fresh remote tablet column");var preparation=ChunkPreparationApi.prepare(peer.player,List.of(column));boolean enabled=TinkererConfig.KAMI_FORCE_ENABLED.get();TinkererConfig.KAMI_FORCE_ENABLED.set(true);
        var ownerHolder=new AnimationTabletBlockEntity[1];
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;if(ownerHolder[0]!=null){ownerHolder[0].leftClick=false;AwakenedMining.tick(new LevelTickEvent.Post(()->true,level));ownerHolder[0].clearContent();}preparation.close();peer.close();TinkererConfig.KAMI_FORCE_ENABLED.set(enabled);}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(()->{
            check(!preparation.closed(),"remote fixture preparation remains open; peer alive="+peer.player.isAlive()+", removed="+peer.player.isRemoved());
            if(!preparation.advance(1))return;
            try{
                var owner=new BlockPos(column.getMinBlockX()+13,300,column.getMinBlockZ()+8);var tablet=tablet(helper,owner,Direction.EAST);ownerHolder[0]=tablet;var target=tablet.target();level.setBlock(target,Blocks.DIRT.defaultBlockState(),18);var tool=IchorTools.AWAKENED_SHOVEL.toStack();AdvancedToolMode.set(tool,1);tablet.setItem(0,tool);tablet.player().prepare();
                check(AwakenedMining.start(tool,target,tablet.player(),AwakenedMiningShape.Kind.SHOVEL),"cold tablet hook defers central removal");var job=waiting().get(tablet.player());check(job!=null,"cold action has a BlockEntity-owned preparation");var request=request(job);
                tablet.leftClick=false;AwakenedMining.tick(new LevelTickEvent.Post(()->true,level));check(request.closed()&&!AwakenedMining.hasPending(tablet.player()),"mode change releases owned tickets");
                check(level.getBlockState(target).is(Blocks.DIRT)&&tablet.getItem(0)==tool&&tablet.player().getMainHandItem().isEmpty(),"cancellation retains original block, slot and empty hand");tablet.clearContent();cleanup.run();helper.succeed();
            }catch(Exception failure){cleanup.run();throw new RuntimeException(failure);}
        });
    }
    private TabletHarvestRulesTest(){}
}
