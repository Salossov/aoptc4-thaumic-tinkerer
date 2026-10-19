package thaumic.tinkerer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.AnimationTabletContent;
import thaumic.tinkerer.menu.AnimationTabletMenu;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AnimationTabletScreen extends AbstractContainerScreen<AnimationTabletMenu> {
    private static final ResourceLocation TEXTURE=ThaumicTinkerer.id("textures/gui/animation_tablet.png");
    public AnimationTabletScreen(AnimationTabletMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event){event.register(AnimationTabletContent.MENU.get(),AnimationTabletScreen::new);}
    @Override protected void init(){super.init();addRenderableWidget(new Radio(0,leftPos+imageWidth/2-7,topPos+60));addRenderableWidget(new Radio(1,leftPos+52,topPos+15));addRenderableWidget(new Radio(2,leftPos+111,topPos+15));}
    private final class Radio extends AbstractButton {
        private final int button;
        Radio(int button,int x,int y){super(x,y,13,13,Component.translatable(button==0?"ttmisc.redstoneControl":button==1?"ttmisc.leftClick":"ttmisc.rightClick"));this.button=button;}
        @Override public void onPress(){menu.clickMenuButton(minecraft.player,button);if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);}
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){boolean selected=button==0?menu.redstone():button==1?menu.leftClick():!menu.leftClick();GuiTextures.blit(g,TEXTURE,getX(),getY(),176,selected?13:0,13,13);}
        @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        GuiTextures.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);var left=Component.translatable("ttmisc.leftClick");var right=Component.translatable("ttmisc.rightClick");var redstone=Component.translatable("ttmisc.redstoneControl");
        g.drawString(font,left,leftPos+48-font.width(left),topPos+18,0x999999,false);g.drawString(font,right,leftPos+128,topPos+18,0x999999,false);g.drawString(font,redstone,leftPos+imageWidth/2-font.width(redstone)/2,topPos+50,0x999999,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
