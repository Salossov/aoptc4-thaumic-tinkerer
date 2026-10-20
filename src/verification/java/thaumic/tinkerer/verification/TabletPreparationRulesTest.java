package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

/** Native waiting/cancellation and vanilla item placement; compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class TabletPreparationRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static AnimationTabletBlockEntity tablet(ServerLevel level,BlockPos pos){
        level.setBlock(pos,AnimationTabletContent.BLOCK.get().defaultBlockState().setValue(AnimationTabletBlock.FACING,Direction.EAST),18);
        var tablet=(AnimationTabletBlockEntity)level.getBlockEntity(pos);tablet.leftClick=false;tablet.redstone=true;tablet.player().prepare();return tablet;
    }
    private static Object pending(AnimationTabletBlockEntity tablet)throws ReflectiveOperationException{var field=AnimationTabletBlockEntity.class.getDeclaredField("preparation");field.setAccessible(true);return field.get(tablet);}
    private static ChunkPreparationApi.Request request(AnimationTabletBlockEntity tablet)throws ReflectiveOperationException{var job=pending(tablet);check(job!=null,"native cold path owns a pending request");var field=job.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(job);}
    private static void tick(AnimationTabletBlockEntity tablet){AnimationTabletBlockEntity.tick(tablet.getLevel(),tablet.getBlockPos(),tablet.getBlockState(),tablet);}
    private static void cold(GameTestHelper helper,int index,int localX,Consumer<AnimationTabletBlockEntity> scenario)throws Exception{
        var column=new ChunkPos(1_230_000+index*20,-1_230_000);
        check(helper.getLevel().getChunkSource().getChunkNow(column.x,column.z)==null,"isolated owner starts cold");
        ReplacementApiRulesTest.prepared(helper,"TTTabletWait"+index,List.of(column),unused->{
            var tablet=tablet(helper.getLevel(),new BlockPos(column.getMinBlockX()+localX,300,column.getMinBlockZ()+8));
            try{scenario.accept(tablet);}finally{tablet.prepareToMove();tablet.clearContent();}
        });
    }
    private static ChunkPreparationApi.Request waitForHit(AnimationTabletBlockEntity tablet)throws ReflectiveOperationException{
        tablet.setItem(0,new ItemStack(Items.DIRT,2));tablet.initiateSwing();tablet.swingProgress=45;tablet.swingHit();
        var request=request(tablet);check(!request.closed()&&tablet.swingProgress==45,"accepted hit waits without rewinding the swing");
        check(tablet.getItem(0).getCount()==2&&tablet.player().getMainHandItem().isEmpty(),"no native item callback or debit before readiness");
        check(!tablet.trigger(),"second ComputerCraft trigger remains rejected during accepted swing");return request;
    }
    @GameTest(template="empty",batch="tablet_preparation",timeoutTicks=1200)
    public static void coldHitRetainsSwingAndRejectsReplacementStack(GameTestHelper helper)throws Exception{
        cold(helper,0,13,tablet->{try{
            var request=waitForHit(tablet);tablet.setItem(0,tablet.getItem(0).copy());tick(tablet);
            check(request.closed()&&pending(tablet)==null&&tablet.swingProgress==0,"same-data replacement stack cancels pending action and closes tickets");
            check(tablet.getItem(0).getCount()==2,"replacement stack stays untouched");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});
    }
    @GameTest(template="empty",batch="tablet_preparation",timeoutTicks=1200)
    public static void modePowerAndLoadedTargetChangesCancel(GameTestHelper helper)throws Exception{
        cold(helper,1,13,tablet->{try{
            var level=(ServerLevel)tablet.getLevel();var target=tablet.target();level.setBlock(target,Blocks.STONE.defaultBlockState(),18);
            var mode=waitForHit(tablet);tablet.leftClick=true;tick(tablet);check(mode.closed()&&pending(tablet)==null,"changed click mode closes request");tablet.leftClick=false;
            var power=waitForHit(tablet);level.setBlock(tablet.getBlockPos(),tablet.getBlockState().setValue(AnimationTabletBlock.POWERED,true),18);tick(tablet);check(power.closed()&&pending(tablet)==null,"changed powered state closes request");
            var changed=waitForHit(tablet);level.setBlock(target,Blocks.DIRT.defaultBlockState(),18);tick(tablet);check(changed.closed()&&pending(tablet)==null,"loaded target changed during wait cancels action");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});
    }
    @GameTest(template="empty",batch="tablet_preparation",timeoutTicks=1200)
    public static void moveUnloadAndRemovalReleaseWaitingRequests(GameTestHelper helper)throws Exception{
        cold(helper,2,13,tablet->{try{
            var move=waitForHit(tablet);check(tablet.prepareToMove()&&move.closed()&&pending(tablet)==null,"moving owner releases request before position change");
            var unload=waitForHit(tablet);tablet.onChunkUnloaded();check(unload.closed()&&pending(tablet)==null,"chunk unload releases request");
            var removed=waitForHit(tablet);tablet.setRemoved();check(removed.closed()&&pending(tablet)==null,"owner removal releases request");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});
    }
    @GameTest(template="empty",batch="tablet_preparation",timeoutTicks=1200)
    public static void coldDetectionOwnsRequestWithoutChangingTriggerContract(GameTestHelper helper)throws Exception{
        cold(helper,3,15,tablet->{try{
            var level=(ServerLevel)tablet.getLevel();var target=tablet.target();tablet.setItem(0,new ItemStack(Items.DIRT,2));
            check(level.getChunkSource().getChunkNow(target.getX()>>4,target.getZ()>>4)==null,"neighboring detection target starts cold");
            tablet.detect();var request=request(tablet);check(!request.closed(),"cold detection uses real BlockEntity-owned preparation");
            check(tablet.trigger()&&tablet.swingProgress==1&&!tablet.trigger(),"first trigger accepts swing immediately; second stays rejected");
            tablet.prepareToMove();check(request.closed()&&pending(tablet)==null&&tablet.getItem(0).getCount()==2,"canceled detection keeps stored item and frees request");
        }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});
    }
    @GameTest(template="empty",batch="tablet_preparation",timeoutTicks=1200)
    public static void warmNativeBlockPlacementConsumesOneStoredItem(GameTestHelper helper)throws Exception{
        var pos=helper.absolutePos(new BlockPos(4,4,8));var columns=new ArrayList<ChunkPos>();
        for(int x=(pos.getX()-20)>>4;x<=(pos.getX()+20)>>4;x++)for(int z=(pos.getZ()-20)>>4;z<=(pos.getZ()+20)>>4;z++)columns.add(new ChunkPos(x,z));
        ReplacementApiRulesTest.prepared(helper,"TTTabletWarmPlace",columns,unused->{
            var tablet=tablet(helper.getLevel(),pos);var target=tablet.target();var stack=new ItemStack(Items.DIRT,2);tablet.setItem(0,stack);
            helper.getLevel().setBlock(target,Blocks.AIR.defaultBlockState(),18);helper.getLevel().setBlock(target.below(),Blocks.STONE.defaultBlockState(),18);
            try{tablet.swingHit();check(pending(tablet)==null,"warm native action remains immediate");check(helper.getLevel().getBlockState(target).is(Blocks.DIRT)&&tablet.getItem(0)==stack&&stack.getCount()==1,"vanilla BlockItem places once and consumes once from stored stack");}
            catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}finally{tablet.clearContent();}
        });
    }
    private TabletPreparationRulesTest(){}
}
