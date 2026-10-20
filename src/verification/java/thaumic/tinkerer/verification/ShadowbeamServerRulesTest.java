package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.FocusBehaviors;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiWandContent;
import thaumic.tinkerer.kami.focus.FocusShadowbeamItem;
import thaumic.tinkerer.kami.focus.KamiFocusContent;

/** Native server scenarios. Compilation alone is not an execution result. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ShadowbeamServerRulesTest {
    private static void check(boolean v,String message){if(!v)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static boolean enabled(GameTestHelper helper){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;helper.succeed();return false;}

    private static final class Peer implements AutoCloseable {
        final NativePlayerFixture fixture;
        final ServerPlayer player;
        final Map<UUID,ServerPlayer> index;
        final List<ServerPlayer> players;
        boolean closed;
        @SuppressWarnings("unchecked") Peer(GameTestHelper helper,String name)throws ReflectiveOperationException{
            fixture=new NativePlayerFixture(helper,name,helper.absolutePos(new BlockPos(3,2,1)));player=fixture.player;
            var list=helper.getLevel().getServer().getPlayerList();
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            index=(Map<UUID,ServerPlayer>)field.get(list);check(index.putIfAbsent(player.getUUID(),player)==null,"unique native peer");
            field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);
            players=(List<ServerPlayer>)field.get(list);players.add(player);
            helper.getLevel().addNewPlayer(player);
            player.setYRot(0);player.setXRot(0);
        }
        @Override public void close(){
            if(closed)return;closed=true;
            KamiFocusContent.FOCUS_SHADOWBEAM.get().end(player,player.getMainHandItem(),ItemStack.EMPTY);
            player.discard();players.remove(player);index.remove(player.getUUID(),player);fixture.close();
        }
    }
    private static ItemStack wand(ServerPlayer player){
        var stack=FocusRulesTest.wand(player,KamiFocusContent.FOCUS_SHADOWBEAM.toStack());
        WandApi.parts(stack,new WandParts(KamiWandContent.ROD,KamiWandContent.CAP));
        for(var aspect:Aspects.PRIMALS)WandApi.set(stack,aspect,WandApi.capacity(stack));
        player.startUsingItem(InteractionHand.MAIN_HAND);return stack;
    }
    @SuppressWarnings("unchecked") private static Map<ServerPlayer,?> waiting()throws ReflectiveOperationException{
        var field=FocusShadowbeamItem.class.getDeclaredField("WAITING");field.setAccessible(true);return (Map<ServerPlayer,?>)field.get(null);
    }
    private static ChunkPreparationApi.Request request(Object waiting)throws ReflectiveOperationException{
        var field=waiting.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(waiting);
    }
    private static void ray(ServerPlayer player)throws ReflectiveOperationException{
        var method=FocusShadowbeamItem.class.getDeclaredMethod("executeBeam",ServerPlayer.class,net.minecraft.server.level.ServerLevel.class);
        method.setAccessible(true);method.invoke(null,player,player.serverLevel());
    }
    private static void enclosure(GameTestHelper helper){
        for(int x=0;x<=6;x++)for(int y=0;y<=6;y++)for(int z=0;z<=6;z++)
            helper.setBlock(new BlockPos(x,y,z),x==0||x==6||y==0||y==6||z==0||z==6?Blocks.STONE:Blocks.AIR);
    }
    private static final class CountingZombie extends Zombie {
        int attempts;float lastDamage;
        CountingZombie(GameTestHelper helper){super(EntityType.ZOMBIE,helper.getLevel());setNoAi(true);}
        @Override public boolean hurt(DamageSource source,float amount){attempts++;lastDamage=amount;return super.hurt(source,amount);}
    }
    private static final class CountingNonLiving extends net.minecraft.world.entity.projectile.Snowball {
        int attempts;
        CountingNonLiving(GameTestHelper helper){super(EntityType.SNOWBALL,helper.getLevel());}
        @Override public boolean isPickable(){return !isRemoved();}
        @Override public boolean hurt(DamageSource source,float amount){attempts++;return super.hurt(source,amount);}
    }

    @GameTest(template="summon_space",batch="shadowbeam_server",timeoutTicks=100)
    public static void registeredSourceProperties(GameTestHelper helper){
        if(!enabled(helper))return;
        var item=KamiFocusContent.FOCUS_SHADOWBEAM.get();var stack=KamiFocusContent.FOCUS_SHADOWBEAM.toStack();
        check(stack.getRarity()==Rarity.EPIC&&stack.getMaxStackSize()==1&&!item.isRepairable(stack),"release rarity/stack/repair");
        check(item.isVisCostPerTick(stack)&&item.continuous(stack)&&item.managesVis(),"native continuous vis ownership");
        check(item.getVisCost(stack).equals(Map.of(Aspects.ORDER,25,Aspects.ENTROPY,25,Aspects.AIR,15)),"release centivis");
        for(int rank=1;rank<=5;rank++)check(item.getPossibleUpgradesByRank(stack,rank).isEmpty(),"no potency/frugal upgrades");
        var behavior=FocusBehaviors.get(stack);check(behavior!=null&&behavior.behavior()==item&&behavior.ornament().equals(ThaumicTinkerer.id("item/focus_shadowbeam_ornament")),"public attached ornament");
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="shadowbeam_server",timeoutTicks=100)
    public static void insufficientVisAndNonfiniteAimDoNotStopUse(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        try(var peer=new Peer(helper,"TTShadowNoVis")){
            var player=peer.player;var stack=wand(player);var item=KamiFocusContent.FOCUS_SHADOWBEAM.get();var focus=KamiFocusContent.FOCUS_SHADOWBEAM.toStack();
            for(var aspect:Aspects.PRIMALS)WandApi.set(stack,aspect,0);
            check(!item.cast(player,stack,focus)&&player.isUsingItem()&&!waiting().containsKey(player),"no vis leaves wand in use without a job");
            for(var aspect:Aspects.PRIMALS)WandApi.set(stack,aspect,WandApi.capacity(stack));
            int before=WandApi.stored(stack,Aspects.ORDER);player.setYRot(-Float.MAX_VALUE);player.setXRot(0);
            check(!item.cast(player,stack,focus)&&player.isUsingItem()&&WandApi.stored(stack,Aspects.ORDER)==before&&!waiting().containsKey(player),"accepted NaN guard costs no vis and creates no tickets");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="shadowbeam_server",timeoutTicks=100)
    public static void repeatedNativeDamageAndExactBroadcastCount(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;enclosure(helper);
        try(var peer=new Peer(helper,"TTShadowRay")){
            var target=new CountingZombie(helper);var pos=helper.absolutePos(new BlockPos(3,2,3));
            target.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);helper.getLevel().addFreshEntity(target);
            var nonLiving=new CountingNonLiving(helper);pos=helper.absolutePos(new BlockPos(3,4,4));
            nonLiving.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);helper.getLevel().addFreshEntity(nonLiving);
            try{
                ray(peer.player);
                check(target.attempts>1&&target.lastDamage==8,"source repeats collision attempts with native hurt/invulnerability");
                check(nonLiving.attempts>0,"source collision includes native non-LivingEntity targets");
                int count=0;
                for(var packet:peer.fixture.packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload().type().id().equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:addon_particle"))){
                    var effect=(ParticleApi.Effect)custom.payload().getClass().getMethod("effect").invoke(custom.payload());
                    check(effect.kind()==ParticleApi.Kind.SHADOW_SPARKLE&&effect.size()==4&&!effect.shrink()&&effect.noClip()&&effect.gravity()==-.7f,"dedicated exact particle contract");count++;
                }
                check(count==297,"300 source steps emit exactly 297 explicitly networked particles, once per peer");
            }finally{target.discard();nonLiving.discard();}
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="shadowbeam_server",timeoutTicks=100)
    public static void portalStopsBeforeTarget(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;enclosure(helper);
        try(var peer=new Peer(helper,"TTShadowPortal")){
            for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x,y,2)),Blocks.NETHER_PORTAL.defaultBlockState(),2);
            var target=new CountingZombie(helper);var pos=helper.absolutePos(new BlockPos(3,2,4));target.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);helper.getLevel().addFreshEntity(target);
            try{ray(peer.player);check(target.attempts==0&&peer.player.level()==helper.getLevel(),"accepted portal stop never transfers or hits beyond portal");}finally{target.discard();}
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="shadowbeam_cold",timeoutTicks=6000)
    public static void coldJobsCoalesceAndReleaseOnAimOrUseChange(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var peer=new Peer(helper,"TTShadowCold");var player=peer.player;var level=helper.getLevel();var origin=player.position();
        var column=new ChunkPos(1_250_000,-1_250_000);
        check(level.getChunkSource().getChunkNow(column.x,column.z)==null,"fresh distant owner column");
        var ownerPreparation=ChunkPreparationApi.prepare(player,List.of(column));
        var cleanup=new Runnable(){boolean closed;@Override public void run(){if(closed)return;closed=true;ownerPreparation.close();if(level.getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(origin.x)>>4,net.minecraft.util.Mth.floor(origin.z)>>4)!=null)player.setPos(origin);peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo test){}
            public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}
            public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}
            public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}
        });
        helper.onEachTick(()->{
            check(!ownerPreparation.closed(),"initial cold-column preparation still has its live owner");
            if(!ownerPreparation.advance(1))return;
            try{
                // The native position setter sees an already prepared FULL column.
                player.setPos(column.getMinBlockX()+8.5,80,column.getMinBlockZ()+8.5);
                var stack=wand(player);var focus=KamiFocusContent.FOCUS_SHADOWBEAM.toStack();var item=KamiFocusContent.FOCUS_SHADOWBEAM.get();
                int before=WandApi.stored(stack,Aspects.ORDER);
                check(!item.cast(player,stack,focus),"cold beam waits without debit");
                var pending=waiting().get(player);check(pending!=null,"real cast creates owner-bound request");var request=request(pending);
                check(!item.cast(player,stack,focus)&&waiting().get(player)==pending,"waiting use ticks coalesce into one beam");
                check(WandApi.stored(stack,Aspects.ORDER)==before,"no debit before beam execution");
                player.setYRot(90);FocusShadowbeamItem.tick(new PlayerTickEvent.Post(player));
                check(request.closed()&&!waiting().containsKey(player),"changed aim cancels and releases actual tickets");
                item.cast(player,stack,focus);pending=waiting().get(player);check(pending!=null,"new aim starts a fresh request");request=request(pending);
                player.stopUsingItem();FocusShadowbeamItem.tick(new PlayerTickEvent.Post(player));
                check(request.closed()&&!waiting().containsKey(player)&&WandApi.stored(stack,Aspects.ORDER)==before,"release cancels without stale execution or debit");
                cleanup.run();helper.succeed();
            }catch(ReflectiveOperationException failure){cleanup.run();throw new IllegalStateException(failure);}
            catch(RuntimeException|Error failure){cleanup.run();throw failure;}
        });
    }
    private ShadowbeamServerRulesTest(){}
}
