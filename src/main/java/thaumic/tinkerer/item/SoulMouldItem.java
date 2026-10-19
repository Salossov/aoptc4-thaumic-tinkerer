package thaumic.tinkerer.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.util.ItemData;

public final class SoulMouldItem extends Item {
    public SoulMouldItem() { super(new Properties().stacksTo(1)); }
    public static String getPatternName(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("pattern"); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Player) && !entity.level().isClientSide()) {
            // Original writes the player's current held stack when a player is supplied.
            ItemStack held = player == null ? stack : player.getItemInHand(hand);
            ItemData.update(held, tag -> tag.putString("pattern", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
        }
        return InteractionResult.sidedSuccess(entity.level().isClientSide());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide()) ItemData.update(stack, tag -> tag.remove("pattern"));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String pattern = getPatternName(stack);
        if (pattern.isEmpty()) { lines.add(Component.translatable("tooltip.thaumictinkerer.soul_mould.unassigned")); return; }
        var id = ResourceLocation.tryParse(pattern);
        Component name = id == null ? Component.translatable("entity." + pattern + ".name")
                : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type -> type.getDescription()).orElse(Component.translatable("entity." + pattern + ".name"));
        lines.add(Component.translatable("tooltip.thaumictinkerer.soul_mould.pattern", name));
    }
}
