package thaumic.tinkerer.crop;

import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import thaumic.tinkerer.TinkererConfig;

/** The source cancels the bone meal event; direct IGrowable growth remains available. */
public final class InfusedBonemeal {
    public static void onBonemeal(BonemealEvent event) {
        if(event.getState().getBlock() instanceof InfusedCropBlock&&!TinkererConfig.CROPS_ALLOW_BONEMEAL.get())
            event.setCanceled(true);
    }
    private InfusedBonemeal() {}
}
