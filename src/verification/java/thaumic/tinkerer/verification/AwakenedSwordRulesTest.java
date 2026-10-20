package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.InfusionInput;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.SoulHearts;
import thaumic.tinkerer.kami.tool.AdvancedToolMode;
import thaumic.tinkerer.kami.tool.IchorTools;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class AwakenedSwordRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @GameTest(template="summon_space",batch="awakened_sword",timeoutTicks=100)
    public static void nativeModesSoulDrainAoeAndInfusion(GameTestHelper helper) {
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        var level=helper.getLevel();var sword=IchorTools.AWAKENED_SWORD.get();var stack=sword.getDefaultInstance();
        var targets=new ArrayList<Mob>();
        try(var fixture=new NativePlayerFixture(helper,"TTAwakenedSword",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;player.setNoGravity(true);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            ItemData.update(stack,data->data.putString("unrelated","kept"));player.setShiftKeyDown(true);
            for(int expected:new int[]{1,2,0}) {
                sword.use(level,player,InteractionHand.MAIN_HAND);
                check(AdvancedToolMode.get(stack)==expected&&!player.isUsingItem(),"sneaking cycles source modes without starting shield");
            }
            player.setShiftKeyDown(false);sword.use(level,player,InteractionHand.MAIN_HAND);check(player.isUsingItem(),"ordinary right click starts modern shield");player.stopUsingItem();
            AdvancedToolMode.set(stack,2);var restored=ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();
            check(restored.is(sword)&&AdvancedToolMode.get(restored)==2&&ItemData.read(restored).getString("unrelated").equals("kept"),"native component save preserves mode and unrelated data");
            var victim=EntityType.PIG.create(level);check(victim!=null,"native soul victim");targets.add(victim);victim.setPos(player.getX()+2,player.getY(),player.getZ());victim.setNoAi(true);victim.setNoGravity(true);level.addFreshEntity(victim);
            check(!sword.onLeftClickEntity(stack,player,victim)&&SoulHearts.get(player)==1,"soul mode awards one HP but lets normal attack continue");
            check(victim.hasEffect(MobEffects.DAMAGE_RESISTANCE)&&victim.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==1&&victim.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==1,"source resistance duration1 and amplifier1");
            victim.hurtTime=1;sword.onLeftClickEntity(stack,player,victim);check(SoulHearts.get(player)==1,"hurtTime guard denies additional soul gain");victim.hurtTime=0;victim.removeAllEffects();
            SoulHearts.set(player,20);sword.onLeftClickEntity(stack,player,victim);check(SoulHearts.get(player)==20,"soul gain respects source cap");victim.removeAllEffects();
            var inside=EntityType.PIG.create(level);var otherClass=EntityType.COW.create(level);var outside=EntityType.PIG.create(level);check(inside!=null&&otherClass!=null&&outside!=null,"native area targets");
            for(var mob:List.of(inside,otherClass,outside)){targets.add(mob);mob.setNoAi(true);mob.setNoGravity(true);}
            inside.setPos(victim.getX()+2,victim.getY(),victim.getZ());otherClass.setPos(victim.getX(),victim.getY(),victim.getZ()+2);outside.setPos(victim.getX()+4,victim.getY(),victim.getZ());
            for(var mob:List.of(inside,otherClass,outside))level.addFreshEntity(mob);
            for(int i=0;i<25;i++)player.tick();player.setOnGround(true);player.setSprinting(true);AdvancedToolMode.set(stack,1);
            sword.onLeftClickEntity(stack,player,victim);
            check(inside.getHealth()<inside.getMaxHealth()&&victim.getHealth()<victim.getMaxHealth(),"area mode attacks same-class targets including original target");
            check(otherClass.getHealth()==otherClass.getMaxHealth()&&outside.getHealth()==outside.getMaxHealth(),"class and three-block cube bounds remain; sprint suppresses native sweep in this fixture");
            var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("ichor_sword_gem")).orElseThrow().value();
            check(recipe.instability()==15&&recipe.components().size()==12,"source infusion instability and repeated component slots");
            var components=recipe.components().stream().map(ingredient->ingredient.getItems()[0].copy()).toList();
            check(recipe.matches(new InfusionInput(IchorTools.SWORD.get().getDefaultInstance(),components)),"native infusion matcher accepts all source ingredients");
            var output=recipe.assemble(new InfusionInput(IchorTools.SWORD.get().getDefaultInstance(),components),level.registryAccess());
            check(output.is(sword)&&AdvancedToolMode.get(output)==0&&!ItemData.read(output).contains("unrelated"),"infusion creates fresh mode0 output");
            var node=ResearchApi.definitions(player).get("thaumictinkerer:ichor_sword_gem");
            check(node.concealed()&&node.column()==16&&node.row()==12&&node.recipePages().size()==1&&node.textPages().size()==2&&node.pageOrder().get(1).equals(node.recipePages().getFirst()),"source concealed research and text/infusion/text order");
        } finally {for(var mob:targets)mob.discard();}
        helper.succeed();
    }
    private AwakenedSwordRulesTest(){}
}
