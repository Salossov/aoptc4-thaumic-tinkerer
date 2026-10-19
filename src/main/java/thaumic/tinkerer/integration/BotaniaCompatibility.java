package thaumic.tinkerer.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import vazkii.botania.api.internal.ManaBurst;

/** The release's early Botania return intentionally includes non-Botania projectiles. */
public final class BotaniaCompatibility {
    public static boolean harmless(Entity entity){
        if(!(entity instanceof ManaBurst burst))return true;
        var lens=BuiltInRegistries.ITEM.getKey(burst.getSourceLens().getItem());
        return !lens.equals(ResourceLocation.parse("botania:damaging_lens"))&&!lens.equals(ResourceLocation.parse("botania:entropic_lens"));
    }
    private BotaniaCompatibility(){}
}
