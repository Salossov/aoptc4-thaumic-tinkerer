package thaumic.tinkerer.summon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Legacy recipe stacks compare item and metadata, ignoring incidental NBT. */
public record MobAspectIngredient(int variant) implements ICustomIngredient {
    public static final MapCodec<MobAspectIngredient> CODEC = Codec.INT.fieldOf("variant").xmap(MobAspectIngredient::new, MobAspectIngredient::variant);
    @Override public boolean test(ItemStack stack) { return stack.is(SummonContent.MOB_ASPECT.get()) && MobAspectItem.variant(stack) == variant; }
    @Override public Stream<ItemStack> getItems() { return Stream.of(MobAspectItem.of(variant)); }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return SummonContent.INGREDIENT.get(); }
}
