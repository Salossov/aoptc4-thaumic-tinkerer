package thaumic.tinkerer.crafting;

import java.util.ArrayList;
import net.minecraft.world.Container;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.INoRemoveEnchant;

public final class SpellClothRecipe extends CustomRecipe {
    public SpellClothRecipe(CraftingBookCategory category) { super(category); }

    @Override public boolean matches(CraftingInput input, Level level) {
        return matches(input, ThaumicTinkerer.SPELL_CLOTH.get());
    }

    static boolean matches(CraftingInput input, Item cloth) {
        boolean foundCloth = false;
        boolean foundEnchanted = false;
        // Preserve SpellClothRecipe's ordered checks, including the first enchanted-stack selection.
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.isEnchanted() && !(stack.getItem() instanceof INoRemoveEnchant) && !foundEnchanted) {
                foundEnchanted = true;
            } else {
                if (!stack.is(cloth) || foundCloth) return false;
                foundCloth = true;
            }
        }
        return foundCloth && foundEnchanted;
    }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return wipe(input);
    }

    static ItemStack wipe(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty() && stack.isEnchanted()) {
                // Original removes only `ench`, preserving every other tag. Stored book enchantments
                // deliberately remain untouched, as do damage, name and unrelated modern components.
                ItemStack result = stack.copy();
                result.remove(DataComponents.ENCHANTMENTS);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        boolean foundTarget = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            // Do not return the target's container item: original ItemCrafted handler clears it.
            if (!foundTarget && stack.isEnchanted()) {
                foundTarget = true;
            } else if (stack.getItem() instanceof thaumic.tinkerer.item.SpellClothItem cloth) {
                result.set(slot, cloth.getCraftingRemainingItem(stack));
            }
        }
        return result;
    }

    public static void crafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            consumeWholeTarget(event.getInventory(), ThaumicTinkerer.SPELL_CLOTH.get());
        }
    }

    static boolean consumeWholeTarget(Container inventory, Item cloth) {
        var stacks = new ArrayList<ItemStack>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) stacks.add(inventory.getItem(slot));
        if (!matches(CraftingInput.of(stacks.size(), 1, stacks), cloth)) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.isEnchanted()) {
                // Quirk kept: the original wipes an entire enchanted stack for ONE cloth use.
                // Leave one for ResultSlot to consume. Clearing the slot here would make the
                // modern recipe lookup fail before it calculates the cloth's damaged remainder.
                inventory.setItem(slot, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumicTinkerer.CLOTH_RECIPE.get(); }
}
