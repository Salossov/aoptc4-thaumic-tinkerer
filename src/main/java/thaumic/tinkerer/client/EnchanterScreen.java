package thaumic.tinkerer.client;

import java.util.*;
import net.minecraft.ChatFormatting;
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
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.enchantment.EnchanterCatalog;
import thaumic.tinkerer.menu.EnchanterMenu;

/** The original 176x166 texture, sixteen candidates, right-hand selections and six vis bars. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EnchanterScreen extends AbstractContainerScreen<EnchanterMenu> {
    private static final ResourceLocation TEXTURE=ThaumicTinkerer.id("textures/gui/enchanter.png");
    private List<EnchanterCatalog.Entry> candidates=List.of();
    private List<Component> tooltip=List.of();
    public EnchanterScreen(EnchanterMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @SubscribeEvent public static void register(RegisterMenuScreensEvent event){event.register(EnchanterContent.MENU.get(),EnchanterScreen::new);}
    @Override protected void init(){super.init();buttons();}
    @Override protected void containerTick(){super.containerTick();buttons();}
    private void send(int button){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);}
    private void buttons(){
        clearWidgets();var stack=menu.getSlot(0).getItem();var found=new ArrayList<EnchanterCatalog.Entry>();
        if(!stack.isEmpty()&&!stack.isEnchanted()&&stack.getItem().getEnchantmentValue()!=0)for(var entry:EnchanterCatalog.ENTRIES){if(entry.unlocked(minecraft.player)&&entry.canApply(stack,minecraft.level.registryAccess(),menu.tile.enchantments())){found.add(entry);if(found.size()==16)break;}}
        candidates=List.copyOf(found);var start=new Control(leftPos+151,topPos+33,15,15,Component.translatable("ttmisc.startEnchant"),0,0,null,false);start.active=!menu.tile.enchantments().isEmpty();addRenderableWidget(start);
        for(int i=0;i<candidates.size();i++){var entry=candidates.get(i);addRenderableWidget(new Control(leftPos+34+i%8*16,topPos+54+(i<8&&candidates.size()>8?-24:0),16,16,entry.holder(minecraft.level.registryAccess()).value().description(),EnchanterMenu.button(entry,0),1,entry,false));}
        for(int i=0;i<menu.tile.enchantments().size();i++){
            var entry=EnchanterCatalog.find(menu.tile.enchantments().get(i));if(entry==null)continue;int level=menu.tile.levels().get(i),y=topPos+i*26;
            addRenderableWidget(new Control(leftPos+imageWidth+4,y,16,16,entry.holder(minecraft.level.registryAccess()).value().description(),EnchanterMenu.button(entry,-1),1,entry,true));
            addRenderableWidget(new Control(leftPos+imageWidth+24,y-4,7,7,Component.literal("−"),EnchanterMenu.button(entry,level==1?-1:level-1),2,null,false));
            addRenderableWidget(new Control(leftPos+imageWidth+31,y-4,7,7,Component.literal("+"),EnchanterMenu.button(entry,level+1),3,null,false));
        }
    }
    private final class Control extends AbstractButton {
        private final int button,kind;private final EnchanterCatalog.Entry entry;private final boolean framed;
        Control(int x,int y,int width,int height,Component label,int button,int kind,EnchanterCatalog.Entry entry,boolean framed){super(x,y,width,height,label);this.button=button;this.kind=kind;this.entry=entry;this.framed=framed;}
        @Override public void onPress(){send(button);}
        @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            if(!active)return;
            if(kind==0)GuiTextures.blit(g,TEXTURE,getX(),getY(),176,menu.tile.working?39:24,15,15);
            else if(kind==2||kind==3)GuiTextures.blit(g,TEXTURE,getX(),getY(),kind==3?225:218,0,7,7);
            else{
                if(framed){GuiTextures.blit(g,TEXTURE,getX()-4,getY()-4,176,0,24,24);int index=menu.tile.enchantments().indexOf(ResourceLocation.parse(entry.id()));if(index>=0)g.drawString(font,Component.translatable("enchantment.level."+menu.tile.levels().get(index)),getX()+26,getY()+8,0xffffff,true);}
                GuiTextures.blit(g,ResourceLocation.parse(entry.texture()),getX(),getY(),0,0,16,16,16,16);
            }
            if(!isHovered())return;
            if(kind==0&&!menu.tile.working)tooltip=List.of(getMessage().copy().withStyle(ChatFormatting.AQUA));
            else if(entry!=null){var lines=new ArrayList<Component>();lines.add(getMessage().copy().withStyle(ChatFormatting.AQUA));var cost=entry.cost(1);
                for(var aspect:cost.getAspectsSorted())lines.add(Component.literal(" ").append(aspectName(aspect)).append(Component.literal(" x "+cost.getAmount(aspect)+" ")).append(Component.translatable("ttmisc.baseCost")));
                if(framed&&!menu.tile.working)lines.add(Component.translatable("ttmisc.clickToRemove").withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));tooltip=lines;
            }
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        GuiTextures.blit(g,TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);var tool=menu.getSlot(0).getItem();
        if(!tool.isEmpty()&&!tool.isEnchanted()){if(candidates.size()>8)GuiTextures.blit(g,TEXTURE,leftPos+30,topPos+26,0,imageHeight,147,24);GuiTextures.blit(g,TEXTURE,leftPos+30,topPos+50,0,imageHeight,147,24);}
        if(!menu.tile.enchantments().isEmpty()){int x=leftPos+55,y=topPos+(candidates.size()>8?26:50);for(var aspect:EnchanterBlockEntity.PRIMALS){bar(g,aspect,x,y,mx,my);x+=15;}}
    }
    private void bar(GuiGraphics g,Aspect aspect,int x,int y,int mx,int my){
        int total=menu.tile.totalAspects.getAmount(aspect),current=menu.tile.currentAspects.getAmount(aspect),size=total==0?11:59;
        if(total==0){GuiTextures.blit(g,TEXTURE,x,y-size,200,0,10,4);GuiTextures.blit(g,TEXTURE,x,y-size+4,200,52,10,10);}
        else{int pixels=(int)(48.0*((double)current/total));GuiTextures.tinted(g,TEXTURE,x+1,y-size+4+48-pixels,8,pixels,210,48-pixels,8,pixels,256,256,aspect.getColor());GuiTextures.blit(g,TEXTURE,x,y-size,200,0,10,size);}
        if(mx>x&&mx<=x+10&&my>y-size&&my<=y)tooltip=List.of(aspectName(aspect),Component.literal(current+"/"+total));
    }
    private static Component aspectName(Aspect aspect){
        String code=aspect.getChatColor();var color=code==null||code.isEmpty()?null:ChatFormatting.getByCode(code.charAt(0));
        var name=aspect.translationKey()==null?Component.literal(aspect.getName()):Component.translatable(aspect.translationKey());
        return name.withStyle(color==null?ChatFormatting.WHITE:color);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial){tooltip=List.of();super.render(g,x,y,partial);renderTooltip(g,x,y);if(!tooltip.isEmpty())g.renderComponentTooltip(font,tooltip,x,y);}
}
