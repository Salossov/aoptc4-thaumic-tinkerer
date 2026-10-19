package thaumic.tinkerer.kami.warpgate;

import java.util.List;
import java.util.Map;
import thaumcraft.api.research.ResearchDefinition;
import thaumcraft.api.world.ContentRules;
import thaumic.tinkerer.kami.KamiResearch;
import thaumic.tinkerer.research.TinkererResearch;

/** Source pages and prerequisites; mirror config is evaluated with the active catalog. */
public final class WarpGateResearch {
    public static void register() {
        String gate="thaumictinkerer:warp_gate", pearl="thaumictinkerer:sky_pearl";
        String page0="page.thaumictinkerer.kami.warp_gate.0", page1="page.thaumictinkerer.kami.warp_gate.1";
        KamiResearch.register(new ResearchDefinition(gate,TinkererResearch.CATEGORY,
                List.of("thaumictinkerer:ichorcloth_chest_gem"),Map.of("iter",2,"alienis",1,"volatus",1,"machina",1),
                5,false,true,19,6,List.of(page0,page1),List.of(gate,pearl),gate,
                false,false,false,false,List.of(),List.of(page0,gate,page1,pearl),false,0,
                List.of(),List.of(),List.of(),List.of("thaumictinkerer:ichorcloth_boots_gem")),ContentRules::mirrorsEnabled);
        String recall="thaumictinkerer:focus_recall", page="page.thaumictinkerer.kami.focus_recall.0";
        KamiResearch.register(new ResearchDefinition(recall,TinkererResearch.CATEGORY,
                List.of(gate),Map.of("iter",2,"alienis",1,"volatus",1,"praecantatio",1),
                5,false,true,20,8,List.of(page),List.of(recall),recall,
                false,false,false,false,List.of(),List.of(page,recall),false,0,
                List.of(),List.of(),List.of(),List.of("thaumictinkerer:rod_ichorcloth")),ContentRules::mirrorsEnabled);
    }
    private WarpGateResearch(){}
}
