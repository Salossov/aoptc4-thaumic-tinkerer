package thaumic.tinkerer.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.warpgate.SkyPearlItem;

/** Dynamic distance belongs to the viewing client, never a common item class. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT)
public final class WarpGateTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        var player=event.getEntity();
        if(player!=null&&event.getItemStack().getItem() instanceof SkyPearlItem)
            SkyPearlItem.addInfo(event.getItemStack(),player.level().dimension(),player.getBoundingBox().getCenter(),event.getToolTip(),false);
    }
    private WarpGateTooltips(){}
}
