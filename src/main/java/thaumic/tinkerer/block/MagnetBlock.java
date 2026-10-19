package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.wands.ICastingWand;

public final class MagnetBlock extends BaseEntityBlock {
    public static final BooleanProperty PULLING = BooleanProperty.create("pulling");
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 2, 15);
    public final boolean mobs;
    public MagnetBlock(boolean mobs) {
        super(Properties.of().mapColor(MapColor.METAL).strength(1.7f, .6f).sound(SoundType.WOOD).noOcclusion());
        this.mobs = mobs; registerDefaultState(stateDefinition.any().setValue(PULLING, true));
    }
    @Override protected MapCodec<MagnetBlock> codec() { return simpleCodec(properties -> new MagnetBlock(mobs)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) { builder.add(PULLING); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return mobs ? new MobMagnetBlockEntity(pos, state) : new MagnetBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Original updateEntity runs on both sides, including its local client-proxy particles.
        return (world, pos, current, tile) -> { if (tile instanceof MagnetBlockEntity magnet) magnet.tick(); };
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ICastingWand) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MobMagnetBlockEntity tile) {
                player.openMenu(tile);
                level.playSound(null, pos, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("thaumcraft:key")), SoundSource.BLOCKS, 1, .5f);
            }
        } else toggle(state, level, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        toggle(state, level, pos); return InteractionResult.sidedSuccess(level.isClientSide());
    }
    private static void toggle(BlockState state, Level level, BlockPos pos) {
        level.setBlock(pos, state.cycle(PULLING), 2);
        if (!level.isClientSide()) level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1, .5f);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof MobMagnetBlockEntity tile) {
            Containers.dropContents(level, pos, tile); level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
