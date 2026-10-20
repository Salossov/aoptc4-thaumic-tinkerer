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
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
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
        check(level.getBlockEntity(pos) instanceof BedrockPortalBlockEntity,"source tile type is actually installed on placement");
        var saved=level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        check(net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,state,saved,level.registryAccess()) instanceof BedrockPortalBlockEntity,"native registered tile save/load");
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
    @GameTest(template="summon_space",batch="bedrock_portal",timeoutTicks=100)
    public static void nativeEntityGroupStagingAndOwnerCancellation(GameTestHelper helper){
        VerificationReports.install();if(!BedrockPortals.enabled()){helper.succeed();return;}
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(8,4,8));
        level.setBlockAndUpdate(pos,BedrockContent.PORTAL.get().defaultBlockState());
        var owner=(BedrockPortalBlockEntity)level.getBlockEntity(pos);
        var boat=net.minecraft.world.entity.EntityType.BOAT.create(level);var rider=net.minecraft.world.entity.EntityType.PIG.create(level);
        check(boat!=null&&rider!=null,"native vehicle and rider types");
        boat.setPos(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5);rider.setPos(boat.position());
        level.addFreshEntity(boat);level.addFreshEntity(rider);check(rider.startRiding(boat,true),"native passenger link");
        try{
            BedrockEntityPortals.touch(owner,rider);
            check(BedrockEntityPortals.hasPending(boat)&&BedrockEntityPortals.hasPending(rider),"rider contact claims one modern root-vehicle operation");
            check(boat.level()==level&&rider.level()==level&&rider.getVehicle()==boat,"staging neither transfers nor dismounts group");
            check(level.getBlockState(pos).is(BedrockContent.PORTAL.get()),"no construction during initial request creation");
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            check(!BedrockEntityPortals.hasPending(boat)&&!BedrockEntityPortals.hasPending(rider),"owner removal cancels entire staged group immediately");
            check(rider.getVehicle()==boat,"cancellation retains original passenger link");
        }finally{BedrockEntityPortals.removed(owner);rider.discard();boat.discard();level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());}
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="bedrock_portal",timeoutTicks=100)
    public static void nativeModernPortalExitRectanglesAndRotation(GameTestHelper helper)throws ReflectiveOperationException {
        VerificationReports.install();var level=helper.getLevel();var entity=net.minecraft.world.entity.EntityType.PIG.create(level);
        check(entity!=null,"native exit dimensions");entity.setYRot(17);entity.setXRot(25);entity.setDeltaMovement(.3,.1,-.2);
        for(int orientation:new int[]{1,0}){
            var origin=helper.absolutePos(new BlockPos(orientation==1?4:12,4,8));
            var site=new LegacyPortalSearch.Site(origin.getX(),origin.getY(),origin.getZ(),orientation,false);
            var axis=orientation==1?Direction.Axis.X:Direction.Axis.Z;
            // Complete the frame before native portal neighbor-shape validation.
            var frame=LegacyPortalSearch.frame(site);
            for(var cell:frame)if(cell.fill()==LegacyPortalSearch.Fill.OBSIDIAN)
                level.setBlock(new BlockPos(cell.pos().x(),cell.pos().y(),cell.pos().z()),Blocks.OBSIDIAN.defaultBlockState(),2);
            for(var cell:frame)if(cell.fill()==LegacyPortalSearch.Fill.PORTAL){
                var pos=new BlockPos(cell.pos().x(),cell.pos().y(),cell.pos().z());
                var state=cell.fill()==LegacyPortalSearch.Fill.OBSIDIAN?Blocks.OBSIDIAN.defaultBlockState():Blocks.NETHER_PORTAL.defaultBlockState().setValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS,axis);
                level.setBlock(pos,state,2);
            }
            check(level.getBlockState(origin).is(Blocks.NETHER_PORTAL),"native exit fixture has a live portal at its anchor: "+level.getBlockState(origin));
            var transition=BedrockPortalExit.transition(level,origin,entity);
            var rectangle=new net.minecraft.BlockUtil.FoundRectangle(origin,2,3);
            var nativeMethod=net.minecraft.world.level.block.NetherPortalBlock.class.getDeclaredMethod("createDimensionTransition",
                    net.minecraft.server.level.ServerLevel.class,net.minecraft.BlockUtil.FoundRectangle.class,Direction.Axis.class,
                    net.minecraft.world.phys.Vec3.class,net.minecraft.world.entity.Entity.class,net.minecraft.world.phys.Vec3.class,
                    float.class,float.class,net.minecraft.world.level.portal.DimensionTransition.PostDimensionTransition.class);
            nativeMethod.setAccessible(true);
            var nativeResult=nativeMethod.invoke(null,level,rectangle,Direction.Axis.X,new net.minecraft.world.phys.Vec3(.5,0,0),entity,
                    entity.getDeltaMovement(),entity.getYRot(),entity.getXRot(),net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
            check(transition.equals(nativeResult),"actual native1.21 NetherPortalBlock exit, including collision-free placement; actual="+transition+" native="+nativeResult);
            check(transition.yRot()==(axis==Direction.Axis.X?17:107)&&transition.xRot()==25,"modern axis rotation preserves pitch");
            var speed=axis==Direction.Axis.X?entity.getDeltaMovement():new net.minecraft.world.phys.Vec3(-.2,.1,-.3);
            check(transition.speed().equals(speed),"modern90degree horizontal velocity rotation");
            for(var cell:LegacyPortalSearch.frame(site))level.setBlock(new BlockPos(cell.pos().x(),cell.pos().y(),cell.pos().z()),Blocks.AIR.defaultBlockState(),2);
        }
        entity.discard();helper.succeed();
    }
    private BedrockPortalRulesTest(){}
}
