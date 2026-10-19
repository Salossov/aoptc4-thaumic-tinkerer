package thaumic.tinkerer.item;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumic.tinkerer.api.LegacyArmor;

public final class RevealingHelmItem extends ArmorItem implements IRepairable, IRevealer, IGoggles, IVisDiscountGear, LegacyArmor {
    public RevealingHelmItem() {
        super(BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ResourceKey.create(Registries.ARMOR_MATERIAL,
                ResourceLocation.parse("thaumcraft:thaumium"))), Type.HELMET, new Properties().durability(500).rarity(Rarity.UNCOMMON));
    }
    @Override public boolean showNodes(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return 5; }
    @Override public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.parse("thaumictinkerer:textures/model/revealing.png");
    }
    @Override public boolean isValidRepairItem(ItemStack damaged, ItemStack ingredient) {
        return ingredient.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot"))) || super.isValidRepairItem(damaged, ingredient);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tc.visdiscount").append(": 5%"));
    }
}
