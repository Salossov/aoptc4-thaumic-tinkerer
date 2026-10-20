package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Real dispatcher and native player; no bootstrap/config/API replacement. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiCommandRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static final class Output implements CommandSource {
        final List<String> lines=new ArrayList<>();
        public void sendSystemMessage(Component component){lines.add(component.getString());}
        public boolean acceptsSuccess(){return true;}
        public boolean acceptsFailure(){return true;}
        public boolean shouldInformAdmins(){return false;}
    }
    @GameTest(template="empty",batch="kami_command",timeoutTicks=100)
    public static void actualConsoleHasClearFailureAtPermissionZero(GameTestHelper helper)throws Exception{
        VerificationReports.install();var server=helper.getLevel().getServer();var output=new Output();
        var source=server.createCommandSourceStack().withSource(output).withPermission(0);
        var dispatcher=server.getCommands().getDispatcher();
        check(dispatcher.getRoot().getChild("iskamiunlocked")!=null,"unconditionally registered independently of debug and KAMI flags");
        check(dispatcher.execute("iskamiunlocked",source)==0,"console fails without throwing");
        check(output.lines.equals(List.of("Only players can check KAMI unlock status")),"accepted visible console error");
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="kami_command",timeoutTicks=100)
    public static void actualPlayerKnowledgeAndSourceMessageOrder(GameTestHelper helper)throws Exception{
        VerificationReports.install();
        try(var fixture=new NativePlayerFixture(helper,"TTKamiCommand",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var output=new Output();var source=player.createCommandSourceStack().withSource(output).withPermission(0);
            var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
            dispatcher.execute("iskamiunlocked",source);
            if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){
                check(output.lines.equals(List.of("No","KAMI is disabled")),"accepted disabled-profile output");
            }else{
                var node=ResearchApi.definitions(player).get("thaumictinkerer:ichor");check(node!=null,"actual active catalog node");
                var missing=new ArrayList<>(node.hiddenParents());missing.removeAll(ResearchApi.knowledge(player).completedResearch());
                check(output.lines.equals(missing.isEmpty()?List.of("Yes"):List.of("No","Remaining: "+String.join(",",missing))),"literal source order and comma join against real knowledge");
                for(var parent:node.hiddenParents())ResearchApi.grant(player,parent);
                output.lines.clear();dispatcher.execute("iskamiunlocked ignored arguments",source);
                check(output.lines.equals(List.of("Yes")),"ordinary player permission and original ignored extra arguments");
            }
        }
        helper.succeed();
    }
    private KamiCommandRulesTest(){}
}
