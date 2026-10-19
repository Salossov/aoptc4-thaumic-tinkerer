package thaumic.tinkerer.kami;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import thaumcraft.api.ISpecialArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.SpecialArmorProperties;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.ThaumicTinkerer;

public class IchorclothArmorItem extends ArmorItem implements IVisDiscountGear,ISpecialArmor {
    private static final int[] SOURCE_POINTS={3,8,6,3};
    public IchorclothArmorItem(Type type) {super(KamiArmorContent.MATERIAL,type,new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public boolean isEnchantable(ItemStack stack) {return true;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var held=player.getItemInHand(hand);var slot=type.getSlot();
        if(!player.getItemBySlot(slot).isEmpty())return InteractionResultHolder.pass(held);
        // Old ItemArmor equips only into an empty slot and consumes even a creative-held stack.
        if(!level.isClientSide()) {player.setItemSlot(slot,held.copy());held.setCount(0);}
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide());
    }
    @Override public SpecialArmorProperties getProperties(LivingEntity wearer,ItemStack stack,DamageSource source,double damage,int slot) {
        // Source quirk: the inventory slot indexes a head/chest/legs/feet material array.
        // Chest therefore absorbs 6 * .0425, while leggings absorb 8 * .0425.
        return new SpecialArmorProperties(0,SOURCE_POINTS[slot]*.0425,Integer.MAX_VALUE);
    }
    @Override public int getArmorDisplay(Player player,ItemStack stack,int slot) {return SOURCE_POINTS[slot];}
    @Override public void damageArmor(LivingEntity wearer,ItemStack stack,DamageSource source,int damage,int slot) {}
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect) {return type==Type.BOOTS?3:4;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("tc.visdiscount").append(": "+getVisDiscount(stack,null,null)+"%"));
    }
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner) {
        return ThaumicTinkerer.id("textures/model/ichor"+(slot==EquipmentSlot.LEGS?"2":"1")+".png");
    }
}
