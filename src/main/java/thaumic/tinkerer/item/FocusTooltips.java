package thaumic.tinkerer.item;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.ItemFocusBasic;

/** Release ItemModFocus and ItemModKamiFocus supply their own cost-only tooltip. */
public final class FocusTooltips {
    public static void append(ItemFocusBasic focus,ItemStack stack,List<Component> lines){
        var cost=focus.getVisCost(stack);if(cost==null)return;
        lines.add(Component.translatable(focus.isVisCostPerTick(stack)?"item.Focus.cost2":"item.Focus.cost1"));
        for(var entry:cost.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Aspect::getTag))).toList()){
            var aspect=entry.getKey();var color=ChatFormatting.getByCode(aspect.getChatColor().charAt(0));
            lines.add(Component.literal(" ")
                    .append(Component.literal(aspect.getName()).withStyle(color==null?ChatFormatting.WHITE:color))
                    .append(" x "+entry.getValue()/100f));
        }
    }
    private FocusTooltips(){}
}
