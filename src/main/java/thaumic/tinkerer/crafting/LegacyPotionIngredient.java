package thaumic.tinkerer.crafting;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;

/** The source matches potion metadata, allowing unrelated NBT/custom effects. */
public record LegacyPotionIngredient(Holder<Potion> potion) implements ICustomIngredient {
    public static final MapCodec<LegacyPotionIngredient> CODEC=Potion.CODEC.fieldOf("potion").xmap(LegacyPotionIngredient::new,LegacyPotionIngredient::potion);
    private static final DeferredRegister<IngredientType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES,ThaumicTinkerer.ID);
    private static final DeferredHolder<IngredientType<?>,IngredientType<LegacyPotionIngredient>> TYPE=TYPES.register("legacy_potion",()->new IngredientType<>(CODEC));
    public static void register(IEventBus bus) {TYPES.register(bus);}
    @Override public boolean test(ItemStack stack) {
        return stack.is(Items.POTION)&&stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().filter(value->value.is(potion)).isPresent();
    }
    @Override public Stream<ItemStack> getItems() {return Stream.of(PotionContents.createItemStack(Items.POTION,potion));}
    @Override public boolean isSimple() {return false;}
    @Override public IngredientType<?> getType() {return TYPE.get();}
}
