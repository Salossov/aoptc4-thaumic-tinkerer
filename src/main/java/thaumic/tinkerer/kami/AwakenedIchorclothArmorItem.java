package thaumic.tinkerer.kami;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.util.ItemData;

/** Shared per-item state. The four equipment effects are handled independently. */
public class AwakenedIchorclothArmorItem extends IchorclothArmorItem {
    public AwakenedIchorclothArmorItem(Type type) {super(type);}
    public static int variant(ItemStack stack) {return ItemData.integer(stack,"variant");}
    public static void variant(ItemStack stack,int variant) {ItemData.update(stack,data->data.putInt("variant",variant));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown())return super.use(level,player,hand);
        int changed=~variant(stack)&1;
        if(level.isClientSide())NeoForge.EVENT_BUS.post(new ToolModeNotification("ttmisc.awakenedArmor"+changed));
        else {
            variant(stack,changed);
            level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.3F,.1F);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags) {
        super.appendHoverText(stack,context,lines,flags);
        if(variant(stack)==1)lines.add(Component.translatable("ttmisc.awakenedArmor1"));
    }
    @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner) {
        return ThaumicTinkerer.id("textures/model/ichor_gem"+(slot==EquipmentSlot.LEGS?"2":"1")+".png");
    }
}
