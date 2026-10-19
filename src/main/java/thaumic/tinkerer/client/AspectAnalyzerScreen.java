package thaumic.tinkerer.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.menu.AspectAnalyzerMenu;

/** Original 176x166 screen and scanned-item display; computer queries intentionally do not require scanning. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AspectAnalyzerScreen extends AbstractContainerScreen<AspectAnalyzerMenu> {
    private static final ResourceLocation TEXTURE=ThaumicTinkerer.id("textures/gui/aspect_analyzer.png");
    private Aspect hovered;
    public AspectAnalyzerScreen(AspectAnalyzerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event){if(GolemConnectorContent.enabled())event.register(AspectAnalyzerContent.MENU.get(),AspectAnalyzerScreen::new);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY){
        hovered=null;GuiTextures.blit(graphics,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var stack=menu.getSlot(0).getItem();if(stack.isEmpty()||!AspectApi.scanned(stack))return;
        int index=0;
        for(var entry:AspectAnalyzerMenu.sortedAmounts(AspectApi.aspects(minecraft.level,stack))){
            var aspect=Aspect.getAspect(entry.getKey());if(aspect==null)continue;
            int x=leftPos+20+index++*18,y=topPos+58;drawTag(graphics,aspect,entry.getValue(),x,y);
            if(mouseX>x&&mouseX<x+16&&mouseY>y&&mouseY<y+16)hovered=aspect;
        }
    }
    private void drawTag(GuiGraphics graphics,Aspect aspect,int amount,int x,int y){
        GuiTextures.tinted(graphics,ResourceLocation.parse(aspect.texture()),x,y,16,16,0,0,32,32,32,32,aspect.getColor());
        if(amount<=0)return;
        graphics.pose().pushPose();graphics.pose().scale(.5f,.5f,.5f);String text=Integer.toString(amount);int tx=32-font.width(text)+x*2,ty=32-font.lineHeight+y*2;
        for(var shift:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})graphics.drawString(font,text,tx+shift[0],ty+shift[1],0xff000000,false);
        graphics.drawString(font,text,tx,ty,0xffffffff,false);graphics.pose().popPose();
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){
        super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);
        if(hovered!=null){
            var name=hovered.translationKey()==null?Component.literal(hovered.getName()):Component.translatable(hovered.translationKey());
            graphics.renderComponentTooltip(font,List.of(name.withStyle(ChatFormatting.AQUA),Component.translatable("tc.aspect."+hovered.getTag()).withStyle(ChatFormatting.GRAY)),x,y);
        }
    }
}
