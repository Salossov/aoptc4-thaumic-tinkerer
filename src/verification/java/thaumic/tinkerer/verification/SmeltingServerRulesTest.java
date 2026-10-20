package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.item.TinkererFocusItem;
import thaumic.tinkerer.item.smelting.SmeltingActions;

/** Real held wand, protected replacement and S2C transport; compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class SmeltingServerRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static List<ParticleApi.Effect> effects(DislocationServerRulesTest.Peer peer){
        try{return peer.effects();}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void wisps(DislocationServerRulesTest.Peer peer,BlockPos pos,int count){
        var effects=effects(peer);check(effects.size()==count,"exact transported wisp count per peer: "+count);
        for(var effect:effects)check(effect.kind()==ParticleApi.Kind.WISP&&effect.palette()==4&&effect.size()>=0&&effect.size()<.5f
                &&effect.x()>=pos.getX()&&effect.x()<pos.getX()+1&&effect.y()>=pos.getY()&&effect.y()<pos.getY()+1
                &&effect.z()>=pos.getZ()&&effect.z()<pos.getZ()+1&&effect.shrink()&&effect.noClip()
                &&effect.gravity()<=0&&effect.gravity()>-.1f,"source wisp shape, color, size and motion");
        peer.fixture.packets.clear();
    }
    @GameTest(template="empty",batch="smelting_server",timeoutTicks=1200)
    public static void progressAndProtectionSendTwoOrTwentyFiveWithoutDuplicates(GameTestHelper helper)throws Exception{
        var pos=helper.absolutePos(new BlockPos(4,4,8));var columns=new ArrayList<ChunkPos>();
        for(int x=(pos.getX()-22)>>4;x<=(pos.getX()+22)>>4;x++)for(int z=(pos.getZ()-24)>>4;z<=(pos.getZ()+22)>>4;z++)columns.add(new ChunkPos(x,z));
        ReplacementApiRulesTest.prepared(helper,"TTSmeltOwner",columns,owner->{
            try(var observer=new DislocationServerRulesTest.Peer(helper,"TTSmeltObserver")){
                var player=owner.player;var level=helper.getLevel();TinkererFocusItem.playerLogout(player.getGameProfile().getName());
                for(int z=-5;z<=0;z++)level.setBlock(pos.offset(0,0,z),Blocks.AIR.defaultBlockState(),18);
                level.setBlock(pos,Blocks.SAND.defaultBlockState(),18);
                player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-3.5);player.setYRot(0);player.setXRot(0);observer.player.setPos(player.position().add(1,0,0));
                var focus=FocusContent.SMELT.toStack();var item=FocusContent.SMELT.get();var wand=FocusRulesTest.wand(player,focus);
                var cost=WandApi.quote(wand,player,item.getVisCost(focus));int fire=WandApi.stored(wand,Aspects.FIRE);
                wand.getItem().use(level,player,InteractionHand.MAIN_HAND);owner.fixture.packets.clear();observer.fixture.packets.clear();
                check(!item.cast(player,wand,focus),"first server call only initializes twenty");wisps(owner,pos,0);wisps(observer,pos,0);
                for(int tick=0;tick<19;tick++){
                    check(!item.cast(player,wand,focus)&&level.getBlockState(pos).is(Blocks.SAND),"one decrement per server call before completion");wisps(owner,pos,2);wisps(observer,pos,2);
                }
                Consumer<BlockEvent.EntityPlaceEvent> deny=event->{if(event.getEntity()==player&&event.getPos().equals(pos))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(deny);
                try{check(!item.cast(player,wand,focus)&&level.getBlockState(pos).is(Blocks.SAND),"denied completion restores original sand");}
                finally{NeoForge.EVENT_BUS.unregister(deny);}
                check(WandApi.stored(wand,Aspects.FIRE)==fire,"denied replacement has no vis debit");wisps(owner,pos,0);wisps(observer,pos,0);
                check(item.cast(player,wand,focus)&&level.getBlockState(pos).is(Blocks.GLASS),"next allowed use retries ready completion");
                check(WandApi.stored(wand,Aspects.FIRE)==fire-cost.get(Aspects.FIRE),"one completion payment");wisps(owner,pos,25);wisps(observer,pos,25);
                WandApi.set(wand,Aspects.FIRE,0);check(!item.cast(player,wand,focus),"unaffordable use has no effects");wisps(owner,pos,0);wisps(observer,pos,0);
                wand.getItem().releaseUsing(wand,level,player,71900);player.stopUsingItem();TinkererFocusItem.playerLogout(player.getGameProfile().getName());
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        });
    }
    @SuppressWarnings("unchecked") private static Map<ServerPlayer,?> waiting()throws ReflectiveOperationException{
        var field=SmeltingActions.class.getDeclaredField("PENDING");field.setAccessible(true);return (Map<ServerPlayer,?>)field.get(null);
    }
    private static ChunkPreparationApi.Request request(Object job)throws ReflectiveOperationException{
        var field=job.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(job);
    }
    // GameTest advances ticks faster than wall time; allow native async generation
    // the same budget as the other cold-column fixtures.
    @GameTest(template="empty",batch="smelting_cold",timeoutTicks=6000)
    public static void coldUseCoalescesAndReleasesTicketsOnLookChangeOrRelease(GameTestHelper helper)throws Exception{
        VerificationReports.install();var peer=new DislocationServerRulesTest.Peer(helper,"TTSmeltCold");var player=peer.player;var level=helper.getLevel();var origin=player.position();
        var column=new ChunkPos(1_230_000,-1_230_000);check(level.getChunkSource().getChunkNow(column.x,column.z)==null,"fresh remote owner column");
        var preparation=ChunkPreparationApi.prepare(player,List.of(column));
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;SmeltingActions.cancel(player);preparation.close();player.stopUsingItem();TinkererFocusItem.playerLogout(player.getGameProfile().getName());if(level.getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(origin.x)>>4,net.minecraft.util.Mth.floor(origin.z)>>4)!=null)player.setPos(origin);peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(()->{
            if(!preparation.advance(1))return;
            try{
                player.setPos(column.getMinBlockX()+15.5,300,column.getMinBlockZ()+8.5);player.setYRot(-90);player.setXRot(0);
                var focus=FocusContent.SMELT.toStack();var item=FocusContent.SMELT.get();var wand=FocusRulesTest.wand(player,focus);int before=WandApi.stored(wand,Aspects.FIRE);
                wand.getItem().use(level,player,InteractionHand.MAIN_HAND);check(!item.cast(player,wand,focus),"cold using tick waits");var first=waiting().get(player);check(first!=null,"continuous use owns native preparation");var firstRequest=request(first);
                check(!item.cast(player,wand,focus)&&waiting().get(player)==first,"repeat tick coalesces with same request");
                player.setYRot(90);SmeltingActions.tick(new LevelTickEvent.Post(()->true,level));check(firstRequest.closed()&&!waiting().containsKey(player),"changed view cancels and releases request");
                player.setYRot(-90);check(!item.cast(player,wand,focus),"next cold use creates new pending action");var second=waiting().get(player);check(second!=null,"new pending action after cancellation");var secondRequest=request(second);
                wand.getItem().releaseUsing(wand,level,player,71999);player.stopUsingItem();check(secondRequest.closed()&&!waiting().containsKey(player),"release cancels preparation immediately");
                check(WandApi.stored(wand,Aspects.FIRE)==before&&effects(peer).isEmpty(),"waiting and cancellation have no debit or wisps");cleanup.run();helper.succeed();
            }catch(Exception failure){cleanup.run();throw new RuntimeException(failure);}
        });
    }
    private SmeltingServerRulesTest(){}
}
