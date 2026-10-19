package thaumic.tinkerer.verification;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.integration.MultipartCompatibility;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class MultipartRulesTest {
    static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }
    @GameTest(template="summon_space", batch="multipart", timeoutTicks=100)
    public static void nativeMultipartMaterials(GameTestHelper helper) {
        VerificationReports.install();
        var specs = MultipartCompatibility.materials();
        check(specs.stream().map(MultipartCompatibility.MaterialSpec::block).toList().equals(List.of(
                "thaumcraft:cinnabar_ore", "thaumcraft:amber_ore", "thaumcraft:greatwood_planks", "thaumcraft:silverwood_planks",
                "thaumcraft:greatwood_log", "thaumcraft:greatwood_leaves", "thaumcraft:amber_block", "thaumcraft:obsidian_tile",
                "thaumcraft:obsidian_totem", "thaumcraft:paving_stone_travel", "thaumcraft:paving_stone_warding", "thaumcraft:thaumium_block",
                "thaumcraft:tallow_block", "thaumcraft:arcane_stone", "thaumictinkerer:dark_quartz", "thaumictinkerer:chiseled_dark_quartz")),
                "exclusive source metadata ranges preserve all sixteen identities and registration order");
        check(!BuiltInRegistries.BLOCK.containsKey(ThaumicTinkerer.id("legacy_pedestal_block")), "existing core Arcane Stone supplies metadata six without a duplicate block");
        if (net.neoforged.fml.ModList.get().isLoaded("cb_microblock")) MultipartPresentChecks.run(helper);
        else {
            check(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("cb_microblock:microblock")), "absent optional mod adds no item or linkage dependency");
            System.out.println("TT_MULTIPART_ABSENT_PASS materials=16 optional_linkage=false"); helper.succeed();
        }
    }
    private MultipartRulesTest() {}
}
