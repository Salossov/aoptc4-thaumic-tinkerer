package thaumic.tinkerer.item;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.block.GasBlock;

public final class GasItem extends Item {
    private final Supplier<GasBlock> gas;
    public GasItem(Supplier<GasBlock> gas) { super(new Properties()); this.gas = gas; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        // Deliberate truncation toward zero, including Y before adding one (release 164).
        var pos = new BlockPos((int) player.getX(), (int) player.getY() + 1, (int) player.getZ());
        boolean air = level.isEmptyBlock(pos);
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                    .5f, .4f / (level.random.nextFloat() * .4f + .8f));
            if (air) {
                level.setBlock(pos, gas.get().defaultBlockState().setValue(GasBlock.SPREAD, 4), 2);
                level.scheduleTick(pos, gas.get(), 10);
            }
        }
        return air ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide()) : InteractionResultHolder.consume(stack);
    }
}
