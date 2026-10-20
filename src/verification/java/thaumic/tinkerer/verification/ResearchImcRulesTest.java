package thaumic.tinkerer.verification;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.InterModComms;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.compat.TinkererResearchImc;
import thaumic.tinkerer.kami.KamiResearch;
import thaumic.tinkerer.research.TinkererResearch;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ResearchImcRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static InterModComms.IMCMessage message(String method,java.util.function.Supplier<?> value){
        return new InterModComms.IMCMessage("tt_native_probe",ThaumicTinkerer.ID,method,value);
    }
    private static AutoCloseable restore(){var before=List.copyOf(KamiResearch.BLACKLIST);return ()->{KamiResearch.BLACKLIST.clear();KamiResearch.BLACKLIST.addAll(before);};}
    @GameTest(template="empty",batch="kami_imc",timeoutTicks=100)
    public static void sourceSplitCaseAndDuplicateContract(GameTestHelper helper)throws Exception{
        VerificationReports.install();try(var ignored=restore()){
            int start=KamiResearch.BLACKLIST.size();String text=",UPPER, mixed,,UPPER,";
            check(TinkererResearchImc.processMessage(message("aDdReSeArChBlAcKlIsT",()->text)),"source case-insensitive operation");
            check(KamiResearch.BLACKLIST.subList(start,KamiResearch.BLACKLIST.size()).equals(Arrays.asList(text.split(","))),"source split including exact whitespace, duplicates and empty prefixes");
            check(!TinkererResearchImc.processMessage(message("OtherMethod",()->{throw new AssertionError("unrelated supplier must not run");})),"unrelated operations not evaluated");
        }helper.succeed();
    }
    @GameTest(template="empty",batch="kami_imc",timeoutTicks=100)
    public static void acceptedIterableAndRecoverableFaults(GameTestHelper helper)throws Exception{
        VerificationReports.install();try(var ignored=restore()){
            int start=KamiResearch.BLACKLIST.size();
            check(TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->Arrays.asList("literal,comma",null,42,"prefix"))),"valid iterable prefixes accepted while invalid entries warn");
            check(KamiResearch.BLACKLIST.subList(start,KamiResearch.BLACKLIST.size()).equals(List.of("literal,comma","prefix")),"iterable elements remain literal and never insert null");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->{throw new IllegalStateException("fixture failure");})),"recoverable supplier exception");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",null)),"null supplier");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->new String[]{"not_an_approved_payload"})),"unapproved array format is rejected");
            Iterable<String> broken=()->new java.util.Iterator<>(){public boolean hasNext(){throw new IllegalStateException("iterator fixture");}public String next(){return "unreachable";}};
            int before=KamiResearch.BLACKLIST.size();check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->broken)),"recoverable iterator exception");
            check(KamiResearch.BLACKLIST.size()==before,"broken iteration never commits partial prefixes");
        }helper.succeed();
    }
    @GameTest(template="empty",batch="kami_imc",timeoutTicks=100)
    public static void actualStartupNodeUsesSameBlacklist(GameTestHelper helper)throws Exception{
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        try(var ignored=restore()){
            var ichor=AddonRegistry.research().get("thaumictinkerer:ichor");check(ichor!=null,"real startup KAMI node");
            String candidate="thaumictinkerer:imc_probe_candidate";
            var definition=new ResearchDefinition(candidate,TinkererResearch.CATEGORY,List.of("BASE"),Map.of("aer",1),1,false,false,0,0,List.of(),List.of(),"minecraft:stone",false,false,false,false,List.of(),List.of(),false,0,List.of(),List.of(),List.of(),List.of());
            var catalog=new LinkedHashMap<String,ResearchDefinition>();catalog.put(ichor.key(),ichor);catalog.put(candidate,definition);
            check(KamiResearch.requirements(ichor,catalog).contains(candidate),"actual registered-node rule initially includes candidate");
            TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->"IMC_PROBE_"));
            check(!KamiResearch.requirements(ichor,catalog).contains(candidate),"same global list feeds real dynamic catalog rule");
        }helper.succeed();
    }
    private ResearchImcRulesTest(){}
}
