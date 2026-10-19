package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumic.tinkerer.ThaumicTinkerer;

/** Temporary light maintained by Bright Nitor and the KAMI leggings. */
public final class NitorGasBlock extends GasBlock {
    public static final TagKey<Item> LEGGINGS = TagKey.create(Registries.ITEM, ThaumicTinkerer.id("nitor_leggings"));
    public NitorGasBlock() { this(properties(false, true)); }
    private NitorGasBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) { super(false, properties); }
    @Override protected com.mojang.serialization.MapCodec<NitorGasBlock> codec() { return simpleCodec(NitorGasBlock::new); }
    public static int interval(Level level) { return level.dimension().equals(Level.NETHER) ? 60 : 20; }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < .03f) thaumcraft.api.client.ParticleApi.sparkle(level, pos.getX() + .5f,
                pos.getY() + .5f, pos.getZ() + .5f, 1, 4, random.nextFloat() / 2);
    }
    @Override public void placeParticle(Level level, BlockPos pos) { }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, interval(level));
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int range = state.getValue(SPREAD) == 1 ? 6 : 1;
        boolean held = false;
        // Use the original integer corner, not the centre or an inflated block bounding box.
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos.getX() - range, pos.getY() - range,
                pos.getZ() - range, pos.getX() + range, pos.getY() + range, pos.getZ() + range))) {
            if (player.getInventory().items.stream().anyMatch(stack -> stack.is(GasContent.BRIGHT_NITOR.get()))
                    || player.getItemBySlot(EquipmentSlot.LEGS).is(LEGGINGS)) { held = true; break; }
        }
        if (held) level.scheduleTick(pos, this, interval(level));
        else level.removeBlock(pos, false);
    }
}
