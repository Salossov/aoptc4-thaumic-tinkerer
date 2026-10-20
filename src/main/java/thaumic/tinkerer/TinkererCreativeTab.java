package thaumic.tinkerer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;
import thaumic.tinkerer.kami.KamiWandContent;

/** Release ModCreativeTab: full wands first, then items sorted by their display names. */
public final class TinkererCreativeTab {
    private record Entry(ItemStack stack,CreativeModeTab.TabVisibility visibility){}

    public static ItemStack wand(boolean kami){
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:iron_capped_wooden_wand")));
        WandApi.parts(stack,kami?new WandParts(KamiWandContent.ROD,KamiWandContent.CAP):new WandParts("silverwood","thaumium"));
        for(var aspect:Aspects.PRIMALS)WandApi.set(stack,aspect,kami?100000:10000);
        return stack;
    }

    public static ItemStack icon(){return wand(TinkererConfig.KAMI_FORCE_ENABLED.get());}

    public static void display(CreativeModeTab.Output destination,Consumer<CreativeModeTab.Output> content){
        destination.accept(wand(false));
        if(TinkererConfig.KAMI_FORCE_ENABLED.get())destination.accept(wand(true));
        var entries=new ArrayList<Entry>();
        content.accept((stack,visibility)->entries.add(new Entry(stack,visibility)));
        entries.sort(Comparator.comparing(entry->entry.stack().getHoverName().getString(),String.CASE_INSENSITIVE_ORDER));
        for(var entry:entries)destination.accept(entry.stack(),entry.visibility());
    }

    private TinkererCreativeTab(){}
}
