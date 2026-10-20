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
        var graphics=event.getGuiGraphics();int x=5,y=WandHudSettings.dialBottom()?graphics.guiHeight()-76:54;
        var title=Component.translatable("ttmisc.focusDislocation.tooltip");
        int maxWidth=Math.max(40,Math.min(180,graphics.guiWidth()-x-32));
        String name=mc.font.plainSubstrByWidth(icon.getHoverName().getString(),maxWidth);
        String heading=mc.font.plainSubstrByWidth(title.getString(),maxWidth);
        var extraLabel=Component.translatable("ttmisc.focusDislocation.tooltipExtra");
        int width=Math.min(maxWidth,Math.max(mc.font.width(name),Math.max(mc.font.width(heading),extra?mc.font.width(extraLabel):0)));
        int height=extra?34:24;
        graphics.fill(x,y,x+width+30,y+height,0xc0181324);
        graphics.fill(x,y,x+2,y+height,0xffb898d8);
        graphics.drawString(mc.font,heading,x+25,y+3,0xffc6addc,false);
        graphics.drawString(mc.font,name,x+25,y+13,0xffeeeeee,false);
        if(extra)graphics.drawString(mc.font,mc.font.plainSubstrByWidth(extraLabel.getString(),maxWidth),x+25,y+23,0xffbfb2cd,false);
        if(!icon.isEmpty())graphics.renderItem(icon,x+5,y+4);

    }
    private static void clear(){lastData=null;lastRegistries=null;icon=ItemStack.EMPTY;selected=extra=false;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    private DislocationHud(){}
}
