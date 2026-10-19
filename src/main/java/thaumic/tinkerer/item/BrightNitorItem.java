package thaumic.tinkerer.item;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.block.GasBlock;
import thaumic.tinkerer.block.GasContent;

public final class BrightNitorItem extends Item {
    public BrightNitorItem() { super(new Properties().stacksTo(1)); }
    public static void light(Level level, BlockPos pos, boolean kami) {
        if (!level.isClientSide() && (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.AIR)
                || level.getBlockState(pos).is(GasContent.NITOR.get()))) {
            level.setBlock(pos, GasContent.NITOR.get().defaultBlockState().setValue(GasBlock.SPREAD, kami ? 1 : 0), 2);
        }
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        light(level, new BlockPos(Mth.floor(entity.getX()), (int) entity.getY() + 1, Mth.floor(entity.getZ())), false);
    }
}
