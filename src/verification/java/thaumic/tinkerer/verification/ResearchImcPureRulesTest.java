package thaumic.tinkerer.verification;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.neoforged.fml.InterModComms;
import thaumic.tinkerer.compat.TinkererResearchImc;
import thaumic.tinkerer.kami.KamiResearch;

/** Actual message consumer against the literal old split loop; no game/bootstrap/world. */
public final class ResearchImcPureRulesTest {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static InterModComms.IMCMessage message(String operation,java.util.function.Supplier<?> supplier){
        return new InterModComms.IMCMessage("tt_pure_probe","thaumictinkerer",operation,supplier);
    }
    public static void main(String[] args){
        var original=List.copyOf(KamiResearch.BLACKLIST);
        try{
            var random=new Random(164);String alphabet="Aa,Bb, ,,,";
            for(int n=0;n<10000;n++){
                var text=new StringBuilder();for(int k=random.nextInt(100);k>0;k--)text.append(alphabet.charAt(random.nextInt(alphabet.length())));
                String payload=text.toString();KamiResearch.BLACKLIST.clear();
                check(TinkererResearchImc.processMessage(message(n%2==0?"AddResearchBlacklist":"addresearchblacklist",()->payload)),"recognized literal source operation");
                check(KamiResearch.BLACKLIST.equals(Arrays.asList(payload.split(","))),"source split including empty, duplicates, whitespace and case");
            }
            KamiResearch.BLACKLIST.clear();
            check(!TinkererResearchImc.processMessage(message("Other",()->{throw new AssertionError("must not evaluate unrelated supplier");})),"ignore unrelated supplier");
            check(TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->List.of("literal,comma"," PREFIX "))),"accepted iterable format");
            check(KamiResearch.BLACKLIST.equals(List.of("literal,comma"," PREFIX ")),"iterable entries are literal prefixes");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->new String[]{"unsupported"})),"unapproved array format rejected");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",()->{throw new IllegalArgumentException("recoverable fixture");})),"recoverable supplier failure");
            check(!TinkererResearchImc.processMessage(message("AddResearchBlacklist",null)),"missing supplier");
            System.out.println("TT_RESEARCH_IMC_PURE_RULES_PASS checks="+checks+" game_started=false bootstrap_replaced=false");
        }finally{KamiResearch.BLACKLIST.clear();KamiResearch.BLACKLIST.addAll(original);}
    }
    private ResearchImcPureRulesTest(){}
}
