package thaumic.tinkerer.kami.bedrock;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import thaumic.tinkerer.TinkererConfig;

public final class BedrockEnabledCondition implements ICondition {
    public static final BedrockEnabledCondition INSTANCE=new BedrockEnabledCondition();
    public static final MapCodec<BedrockEnabledCondition> CODEC=MapCodec.unit(INSTANCE);
    @Override public boolean test(IContext context){return TinkererConfig.KAMI_FORCE_ENABLED.get()&&TinkererConfig.KAMI_BEDROCK_ENABLED.get();}
    @Override public MapCodec<? extends ICondition> codec(){return CODEC;}
    private BedrockEnabledCondition(){}
}
