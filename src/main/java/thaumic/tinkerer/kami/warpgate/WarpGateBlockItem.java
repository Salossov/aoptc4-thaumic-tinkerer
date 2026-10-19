package thaumic.tinkerer.kami.warpgate;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class WarpGateBlockItem extends BlockItem {
    public WarpGateBlockItem(Block block) {
        super(block, new Properties().rarity(Rarity.EPIC));
    }
}
