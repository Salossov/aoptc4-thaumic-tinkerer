package thaumic.tinkerer.summon;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IObjectAspectItem;
import thaumic.tinkerer.util.ItemData;

/** The original metadata stride is 21, although NumericAspectHelper registers only 17 aspects. */
public final class MobAspectItem extends Item implements IObjectAspectItem {
    public MobAspectItem() { super(new Properties().stacksTo(16).component(SummonContent.VARIANT.get(), 0)); }
    public static int variant(ItemStack stack) { return stack.getOrDefault(SummonContent.VARIANT.get(), 0); }
    public static ItemStack of(int variant) { var stack = new ItemStack(SummonContent.MOB_ASPECT.get()); stack.set(SummonContent.VARIANT.get(), variant); return stack; }
    public static ItemStack of(Aspect aspect) { return of(MobAspects.ASPECTS.indexOf(aspect)); }
    public static Aspect aspect(ItemStack stack) { return MobAspects.ASPECTS.get(variant(stack) % 21); }
    public static boolean condensed(ItemStack stack) { int variant = variant(stack); return variant >= 21 && variant < 42; }
    public static boolean infused(ItemStack stack) { return variant(stack) >= 42; }
    public static void markLastUsed(ItemStack stack, BlockPos tablet) { ItemData.update(stack, tag -> { tag.putInt("LastX", tablet.getX()); tag.putInt("LastY", tablet.getY()); tag.putInt("LastZ", tablet.getZ()); }); }
    public static boolean lastUsedMatches(ItemStack stack, BlockPos tablet) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return true;
        var tag = ItemData.read(stack); return tag.getInt("LastX") == tablet.getX() && tag.getInt("LastY") == tablet.getY() && tag.getInt("LastZ") == tablet.getZ();
    }
    @Override public boolean isFoil(ItemStack stack) { return infused(stack); }
    @Override public Component getName(ItemStack stack) { return Component.translatable(getDescriptionId() + (condensed(stack) ? ".condensed" : infused(stack) ? ".infused" : "")); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) { lines.add(Component.literal(aspect(stack).getName())); }
    @Override public Map<String, Integer> objectAspects(ItemStack stack) {
        int variant = variant(stack); if (variant < 0 || variant >= 63 || variant % 21 >= MobAspects.ASPECTS.size()) return Map.of();
        // TC4: raw explicit 8; nine raw * .75 -> 54; infusion's nine condensed * .75 + sqrt(10), capped at 64.
        return Map.of(aspect(stack).getTag(), variant < 21 ? 8 : variant < 42 ? 54 : 64);
    }
    @Override public String scanVariant(ItemStack stack) { return Integer.toString(variant(stack)); }
    @Override public ItemStack inventoryScanStack(ItemStack stack) { return of(variant(stack)); }
}
