package thaumic.tinkerer.kami;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IGoggles;
import thaumcraft.api.nodes.IRevealer;

public final class AwakenedIchorclothHelmItem extends AwakenedIchorclothArmorItem implements IGoggles,IRevealer {
    public AwakenedIchorclothHelmItem() {super(Type.HELMET);}
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer) {return true;}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer) {return true;}
}
