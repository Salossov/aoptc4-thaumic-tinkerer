package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

public abstract class CamoBlock extends BaseEntityBlock {
    protected CamoBlock(Properties properties) { super(properties); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    public static boolean validCamouflage(BlockState state, Level level, BlockPos pos) {
        var block = state.getBlock();
        if (block instanceof CamoBlock || state.isAir() || state.getRenderShape() != RenderShape.MODEL) return false;
        // Original standard-cube/log/quartz renderers also accept thin standard-rendered
        // blocks; render the selected icons on our full cube, not their original mesh.
        if (block instanceof SlabBlock || block instanceof BasePressurePlateBlock || block instanceof ButtonBlock
                || block instanceof SnowLayerBlock || block instanceof FarmBlock || block instanceof TrapDoorBlock) return true;
        return net.minecraft.world.level.block.Block.isShapeFullBlock(state.getShape(level, pos, CollisionContext.empty()));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(stack.getItem() instanceof BlockItem item) || !(level.getBlockEntity(pos) instanceof CamoBlockEntity tile)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        var camouflage = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(item.getBlock().defaultBlockState());
        if (!validCamouflage(camouflage, level, pos)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (item.getBlock() instanceof HorizontalDirectionalBlock && hit.getDirection().getAxis().isHorizontal()
                && camouflage.hasProperty(HorizontalDirectionalBlock.FACING)) camouflage = camouflage.setValue(HorizontalDirectionalBlock.FACING, hit.getDirection());
        tile.camouflage(camouflage);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CamoBlockEntity tile) tile.camouflage(Blocks.AIR.defaultBlockState());
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
