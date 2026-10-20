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
import net.minecraft.world.item.ItemStack;
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

    private ItemStack lastStack=ItemStack.EMPTY;
    private List<ResourceLocation> lastEnchants=List.of();
    private List<Integer> lastLevels=List.of();
    private boolean lastWorking=false;
    private Set<String> lastResearch=Set.of();
    private net.minecraft.core.RegistryAccess lastRegistries;

    public EnchanterScreen(EnchanterMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        this.imageWidth=EnchanterVisualTransform.GUI_WIDTH+58;
        this.imageHeight=EnchanterVisualTransform.GUI_HEIGHT;
    }

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event){
        event.register(EnchanterContent.MENU.get(),EnchanterScreen::new);
    }

    @Override
    protected void init(){
        super.init();
        rebuildButtons();
    }

    @Override
    protected void containerTick(){
        super.containerTick();
        if(minecraft==null||minecraft.level==null||minecraft.player==null)return;
        var stack=menu.getSlot(0).getItem();
        if(!ItemStack.matches(lastStack,stack)
                ||!lastEnchants.equals(menu.tile.enchantments())
                ||!lastLevels.equals(menu.tile.levels())
                ||lastWorking!=menu.tile.working
                ||!lastResearch.equals(thaumcraft.api.research.ResearchApi.clientKnowledge().completedResearch())
                ||lastRegistries!=minecraft.level.registryAccess()){
            rebuildButtons();
        }
    }

    private void send(int button){
        if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,button);
    }

    private void rebuildButtons(){
        clearWidgets();
        var stack=menu.getSlot(0).getItem();
        lastStack=stack.copy();
        lastEnchants=List.copyOf(menu.tile.enchantments());
        lastLevels=List.copyOf(menu.tile.levels());
        lastWorking=menu.tile.working;
        lastResearch=Set.copyOf(thaumcraft.api.research.ResearchApi.clientKnowledge().completedResearch());
        lastRegistries=minecraft.level.registryAccess();

        var found=new ArrayList<EnchanterCatalog.Entry>();
        if(!stack.isEmpty()&&!stack.isEnchanted()&&stack.getItem().getEnchantmentValue()!=0){
            for(var entry:EnchanterCatalog.ENTRIES){
                if(entry.unlocked(minecraft.player)&&entry.canApply(stack,minecraft.level.registryAccess(),menu.tile.enchantments())){
                    found.add(entry);
                    if(found.size()==EnchanterVisualTransform.CANDIDATE_MAX)break;
                }
            }
        }
        candidates=List.copyOf(found);

        var start=new Control(
            leftPos+EnchanterVisualTransform.START_X,
            topPos+EnchanterVisualTransform.START_Y,
            EnchanterVisualTransform.START_SIZE,
            EnchanterVisualTransform.START_SIZE,
            Component.translatable("ttmisc.startEnchant"),
            0,0,null,false
        );
        start.active=!menu.tile.enchantments().isEmpty();
        addRenderableWidget(start);

        for(int i=0;i<candidates.size();i++){
            var entry=candidates.get(i);
            int z=(i<8&&candidates.size()>8)?-EnchanterVisualTransform.CANDIDATE_STEP_Y:0;
            addRenderableWidget(new Control(
                leftPos+EnchanterVisualTransform.CANDIDATE_BASE_X+(i%8)*EnchanterVisualTransform.CANDIDATE_STEP_X,
                topPos+EnchanterVisualTransform.CANDIDATE_Y_SINGLE_ROW+z,
                EnchanterVisualTransform.CANDIDATE_BUTTON_SIZE,
                EnchanterVisualTransform.CANDIDATE_BUTTON_SIZE,
                entry.holder(minecraft.level.registryAccess()).value().description(),
                EnchanterMenu.button(entry,0),
                1,entry,false
            ));
        }

        for(int i=0;i<menu.tile.enchantments().size();i++){
            var entry=EnchanterCatalog.find(menu.tile.enchantments().get(i));
            if(entry==null)continue;
            int level=menu.tile.levels().get(i);
            int y=topPos+i*EnchanterVisualTransform.FRAMED_ROW_HEIGHT;

            addRenderableWidget(new Control(
                leftPos+EnchanterVisualTransform.GUI_WIDTH+4,
                y,
                EnchanterVisualTransform.CANDIDATE_BUTTON_SIZE,
                EnchanterVisualTransform.CANDIDATE_BUTTON_SIZE,
                entry.holder(minecraft.level.registryAccess()).value().description(),
                EnchanterMenu.button(entry,-1),
                1,entry,true
            ));
            addRenderableWidget(new Control(
                leftPos+EnchanterVisualTransform.GUI_WIDTH+EnchanterVisualTransform.LEVEL_MINUS_OFFSET_X,
                y+EnchanterVisualTransform.LEVEL_BUTTON_OFFSET_Y,
                EnchanterVisualTransform.LEVEL_BUTTON_SIZE,
                EnchanterVisualTransform.LEVEL_BUTTON_SIZE,
                Component.literal("−"),
                EnchanterMenu.button(entry,level==1?-1:level-1),
                2,null,false
            ));
            addRenderableWidget(new Control(
                leftPos+EnchanterVisualTransform.GUI_WIDTH+EnchanterVisualTransform.LEVEL_PLUS_OFFSET_X,
                y+EnchanterVisualTransform.LEVEL_BUTTON_OFFSET_Y,
                EnchanterVisualTransform.LEVEL_BUTTON_SIZE,
                EnchanterVisualTransform.LEVEL_BUTTON_SIZE,
                Component.literal("+"),
                EnchanterMenu.button(entry,level+1),
                3,null,false
            ));
        }
    }

    private final class Control extends AbstractButton {
        private final int button,kind;
        private final EnchanterCatalog.Entry entry;
        private final boolean framed;

        Control(int x,int y,int width,int height,Component label,int button,int kind,EnchanterCatalog.Entry entry,boolean framed){
            super(x,y,width,height,label);
            this.button=button;
            this.kind=kind;
            this.entry=entry;
            this.framed=framed;
        }

        @Override
        public void onPress(){
            send(button);
        }

        @Override
        protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
            if(!active)return;
            if(kind==0){
                GuiTextures.blit(g,TEXTURE,getX(),getY(),EnchanterVisualTransform.START_U,menu.tile.working?EnchanterVisualTransform.START_V_WORKING:EnchanterVisualTransform.START_V_IDLE,EnchanterVisualTransform.START_SIZE,EnchanterVisualTransform.START_SIZE);
            } else if(kind==2||kind==3){
                GuiTextures.blit(g,TEXTURE,getX(),getY(),kind==3?EnchanterVisualTransform.LEVEL_PLUS_U:EnchanterVisualTransform.LEVEL_MINUS_U,EnchanterVisualTransform.LEVEL_BUTTON_V,EnchanterVisualTransform.LEVEL_BUTTON_SIZE,EnchanterVisualTransform.LEVEL_BUTTON_SIZE);
            } else {
                if(framed){
                    GuiTextures.blit(g,TEXTURE,getX()-4,getY()-4,EnchanterVisualTransform.FRAMED_UV_X,EnchanterVisualTransform.FRAMED_UV_Y,EnchanterVisualTransform.FRAMED_UV_SIZE,EnchanterVisualTransform.FRAMED_UV_SIZE);
                    int index=menu.tile.enchantments().indexOf(ResourceLocation.parse(entry.id()));
                    if(index>=0){
                        g.drawString(font,Component.translatable("enchantment.level."+menu.tile.levels().get(index)),getX()+EnchanterVisualTransform.FRAMED_TEXT_OFFSET_X,getY()+EnchanterVisualTransform.FRAMED_TEXT_OFFSET_Y,0xffffff,true);
                    }
                }
                GuiTextures.blit(g,ResourceLocation.parse(entry.texture()),getX(),getY(),0,0,16,16,16,16);
            }

            if(!isHovered())return;
            if(kind==0&&!menu.tile.working){
                tooltip=List.of(getMessage().copy().withStyle(ChatFormatting.AQUA));
            } else if(entry!=null){
                var lines=new ArrayList<Component>();
                lines.add(getMessage().copy().withStyle(ChatFormatting.AQUA));
                var cost=entry.cost(1);
                for(var aspect:cost.getAspectsSorted()){
                    lines.add(Component.literal(" ").append(aspectName(aspect)).append(Component.literal(" x "+cost.getAmount(aspect)+" ")).append(Component.translatable("ttmisc.baseCost")));
                }
                if(framed&&!menu.tile.working){
                    lines.add(Component.translatable("ttmisc.clickToRemove").withStyle(ChatFormatting.GRAY,ChatFormatting.ITALIC));
                }
                tooltip=lines;
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output){
            defaultButtonNarrationText(output);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g,int x,int y){}

    @Override
    protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        GuiTextures.blit(g,TEXTURE,leftPos,topPos,0,0,EnchanterVisualTransform.GUI_WIDTH,imageHeight);
        var tool=menu.getSlot(0).getItem();
        if(!tool.isEmpty()&&!tool.isEnchanted()){
            if(candidates.size()>8){
                GuiTextures.blit(g,TEXTURE,leftPos+EnchanterVisualTransform.PLATE_X,topPos+EnchanterVisualTransform.PLATE_UPPER_Y,EnchanterVisualTransform.PLATE_U,imageHeight,EnchanterVisualTransform.PLATE_WIDTH,EnchanterVisualTransform.PLATE_HEIGHT);
            }
            GuiTextures.blit(g,TEXTURE,leftPos+EnchanterVisualTransform.PLATE_X,topPos+EnchanterVisualTransform.PLATE_LOWER_Y,EnchanterVisualTransform.PLATE_U,imageHeight,EnchanterVisualTransform.PLATE_WIDTH,EnchanterVisualTransform.PLATE_HEIGHT);
        }
        if(!menu.tile.enchantments().isEmpty()){
            int x=leftPos+EnchanterVisualTransform.BAR_BASE_X;
            int y=topPos+(candidates.size()>8?EnchanterVisualTransform.BAR_Y_TWO_ROWS:EnchanterVisualTransform.BAR_Y_SINGLE_ROW);
            for(var aspect:EnchanterBlockEntity.PRIMALS){
                bar(g,aspect,x,y,mx,my);
                x+=EnchanterVisualTransform.BAR_STEP_X;
            }
        }
    }

    private void bar(GuiGraphics g,Aspect aspect,int x,int y,int mx,int my){
        int total=menu.tile.totalAspects.getAmount(aspect);
        int current=menu.tile.currentAspects.getAmount(aspect);
        int size=total==0?EnchanterVisualTransform.BAR_EMPTY_SIZE:EnchanterVisualTransform.BAR_ACTIVE_SIZE;
        if(total==0){
            GuiTextures.blit(g,TEXTURE,x,y-size,EnchanterVisualTransform.BAR_FRAME_U,EnchanterVisualTransform.BAR_FRAME_V,EnchanterVisualTransform.BAR_WIDTH,4);
            GuiTextures.blit(g,TEXTURE,x,y-size+4,EnchanterVisualTransform.BAR_FRAME_U,EnchanterVisualTransform.BAR_CAP_BOTTOM_V,EnchanterVisualTransform.BAR_WIDTH,10);
        } else {
            int pixels=EnchanterVisualTransform.aspectBarFillPixels(current,total);
            GuiTextures.tinted(g,TEXTURE,x+EnchanterVisualTransform.BAR_FILL_OFFSET_X,y-size+4+EnchanterVisualTransform.BAR_FILL_MAX_HEIGHT-pixels,EnchanterVisualTransform.BAR_FILL_WIDTH,pixels,EnchanterVisualTransform.BAR_FILL_U,EnchanterVisualTransform.BAR_FILL_MAX_HEIGHT-pixels,EnchanterVisualTransform.BAR_FILL_WIDTH,pixels,EnchanterVisualTransform.TEXTURE_WIDTH,EnchanterVisualTransform.TEXTURE_HEIGHT,aspect.getColor());
            GuiTextures.blit(g,TEXTURE,x,y-size,EnchanterVisualTransform.BAR_FRAME_U,EnchanterVisualTransform.BAR_FRAME_V,EnchanterVisualTransform.BAR_WIDTH,size);
        }
        if(mx>x&&mx<=x+EnchanterVisualTransform.BAR_WIDTH&&my>y-size&&my<=y){
            tooltip=List.of(aspectName(aspect),Component.literal(current+"/"+total));
        }
    }

    private static Component aspectName(Aspect aspect){
        String code=aspect.getChatColor();
        var color=code==null||code.isEmpty()?null:ChatFormatting.getByCode(code.charAt(0));
        var name=aspect.translationKey()==null?Component.literal(aspect.getName()):Component.translatable(aspect.translationKey());
        return name.withStyle(color==null?ChatFormatting.WHITE:color);
    }

    @Override
    public void render(GuiGraphics g,int x,int y,float partial){
        tooltip=List.of();
        super.render(g,x,y,partial);
        renderTooltip(g,x,y);
        if(!tooltip.isEmpty()){
            g.renderComponentTooltip(font,tooltip,x,y);
        }
    }
}
