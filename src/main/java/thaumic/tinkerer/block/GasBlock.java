package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Release-164 BlockGas: air-like cells spread a finite number of steps, then persist. */
public class GasBlock extends Block implements thaumcraft.api.world.IPersistentAir {
    public static final IntegerProperty SPREAD = IntegerProperty.create("spread", 0, 15);
    private static final MapCodec<GasBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("shadow").forGetter(block -> block.shadow), propertiesCodec()).apply(instance, GasBlock::new));
    private final boolean shadow;
    public GasBlock(boolean shadow) {
        this(shadow, properties(shadow, false));
    }
    protected GasBlock(boolean shadow, BlockBehaviour.Properties properties) {
        super(properties);
        this.shadow = shadow;
        registerDefaultState(stateDefinition.any().setValue(SPREAD, 0));
    }
    protected static BlockBehaviour.Properties properties(boolean shadow, boolean nitor) {
        return BlockBehaviour.Properties.of().noCollission().noOcclusion().air().replaceable().randomTicks().noLootTable()
                .lightLevel(state -> shadow ? 0 : nitor && state.getValue(SPREAD) == 1 ? 15 : 12);
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(SPREAD); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return shadow ? 15 : 0; }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return !shadow; }
    @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) { return false; }
    @Override public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                               net.minecraft.world.level.LevelReader level, BlockPos pos,
                                               net.minecraft.world.entity.player.Player player) { return ItemStack.EMPTY; }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int spread = state.getValue(SPREAD);
        if (spread == 0) return;
        // The original calls these in X-,X+,Y-,Y+,Z-,Z+ order. Other kinds of gas are replaceable.
        for (Direction direction : new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}) {
            BlockPos next = pos.relative(direction);
            if (level.isEmptyBlock(next) && !level.getBlockState(next).is(this)) {
                level.setBlock(next, defaultBlockState().setValue(SPREAD, spread - 1), 2);
                level.scheduleTick(next, this, 10);
            }
        }
        level.setBlock(pos, state.setValue(SPREAD, 0), 2);
        placeParticle(level, pos);
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { tick(state, level, pos, random); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < .0075f) {
            if (shadow) placeParticle(level, pos);
            else thaumcraft.api.client.ParticleApi.sparkle(level, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, 1, 1, random.nextFloat() / 2);
        }
    }
    public void placeParticle(Level level, BlockPos pos) {
        if (shadow) thaumcraft.api.client.ParticleApi.wisp(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, .125f, 5, true, true, -.02f);
        else thaumcraft.api.client.ParticleApi.sparkle(level, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, 1);
    }
}
