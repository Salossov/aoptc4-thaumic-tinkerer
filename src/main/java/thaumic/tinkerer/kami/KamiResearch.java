package thaumic.tinkerer.kami;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.research.ResearchCatalogRules;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.research.TinkererResearch;

/** The source KamiResearchItem rule, evaluated against the active complete catalog. */
public final class KamiResearch {
    public static final List<String> BLACKLIST=new CopyOnWriteArrayList<>(List.of("MINILITH"));
    private static final Set<String> NODES=new LinkedHashSet<>();
    private static final Set<String> CATEGORIES=Set.of(TinkererResearch.CATEGORY,"BASICS","GOLEMANCY","ARTIFICE","ALCHEMY","THAUMATURGY");
    public static void register(ResearchDefinition definition) {register(definition,()->true);}
    public static void register(ResearchDefinition definition,java.util.function.BooleanSupplier enabled) {
        NODES.add(definition.key());AddonRegistry.research(definition,enabled);
    }
    public static void catalogRule() {ResearchCatalogRules.register(ThaumicTinkerer.id("kami_requirements"),KamiResearch::requirements);}
    public static List<String> requirements(ResearchDefinition definition,Map<String,ResearchDefinition> catalog) {
        if(!NODES.contains(definition.key())||definition.autoUnlock())return List.of();
        var result=new ArrayList<String>();
        for(var candidate:catalog.values()) {
            if(candidate.lost()||candidate.virtual()||NODES.contains(candidate.key())||!CATEGORIES.contains(candidate.category()))continue;
            if(candidate.parents().isEmpty()&&candidate.hiddenParents().isEmpty())continue;
            if(!candidate.itemTriggers().isEmpty()||!candidate.entityTriggers().isEmpty()||!candidate.aspectTriggers().isEmpty())continue;
            String raw=candidate.key(),legacy=(raw.contains(":")?raw.substring(raw.indexOf(':')+1):raw).toUpperCase(Locale.ROOT);
            if(legacy.endsWith("KAMI")||BLACKLIST.stream().anyMatch(prefix->raw.startsWith(prefix)||legacy.startsWith(prefix)))continue;
            result.add(raw);
        }
        return List.copyOf(result);
    }
    private KamiResearch() {}
}
