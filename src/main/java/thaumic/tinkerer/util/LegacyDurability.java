package thaumic.tinkerer.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** The old ItemStack breaks above max damage, rather than at max damage. */
@net.neoforged.fml.common.EventBusSubscriber(modid = thaumic.tinkerer.ThaumicTinkerer.ID)
public final class LegacyDurability {
    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void armor(net.neoforged.neoforge.event.entity.living.ArmorHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        for (var entry : java.util.List.copyOf(event.getArmorMap().entrySet())) {
            var stack = entry.getValue().armorItemStack;
            if (stack.getItem() instanceof thaumic.tinkerer.api.LegacyArmor) {
                int amount = (int) entry.getValue().newDamage;
                event.setNewDamage(entry.getKey(), 0);
                damage(stack, amount, event.getEntity(), broken -> event.getEntity().onEquippedItemBroken(broken.getItem(), entry.getKey()));
                // Native hurtAndBreak(0) still breaks damage==max; remove this handled
                // entry so the engine does not apply its different boundary afterward.
                event.getArmorMap().remove(entry.getKey());
            }
        }
    }
    public static void damage(ItemStack stack, int amount, LivingEntity user, java.util.function.Consumer<ItemStack> broken) {
        if (!(user.level() instanceof ServerLevel level) || stack.isEmpty() || !stack.isDamageableItem()
                || (user instanceof Player player && player.getAbilities().instabuild)) return;
        int damage = EnchantmentHelper.processDurabilityChange(level, stack, amount);
        if (damage <= 0) return;
        int total = stack.getDamageValue() + damage;
        if (total > stack.getMaxDamage()) {
            ItemStack snapshot = stack.copyWithCount(1);
            stack.shrink(1);
            if (!stack.isEmpty()) stack.setDamageValue(0);
            broken.accept(snapshot);
        } else stack.setDamageValue(total);
    }
    private LegacyDurability() {}
}
