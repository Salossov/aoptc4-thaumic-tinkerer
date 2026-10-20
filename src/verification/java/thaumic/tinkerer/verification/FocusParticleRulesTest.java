package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.item.TinkererFocusItem;
import thaumic.tinkerer.util.LegacyMotion;

/** Native wand callbacks and two real packet recipients; compilation does not prove execution. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class FocusParticleRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static BlockPos base(GameTestHelper helper){var anchor=helper.absolutePos(BlockPos.ZERO);return new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+70,(anchor.getZ()>>4)*16+8);}
    private static List<ChunkPos> columns(GameTestHelper helper){var pos=base(helper);var result=new ArrayList<ChunkPos>();for(int x=(pos.getX()-20)>>4;x<=(pos.getX()+20)>>4;x++)for(int z=(pos.getZ()-20)>>4;z<=(pos.getZ()+20)>>4;z++)result.add(new ChunkPos(x,z));return result;}
    private static void sparkle(ParticleApi.Effect effect){check(effect.kind()==ParticleApi.Kind.SPARKLE&&effect.palette()==0&&effect.size()==1.5f&&effect.shrink()&&effect.noClip()&&effect.gravity()==0,"original ordinary-focus sparkle parameters");}
    private static void sameEffects(DislocationServerRulesTest.Peer owner,DislocationServerRulesTest.Peer observer,int count)throws ReflectiveOperationException{
        var effects=owner.effects();check(effects.size()==count,"exactly one packet per source effect to owner");check(effects.equals(observer.effects()),"second nearby recipient gets the same effects once");effects.forEach(FocusParticleRulesTest::sparkle);
    }

    @GameTest(template="summon_space",batch="ordinary_focus_particles",timeoutTicks=1200)
    public static void healingCounterPausesResetsAndBroadcastsOncePerQualifyingTick(GameTestHelper helper)throws Exception{
        VerificationReports.install();var peer=new DislocationServerRulesTest.Peer(helper,"TTHealPackets");
        var observer=new DislocationServerRulesTest.Peer(helper,"TTHealObserver");var player=peer.player;
        var preparation=ChunkPreparationApi.prepare(player,columns(helper));var wand=new ItemStack[1];
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;if(wand[0]!=null)FocusRulesTest.stop(player,wand[0]);TinkererFocusItem.playerLogout(player.getGameProfile().getName());preparation.close();observer.close();peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){cleanup.run();}});
        helper.onEachTick(new Runnable(){int tick;boolean initialized,done;java.util.Map<thaumcraft.api.aspects.Aspect,Integer> cost;public void run(){
            if(done||!preparation.advance(2))return;
            try{
                if(!initialized){initialized=true;
                    // Isolate the long-running particle capture from the other
                    // nearby scenario's 64-block broadcasts.
                    player.setPos(Vec3.atCenterOf(base(helper).above(100)));player.setNoGravity(true);player.getFoodData().setFoodLevel(19);
                    observer.player.setPos(player.position().add(2,0,0));observer.player.setNoGravity(true);
                    var item=FocusContent.HEAL.get();var focus=new ItemStack(item);
                    for(int rank=1;rank<=3;rank++)check(item.applyUpgrade(focus,FocusUpgradeType.potency,rank),"attached focus potency accepted");
                    wand[0]=FocusRulesTest.wand(player,focus);cost=WandApi.quote(wand[0],player,item.getVisCost(focus));
                    TinkererFocusItem.playerLogin(player.getGameProfile().getName());player.setHealth(10);
                    peer.fixture.packets.clear();observer.fixture.packets.clear();wand[0].getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
                }
                if(tick==29)player.setHealth(player.getMaxHealth());
                if(tick==39){player.setHealth(10);WandApi.set(wand[0],Aspects.WATER,0);}
                if(tick==49)WandApi.set(wand[0],Aspects.WATER,1000);
                if(tick==60){FocusRulesTest.stop(player,wand[0]);player.setItemInHand(InteractionHand.MAIN_HAND,wand[0]);wand[0].getItem().use(player.level(),player,InteractionHand.MAIN_HAND);}
                // Exactly one native callback per real server tick, including
                // the core cooldown following a successful heal.
                wand[0].getItem().onUseTick(player.level(),player,wand[0],72000-tick);
                if(tick==28){check(player.getHealth()==10&&WandApi.stored(wand[0],Aspects.EARTH)==1000,"29 qualifying ticks do not heal/pay; attached potency does not shorten original wand-based timer");sameEffects(peer,observer,29);}
                if(tick==48){sameEffects(peer,observer,29);check(player.getHealth()==10&&WandApi.stored(wand[0],Aspects.EARTH)==1000,"full health and insufficient vis pause without effects/payment");}
                if(tick==49){
                    check(player.getHealth()==11&&WandApi.stored(wand[0],Aspects.EARTH)==1000-cost.get(Aspects.EARTH)&&WandApi.stored(wand[0],Aspects.WATER)==1000-cost.get(Aspects.WATER),"thirtieth qualifying callback heals one and charges exactly one discounted45/45 payment");sameEffects(peer,observer,30);
                    for(var effect:peer.effects())check(effect.x()>=(double)((float)player.getX()-.5f)&&effect.x()<=(double)((float)player.getX()+.5f)&&effect.y()>=(float)player.getY()&&effect.y()<=(double)((float)player.getY()+1f)&&effect.z()>=(double)((float)player.getZ()-.5f)&&effect.z()<=(double)((float)player.getZ()+.5f),"heal sparkle retains source float-rounded coordinate bounds");
                }
                if(tick==88)check(player.getHealth()==11,"native release resets previous ten ticks instead of resuming progress");
                if(tick++==89){check(player.getHealth()==12,"new use heals only after30 new qualifying callbacks");sameEffects(peer,observer,70);done=true;System.out.println("TT_HEAL_PARTICLES_NATIVE_PASS recipients=2 effects_each=70 timer=30 pause_and_release=true");helper.succeed();}
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        }});
    }

    @GameTest(template="empty",batch="ordinary_focus_particles",timeoutTicks=1200)
    public static void telekinesisPaysOnceForGroupAndBroadcastsAtLegacyItemHeight(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var peer=new DislocationServerRulesTest.Peer(helper,"TTPullPackets");
        var observer=new DislocationServerRulesTest.Peer(helper,"TTPullObserver");var preparation=ChunkPreparationApi.prepare(peer.player,columns(helper));
        var items=new ArrayList<ItemEntity>();var wand=new ItemStack[1];
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;items.forEach(ItemEntity::discard);if(wand[0]!=null)FocusRulesTest.stop(peer.player,wand[0]);preparation.close();observer.close();peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(new Runnable(){boolean staged,done;Vec3 target;public void run(){
            if(done||!preparation.advance(2))return;
            if(!staged){staged=true;var player=peer.player;player.setPos(Vec3.atCenterOf(base(helper)));player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.setYRot(0);player.setXRot(0);observer.player.setPos(player.position().add(2,0,0));observer.player.setNoGravity(true);observer.player.setDeltaMovement(Vec3.ZERO);
                target=player.position().add(0,player.getBbHeight()/2f,0).add(LegacyMotion.look(0,0).scale(5)).add(0,.5,0);
                var focus=FocusContent.TELEKINESIS.toStack();wand[0]=FocusRulesTest.wand(player,focus);
                peer.fixture.packets.clear();observer.fixture.packets.clear();FocusContent.TELEKINESIS.get().cast(player,wand[0],focus);
                check(WandApi.stored(wand[0],Aspects.AIR)==1000,"empty query has no charge");
                for(var at:List.of(target.add(3,0,0),target.add(.5,-.125,0))){var entity=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(Items.STICK));entity.setNoGravity(true);entity.setPickUpDelay(1000);entity.setDeltaMovement(Vec3.ZERO);check(level.addFreshEntity(entity),"real native item added");items.add(entity);}return;
            }
            if(!level.getEntitiesOfClass(ItemEntity.class,new AABB(target.subtract(6,6,6),target.add(6,6,6))).containsAll(items))return;
            done=true;
            try{
                var player=peer.player;var item=FocusContent.TELEKINESIS.get();var cost=WandApi.quote(wand[0],player,item.getVisCost(WandApi.focus(wand[0])));
                peer.fixture.packets.clear();observer.fixture.packets.clear();var positions=items.stream().map(ItemEntity::position).toList();
                wand[0].getItem().use(level,player,InteractionHand.MAIN_HAND);wand[0].getItem().onUseTick(level,player,wand[0],71999);
                check(WandApi.stored(wand[0],Aspects.AIR)==1000-cost.get(Aspects.AIR)&&WandApi.stored(wand[0],Aspects.ENTROPY)==1000-cost.get(Aspects.ENTROPY),"one5/5 discounted group payment, not per item");sameEffects(peer,observer,2);
                var expected=positions.stream().map(at->new Vec3((float)at.x,(float)(at.y+.125),(float)at.z)).toList();
                for(var effect:peer.effects())check(expected.contains(new Vec3(effect.x(),effect.y(),effect.z())),"source item raw posY is feet+0.125, with cast after addition");
                check(items.get(0).getDeltaMovement().length()>.3332&&items.get(0).getDeltaMovement().length()<.3334,"far item normalizes to source speed");
                check(items.get(1).getDeltaMovement().distanceTo(new Vec3(-.5*.3333f,0,0))<1e-8,"near item retains unnormalised source approach");
                check(items.stream().allMatch(e->e.hasImpulse&&e.hurtMarked),"each telekinesis tick immediately marks both position and velocity for client synchronization");
                var before=items.stream().map(ItemEntity::getDeltaMovement).toList();WandApi.set(wand[0],Aspects.AIR,0);wand[0].getItem().onUseTick(level,player,wand[0],71998);
                sameEffects(peer,observer,2);check(before.equals(items.stream().map(ItemEntity::getDeltaMovement).toList()),"unaffordable tick has no motion or extra effects");
                System.out.println("TT_TELEKINESIS_PARTICLES_NATIVE_PASS recipients=2 effects_each=2 group_payment=true item_offset=0.125");helper.succeed();
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{cleanup.run();}
        }});
    }
    private FocusParticleRulesTest(){}
}
