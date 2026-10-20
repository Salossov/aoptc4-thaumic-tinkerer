package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.kami.KamiWandContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiWandRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @GameTest(template="summon_space",batch="kami_wands",timeoutTicks=100)
    public static void nativeWandAssemblyAndCosts(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            check(!WandRod.rods.containsKey(KamiWandContent.ROD)&&!WandCap.caps.containsKey(KamiWandContent.CAP),"disabled KAMI omits wand materials");helper.succeed();return;
        }
        var rod=WandRod.rods.get(KamiWandContent.ROD);var cap=WandCap.caps.get(KamiWandContent.CAP);
        check(rod.getCapacity()==1000&&rod.getCraftCost()==10&&cap.getCraftCost()==10&&cap.getBaseCostModifier()==.7f,"source material capacity,costs and discount");
        check(rod.getItem().is(KamiMaterials.ICHORCLOTH_ROD.get())&&cap.getItem().is(KamiMaterials.ICHOR_CAP.get()),"correct material items rather than source unused constructor variants");
        var parts=new WandParts(KamiWandContent.ROD,KamiWandContent.CAP);
        for(var kind:List.of("wand","sceptre")) {
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+kind+"_assembly")).orElseThrow().value();
            var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(4,rod.getItem());grid.set(6,cap.getItem());
            if(kind.equals("sceptre")) {
                grid.set(1,cap.getItem());grid.set(7,cap.getItem());grid.set(2,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:primal_charm"))));
            } else grid.set(2,cap.getItem());
            var input=CraftingInput.of(3,3,grid);check(recipe.matches(input,level),"native assembly accepts namespaced parts");
            int cost=kind.equals("sceptre")?150:100;check(recipe.vis(input).size()==6&&recipe.vis(input).values().stream().allMatch(value->value==cost),"source whole-vis assembly cost");
            var result=recipe.assemble(input,level.registryAccess());
            check(!result.isEmpty()&&WandApi.parts(result).equals(parts)&&WandApi.capacity(result)==(kind.equals("sceptre")?150000:100000),"actual output retains keys and centivis capacity");
            WandApi.set(result,Aspects.AIR,100000);var restored=ItemStack.parse(level.registryAccess(),result.save(level.registryAccess())).orElseThrow();
            check(WandApi.parts(restored).equals(parts)&&WandApi.stored(restored,Aspects.AIR)==100000,"native wand parts and vis persist together");
            try(var fixture=new NativePlayerFixture(helper,"TTKamiWand"+kind,helper.absolutePos(new BlockPos(8,4,8)))) {
                check(!recipe.mayCraft(fixture.player,input),"assembly denies missing component research");
                ResearchApi.grant(fixture.player,rod.getResearch());ResearchApi.grant(fixture.player,cap.getResearch());
                if(kind.equals("sceptre"))ResearchApi.grant(fixture.player,"SCEPTRE");
                check(recipe.mayCraft(fixture.player,input),"component research authorizes namespaced assembly");
                // Source float subtraction .7f-.1f truncates 59.999996 to 59 centivis.
                int expected=kind.equals("sceptre")?59:70;
                check(WandApi.quoteCrafting(result,fixture.player,Map.of(Aspects.AIR,1)).get(Aspects.AIR)==expected,"source cap and sceptre cost modifiers");
            }
            var layers=WandVisuals.layers(kind,parts);check(layers.size()==2&&layers.get(1).lighting()==null,"rod and ordinary world-lit cap have separate passes");
            check(layers.getFirst().lighting().packedLight(0xf000f0,0)==205&&layers.getFirst().lighting().packedLight(0,0)==205,"source raw lightmap coordinate replaces world/sky at viewer tick zero");
        }
        check(WandVisuals.model("staff",new WandParts("silverwood_staff",KamiWandContent.CAP))!=null&&WandVisuals.model("wand",new WandParts("wood",KamiWandContent.CAP))!=null,"mixed core rods and ichor cap have registered models");
        helper.succeed();
    }
}
