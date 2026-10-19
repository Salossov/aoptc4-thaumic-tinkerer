package thaumic.tinkerer.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Both original ItemSlab registrations used isDouble=false, including the hidden full-block item. */
public final class DarkQuartzSlabItem extends BlockItem {
    public DarkQuartzSlabItem(Block block) { super(block, new Properties()); }
    private static boolean half(BlockState state) {
        return state.is(DarkQuartzContent.SLAB.get()) && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var stack = context.getItemInHand(); var pos = context.getClickedPos();
        if (player == null || stack.isEmpty() || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var state = context.getLevel().getBlockState(pos);
        if (half(state) && (context.getClickedFace() == Direction.UP && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                || context.getClickedFace() == Direction.DOWN && state.getValue(SlabBlock.TYPE) == SlabType.TOP)) return merge(context, pos);
        var adjacent = pos.relative(context.getClickedFace());
        if (half(context.getLevel().getBlockState(adjacent))) return merge(context, adjacent);
        return super.useOn(context);
    }
    private InteractionResult merge(UseOnContext context, BlockPos pos) {
        var level = context.getLevel(); var player = context.getPlayer(); var stack = context.getItemInHand();
        var full = DarkQuartzContent.FULL_SLAB.get().defaultBlockState();
        if (level.isUnobstructed(full, pos, CollisionContext.empty()) && level.setBlock(pos, full, 3)) {
            var sound = full.getSoundType(level, pos, player);
            level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8f);
            if (player instanceof ServerPlayer server) CriteriaTriggers.PLACED_BLOCK.trigger(server, pos, stack);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, full));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        // Original ItemSlab reports handled even when entity collision blocks the merge.
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
