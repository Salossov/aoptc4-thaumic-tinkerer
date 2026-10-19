package thaumic.tinkerer.kami.blocktalisman;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

public final class BlockTalismanContent {
    public static final String ID = "block_talisman";
    public static final DeferredItem<BlockTalismanItem> TALISMAN = ThaumicTinkerer.ITEMS.register(ID, BlockTalismanItem::new);

    /** Initialize this holder before the common item register is attached. */
    public static void initialize() {}

    public static void display(CreativeModeTab.Output output) {
        output.accept(TALISMAN.get());
    }

    private BlockTalismanContent() {}
}
