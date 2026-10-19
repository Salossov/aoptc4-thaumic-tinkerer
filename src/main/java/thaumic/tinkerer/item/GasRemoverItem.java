package thaumic.tinkerer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.block.GasBlock;

public final class GasRemoverItem extends Item {
    private static final SoundEvent WAND = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wand"));
    public GasRemoverItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!level.isClientSide()) {
            int x0 = (int) player.getX(), y0 = (int) player.getY(), z0 = (int) player.getZ();
            for (int x = x0 - 3; x < x0 + 3; x++) for (int y = y0 - 3; y < y0 + 3; y++) for (int z = z0 - 3; z < z0 + 3; z++) {
                var pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).getBlock() instanceof GasBlock gas) {
                    gas.placeParticle(level, pos);
                    level.removeBlock(pos, false);
                }
            }
            level.playSound(null, player.blockPosition(), WAND, SoundSource.PLAYERS, .2f, 1);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
