package thaumic.tinkerer.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumic.tinkerer.api.INoRemoveEnchant;
import thaumic.tinkerer.item.SpellClothItem;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.verification.ClothTestMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual recipe methods in a real NeoForge test server; excluded from release artifacts. */
@GameTestHolder(ClothTestMod.ID)
@PrefixGameTestTemplate(false)
public final class SpellClothRulesTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static CraftingInput grid(ItemStack... stacks) {
        var items = new ArrayList<ItemStack>(List.of(stacks));
        while (items.size() < 4) items.add(ItemStack.EMPTY);
        return CraftingInput.of(2, 2, items);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void rules(GameTestHelper helper) {
        thaumic.tinkerer.verification.VerificationReports.install();
        checks = 0;
        var cloth = ThaumicTinkerer.SPELL_CLOTH.get();
        var protectedItem = ClothTestMod.PROTECTED;
        var registries = helper.getLevel().registryAccess();
        var sharpness = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        ItemStack target = new ItemStack(Items.DIAMOND_SWORD);
        target.enchant(sharpness, 3);
        target.setDamageValue(17);
        target.set(DataComponents.CUSTOM_NAME, Component.literal("Original name"));
        ItemStack rag = new ItemStack(cloth);
        var input = grid(rag, target);
        var recipe = new SpellClothRecipe(CraftingBookCategory.MISC);
        check(SpellClothRecipe.matches(input, cloth), "cloth + enchanted item");
        check(SpellClothRecipe.matches(grid(target, rag), cloth), "swapped order");
        check(!SpellClothRecipe.matches(grid(rag), cloth), "cloth alone rejected");
        check(!SpellClothRecipe.matches(grid(target), cloth), "target alone rejected");
        check(!SpellClothRecipe.matches(grid(rag, new ItemStack(Items.DIAMOND_SWORD)), cloth), "plain item rejected");
        check(!SpellClothRecipe.matches(grid(rag, target, rag), cloth), "second cloth rejected");
        check(!SpellClothRecipe.matches(grid(rag, target, target), cloth), "second target rejected");
        check(!SpellClothRecipe.matches(grid(rag, target, new ItemStack(Items.DIRT)), cloth), "extra ingredient rejected");
        ItemStack protectedStack = new ItemStack(protectedItem);
        protectedStack.enchant(sharpness, 1);
        check(!SpellClothRecipe.matches(grid(rag, protectedStack), cloth), "original opt-out interface honored");
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(sharpness, 2);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        check(!SpellClothRecipe.matches(grid(rag, book), cloth), "stored book enchantments are not the old ench tag");

        ItemStack output = recipe.assemble(input, registries);
        check(!output.isEnchanted(), "applied enchantments removed");
        check(output.is(Items.DIAMOND_SWORD) && output.getCount() == 1, "target identity and count retained");
        check(output.getDamageValue() == 17, "target damage retained");
        check(output.getHoverName().getString().equals("Original name"), "custom name retained");
        check(target.isEnchanted(), "preview does not mutate the input");
        target.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        check(SpellClothRecipe.wipe(input).get(DataComponents.STORED_ENCHANTMENTS).equals(stored.toImmutable()), "unrelated stored-enchantment component retained");
        ItemStack remainder = recipe.getRemainingItems(input).stream().filter(s -> s.is(cloth)).findFirst().orElseThrow();
        check(remainder.getDamageValue() == 1 && rag.getDamageValue() == 0, "one-use remainder without mutating original cloth");
        check(recipe.getRemainingItems(input).stream().filter(s -> !s.isEmpty()).count() == 1, "target never returned as a container");
        int uses = 0;
        ItemStack next = rag;
        while (!next.isEmpty() && uses < 40) { next = cloth.getCraftingRemainingItem(next); uses++; }
        check(uses == 36 && next.isEmpty(), "Forge 1.7.10 greater-than-max quirk: exactly 36 uses");
        check(!cloth.isRepairable(rag) && !cloth.isFoil(rag) && !cloth.isBarVisible(remainder), "no repairs, glint or durability bar");
        check(recipe.canCraftInDimensions(2, 2) && recipe.canCraftInDimensions(3, 3) && !recipe.canCraftInDimensions(1, 1), "two crafting slots required");

        ItemStack stackableTarget = new ItemStack(Items.STICK, 12);
        stackableTarget.enchant(sharpness, 1);
        var stackedInput = grid(rag, stackableTarget);
        check(SpellClothRecipe.wipe(stackedInput).getCount() == 12, "original wipes a whole enchanted stack");
        var container = new SimpleContainer(rag.copy(), stackableTarget.copy());
        check(SpellClothRecipe.consumeWholeTarget(container, cloth), "whole-stack consumption recognized");
        check(container.getItem(1).getCount() == 1, "only one target left for automatic ResultSlot consumption");
        container.removeItem(1, 1);
        check(container.getItem(1).isEmpty(), "no enchanted leftovers or duplication after consumption");
        var invalid = new SimpleContainer(rag.copy(), target.copy(), new ItemStack(Items.DIRT));
        check(!SpellClothRecipe.consumeWholeTarget(invalid, cloth) && invalid.getItem(1).isEnchanted(), "invalid craft leaves inventory intact");

        var definition = AddonRegistry.research().get("thaumictinkerer:spell_cloth");
        check(definition != null && definition.hiddenParents().equals(List.of("ENCHFABRIC")),
                "real research keeps its hidden parent: " + definition + "; registered=" + AddonRegistry.research().keySet());
        check(definition.complexity() == 2 && definition.column() == 3 && definition.row() == 2,
                "original research complexity and coordinates");
        check(definition.aspects().equals(java.util.Map.of("praecantatio", 2, "pannus", 1)), "original research aspect amounts");
        var registered = helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("spell_cloth")).orElseThrow().value();
        check(registered instanceof CrucibleRecipe, "real crucible JSON was decoded by the core");
        var crucible = (CrucibleRecipe) registered;
        check(crucible.aspects().equals(java.util.Map.of("praecantatio", 10, "perditio", 6, "permutatio", 4)), "real crucible keeps original costs");
        check(crucible.research().equals("thaumictinkerer:spell_cloth") && crucible.getRecipeOutput().is(cloth), "real crucible research and output");

        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        var menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)));
        var craftGrid = menu.getSlot(1).container;
        menu.getSlot(1).set(rag.copy());
        menu.getSlot(2).set(stackableTarget.copy());
        menu.slotsChanged(craftGrid);
        var resultSlot = menu.getSlot(0);
        check(resultSlot.getItem().getCount() == 12 && !resultSlot.getItem().isEnchanted(), "vanilla crafting menu resolves the registered custom recipe");
        var taken = resultSlot.remove(12);
        resultSlot.onTake(player, taken);
        check(menu.getSlot(2).getItem().isEmpty(), "real ItemCrafted event and ResultSlot consume the entire target stack");
        check(menu.getSlot(1).getItem().is(cloth) && menu.getSlot(1).getItem().getDamageValue() == 1,
                "real crafting flow returns one damaged cloth");
        var lastUse = new ItemStack(cloth);
        lastUse.setDamageValue(35);
        menu.getSlot(1).set(lastUse);
        menu.getSlot(2).set(target.copyWithCount(1));
        menu.slotsChanged(craftGrid);
        var lastResult = resultSlot.remove(1);
        check(!lastResult.isEmpty(), "maximum-damage cloth can perform its final original use");
        resultSlot.onTake(player, lastResult);
        check(menu.getSlot(1).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty(), "final craft consumes worn cloth and target exactly once");
        System.out.println("TT_SPELL_CLOTH_PASS checks=" + checks);
        helper.succeed();
    }
}
