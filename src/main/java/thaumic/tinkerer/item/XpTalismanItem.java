package thaumic.tinkerer.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.client.ParticleApi;
import thaumic.tinkerer.util.ItemData;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class XpTalismanItem extends Item implements ICurioItem {
    public static final int CAPACITY = 1500;
    private static final SoundEvent ZAP = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:zap"));
    public XpTalismanItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    public static int xp(ItemStack stack) { return ItemData.integer(stack, "xp"); }
    public static void xp(ItemStack stack, int amount) { ItemData.update(stack, tag -> tag.putInt("xp", amount)); }
    public static boolean absorbing(ItemStack stack) { return ItemData.flag(stack, "absorbing"); }
    public static void absorbing(ItemStack stack, boolean value) { ItemData.update(stack, tag -> tag.putBoolean("absorbing", value)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (xp(stack) >= CAPACITY) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide()) {
                absorbing(stack, !absorbing(stack));
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .3f, .1f);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (xp(stack) < 10) return InteractionResultHolder.pass(stack);
        int bottleSlot = -1;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.GLASS_BOTTLE)) { bottleSlot = slot; break; }
        }
        if (bottleSlot < 0) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) {
            player.getInventory().removeItem(bottleSlot, 1);
            var bottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            xp(stack, xp(stack) - 10);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .1f, .1f + level.random.nextFloat() * .5f);
        } else {
            for (int i = 0; i < 6; i++) ParticleApi.sparkle(level, (float) (player.getX() + Math.random() - .5),
                    (float) (player.getY() + Math.random() - .5), (float) (player.getZ() + Math.random() - .5), 3);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override public void curioTick(SlotContext context, ItemStack stack) { wornTick(stack, context.entity()); }

    public void wornTick(ItemStack stack, LivingEntity wearer) {
        if (!absorbing(stack) || !(wearer.level() instanceof ServerLevel level)) return;
        int current = xp(stack);
        int remaining = CAPACITY - current;
        if (remaining <= 0) { absorbing(stack, false); return; }
        int added = 0;
        var box = new AABB(wearer.getX() - 3, wearer.getY() - 3, wearer.getZ() - 3,
                wearer.getX() + 3, wearer.getY() + 3, wearer.getZ() + 3);
        for (var orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            if (!orb.isAlive()) continue;
            CompoundTag saved = new CompoundTag();
            orb.addAdditionalSaveData(saved);
            int units = Math.max(1, saved.getInt("Count"));
            int consumed = 0;
            int value = orb.getValue();
            // A modern merged entity represents several original orbs. Consume those units
            // individually, retaining unconsumed units without discarding their experience.
            for (int unit = 0; unit < units; unit++) {
                if (added + value <= remaining) {
                    added += value;
                    consumed++;
                    level.playSound(null, orb.blockPosition(), ZAP, SoundSource.PLAYERS, value / 10f, 1);
                    ParticleApi.wisp(level, orb.getX(), orb.getY(), orb.getZ(), value / 5f, .1f, .9f, .1f);
                }
                // Quirk kept verbatim from ItemXPTalisman: subtract the cumulative sum
                // after EVERY orb, including an orb that was too large to absorb.
                remaining -= added;
                if (remaining <= 0) break;
            }
            if (consumed == units) orb.discard();
            else if (consumed > 0) {
                saved.putInt("Count", units - consumed);
                orb.readAdditionalSaveData(saved);
                orb.value = value; // The modern save format stores Value as a short.
            }
            if (remaining <= 0) break;
        }
        xp(stack, Math.min(CAPACITY, current + added));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("XP: " + xp(stack)));
        lines.add(Component.translatable(xp(stack) >= CAPACITY ? "tooltip.thaumictinkerer.full"
                : absorbing(stack) ? "tooltip.thaumictinkerer.absorbing" : "tooltip.thaumictinkerer.not_absorbing"));
    }
}
