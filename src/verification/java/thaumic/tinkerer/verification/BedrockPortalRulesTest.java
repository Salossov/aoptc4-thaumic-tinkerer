package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.bedrock.*;
import thaumic.tinkerer.kami.tool.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BedrockPortalRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="bedrock_portal",timeoutTicks=100)
    public static void nativePortalPropertiesAndDimensionTag(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(8,4,8));
        var block=BedrockContent.PORTAL.get();var state=block.defaultBlockState();
        level.setBlockAndUpdate(pos,state);
        check(state.getDestroySpeed(level,pos)==-1,"source unbreakable hardness");
        check(block.getExplosionResistance()==3600000,"actual old resistance after 3/5 setter/getter conversion");
        check(state.getCollisionShape(level,pos).isEmpty(),"source portal has no collision");
        check(state.getShape(level,pos).bounds().equals(Shapes.block().bounds()),"source selection remains a full cube");
        check(block.asItem()==Items.AIR&&!state.isAir(),"no ItemBlock, ordinary persistent block state");
        check(BedrockPortalWorlds.isSurfaceWorld(level)==(level.dimension()==Level.OVERWORLD),"default dimension-key tag contains ordinary world");
        var destination=level.getServer().getLevel(BedrockContent.DIMENSION);
        if(destination!=null)check(!BedrockPortalWorlds.isSurfaceWorld(destination),"bedrock is excluded by default");
        try(var fixture=new NativePlayerFixture(helper,"TTBedrockPortalProps",pos)){
            check(!block.canHarvestBlock(state,level,pos,fixture.player),"release portal cannot be harvested");
            check(!block.canEntityDestroy(state,level,pos,fixture.player),"entities cannot destroy portal");
        }
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());helper.succeed();
    }
    @GameTest(template="summon_space",batch="bedrock_portal",timeoutTicks=400)
    @SuppressWarnings("unchecked")
    public static void nativeEarlyPermissionSurvivalAndCreativeOrder(GameTestHelper helper)throws ReflectiveOperationException {
        VerificationReports.install();if(!BedrockPortals.enabled()){helper.succeed();return;}
        var level=helper.getLevel();check(BedrockPortalWorlds.isSurfaceWorld(level),"ordinary test world is in default surface tag");
        var anchor=helper.absolutePos(new BlockPos(8,4,8));var root=new BlockPos(anchor.getX(),4,anchor.getZ());
        var fixture=new NativePlayerFixture(helper,"TTBedrockEarlyHook",root);var player=fixture.player;
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        var index=(Map<UUID,ServerPlayer>)field.get(level.getServer().getPlayerList());
        check(index.putIfAbsent(player.getUUID(),player)==null,"indexed owner for actual player preparation");
        var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int dy=0;dy<=4;dy++){var pos=root.above(dy);saved.put(pos,level.getBlockState(pos));level.setBlockAndUpdate(pos,dy==0?Blocks.BEDROCK.defaultBlockState():Blocks.AIR.defaultBlockState());}
        player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setPos(root.getX()+.5,root.getY()+1,root.getZ()+.5);player.setXRot(90);player.setYRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND,IchorTools.AWAKENED_PICK.toStack());
        var seen=new ArrayList<BlockPos>();
        Consumer<BlockEvent.BreakEvent> cancel=event->{if(event.getPlayer()==player&&event.getPos().equals(root)){seen.add(root);event.setCanceled(true);}};
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,cancel);
        var closed=new boolean[1];Runnable close=()->{if(closed[0])return;closed[0]=true;NeoForge.EVENT_BUS.unregister(cancel);index.remove(player.getUUID(),player);fixture.close();saved.forEach(level::setBlockAndUpdate);};
        helper.testInfo.addListener(new GameTestListener(){
            public void testStructureLoaded(GameTestInfo info){}
            public void testPassed(GameTestInfo info,GameTestRunner runner){close.run();}
            public void testFailed(GameTestInfo info,GameTestRunner runner){close.run();}
            public void testAddedForRerun(GameTestInfo oldTest,GameTestInfo newTest,GameTestRunner runner){close.run();}
        });
        player.gameMode.handleBlockBreakAction(root,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.UP,level.getMaxBuildHeight(),1);
        helper.startSequence().thenWaitUntil(()->check(!AwakenedMining.hasPending(player),"canceled action preparation finishes"))
            .thenExecute(()->{
                check(seen.size()==1&&level.getBlockState(root).is(Blocks.BEDROCK),"BreakEvent cancellation precedes central mutation");
                NeoForge.EVENT_BUS.unregister(cancel);
                player.gameMode.handleBlockBreakAction(root,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.UP,level.getMaxBuildHeight(),2);
            }).thenWaitUntil(()->check(!AwakenedMining.hasPending(player),"survival action preparation finishes"))
            .thenExecute(()->{
                check(level.getBlockState(root).is(BedrockContent.PORTAL.get()),"survival mode0 bypasses bedrock hardness and keeps portal");
                level.setBlockAndUpdate(root,Blocks.BEDROCK.defaultBlockState());player.setGameMode(GameType.CREATIVE);
                player.gameMode.handleBlockBreakAction(root,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.UP,level.getMaxBuildHeight(),3);
            }).thenWaitUntil(()->check(!AwakenedMining.hasPending(player),"creative action preparation finishes"))
            .thenExecute(()->check(level.getBlockState(root).isAir(),"creative's same click removes newly created portal"))
            .thenSucceed();
    }
    private BedrockPortalRulesTest(){}
}
