package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.kami.tool.IchorTools;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class IchorToolsRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="ichor_tools",timeoutTicks=100)
    public static void nativeToolPropertiesRecipesAndResearch(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();
        var names=List.of("ichor_pick","ichor_shovel","ichor_axe","ichor_sword");
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            for(var name:names)check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id(name))&&level.getRecipeManager().byKey(ThaumicTinkerer.id(name)).isEmpty(),"disabled KAMI omits tool and recipe");
            helper.succeed();return;
        }
        var enchantments=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(var item:IchorTools.sourceOrder()) {
            var stack=item.get().getDefaultInstance();
            check(stack.getMaxStackSize()==1&&stack.getRarity()==Rarity.EPIC,"source size and rarity");
            check(!stack.isDamageableItem()&&!stack.has(DataComponents.MAX_DAMAGE)&&!stack.has(DataComponents.DAMAGE),"source non-damageable tool has valid absent durability components");
            check(item.get().getEnchantmentValue()==25&&stack.isEnchantable(),"indestructible tool remains enchantable");
            check(!stack.supportsEnchantment(enchantments.getOrThrow(Enchantments.UNBREAKING))&&!stack.supportsEnchantment(enchantments.getOrThrow(Enchantments.MENDING)),"durability enchantments excluded despite vanilla tool identity tags");
            check(ItemStack.isSameItemSameComponents(stack,ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow()),"native stack codec accepts non-damageable tier");
        }
        var pick=IchorTools.PICK.get().getDefaultInstance();var axe=IchorTools.AXE.get().getDefaultInstance();var shovel=IchorTools.SHOVEL.get().getDefaultInstance();var sword=IchorTools.SWORD.get().getDefaultInstance();
        check(pick.getDestroySpeed(Blocks.STONE.defaultBlockState())==10&&pick.getDestroySpeed(Blocks.ICE.defaultBlockState())==10,"pick material and explicit ice set");
        check(pick.getDestroySpeed(Blocks.PACKED_ICE.defaultBlockState())==1&&pick.getDestroySpeed(Blocks.GLASS.defaultBlockState())==1,"source packed ice and glass are slow");
        check(axe.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState())==10&&axe.getDestroySpeed(Blocks.OAK_LEAVES.defaultBlockState())==1,"axe wood versus original leaves");
        check(shovel.getDestroySpeed(Blocks.SNOW.defaultBlockState())==10&&sword.getDestroySpeed(Blocks.COBWEB.defaultBlockState())==15,"shovel snow and sword web");
        check(pick.getDestroySpeed(Blocks.DEEPSLATE.defaultBlockState())==10&&pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"modern blocks use modern mining rules; source harvest level reaches obsidian");
        check(pick.supportsEnchantment(enchantments.getOrThrow(Enchantments.EFFICIENCY))&&sword.supportsEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS))&&!axe.supportsEnchantment(enchantments.getOrThrow(Enchantments.SHARPNESS)),"old digger versus sword enchantment eligibility");
        check(!axe.canPerformAction(ItemAbilities.AXE_STRIP)&&!shovel.canPerformAction(ItemAbilities.SHOVEL_FLATTEN),"no modern stripping or paths added");
        var rod=new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:silverwood_wand_rod")));
        var ingot=KamiMaterials.ICHORIUM.get().getDefaultInstance();
        var inputs=List.of(CraftingInput.of(3,3,List.of(ingot,ingot,ingot,ItemStack.EMPTY,rod,ItemStack.EMPTY,ItemStack.EMPTY,rod,ItemStack.EMPTY)),
                CraftingInput.of(1,3,List.of(ingot,rod,rod)),
                CraftingInput.of(2,3,List.of(ingot,ingot,ingot,rod,ItemStack.EMPTY,rod)),
                CraftingInput.of(1,3,List.of(ingot,ingot,rod)));
        var costs=List.of("ignis","terra","aqua","aer");
        for(int i=0;i<names.size();i++) {
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id(names.get(i))).orElseThrow().value();
            check(recipe.vis().equals(Map.of(costs.get(i),75))&&recipe.matches(inputs.get(i),level),"source layout and single-aspect vis75");
            check(recipe.assemble(inputs.get(i),level.registryAccess()).is(IchorTools.sourceOrder().get(i).get()),"source tool output");
        }
        try(var fixture=new NativePlayerFixture(helper,"TTIchorTools",helper.absolutePos(new BlockPos(8,4,8)))) {
            var node=ResearchApi.definitions(fixture.player).get("thaumictinkerer:ichor_tools");
            check(node.column()==13&&node.row()==12&&node.complexity()==5&&node.warp()==2&&node.concealed(),"source research coordinates, complexity, warp and concealment");
            check(node.parents().equals(List.of("thaumictinkerer:ichorium"))&&node.hiddenParents().contains("thaumictinkerer:rod_ichorcloth")&&node.recipePages().size()==4,"shared research and hidden rod prerequisite");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="ichor_tools",timeoutTicks=100)
    public static void modernShieldAndAttackCooldown(GameTestHelper helper) {
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        try(var fixture=new NativePlayerFixture(helper,"TTIchorCombat",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var sword=IchorTools.SWORD.get().getDefaultInstance();player.setItemInHand(InteractionHand.MAIN_HAND,sword);
            for(int i=0;i<25;i++)player.tick();player.setYRot(0);player.setYHeadRot(0);
            check(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE)-10)<.0001,"source sword base damage plus player base");
            check(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED)-1.6)<.0001,"modern sword attack speed");
            var front=EntityType.PIG.create(helper.getLevel());var behind=EntityType.PIG.create(helper.getLevel());
            check(front!=null&&behind!=null,"native combat source entities");
            front.setPos(player.getX(),player.getY(),player.getZ()+3);behind.setPos(player.getX(),player.getY(),player.getZ()-3);
            IchorTools.SWORD.get().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
            check(!player.isBlocking(),"modern shield delay starts unprotected");
            for(int i=0;i<5;i++)player.tick();
            check(player.isBlocking()&&player.isDamageSourceBlocked(player.damageSources().mobAttack(front))&&!player.isDamageSourceBlocked(player.damageSources().mobAttack(behind)),"modern shield starts after5 ticks and checks incoming direction");
            player.disableShield();check(!player.isUsingItem()&&player.getCooldowns().isOnCooldown(sword.getItem()),"modern axe disable applies item cooldown");
            check(sword.canPerformAction(ItemAbilities.SWORD_SWEEP),"modern sweep remains enabled");
            check(player.getAttackStrengthScale(0)==1,"attack starts charged");player.resetAttackStrengthTicker();
            check(player.getAttackStrengthScale(0)==0,"attack recharge is retained");
            for(int i=0;i<13;i++)player.tick();check(player.getAttackStrengthScale(0)==1,"modern sword attack recharges normally");
        }
        helper.succeed();
    }
    private IchorToolsRulesTest(){}
}
