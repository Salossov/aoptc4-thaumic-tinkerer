package thaumic.tinkerer.kami;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

public final class KamiEnabledCondition implements ICondition {
    public static final KamiEnabledCondition INSTANCE=new KamiEnabledCondition();
    public static final MapCodec<KamiEnabledCondition> CODEC=MapCodec.unit(INSTANCE);
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,ThaumicTinkerer.ID);
    public static void register(IEventBus bus) {CONDITIONS.register("kami_enabled",()->CODEC);CONDITIONS.register(bus);}
    @Override public boolean test(IContext context) {return TinkererConfig.KAMI_FORCE_ENABLED.get();}
    @Override public MapCodec<? extends ICondition> codec() {return CODEC;}
    private KamiEnabledCondition() {}
}
