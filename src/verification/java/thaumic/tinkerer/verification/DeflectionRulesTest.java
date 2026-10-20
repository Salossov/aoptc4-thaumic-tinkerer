package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.entities.LivingEquipmentTickEvent;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.block.AnimationTabletContent;
import thaumic.tinkerer.block.TabletPlayer;
import thaumic.tinkerer.item.DeflectionActions;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.item.TinkererFocusItem;
import thaumic.tinkerer.kami.*;
import thaumic.tinkerer.util.LegacyPlayerCoordinates;

/** Real entity movement, native item callbacks and actual particle packets; compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DeflectionRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static Arrow arrow(ServerLevel level,Vec3 position,Vec3 velocity){var arrow=new Arrow(EntityType.ARROW,level);arrow.setPos(position);arrow.setDeltaMovement(velocity);check(level.addFreshEntity(arrow),"native arrow added");return arrow;}
    private static void warm(GameTestHelper helper,String name,BiConsumer<DislocationServerRulesTest.Peer,Vec3> scenario)throws Exception{
        var pos=helper.absolutePos(new BlockPos(4,6,4));var columns=new ArrayList<ChunkPos>();
        for(int x=(pos.getX()-20)>>4;x<=(pos.getX()+20)>>4;x++)for(int z=(pos.getZ()-20)>>4;z<=(pos.getZ()+20)>>4;z++)columns.add(new ChunkPos(x,z));
        ReplacementApiRulesTest.prepared(helper,name,columns,peer->{var origin=Vec3.atCenterOf(pos);peer.player.setPos(origin);try{scenario.accept(peer,origin);}finally{DeflectionActions.logout(peer.player.getGameProfile().getName());}});
    }
    @SuppressWarnings("unchecked") private static Map<Object,?> pending(ServerPlayer player)throws ReflectiveOperationException{
        var field=DeflectionActions.class.getDeclaredField("PENDING");field.setAccessible(true);return ((Map<ServerPlayer,Map<Object,?>>)field.get(null)).get(player);
    }
    private static ChunkPreparationApi.Request request(ServerPlayer player)throws ReflectiveOperationException{
        var byKind=pending(player);check(byKind!=null&&byKind.size()==1,"one current source owns a native request");var job=byKind.values().iterator().next();var field=job.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(job);
    }
    private static void sparkles(DislocationServerRulesTest.Peer peer,int count)throws ReflectiveOperationException{
        var effects=peer.effects();check(effects.size()==count,"exactly "+count+" explicit particle packets");
        for(var effect:effects)check(effect.kind()==ParticleApi.Kind.SPARKLE&&effect.palette()==6&&effect.size()==1.5f&&effect.shrink()&&effect.noClip()&&effect.gravity()==0,"original deflection sparkle parameters");
    }
    @GameTest(template="empty",batch="deflection",timeoutTicks=1200)
    public static void warmDisplacementKeepsVelocityOwnerAndSendsSixToEachPeer(GameTestHelper helper)throws Exception{
        warm(helper,"TTDeflectWarm",(peer,origin)->{
            try(var observer=new DislocationServerRulesTest.Peer(helper,"TTDeflectObserver")){
                observer.player.setPos(origin.add(2,0,0));var velocity=new Vec3(0,0,-.2);var arrow=arrow(helper.getLevel(),origin.add(0,0,2),velocity);arrow.setOwner(observer.player);
                var own=arrow(helper.getLevel(),origin.add(1,0,0),velocity);own.setOwner(peer.player);
                var bottle=new ThrownExperienceBottle(EntityType.EXPERIENCE_BOTTLE,helper.getLevel());bottle.setPos(origin.add(-1,0,0));bottle.setDeltaMovement(velocity);helper.getLevel().addFreshEntity(bottle);var ownPosition=own.position();var bottlePosition=bottle.position();
                peer.fixture.packets.clear();observer.fixture.packets.clear();
                try{TinkererFocusItem.protectFromProjectiles(peer.player);check(arrow.position().equals(origin.add(0,0,-2)),"original forward displacement is distance times two, not a velocity reversal");check(arrow.getDeltaMovement().equals(velocity)&&arrow.getOwner()==observer.player,"velocity and foreign owner unchanged");check(own.position().equals(ownPosition)&&bottle.position().equals(bottlePosition),"own arrow and original XP-bottle blacklist excluded");sparkles(peer,6);sparkles(observer,6);check(pending(peer.player)==null,"warm path creates no preparation job");}
                finally{arrow.discard();own.discard();bottle.discard();}
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        });
    }
    @GameTest(template="empty",batch="deflection",timeoutTicks=1200)
    public static void focusRetainsEveryTickCostAfterProjectilesAreRemoved(GameTestHelper helper)throws Exception{
        warm(helper,"TTDeflectCost",(peer,origin)->{
            var player=peer.player;var focus=FocusContent.DEFLECT.toStack();var wand=FocusRulesTest.wand(player,focus);var cost=WandApi.quote(wand,player,FocusContent.DEFLECT.get().getVisCost(focus));var arrow=arrow(helper.getLevel(),origin.add(0,0,2),new Vec3(0,0,-.2));
            try{wand.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);for(int tick=0;tick<5;tick++){if(tick==2)arrow.discard();wand.getItem().onUseTick(helper.getLevel(),player,wand,72000-tick);}
                check(WandApi.stored(wand,Aspects.ORDER)==1000-cost.get(Aspects.ORDER)*5&&WandApi.stored(wand,Aspects.AIR)==1000-cost.get(Aspects.AIR)*5,"native focus pays original discounted8/4 every active tick, including empty scans");
                check(arrow.position().equals(origin.add(0,0,-6))&&arrow.getDeltaMovement().equals(new Vec3(0,0,-.2)),"first two source ticks displace; later empty ticks preserve position and motion");
            }finally{FocusRulesTest.stop(player,wand);arrow.discard();}
        });
    }
    @GameTest(template="empty",batch="deflection",timeoutTicks=1200)
    public static void actualArmorEventKeepsBothSourcePassesWithoutVis(GameTestHelper helper)throws Exception{
        warm(helper,"TTDeflectArmor",(peer,origin)->{
            var player=peer.player;var chest=KamiArmorContent.GEM_CHEST.toStack();AwakenedIchorclothArmorItem.variant(chest,2);player.setItemSlot(EquipmentSlot.CHEST,chest);KamiArmorState.save(player,true);
            var arrow=arrow(helper.getLevel(),origin.add(0,0,1),new Vec3(0,0,-.2));peer.fixture.packets.clear();
            try{LivingEquipmentTickEvent.BUS.post(new LivingEquipmentTickEvent(player));check(arrow.position().equals(origin.add(0,0,-3)),"inherited and own KAMI callbacks both perform original displacement");check(player.getMainHandItem().isEmpty()&&player.getItemBySlot(EquipmentSlot.CHEST)==chest,"free protection requires no wand or payment");sparkles(peer,12);}
            catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);arrow.discard();}
        });
    }
    @GameTest(template="empty",batch="deflection",timeoutTicks=1200)
    public static void modernSectionIndexPreventsOldSecondPassAcrossBoundary(GameTestHelper helper)throws Exception{
        warm(helper,"TTDeflectSection",(peer,ignored)->{
            var origin=new Vec3((peer.player.blockPosition().getX()>>4)*16+8.5,peer.player.getY(),(peer.player.blockPosition().getZ()>>4)*16+8.5);peer.player.setPos(origin);
            var chest=KamiArmorContent.GEM_CHEST.toStack();AwakenedIchorclothArmorItem.variant(chest,2);peer.player.setItemSlot(EquipmentSlot.CHEST,chest);KamiArmorState.save(peer.player,true);
            var arrow=arrow(helper.getLevel(),origin.add(2.5,0,0),new Vec3(.2,0,0));peer.fixture.packets.clear();
            try{LivingEquipmentTickEvent.BUS.post(new LivingEquipmentTickEvent(peer.player));check(arrow.getX()==origin.x+7.5,"first pass moves into next section; modern index removes it from second query");sparkles(peer,6);}
            catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{peer.player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);arrow.discard();}
        });
    }
    private static void cold(GameTestHelper helper,int index,BiConsumer<TabletPlayer,List<Arrow>> scenario)throws Exception{
        var column=new ChunkPos(1_240_000+index*20,-1_240_000);check(helper.getLevel().getChunkSource().getChunkNow(column.x,column.z)==null,"isolated origin starts cold");
        VerificationReports.install();var level=helper.getLevel();var loader=new DislocationServerRulesTest.Peer(helper,"TTDeflectColdLoader"+index);
        var columns=List.of(column,new ChunkPos(column.x+1,column.z),new ChunkPos(column.x,column.z+1),new ChunkPos(column.x+1,column.z+1));var preparation=ChunkPreparationApi.prepare(loader.player,columns);
        var owner=new BlockPos(column.getMinBlockX()+13,300,column.getMinBlockZ()+13);var holder=new AnimationTabletBlockEntity[1];var arrows=new ArrayList<Arrow>();var forced=new boolean[1];
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;arrows.forEach(Arrow::discard);if(holder[0]!=null){holder[0].clearContent();level.removeBlock(owner,false);DeflectionActions.tick(new LevelTickEvent.Post(()->true,level));}if(forced[0])level.setChunkForced(column.x,column.z,false);preparation.close();loader.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(new Runnable(){boolean initialized,done;public void run(){
            if(done)return;
            if(!initialized){if(!preparation.advance(2))return;initialized=true;level.setChunkForced(column.x,column.z,true);forced[0]=true;
                level.setBlock(owner,AnimationTabletContent.BLOCK.get().defaultBlockState(),18);holder[0]=(AnimationTabletBlockEntity)level.getBlockEntity(owner);holder[0].leftClick=false;holder[0].redstone=true;var player=holder[0].player();player.prepare();var origin=new Vec3(player.getX(),LegacyPlayerCoordinates.rawY(player),player.getZ());
                arrows.add(arrow(level,origin.add(2.5,0,0),new Vec3(1,0,0)));arrows.add(arrow(level,origin.add(0,0,2.5),new Vec3(0,0,1)));arrows.add(arrow(level,origin.add(2.5,0,2.5),new Vec3(1,0,1)));
                // Real setPosRaw retains the old box, as source deflection does.
                // Adjacent tracked sections stay outside entity ticking; force only
                // the owner, rather than warming destinations through a real player.
                arrows.get(0).setPosRaw(origin.x+17,origin.y,origin.z);arrows.get(1).setPosRaw(origin.x,origin.y,origin.z+17);arrows.get(2).setPosRaw(origin.x+17,origin.y,origin.z+17);
            }
            var player=holder[0].player();var origin=new Vec3(player.getX(),LegacyPlayerCoordinates.rawY(player),player.getZ());var bounds=new AABB(origin.x-4,origin.y-4,origin.z-4,origin.x+3,origin.y+3,origin.z+3);
            if(!level.getEntitiesOfClass(Arrow.class,bounds).containsAll(arrows))return;
            done=true;
            try{check(level.getChunkSource().getChunkNow(column.x+4,column.z)==null&&level.getChunkSource().getChunkNow(column.x,column.z+4)==null&&level.getChunkSource().getChunkNow(column.x+4,column.z+4)==null,"three destinations remain outside native forced-neighbor area");scenario.accept(player,List.copyOf(arrows));helper.succeed();}finally{cleanup.run();}
        }});
    }
    @GameTest(template="empty",batch="deflection_cold",timeoutTicks=1200)
    public static void coldFocusPaysOnceThenReleaseCancelsWithoutMoving(GameTestHelper helper)throws Exception{
        cold(helper,0,(player,arrows)->{var focus=FocusContent.DEFLECT.toStack();var wand=FocusRulesTest.wand(player,focus);player.tablet().setItem(0,wand);var cost=WandApi.quote(wand,player,FocusContent.DEFLECT.get().getVisCost(focus));var before=arrows.stream().map(Arrow::position).toList();
            try{wand.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);wand.getItem().onUseTick(helper.getLevel(),player,wand,72000);var request=request(player);
                check(!request.closed()&&request.submitted()<=2&&arrows.stream().map(Arrow::position).toList().equals(before),"bounded real request does not move projectiles before readiness");
                check(WandApi.stored(wand,Aspects.ORDER)==1000-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.AIR)==1000-cost.get(Aspects.AIR),"waiting retains exactly one source tick payment");
                FocusRulesTest.stop(player,wand);check(request.closed()&&pending(player)==null,"native release closes focus tickets immediately");check(arrows.stream().map(Arrow::position).toList().equals(before),"canceled waiting never replays saved movement");
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{FocusRulesTest.stop(player,wand);}
        });
    }
    @GameTest(template="empty",batch="deflection_cold",timeoutTicks=1200)
    public static void changedProjectileOwnersAreRescannedBeforeContinuation(GameTestHelper helper)throws Exception{
        cold(helper,1,(player,arrows)->{var before=arrows.stream().map(Arrow::position).toList();
            try{TinkererFocusItem.protectFromProjectiles(player);var request=request(player);arrows.forEach(arrow->arrow.setOwner(player));TinkererFocusItem.protectFromProjectiles(player);
                check(request.closed()&&pending(player)==null,"empty current eligible scan releases obsolete destinations");check(arrows.stream().map(Arrow::position).toList().equals(before),"newly owned arrows never receive old displacement");
                DeflectionActions.tick(new LevelTickEvent.Post(()->true,helper.getLevel()));check(pending(player)==null,"cleanup never replays a projectile list");
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        });
    }
    private DeflectionRulesTest(){}
}
