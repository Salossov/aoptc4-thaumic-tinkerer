package thaumic.tinkerer.crop;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumcraft.api.aspects.*;

/** Release fruit is an essentia-container item, not edible food. */
public final class InfusedGrainItem extends Item implements IEssentiaContainerItem {
    private final Aspect aspect;
    public InfusedGrainItem(Aspect aspect){super(new Properties());this.aspect=aspect;}
    public Aspect aspect(){return aspect;}
    @Override public AspectList getAspects(ItemStack stack){return new AspectList().add(aspect,1).add(Aspects.CROP,1);}
    @Override public void setAspects(ItemStack stack,AspectList values){}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.literal(aspect.getName()));}
}
