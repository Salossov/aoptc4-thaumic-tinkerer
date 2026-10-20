package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InkwellRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void infusedInkwell(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = FakePlayerFactory.getMinecraft(level);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var item = ThaumicTinkerer.INFUSED_INKWELL.get();
        var stack = new ItemStack(item);
        check(item instanceof IScribeTools && stack.getMaxDamage() == 800 && stack.getMaxStackSize() == 1, "original marker, eight-hundred capacity and singleton stack");
        check(stack.hasFoil() && !stack.isEnchanted(), "intrinsic glint without invented enchantments");
        stack.setDamageValue(799);
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Preserved ink"));
        var restored = ItemStack.parse(level.registryAccess(), stack.save(level.registryAccess())).orElseThrow();
        check(restored.getDamageValue() == 799 && restored.getHoverName().getString().equals("Preserved ink"), "damage and other components survive real serialization");
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("infused_inkwell_refill")).orElseThrow().value();
        var ingredients = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) ingredients.add(i == 4 ? stack : new ItemStack(Items.INK_SAC));
        var input = CraftingInput.of(3, 3, ingredients);
        check(recipe.matches(input, level), "wildcard legacy damage accepts an almost exhausted inkwell");
        stack.setDamageValue(800);
        check(recipe.matches(input, level), "exhausted ink can be refilled");
        var result = recipe.assemble(input, level.registryAccess());
        check(result.is(item) && result.getDamageValue() == 0 && result.getCount() == 1, "eight ink sacs refill to a fresh item");
        check(!result.has(DataComponents.CUSTOM_NAME), "original shaped output does not copy the old item's custom tags");
        check(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "old inkwell is consumed without leaving a crafting remainder");
        ingredients.set(0, new ItemStack(Items.BLACK_DYE));
        check(!recipe.matches(CraftingInput.of(3, 3, ingredients), level), "legacy dye metadata zero means ink sac, not modern black dye");
        var infusion = (InfusionRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("infused_inkwell")).orElseThrow().value();
        check(infusion.research().equals("INFUSED_INKWELL") && infusion.instability() == 2
                && infusion.aspects().equals(Map.of("vacuos", 8, "tenebrae", 8)), "exact original gated infusion and costs");
        var coreInk = BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:scribing_tools"));
        check(coreInk instanceof IScribeTools && infusion.input().test(new ItemStack(coreInk)), "core ink implements the same public extension marker");
        var damagedInk = new ItemStack(coreInk); damagedInk.setDamageValue(1);
        check(!infusion.input().test(damagedInk), "original infusion requires undamaged ink rather than wildcard metadata");
        damagedInk.setDamageValue(0); damagedInk.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named ink"));
        check(infusion.input().test(damagedInk), "unrelated item data is allowed when the original recipe supplied no tags");
        for (int i = 0; i < 3; i++) check(infusion.components().get(i).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:" + List.of("air_shard", "warded_jar", "quicksilver").get(i))))), "infusion component order " + i);
        check(!infusion.components().get(2).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:alumentum")))), "resource metadata 3 is quicksilver, not alumentum");
        check(!AddonRegistry.research().containsKey("INFUSED_INKWELL") && !AddonRegistry.research().containsKey("thaumictinkerer:infused_inkwell"), "no missing original research is silently invented");
        try {
            player.setPos(pos.getX(), pos.getY(), pos.getZ() + 2);
            var table = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:research_table"));
            level.setBlockAndUpdate(pos, table.defaultBlockState());
            check(level.getBlockEntity(pos) instanceof MenuProvider, "real core research table provides a menu");
            var menu = ((MenuProvider) level.getBlockEntity(pos)).createMenu(1, player.getInventory(), player);
            check(menu != null && menu.getSlot(0).mayPlace(stack), "actual core research-table slot accepts addon ink through IScribeTools");
            check(menu.getSlot(0).mayPlace(new ItemStack(coreInk)) && !menu.getSlot(0).mayPlace(new ItemStack(Items.INK_SAC)), "marker extension retains native ink and rejects raw sacs");
            menu.getSlot(0).set(stack);
            check(menu.getSlot(0).getItem().is(item) && menu.getSlot(0).getItem().getDamageValue() == 800, "menu retains exhausted inkwell and its exact damage");
            menu.getSlot(0).set(ItemStack.EMPTY);
            System.out.println("TT_INKWELL_PASS checks=" + checks); helper.succeed();
        } finally {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) container.clearContent();
            level.removeBlock(pos, false);
        }
    }
}
