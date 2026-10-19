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
    private static final ResourceLocation TEXTURE=TabletVisualTransform.GUI_TEXTURE;
    public AnimationTabletScreen(AnimationTabletMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event){event.register(AnimationTabletContent.MENU.get(),AnimationTabletScreen::new);}
    @Override protected void init(){
        super.init();
        addRenderableWidget(new Radio(0,leftPos+TabletVisualTransform.BTN_REDSTONE_REL_X,topPos+TabletVisualTransform.BTN_REDSTONE_REL_Y));
        addRenderableWidget(new Radio(1,leftPos+TabletVisualTransform.BTN_LEFT_CLICK_REL_X,topPos+TabletVisualTransform.BTN_LEFT_CLICK_REL_Y));
        addRenderableWidget(new Radio(2,leftPos+TabletVisualTransform.BTN_RIGHT_CLICK_REL_X,topPos+TabletVisualTransform.BTN_RIGHT_CLICK_REL_Y));
    }
    private final class Radio extends AbstractButton {
        private final int button;
        Radio(int button,int x,int y){
            super(x,y,TabletVisualTransform.BTN_SIZE,TabletVisualTransform.BTN_SIZE,Component.translatable(button==0?"ttmisc.redstoneControl":button==1?"ttmisc.leftClick":"ttmisc.rightClick"));
            this.button=button;
        }
        @Override public void onPress(){
            if(minecraft.player!=null)menu.clickMenuButton(minecraft.player,button);
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);
        }
        @Override protected void renderWidget(GuiGraphics g,int x,int y,float partial){
            boolean selected=button==0?menu.redstone():button==1?menu.leftClick():!menu.leftClick();
            GuiTextures.blit(g,TEXTURE,getX(),getY(),TabletVisualTransform.BTN_TEX_U,selected?TabletVisualTransform.BTN_TEX_V_ON:TabletVisualTransform.BTN_TEX_V_OFF,TabletVisualTransform.BTN_SIZE,TabletVisualTransform.BTN_SIZE);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){
        GuiTextures.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        var left=Component.translatable("ttmisc.leftClick");
        var right=Component.translatable("ttmisc.rightClick");
        var redstone=Component.translatable("ttmisc.redstoneControl");
        g.drawString(font,left,leftPos+TabletVisualTransform.LABEL_LEFT_ANCHOR_X-font.width(left),topPos+TabletVisualTransform.LABEL_LEFT_Y,TabletVisualTransform.LABEL_COLOR,false);
        g.drawString(font,right,leftPos+TabletVisualTransform.LABEL_RIGHT_X,topPos+TabletVisualTransform.LABEL_RIGHT_Y,TabletVisualTransform.LABEL_COLOR,false);
        g.drawString(font,redstone,leftPos+imageWidth/2-font.width(redstone)/2,topPos+TabletVisualTransform.LABEL_REDSTONE_Y,TabletVisualTransform.LABEL_COLOR,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
