package thaumic.tinkerer.kami.focus;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.wands.FocusBehavior;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;

/**
 * Shared base class for KAMI wand foci (Recall, Shadowbeam, XP Drain).
 * Extends ItemFocusBasic and implements FocusBehavior using Thaumcraft 4 modern public API.
 */
public abstract class KamiFocusItem extends ItemFocusBasic implements FocusBehavior {

    public KamiFocusItem() {
        this(new Properties().durability(1).rarity(Rarity.RARE));
    }

    protected KamiFocusItem(Properties properties){super(properties);}

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public boolean managesVis() {
        return true;
    }

    @Override
    public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack, int rank) {
        return List.of(FocusUpgradeType.treasure);
    }

    @Override
    public boolean begin(ServerPlayer player, ItemStack wand, ItemStack focus, InteractionHand hand) {
        return true;
    }

    @Override
    public void end(ServerPlayer player, ItemStack wand, ItemStack focus) {
    }
}
