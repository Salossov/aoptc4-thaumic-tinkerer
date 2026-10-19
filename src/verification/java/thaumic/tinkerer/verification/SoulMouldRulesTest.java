package thaumic.tinkerer.verification;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.SoulMouldItem;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class SoulMouldRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void soulMould(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = FocusRulesTest.player(helper, "[TTSoulMould]");
        var item = ThaumicTinkerer.SOUL_MOULD.get(); var held = new ItemStack(item); var decoy = new ItemStack(item);
        var pig = EntityType.PIG.create(level); check(pig != null, "registered living entity fixture");
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            check(held.getMaxStackSize() == 1 && SoulMouldItem.getPatternName(held).isEmpty(), "singleton empty pattern default");
            CustomData.update(DataComponents.CUSTOM_DATA, held, tag -> tag.putInt("other", 7));
            check(item.interactLivingEntity(decoy, player, pig, InteractionHand.MAIN_HAND).consumesAction(), "entity interaction is handled");
            check(SoulMouldItem.getPatternName(held).equals("minecraft:pig") && SoulMouldItem.getPatternName(decoy).isEmpty(), "original writes the current held stack rather than a supplied decoy");
            check(held.get(DataComponents.CUSTOM_DATA).copyTag().getInt("other") == 7, "pattern assignment preserves unrelated custom data");
            pig.setBaby(true); item.interactLivingEntity(held, player, pig, InteractionHand.MAIN_HAND);
            check(SoulMouldItem.getPatternName(held).equals("minecraft:pig"), "pattern identifies entity type, not age or variants");
            check(item.interactLivingEntity(held, player, player, InteractionHand.MAIN_HAND).consumesAction()
                    && SoulMouldItem.getPatternName(held).equals("minecraft:pig"), "players are handled without replacing the pattern");
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(SoulMouldItem.getPatternName(held).equals("minecraft:pig"), "ordinary air use keeps pattern");
            var saved = ItemStack.parse(level.registryAccess(), held.save(level.registryAccess())).orElseThrow();
            check(SoulMouldItem.getPatternName(saved).equals("minecraft:pig"), "pattern survives real data-component serialization");
            player.setShiftKeyDown(true); item.use(level, player, InteractionHand.MAIN_HAND);
            check(SoulMouldItem.getPatternName(held).isEmpty() && held.get(DataComponents.CUSTOM_DATA).copyTag().getInt("other") == 7, "sneaking clears only the pattern key");
            item.interactLivingEntity(decoy, null, pig, InteractionHand.MAIN_HAND);
            check(SoulMouldItem.getPatternName(decoy).equals("minecraft:pig"), "original null-player fallback writes the supplied stack");
            var recipe = (CrucibleRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("soul_mould")).orElseThrow().value();
            check(recipe.research().equals("thaumictinkerer:magnets") && recipe.catalyst().test(new ItemStack(Items.ENDER_PEARL)), "original MAGNETS gate and ender-pearl catalyst");
            check(recipe.aspects().equals(Map.of("bestia", 4, "cognitio", 8, "sensus", 8)) && recipe.getRecipeOutput().is(item), "exact original crucible output and costs");
            System.out.println("TT_SOUL_MOULD_PASS checks=" + checks); helper.succeed();
        } finally { player.setShiftKeyDown(false); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); pig.discard(); }
    }
}
