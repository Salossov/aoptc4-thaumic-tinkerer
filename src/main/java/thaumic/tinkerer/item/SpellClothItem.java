package thaumic.tinkerer.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class SpellClothItem extends Item {
    public static final int DURABILITY = 35;

    public SpellClothItem() {
        super(new Properties().durability(DURABILITY).rarity(Rarity.UNCOMMON));
    }

    @Override public boolean isRepairable(ItemStack stack) { return false; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) {
        // ItemSpellCloth.getContainerItem: every successful craft costs exactly one use.
        int damage = stack.getDamageValue() + 1;
        // Quirk kept: Forge 1.7.10 SlotCrafting discards a remainder only ABOVE max damage.
        // Damage 35 is therefore a valid remainder and the cloth supports 36 crafts.
        if (damage > stack.getMaxDamage()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copyWithCount(1);
        remainder.setDamageValue(damage);
        return remainder;
    }
}
