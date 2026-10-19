package thaumic.tinkerer.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.block.MagnetContent;

/** RecipeHelper's actual expression, rather than the different behavior suggested by its config comment. */
public record OreMetalIngredient(TagKey<Item> tag, Item fallback) implements ICustomIngredient {
    public static final MapCodec<OreMetalIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(OreMetalIngredient::tag),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("fallback").forGetter(OreMetalIngredient::fallback)
    ).apply(instance, OreMetalIngredient::new));
    private boolean useFallback() { return BuiltInRegistries.ITEM.getTag(tag).map(set -> set.size() == 0).orElse(true) && TinkererConfig.ORE_DICT_METAL.get(); }
    @Override public boolean test(ItemStack stack) { return useFallback() ? stack.is(fallback) : stack.is(tag); }
    @Override public Stream<ItemStack> getItems() {
        return useFallback() ? Stream.of(new ItemStack(fallback)) : BuiltInRegistries.ITEM.getTag(tag).stream().flatMap(set -> set.stream()).map(holder -> new ItemStack(holder));
    }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return MagnetContent.ORE_METAL.get(); }
}
