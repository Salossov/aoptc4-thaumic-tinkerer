package thaumic.tinkerer.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;

/** Old infusion component stack sizes are display hints; its matcher ignores
 * the recipe stack's size. Do not turn that hint into extra consumed components.
 */
public record LegacyDisplayCountIngredient(Ingredient ingredient,int displayCount) implements ICustomIngredient {
    public static final MapCodec<LegacyDisplayCountIngredient> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(LegacyDisplayCountIngredient::ingredient),
            Codec.intRange(1,64).fieldOf("display_count").forGetter(LegacyDisplayCountIngredient::displayCount)).apply(instance,LegacyDisplayCountIngredient::new));
    private static final DeferredRegister<IngredientType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,ThaumicTinkerer.ID);
    private static final DeferredHolder<IngredientType<?>,IngredientType<LegacyDisplayCountIngredient>> TYPE=TYPES.register("legacy_display_count",()->new IngredientType<>(CODEC));
    public LegacyDisplayCountIngredient {Objects.requireNonNull(ingredient);if(displayCount<1||displayCount>64)throw new IllegalArgumentException("Invalid display count");}
    public static void register(IEventBus bus) {TYPES.register(bus);}
    @Override public boolean test(ItemStack stack) {return ingredient.test(stack);}
    @Override public Stream<ItemStack> getItems() {return java.util.Arrays.stream(ingredient.getItems()).map(stack->stack.copyWithCount(displayCount));}
    @Override public boolean isSimple() {return false;}
    @Override public IngredientType<?> getType() {return TYPE.get();}
}
