package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import thaumcraft.api.client.WandHudSettings;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.DislocationFocusItem;

/** Original HOTBAR-post preview of the captured block and its extra contents. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT)
public final class DislocationHud {
    private static CustomData lastData;
    private static RegistryAccess lastRegistries;
    private static ItemStack icon=ItemStack.EMPTY;
    private static boolean selected,extra;

    @SubscribeEvent public static void render(RenderGuiLayerEvent.Post event) {
        if(!event.getName().equals(VanillaGuiLayers.HOTBAR))return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui){clear();return;}
        var wand=mc.player.getMainHandItem();
        if(WandApi.kind(wand)==WandApi.Kind.NONE){clear();return;}
        var focus=WandApi.focus(wand);
        if(!(focus.getItem() instanceof DislocationFocusItem item)){clear();return;}
        var data=focus.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY);
        var registries=mc.level.registryAccess();
        if(lastData!=data||lastRegistries!=registries) {
            lastData=data;lastRegistries=registries;
            var state=item.captured(focus,registries);selected=state.isPresent();
            icon=state.map(value->new ItemStack(value.getBlock())).orElse(ItemStack.EMPTY);
            extra=selected&&!data.copyTag().getCompound("tileCmp").isEmpty();
        }
        if(!selected)return;
        var graphics=event.getGuiGraphics();int x=4,y=WandHudSettings.dialBottom()?graphics.guiHeight()-70:50;
        var title=Component.translatable("ttmisc.focusDislocation.tooltip");int width=mc.font.width(title);
        graphics.fill(x-1,y-1,x+18,y+18,0x66000000);
        graphics.fill(x+18,y,x+18+width+4,y+9,0x66000000);
        graphics.drawString(mc.font,title,x+20,y,0xffffaa00,true);
        if(extra) {
            // Source deliberately sizes both backgrounds using the title width.
            graphics.fill(x+18,y+9,x+18+width+4,y+18,0x66000000);
            graphics.drawString(mc.font,Component.translatable("ttmisc.focusDislocation.tooltipExtra"),x+20,y+9,0xffffaa00,true);
        }
        if(!icon.isEmpty())graphics.renderItem(icon,x,y);
    }
    private static void clear(){lastData=null;lastRegistries=null;icon=ItemStack.EMPTY;selected=extra=false;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    private DislocationHud(){}
}
