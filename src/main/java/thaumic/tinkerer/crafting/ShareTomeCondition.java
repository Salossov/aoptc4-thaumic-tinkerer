package thaumic.tinkerer.crafting;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

public final class ShareTomeCondition implements ICondition {
    public static final ShareTomeCondition INSTANCE=new ShareTomeCondition();
    public static final MapCodec<ShareTomeCondition> CODEC=MapCodec.unit(INSTANCE);
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,ThaumicTinkerer.ID);
    public static void register(IEventBus bus){CONDITIONS.register("survival_share_tome_enabled",()->CODEC);CONDITIONS.register(bus);}
    @Override public boolean test(IContext context){return TinkererConfig.SURVIVAL_SHARE_TOME.get();}
    @Override public MapCodec<? extends ICondition> codec(){return CODEC;}
    private ShareTomeCondition(){}
}
