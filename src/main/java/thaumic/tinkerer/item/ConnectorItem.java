package thaumic.tinkerer.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import thaumic.tinkerer.block.TransvectorInterfaceBlockEntity;
import thaumic.tinkerer.block.TransvectorBlockEntity;
import thaumic.tinkerer.block.GolemConnectorBlockEntity;
import thaumic.tinkerer.util.ItemData;

public final class ConnectorItem extends Item {
    public ConnectorItem() { super(new Properties().stacksTo(1)); }
    public static Optional<BlockPos> selected(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean selected = tag.contains("selected") ? tag.getBoolean("selected") : tag.contains("posy") && tag.getInt("posy") != -1;
        return selected ? Optional.of(new BlockPos(tag.getInt("posx"), tag.getInt("posy"), tag.getInt("posz"))) : Optional.empty();
    }
    public static void select(ItemStack stack, BlockPos pos) { ItemData.update(stack, tag -> { tag.putInt("posx", pos.getX()); tag.putInt("posy", pos.getY()); tag.putInt("posz", pos.getZ()); tag.putBoolean("selected", true); }); }
    public static void clear(ItemStack stack) { ItemData.update(stack, tag -> tag.putBoolean("selected", false)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); if (level.isClientSide()) return InteractionResult.PASS;
        var player = context.getPlayer(); if (player == null) return InteractionResult.PASS;
        var stack = context.getItemInHand(); var pos = context.getClickedPos(); var selected = selected(stack); String message; String messageGroup = "connector";
        if (selected.isEmpty()) {
            if (level.getBlockEntity(pos) instanceof TransvectorBlockEntity || level.getBlockEntity(pos) instanceof GolemConnectorBlockEntity) {
                select(stack, pos); message = "set"; sound(context);
                if (level.getBlockEntity(pos) instanceof GolemConnectorBlockEntity) messageGroup = "golemconnector";
            }
            else message = "notinterf";
        } else if (level.getBlockEntity(selected.get()) instanceof TransvectorBlockEntity origin) {
            if (origin instanceof TransvectorInterfaceBlockEntity && level.getBlockEntity(pos) instanceof TransvectorInterfaceBlockEntity) message = "interffail";
            else if (Math.abs((long)pos.getX() - selected.get().getX()) > origin.maxDistance()
                    || Math.abs((long)pos.getY() - selected.get().getY()) > origin.maxDistance()
                    || Math.abs((long)pos.getZ() - selected.get().getZ()) > origin.maxDistance()) message = "toofar";
            else {
                origin.bind(pos); clear(stack); message = "complete"; sound(context);
                // Original connector requests the target's update, not the origin's.
                var state = level.getBlockState(pos); level.sendBlockUpdated(pos, state, state, 2);
            }
        } else { clear(stack); message = "notpresent"; }
        player.displayClientMessage(Component.translatable("message.thaumictinkerer." + messageGroup + "." + message), false);
        return InteractionResult.CONSUME;
    }
    private static void sound(UseOnContext context) { context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, .8f, 1); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.LivingEntity target, net.minecraft.world.InteractionHand hand) {
        if (!player.isShiftKeyDown() || !(target instanceof thaumcraft.api.entities.IControllableGolem)) return InteractionResult.PASS;
        stack = player.getItemInHand(hand); // ItemConnector writes the player's actual held stack.
        var level = player.level(); var selected = selected(stack);
        if (selected.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.PASS;
            player.displayClientMessage(Component.translatable("message.thaumictinkerer.golemconnector.notinterf"), false);
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(selected.get()) instanceof GolemConnectorBlockEntity connector)) {
            clear(stack);
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.thaumictinkerer.golemconnector.notpresent"), false);
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) { player.swing(hand); return InteractionResult.PASS; }
        connector.connect(target.getUUID()); clear(stack);
        var soundPos = new BlockPos((int)target.getX(), (int)target.getY(), (int)target.getZ());
        level.playSound(null, soundPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, .8f, 1);
        player.displayClientMessage(Component.translatable("message.thaumictinkerer.golemconnector.complete"), false);
        var state = connector.getBlockState(); level.sendBlockUpdated(connector.getBlockPos(), state, state, 2);
        return InteractionResult.CONSUME;
    }
}
