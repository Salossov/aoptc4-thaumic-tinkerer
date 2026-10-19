package thaumic.tinkerer.item;

import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.util.ItemData;
import thaumic.tinkerer.util.LegacyDurability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CleansingTalismanItem extends Item implements ICurioItem {
    private static final Set<String> COSTLY = Set.of("thaumcraft:blurred_vision", "thaumcraft:death_gaze",
            "thaumcraft:infectious_vis_exhaust", "thaumcraft:sun_scorned", "thaumcraft:unnatural_hunger");
    private static final SoundEvent WAND = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wand"));
    public CleansingTalismanItem() { super(new Properties().durability(100).rarity(Rarity.UNCOMMON)); }
    public static boolean enabled(ItemStack stack) { return ItemData.flag(stack, "enabled"); }
    public static void enabled(ItemStack stack, boolean value) { ItemData.update(stack, tag -> tag.putBoolean("enabled", value)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            enabled(stack, !enabled(stack));
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .3f, .1f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        if (!enabled(stack) || context.entity().level().isClientSide() || context.entity().tickCount % 20 != 0
                || !(context.entity() instanceof Player player)) return;
        boolean removed = false;
        int damage = 1;
        if (player.isOnFire()) {
            player.clearFire();
            removed = true;
        } else {
            for (var effect : player.getActiveEffects()) {
                var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !id.equals("thaumcraft:warp_ward")) {
                    player.removeEffect(effect.getEffect());
                    removed = true;
                    if (COSTLY.contains(id)) damage = 10;
                    break; // Only the first harmful effect, in the original collection order.
                }
            }
        }
        if (removed) {
            LegacyDurability.damage(stack, damage, player, snapshot -> curioBreak(context, snapshot));
            player.level().playSound(null, player.blockPosition(), WAND, SoundSource.PLAYERS, .3f, .1f);
        }
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(enabled(stack) ? "tooltip.thaumictinkerer.active" : "tooltip.thaumictinkerer.inactive"));
    }
}
