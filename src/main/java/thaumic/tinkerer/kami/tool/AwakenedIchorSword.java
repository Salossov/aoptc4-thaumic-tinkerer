package thaumic.tinkerer.kami.tool;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumic.tinkerer.kami.SoulHearts;

public final class AwakenedIchorSword extends IchorTools.Sword {
    private boolean ignoreLeftClick;
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown())return super.use(level,player,hand);
        AdvancedToolMode.change(player,stack,"sword");return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity entity) {
        if(!player.level().isClientSide()&&!ignoreLeftClick&&entity instanceof LivingEntity living&&living.hurtTime==0&&!entity.isRemoved()) {
            switch(AdvancedToolMode.get(stack)) {
                case 1 -> {
                    var box=new AABB(entity.getX()-3,entity.getY()-3,entity.getZ()-3,entity.getX()+3,entity.getY()+3,entity.getZ()+3);
                    var targets=player.level().getEntitiesOfClass(entity.getClass(),box);
                    ignoreLeftClick=true;
                    try {for(var target:targets)player.attack(target);}finally{ignoreLeftClick=false;}
                }
                case 2 -> {
                    living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1,1));
                    SoulHearts.addHeart(player);
                }
            }
        }
        return super.onLeftClickEntity(stack,player,entity);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable(AdvancedToolMode.key("sword",AdvancedToolMode.get(stack))));
    }
}
