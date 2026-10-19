package thaumic.tinkerer.verification;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.*;
import thaumic.tinkerer.crop.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfusedBonemealRulesTest {
    private static void check(boolean value,String message) {if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="summon_space",batch="infused_bonemeal",timeoutTicks=100)
    public static void nativeConfiguredBonemeal(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();var p=helper.absolutePos(new BlockPos(8,4,8));
        boolean previous=TinkererConfig.CROPS_ALLOW_BONEMEAL.get();
        try(var fixture=new NativePlayerFixture(helper,"TTBoneMeal",p.east(2))) {
            var player=fixture.player;var meal=new ItemStack(Items.BONE_MEAL,12);player.setItemInHand(InteractionHand.MAIN_HAND,meal);
            var hit=new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false);var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
            level.setBlockAndUpdate(p.below(),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(p,InfusedContent.CROP.get().defaultBlockState());
            var crop=(InfusedCropBlockEntity)level.getBlockEntity(p);crop.aspect=Aspects.AIR;crop.primalTendencies=new AspectList().add(Aspects.WATER,2);
            check(!TinkererConfig.CROPS_ALLOW_BONEMEAL.getDefault(),"source config loading defaults false despite the old field initializer true");
            TinkererConfig.CROPS_ALLOW_BONEMEAL.set(false);
            var event=NeoForge.EVENT_BUS.post(new BonemealEvent(player,level,p,level.getBlockState(p),meal));
            check(event.isCanceled()&&!event.isSuccessful()&&event.isValidBonemealTarget(),"native event cancels handling without changing the crop's underlying growability");
            check(!meal.getItem().useOn(context).consumesAction()&&meal.getCount()==12&&level.getBlockState(p).getValue(CropBlock.AGE)==0,"real hand use is rejected without growth or consumption when disabled");
            check(!BoneMealItem.applyBonemeal(meal,level,p,null)&&meal.getCount()==12,"automation's null-player native path obeys the same source event");
            InfusedContent.CROP.get().performBonemeal(level,level.random,p,level.getBlockState(p));
            check(level.getBlockState(p).getValue(CropBlock.AGE)>=2&&level.getBlockState(p).getValue(CropBlock.AGE)<=5,"direct crop growth still bypasses the event, as in the original");
            level.setBlockAndUpdate(p,InfusedContent.CROP.get().defaultBlockState());TinkererConfig.CROPS_ALLOW_BONEMEAL.set(true);
            event=NeoForge.EVENT_BUS.post(new BonemealEvent(player,level,p,level.getBlockState(p),meal));check(!event.isCanceled(),"enabled setting leaves normal native handling available");
            check(meal.getItem().useOn(context).consumesAction()&&meal.getCount()==11,"enabled real hand use consumes exactly one bone meal");
            int age=level.getBlockState(p).getValue(CropBlock.AGE);check(age>=2&&age<=5,"enabled growth retains vanilla/source two-to-five stage increase");
            check(level.getBlockEntity(p)==crop&&crop.aspect==Aspects.AIR&&crop.primalTendencies.getAmount(Aspects.WATER)==2,"growth keeps crop identity, aspect and tendencies");
            level.setBlockAndUpdate(p,level.getBlockState(p).setValue(CropBlock.AGE,6));
            check(BoneMealItem.applyBonemeal(meal,level,p,null)&&meal.getCount()==10&&level.getBlockState(p).getValue(CropBlock.AGE)==7,"native automation clamps growth at maturity and consumes one");
            check(!BoneMealItem.applyBonemeal(meal,level,p,null)&&meal.getCount()==10,"mature crop consumes nothing under the enabled setting");
            TinkererConfig.CROPS_ALLOW_BONEMEAL.set(false);level.setBlockAndUpdate(p,Blocks.WHEAT.defaultBlockState());
            check(BoneMealItem.applyBonemeal(meal,level,p,null)&&meal.getCount()==9&&level.getBlockState(p).getValue(CropBlock.AGE)>0,"source restriction does not affect ordinary wheat");
            System.out.println("TT_INFUSED_BONEMEAL_NATIVE_PASS native_event=true native_hand_use=true native_automation=true direct_growth=true toggle_and_default=true");helper.succeed();
        }finally {TinkererConfig.CROPS_ALLOW_BONEMEAL.set(previous);level.removeBlock(p,false);level.removeBlock(p.below(),false);}
    }
    private InfusedBonemealRulesTest() {}
}
