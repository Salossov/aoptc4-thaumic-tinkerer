package thaumic.tinkerer.fire;

import java.util.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.*;
import thaumic.tinkerer.research.TinkererResearch;

public final class ImbuedFireContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    public static final DeferredBlock<LegacyDryShrubBlock> DRY_SHRUB=BLOCKS.register("legacy_dry_shrub",LegacyDryShrubBlock::new);
    public static final Map<ImbuedFireKind,DeferredBlock<ImbuedFireBlock>> BLOCKS_BY_KIND=new EnumMap<>(ImbuedFireKind.class);
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,ThaumicTinkerer.ID);
    static{ThaumicTinkerer.ITEMS.register("legacy_dry_shrub",()->new BlockItem(DRY_SHRUB.get(),new Item.Properties()));for(var kind:ImbuedFireKind.values()){
        var block=BLOCKS.register(kind.path(),()->new ImbuedFireBlock(kind));BLOCKS_BY_KIND.put(kind,block);ThaumicTinkerer.ITEMS.register(kind.path(),()->new BlockItem(block.get(),new Item.Properties()));
    }CONDITIONS.register("imbued_fire_enabled",()->Enabled.CODEC);}
    public static void register(IEventBus bus){BLOCKS.register(bus);CONDITIONS.register(bus);}
    public static void display(CreativeModeTab.Output output){BLOCKS_BY_KIND.values().forEach(block->output.accept(block.get()));}
    public static void research(){if(TinkererConfig.IMBUED_FIRE.get())for(var kind:ImbuedFireKind.values())AddonRegistry.research(definition(kind));}
    public static ResearchDefinition definition(ImbuedFireKind kind){
        var id="thaumictinkerer:"+kind.path();var page="page.thaumictinkerer."+kind.path()+".0";
        var aspects=kind==ImbuedFireKind.IGNIS?Map.of("ignis",10):Map.of("ignis",5,kind.tag,5);
        return new ResearchDefinition(id,TinkererResearch.CATEGORY,List.of("thaumictinkerer:bright_nitor"),aspects,kind.complexity,false,true,kind.x,kind.y,List.of(page),List.of(id),id,false,false,false,true,List.of(),List.of(page,id),false,0,List.of(),List.of(),List.of(),List.of());
    }
    public record Enabled() implements ICondition{
        public static final MapCodec<Enabled> CODEC=MapCodec.unit(new Enabled());
        @Override public boolean test(IContext context){return TinkererConfig.IMBUED_FIRE.get();}
        @Override public MapCodec<? extends ICondition> codec(){return CODEC;}
    }
    private ImbuedFireContent(){}
}
