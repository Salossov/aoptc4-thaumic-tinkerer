package thaumic.tinkerer.verification;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionInput;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiEquipmentRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new IllegalStateException(label);}
    private static ItemStack stack(String name) {return new ItemStack(BuiltInRegistries.ITEM.get(ThaumicTinkerer.id(name)));}
    @GameTest(template="summon_space",batch="kami_equipment",timeoutTicks=100)
    public static void nativeArmorAndCatRecipes(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();
        var names=List.of("ichorcloth_helm","ichorcloth_chest","ichorcloth_legs","ichorcloth_boots");
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            for(var name:names)for(var suffix:List.of("","_gem"))check(level.getRecipeManager().byKey(ThaumicTinkerer.id(name+suffix)).isEmpty(),"disabled KAMI omits armor recipe");
            check(level.getRecipeManager().byKey(ThaumicTinkerer.id("cat_amulet")).isEmpty(),"disabled KAMI omits cat recipe");helper.succeed();return;
        }
        var patterns=List.of(List.of("CCC","C C"),List.of("C C","CCC","CCC"),List.of("CCC","C C","C C"),List.of("C C","C C"));
        var costs=List.of("aqua","aer","ignis","terra");
        for(int i=0;i<names.size();i++) {
            String name=names.get(i);var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id(name)).orElseThrow().value();
            var inputs=new ArrayList<ItemStack>();for(var row:patterns.get(i))for(char c:row.toCharArray())inputs.add(c=='C'?KamiMaterials.ICHOR_CLOTH.get().getDefaultInstance():ItemStack.EMPTY);
            var input=CraftingInput.of(3,patterns.get(i).size(),inputs);
            check(recipe.matches(input,level)&&recipe.assemble(input,level.registryAccess()).is(stack(name).getItem()),"native shaped source armor pattern/output "+name);
            check(recipe.research().equals("thaumictinkerer:ichorcloth_armor")&&recipe.vis().equals(Map.of(costs.get(i),75)),"shared source research and single primal cost");
            var infusion=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id(name+"_gem")).orElseThrow().value();
            var center=stack(name);ItemData.update(center,data->data.putString("extra","not inherited"));
            var supplied=infusion.components().stream().map(ingredient->ingredient.getItems()[0].copy()).toList();
            check(supplied.size()==12&&infusion.instability()==13&&infusion.matches(new InfusionInput(center,supplied)),"native twelve-component awakened infusion");
            var output=infusion.assemble(new InfusionInput(center,supplied),level.registryAccess());
            check(output.is(stack(name+"_gem").getItem())&&!ItemData.read(output).contains("extra"),"source produces fresh advanced armor rather than copying center NBT");
            if(i==0)checkPotion(helper,infusion.components().get(6),Potions.NIGHT_VISION,Potions.LONG_NIGHT_VISION);
            if(i==2) {
                var gold=infusion.components().get(5);var damaged=new ItemStack(Items.GOLDEN_CHESTPLATE);damaged.setDamageValue(1);
                check(gold.test(new ItemStack(Items.GOLDEN_CHESTPLATE))&&!gold.test(new ItemStack(Items.GOLDEN_LEGGINGS))&&!gold.test(damaged),"source leg recipe uses undamaged golden CHESTPLATE");
                checkPotion(helper,infusion.components().get(6),Potions.FIRE_RESISTANCE,Potions.LONG_FIRE_RESISTANCE);
            }
        }
        var cat=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("cat_amulet")).orElseThrow().value();
        check(cat.instability()==8&&cat.components().size()==6&&cat.input().test(new ItemStack(Items.QUARTZ_BLOCK)),"cat original central block and recipe shape");
        check(cat.components().get(3).test(new ItemStack(Items.COCOA_BEANS))&&cat.components().get(4).test(new ItemStack(Items.JUNGLE_LEAVES))&&cat.components().get(5).test(new ItemStack(Items.COD)),"old dye/leaves/fish variants map exactly");
        try(var fixture=new NativePlayerFixture(helper,"TTKamiEquipment",helper.absolutePos(new BlockPos(8,4,8)))) {
            var catalog=ResearchApi.definitions(fixture.player);var armor=catalog.get("thaumictinkerer:ichorcloth_armor");
            check(armor.warp()==4&&armor.column()==17&&armor.row()==5&&armor.pageOrder().size()==5&&armor.recipePages().size()==4,"source shared armor research and four recipe pages");
            var legs=catalog.get("thaumictinkerer:ichorcloth_legs_gem");
            check(legs.textPages().size()==2&&legs.pageOrder().get(1).equals(legs.recipePages().getFirst())&&legs.pageOrder().get(2).endsWith(".1"),"second legs text remains after infusion page");
            check(catalog.get("thaumictinkerer:cat_amulet").parents().equals(List.of("thaumictinkerer:ichorium")),"cat parent retains source ichorium");
        }
        helper.succeed();
    }
    private static void checkPotion(GameTestHelper helper,Ingredient ingredient,Holder<Potion> correct,Holder<Potion> extended) {
        var custom=PotionContents.createItemStack(Items.POTION,correct);
        custom.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.of(correct),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,37,2))));
        ItemData.update(custom,data->data.putString("unrelated","allowed"));
        check(ingredient.test(custom),"source permits unrelated potion NBT, custom color and custom effects");
        check(!ingredient.test(PotionContents.createItemStack(Items.POTION,extended))&&!ingredient.test(PotionContents.createItemStack(Items.SPLASH_POTION,correct))&&!ingredient.test(new ItemStack(Items.POTION)),"long/splash/empty potion differs from original required metadata");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),helper.getLevel().registryAccess());
        try{Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,ingredient);var restored=Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);check(restored.test(custom)&&!restored.test(PotionContents.createItemStack(Items.POTION,extended)),"native custom ingredient network round trip");}finally{buffer.release();}
    }
}
