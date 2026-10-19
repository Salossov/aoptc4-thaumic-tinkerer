package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
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
public final class KamiFoundationRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="kami_foundation",timeoutTicks=100)
    public static void nativeMaterialsRecipesAndCatalog(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();
        var names=List.of("ichor","ichor_cloth","ichorium","ichor_nugget","ichor_cap","ichorcloth_rod","nether_shard","ender_shard");
        var recipes=List.of("ichor","ichor_cloth","ichorium","cap_ichor","rod_ichorcloth","ichor_nuggets");
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            for(var name:names)check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id(name)),"KAMI-off omits material "+name);
            for(var name:recipes)check(level.getRecipeManager().byKey(ThaumicTinkerer.id(name)).isEmpty(),"KAMI condition omits recipe "+name);
            helper.succeed();return;
        }
        for(int i=0;i<names.size();i++) {
            var stack=KamiMaterials.sourceOrder().get(i).get().getDefaultInstance();
            check(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ThaumicTinkerer.id(names.get(i))),"permanent ID matches original variant "+i);
            check(stack.getMaxStackSize()==64&&stack.getRarity()==(i<6?Rarity.EPIC:Rarity.COMMON),"source stack size and variant rarity");
            ItemData.update(stack,data->data.putString("unrelated","preserved"));
            var restored=ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();
            check(restored.is(stack.getItem())&&ItemData.read(restored).getString("unrelated").equals("preserved"),"native item ID/components round trip");
        }
        var infusion=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("ichor")).orElseThrow().value();
        check(infusion.getRecipeOutput().is(KamiMaterials.ICHOR.get())&&infusion.getRecipeOutput().getCount()==8&&infusion.instability()==7,"source ichor output and instability");
        check(infusion.aspects().equals(Map.of("humanus",32,"lux",32,"spiritus",64)),"source ichor essentia");
        var inputs=List.of(new ItemStack(Items.DIAMOND),new ItemStack(KamiMaterials.ENDER_SHARD.get()),new ItemStack(Items.ENDER_EYE),new ItemStack(KamiMaterials.NETHER_SHARD.get()));
        check(infusion.matches(new InfusionInput(new ItemStack(Items.NETHER_STAR),inputs)),"one shard on each of two pedestals matches original count-insensitive infusion");
        check(infusion.components().size()==4&&infusion.components().get(1).getItems()[0].getCount()==8&&infusion.components().get(3).getItems()[0].getCount()==8,"source eight-shard hints are displayed without adding pedestal requirements");
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,infusion.components().get(1));var decoded=Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            check(decoded.test(inputs.get(1))&&decoded.getItems()[0].getCount()==8,"native ingredient wire codec retains hint and one-item matching");
        } finally {buffer.release();}
        for(var name:List.of("ichor_cloth","ichorium","cap_ichor")) {
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id(name)).orElseThrow().value();
            int cost=name.equals("ichor_cloth")?125:100;
            check(recipe.vis().equals(Map.of("aer",cost,"ignis",cost,"aqua",cost,"terra",cost,"ordo",cost,"perditio",cost)),"all six source primal costs "+name);
        }
        var nuggets=(net.minecraft.world.item.crafting.CraftingRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("ichor_nuggets")).orElseThrow().value();
        check(nuggets.matches(CraftingInput.of(1,1,List.of(KamiMaterials.ICHORIUM.get().getDefaultInstance())),level)&&nuggets.getResultItem(level.registryAccess()).getCount()==9,"actual shapeless nine-nugget recipe");
        try(var fixture=new NativePlayerFixture(helper,"TTKamiCatalog",helper.absolutePos(new BlockPos(8,4,8)))) {
            var catalog=ResearchApi.definitions(fixture.player);var ichor=catalog.get("thaumictinkerer:ichor");
            check(ichor!=null&&ichor.warp()==5&&ichor.concealed(),"actual effective server catalog retains source ichor flags");
            check(ichor.hiddenParents().contains("INFUSION")&&ichor.hiddenParents().contains("thaumictinkerer:blood_sword"),"active catalog supplies vanilla and addon requirements dynamically");
            check(!ichor.hiddenParents().contains("thaumictinkerer:ichorium")&&!ichor.hiddenParents().contains("ELDRITCHMINOR"),"KAMI and eldritch research are excluded from universal requirements");
            var shards=catalog.get("thaumictinkerer:dimension_shards");check(shards.autoUnlock()&&shards.hiddenParents().isEmpty()&&shards.round()&&shards.stub(),"auto-unlocked dimensional shard entry remains ungated");
            check(catalog.get("thaumictinkerer:ichorium").hiddenParents().contains("thaumictinkerer:ichor_cloth"),"explicit hidden KAMI parent survives dynamic augmentation");
            boolean immutable=false;try{catalog.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"server catalog API returns immutable definitions");
        }
        helper.succeed();
    }
}
