package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.*;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.item.XpTalismanItem;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.kami.focus.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class XpDrainRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    private static boolean enabled(GameTestHelper helper){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;helper.succeed();return false;}
    private static void xp(ServerPlayer player,int level,int points){player.experienceLevel=level;player.experienceProgress=0;player.totalExperience=points;}
    private static AutoCloseable resetCycle()throws Exception{
        var field=FocusXPDrainItem.class.getDeclaredField("cycle");field.setAccessible(true);var cycle=field.get(KamiFocusContent.FOCUS_XP_DRAIN.get());
        var index=XpDrainCycle.class.getDeclaredField("lastGiven");index.setAccessible(true);int previous=index.getInt(cycle);index.setInt(cycle,0);
        return ()->index.setInt(cycle,previous);
    }
    @GameTest(template="summon_space",batch="xp_drain",timeoutTicks=100)
    public static void actualRegisteredRecipeResearchAndFocus(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTXpData",helper.absolutePos(new BlockPos(8,4,8)))){
            var stack=KamiFocusContent.FOCUS_XP_DRAIN.toStack();var item=KamiFocusContent.FOCUS_XP_DRAIN.get();
            check(stack.getRarity()==Rarity.EPIC&&stack.getMaxDamage()==1&&stack.getMaxStackSize()==1&&!item.isRepairable(stack)&&item.getEnchantmentValue()==5,"source item properties");
            check(item.continuous(stack)&&item.managesVis()&&item.isVisCostPerTick(stack)&&item.getVisCost(stack).isEmpty(),"source use/cost contract");
            for(int rank=1;rank<=5;rank++)check(item.getPossibleUpgradesByRank(stack,rank).isEmpty(),"no source upgrades");
            check(FocusBehaviors.get(stack).behavior()==item,"real public behavior registration");
            check(ItemStack.isSameItemSameComponents(stack,ItemStack.parse(helper.getLevel().registryAccess(),stack.save(helper.getLevel().registryAccess())).orElseThrow()),"real registered item codec");
            var recipe=(InfusionRecipe)helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("focus_xp_drain")).orElseThrow().value();
            check(recipe.instability()==12&&recipe.aspects().equals(Map.of("cognitio",65,"vitium",16,"praecantatio",50,"auram",32)),"source infusion values");
            check(recipe.matches(new InfusionInput(new ItemStack(Items.ENDER_PEARL),List.of(KamiMaterials.ICHOR.toStack(),new ItemStack(Items.EXPERIENCE_BOTTLE),new ItemStack(Items.DIAMOND),ThaumicTinkerer.XP_TALISMAN.toStack(),new ItemStack(Items.ENCHANTING_TABLE),KamiMaterials.ICHOR.toStack()))),"source infusion ingredients");
            var node=ResearchApi.definitions(fixture.player).get("thaumictinkerer:focus_xp_drain");check(node.parents().equals(List.of("thaumictinkerer:rod_ichorcloth")),"source rod research parent");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="xp_drain",timeoutTicks=100)
    public static void sharedSourceCycleAcrossTwoPlayers(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        try(var reset=resetCycle();var first=new NativePlayerFixture(helper,"TTXpFirst",helper.absolutePos(new BlockPos(8,4,8)));var second=new NativePlayerFixture(helper,"TTXpSecond",helper.absolutePos(new BlockPos(10,4,8)))){
            var focusA=KamiFocusContent.FOCUS_XP_DRAIN.toStack();var focusB=KamiFocusContent.FOCUS_XP_DRAIN.toStack();
            var a=FocusRulesTest.wand(first.player,focusA);var b=FocusRulesTest.wand(second.player,focusB);
            for(var aspect:Aspects.PRIMALS){WandApi.set(a,aspect,0);WandApi.set(b,aspect,0);}
            xp(first.player,10,160);xp(second.player,10,160);
            check(FocusBehaviors.get(focusA).behavior().cast(first.player,a,focusA),"first actual cast");
            check(FocusBehaviors.get(focusB).behavior().cast(second.player,b,focusB),"second actual cast");
            check(WandApi.stored(a,Aspects.EARTH)==500&&WandApi.stored(b,Aspects.FIRE)==500,"source AIR EARTH FIRE WATER order and shared counter");
            check(ExperienceHelper.getPlayerXP(first.player)==145&&ExperienceHelper.getPlayerXP(second.player)==145&&first.player.totalExperience==145&&second.player.totalExperience==145,"exact modern fifteen points each");
            check(!ItemData.read(focusA).contains("lastGiven")&&!ItemData.read(focusB).contains("lastGiven"),"counter is not persisted in either focus");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="xp_drain",timeoutTicks=100)
    public static void fullWandStaleExperienceAndPartialCapacity(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTXpGuards",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var focus=KamiFocusContent.FOCUS_XP_DRAIN.toStack();var item=KamiFocusContent.FOCUS_XP_DRAIN.get();var wand=FocusRulesTest.wand(player,focus);int capacity=WandApi.capacity(wand);
            for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,capacity);
            xp(player,10,160);player.startUsingItem(InteractionHand.MAIN_HAND);
            check(!item.cast(player,wand,focus)&&player.isUsingItem()&&player.totalExperience==160,"full wand keeps source use and spends no XP");
            WandApi.set(wand,Aspects.AIR,capacity-1);
            var talisman=ThaumicTinkerer.XP_TALISMAN.toStack();XpTalismanItem.xp(talisman,1500);player.getInventory().setItem(3,talisman);
            xp(player,0,160);
            check(!item.cast(player,wand,focus)&&player.experienceLevel==0&&player.totalExperience==160&&player.isUsingItem(),"stale total cannot create negative XP or stop source use");
            check(XpTalismanItem.xp(talisman)==1500&&WandApi.stored(wand,Aspects.AIR)==capacity-1,"no implicit talisman debit or free vis");
            xp(player,10,14);check(!item.cast(player,wand,focus)&&player.experienceLevel==10,"source totalExperience threshold is preserved");
            xp(player,10,160);check(item.cast(player,wand,focus),"partly full aspect accepts a cast");
            check(WandApi.stored(wand,Aspects.AIR)==capacity&&ExperienceHelper.getPlayerXP(player)==145,"source full price for a one-centivis remainder");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="xp_drain",timeoutTicks=100)
    public static void realWandUseTickAndStop(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTXpUse",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var focus=KamiFocusContent.FOCUS_XP_DRAIN.toStack();var wand=FocusRulesTest.wand(player,focus);
            for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);xp(player,10,160);
            wand.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);check(player.isUsingItem(),"registered native wand starts continuous focus");
            wand.getItem().onUseTick(helper.getLevel(),player,wand,wand.getUseDuration(player)-1);
            int total=0;for(var aspect:Aspects.PRIMALS)total+=WandApi.stored(wand,aspect);
            check(total==500&&ExperienceHelper.getPlayerXP(player)==145,"real native wand tick invokes focus exactly once");
            FocusRulesTest.stop(player,wand);check(!player.isUsingItem(),"native release stops use");
        }
        helper.succeed();
    }
    private XpDrainRulesTest(){}
}
