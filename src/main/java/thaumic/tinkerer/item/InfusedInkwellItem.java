package thaumic.tinkerer.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IScribeTools;

/** The registered but creative-hidden 800-use ink container from release 164. */
public final class InfusedInkwellItem extends Item implements IScribeTools {
    public InfusedInkwellItem() { super(new Properties().durability(800)); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
}
