package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.wands.ICastingWand;

public final class TransvectorDislocatorBlock extends CamoBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public TransvectorDislocatorBlock() { this(Properties.of().mapColor(MapColor.METAL).strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.STONE)); }
    private TransvectorDislocatorBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(POWERED, false)); }
    @Override protected MapCodec<TransvectorDislocatorBlock> codec() { return simpleCodec(TransvectorDislocatorBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(POWERED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TransvectorDislocatorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return createTickerHelper(type, TransvectorContent.DISLOCATOR_TILE.get(), TransvectorDislocatorBlockEntity::tick); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos neighbor, boolean moving) {
        if (level.isClientSide()) return;
        boolean power = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (power && !state.getValue(POWERED)) { level.scheduleTick(pos, this, 1); level.setBlock(pos, state.setValue(POWERED, true), 4); }
        else if (!power && state.getValue(POWERED)) level.setBlock(pos, state.setValue(POWERED, false), 4);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TransvectorDislocatorBlockEntity tile) tile.receiveRedstonePulse();
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ICastingWand && level.getBlockEntity(pos) instanceof TransvectorDislocatorBlockEntity tile) {
            tile.orientation(hit.getDirection().get3DDataValue());
            level.playSound(player, pos, net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:tool")), net.minecraft.sounds.SoundSource.BLOCKS, .6f, 1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof TransvectorDislocatorBlockEntity tile) tile.orientation(placementOrientation(pos, entity).get3DDataValue());
    }
    public static Direction placementOrientation(BlockPos pos, LivingEntity entity) {
        if (Math.abs((float)entity.getX() - (float)pos.getX()) < 2 && Math.abs((float)entity.getZ() - (float)pos.getZ()) < 2) {
            double eyes = entity.getY() + 1.82;
            if (eyes - pos.getY() > 2) return Direction.UP;
            if (pos.getY() - eyes > 0) return Direction.DOWN;
        }
        return new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[Mth.floor((double)(entity.getYRot() * 4f / 360f) + .5) & 3];
    }
}
