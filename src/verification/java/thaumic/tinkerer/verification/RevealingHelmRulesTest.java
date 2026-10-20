package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.EquipmentVision;
import thaumcraft.api.IRepairable;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class RevealingHelmRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(label); }
    private static ItemStack core(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:" + path))); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void revealingHelmet(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = FocusRulesTest.player(helper, "[TTRevealing]");
        var item = ThaumicTinkerer.REVEALING_HELM.get(); var stack = new ItemStack(item);
        try {
            check(item instanceof IRepairable && stack.getMaxDamage() == 500 && stack.getMaxStackSize() == 1, "original Repair marker and durability override");
            check(item.getDefense() == 2 && item.getEnchantmentValue() == 25, "actual original thaumium helmet defense two and enchantability twenty-five");
            check(item.getVisDiscount(stack, player, Aspects.AIR) == 5 && item.getVisDiscount(stack, player, Aspects.ENTROPY) == 5, "five percent discount for every primal aspect");
            check(item.isValidRepairItem(stack, core("thaumium_ingot")) && !item.isValidRepairItem(stack, new ItemStack(Items.IRON_INGOT)), "repair ingredient is original thaumium");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            check(!EquipmentVision.canSeeNodes(player) && !EquipmentVision.canSeeAspects(player), "held helmet does not enable head-slot render hooks");
            player.setItemSlot(EquipmentSlot.HEAD, stack);
            check(EquipmentVision.canSeeNodes(player) && EquipmentVision.canSeeAspects(player), "actual core visibility path recognizes the addon interfaces");
            var wand = FocusRulesTest.wand(player, new ItemStack(thaumic.tinkerer.item.FocusContent.FLIGHT.get()));
            var withHelm = WandApi.quote(wand, player, Map.of(Aspects.AIR, 1000)).get(Aspects.AIR);
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            check(WandApi.quote(wand, player, Map.of(Aspects.AIR, 1000)).get(Aspects.AIR) > withHelm, "native wand pricing applies the addon vis-discount interface");
            FocusRulesTest.stop(player, wand);
            player.setItemSlot(EquipmentSlot.HEAD, core("goggles_of_revealing"));
            check(EquipmentVision.canSeeNodes(player) && EquipmentVision.canSeeAspects(player), "native goggles retain both abilities");
            var fortress = core("thaumium_fortress_helmet"); player.setItemSlot(EquipmentSlot.HEAD, fortress);
            check(!EquipmentVision.canSeeNodes(player), "native fortress helmet still requires its goggles upgrade");
            CustomData.update(DataComponents.CUSTOM_DATA, fortress, tag -> tag.putBoolean("goggles", true));
            check(EquipmentVision.canSeeNodes(player) && EquipmentVision.canSeeAspects(player), "native fortress goggles upgrade remains supported");
            player.setItemSlot(EquipmentSlot.HEAD, core("void_robe_helmet"));
            check(EquipmentVision.canSeeNodes(player), "native void robe helmet retains node visibility");
            var conditional = new ItemStack(ClothTestMod.PROTECTED);
            CustomData.update(DataComponents.CUSTOM_DATA, conditional, tag -> tag.putBoolean("nodes", true));
            player.setItemSlot(EquipmentSlot.HEAD, conditional);
            check(EquipmentVision.canSeeNodes(player) && !EquipmentVision.canSeeAspects(player), "node and popup interfaces are evaluated independently");
            CustomData.update(DataComponents.CUSTOM_DATA, conditional, tag -> { tag.putBoolean("nodes", false); tag.putBoolean("popups", true); });
            check(!EquipmentVision.canSeeNodes(player) && EquipmentVision.canSeeAspects(player), "interface callback results are not replaced with a fixed item-class check");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.CHEST, conditional);
            check(!EquipmentVision.canSeeNodes(player) && !EquipmentVision.canSeeAspects(player), "original interfaces are queried only on the head slot");
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            var recipe = (IArcaneRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("revealing_helm")).orElseThrow().value();
            var goggles = core("goggles_of_revealing"); var helmet = core("thaumium_helmet");
            check(recipe.matches(CraftingInput.of(2, 1, List.of(goggles, helmet)), level), "original goggles plus thaumium helmet pattern");
            check(recipe.matches(CraftingInput.of(2, 1, List.of(helmet, goggles)), level), "original arcane recipe allows mirroring");
            goggles.setDamageValue(1);
            check(!recipe.matches(CraftingInput.of(2, 1, List.of(goggles, helmet)), level), "damaged goggles are not wildcard recipe ingredients");
            goggles.setDamageValue(0); helmet.setDamageValue(1);
            check(!recipe.matches(CraftingInput.of(2, 1, List.of(goggles, helmet)), level), "damaged thaumium helmet is rejected");
            helmet.setDamageValue(0); goggles.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named goggles"));
            check(recipe.matches(CraftingInput.of(2, 1, List.of(goggles, helmet)), level), "extra tags are allowed for a metadata-zero ingredient without recipe tags");
            check(recipe.vis().equals(Map.of("terra", 5, "ignis", 5, "aqua", 5, "aer", 5, "ordo", 5, "perditio", 5)), "original six-aspect arcane cost");
            var research = AddonRegistry.research().get("thaumictinkerer:revealing_helm");
            check(research.parents().equals(List.of("GOGGLES")) && research.hiddenParents().equals(List.of("THAUMIUM"))
                    && research.complexity() == 1 && research.column() == 0 && research.row() == 0, "original research prerequisites, difficulty and coordinates");
            var victimStub = FocusRulesTest.player(helper, "[TTArmorWear]");
            var victim = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, victimStub.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
            victim.connection = victimStub.connection;
            victim.getAbilities().invulnerable = false; victim.getAbilities().instabuild = false;
            // New ServerPlayers have sixty ticks of spawn protection. Cactus damage
            // exercises armor without mob damage scaling to zero in peaceful tests.
            for (int tick = 0; tick < 60; tick++) victim.tick();
            var physicalDamage = level.damageSources().cactus();
            try {
                var worn = new ItemStack(item); worn.setDamageValue(499); victim.setItemSlot(EquipmentSlot.HEAD, worn);
                boolean hit = victim.hurt(physicalDamage, 4);
                check(hit && worn.getDamageValue() == 500 && !worn.isEmpty(), "actual damage pipeline retains armor at damage equal to five hundred: hit=" + hit + ", damage=" + worn.getDamageValue() + ", count=" + worn.getCount());
                victim.invulnerableTime = 0;
                check(victim.hurt(physicalDamage, 4) && worn.isEmpty(), "next actual armor hit breaks only above the original maximum");
            } finally { victim.discard(); }
            System.out.println("TT_REVEALING_HELM_PASS checks=" + checks); helper.succeed();
        } finally { player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); }
    }
}
