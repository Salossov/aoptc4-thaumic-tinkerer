package thaumic.tinkerer.verification;

import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfusionMaterialRulesTest {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    private static InfusionRecipe recipe(GameTestHelper helper,String id){return (InfusionRecipe)helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id(id)).orElseThrow().value();}
    private static Ingredient reload(GameTestHelper helper,Ingredient ingredient){
        var ops=RegistryOps.create(JsonOps.INSTANCE,helper.getLevel().registryAccess());
        return Ingredient.CODEC_NONEMPTY.parse(ops,Ingredient.CODEC_NONEMPTY.encodeStart(ops,ingredient).getOrThrow()).getOrThrow();
    }
    @GameTest(template="summon_space",batch="infusion_materials",timeoutTicks=100)
    public static void nativeTagSubstitutionAndLiteralFallback(GameTestHelper helper) throws Exception {
        VerificationReports.install();
        var ingredient=recipe(helper,"enchanter").components().get(5);
        var thaumium=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot")));
        var copper=new ItemStack(Items.COPPER_INGOT);
        check(ingredient.test(thaumium)&&!ingredient.test(copper),"stock infusion accepts thaumium rather than an unrelated metal");
        var tag=BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ingots/thaumium"))).orElseThrow();
        var saved=tag.stream().toList();
        var bind=HolderSet.Named.class.getDeclaredMethod("bind",List.class);bind.setAccessible(true);
        try {
            // A temporary holder list models a foreign tag member. Fresh recipe
            // ingredients model datapack reload; no cached live recipe is changed.
            bind.invoke(tag,List.of(Items.COPPER_INGOT.builtInRegistryHolder()));
            var extended=reload(helper,ingredient);
            check(extended.test(copper)&&extended.test(thaumium),"a foreign material tag member works and the literal survives tag replacement");
            copper.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("extra NBT"));
            check(extended.test(copper)&&!extended.test(new ItemStack(Items.GOLD_INGOT)),"unrelated data is permitted without accepting another metal");
            bind.invoke(tag,List.of());
            var empty=reload(helper,ingredient);
            check(empty.test(thaumium)&&!empty.test(copper),"empty material tag retains the exact source item");
        } finally {bind.invoke(tag,saved);}
        check(reload(helper,ingredient).test(thaumium)&&!reload(helper,ingredient).test(copper),"fixture restores the real tag before other tests");
        if(TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            var cat=recipe(helper,"cat_amulet");
            for(var item:List.of(Items.COCOA_BEANS,Items.LAPIS_LAZULI,Items.INK_SAC,Items.BONE_MEAL,Items.RED_DYE))
                check(cat.components().get(3).test(new ItemStack(item)),"old wildcard dye and modern dye equivalents "+item);
            check(cat.components().get(4).test(new ItemStack(Items.OAK_LEAVES)),"old treeLeaves group permits another leaf species");
            check(!cat.components().get(3).test(new ItemStack(Items.DIRT)),"dye substitution remains material-specific");
        }
        System.out.println("TT_INFUSION_MATERIALS_NATIVE_PASS foreign_member=true empty_tag_fallback=true source_dyes=true leaves=true");
        helper.succeed();
    }
    private InfusionMaterialRulesTest(){}
}
