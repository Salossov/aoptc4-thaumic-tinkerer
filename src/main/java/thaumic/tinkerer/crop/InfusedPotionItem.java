package thaumic.tinkerer.crop;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Fixed release effect, independent of arbitrary vanilla PotionContents. */
public final class InfusedPotionItem extends Item {
    private final int variant;
    public InfusedPotionItem(int variant){super(new Properties().stacksTo(1));this.variant=variant;}
    public int variant(){return variant;}
    @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return ItemUtils.startUsingInstantly(level,player,hand);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(entity instanceof ServerPlayer player)CriteriaTriggers.CONSUME_ITEM.trigger(player,stack);
        if(entity instanceof Player player&&!player.getAbilities().instabuild)stack.shrink(1);
        if(!level.isClientSide())entity.addEffect(new MobEffectInstance(InfusedContent.IMBUED.get(variant),3600));
        if(entity instanceof Player player){
            player.awardStat(Stats.ITEM_USED.get(this));
            if(!player.getAbilities().instabuild){if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));}
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.literal(InfusedContent.FRUIT_ASPECTS.get(variant).getName()));}
}
