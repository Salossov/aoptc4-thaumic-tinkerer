package thaumic.tinkerer.crop;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumic.tinkerer.util.ItemData;

/** Approved preservation of the source bestia loot: IDs 0..23 never spawn a mob. */
public final class LegacySpawnEggItem extends Item {
    public LegacySpawnEggItem(){super(new Properties());}
    public static ItemStack of(int legacyId){var stack=new ItemStack(InfusedContent.LEGACY_EGG.get());ItemData.update(stack,tag->tag.putInt("LegacyEntityId",legacyId));return stack;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.thaumictinkerer.legacy_spawn_egg",ItemData.integer(stack,"LegacyEntityId")));}
}
