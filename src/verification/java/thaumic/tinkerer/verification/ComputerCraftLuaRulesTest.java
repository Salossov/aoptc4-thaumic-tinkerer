package thaumic.tinkerer.verification;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ComputerCraftLuaRulesTest {
    @GameTest(template="empty",batch="actual_cc_lua",timeoutTicks=12000)
    public static void tabletUsesLiveLuaArguments(GameTestHelper helper)throws Exception {
        VerificationReports.install();
        if(!ModList.get().isLoaded("computercraft")){helper.succeed();return;}
        ComputerCraftLuaChecks.run(helper);
    }
    private ComputerCraftLuaRulesTest(){}
}
