package thaumic.tinkerer.kami.focus;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;

/**
 * Focus XP Drain: drains 15 points of player experience per tick to replenish 5.00 vis (500 centivis)
 * into primal aspects of the held wand in round-robin order.
 */
public class FocusXPDrainItem extends KamiFocusItem {
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(thaumic.tinkerer.ThaumicTinkerer.ID);

    public static final int XP_DRAIN_PER_TICK = 15;
    public static final int VIS_RESTORED_PER_TICK = 500; // 500 centivis = 5.00 vis

    private static final List<Aspect> PRIMALS=List.of(Aspects.AIR,Aspects.EARTH,Aspects.FIRE,Aspects.WATER,Aspects.ORDER,Aspects.ENTROPY);
    private final XpDrainCycle cycle=new XpDrainCycle();

    public FocusXPDrainItem() {
        super(new Properties().durability(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override
    public Map<Aspect, Integer> getVisCost(ItemStack stack) {
        return Map.of();
    }

    @Override
    public boolean isVisCostPerTick(ItemStack stack) {
        return true;
    }

    @Override
    public boolean continuous(ItemStack focus) {
        return true;
    }

    @Override
    public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack, int rank) {
        return List.of();
    }

    @Override
    public int getFocusColor(ItemStack stack) {
        return XpDrainVisual.color();
    }

    @Override
    public String getSortingHelper(ItemStack stack) {
        return "XPDRAIN";
    }

    public int getXpUse(ItemStack stack) {
        return XP_DRAIN_PER_TICK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.literal(" ")
            .append(Component.translatable("ttmisc.experience").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" x " + getXpUse(stack)).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public boolean cast(ServerPlayer player,ItemStack wand,ItemStack focus){
        int capacity=WandApi.capacity(wand);
        int next=cycle.next(index->WandApi.stored(wand,PRIMALS.get(index))<capacity);
        if(next<0||player.totalExperience<XP_DRAIN_PER_TICK)return false;
        if(!ExperienceHelper.drainPlayerXP(player,XP_DRAIN_PER_TICK))return false;
        var aspect=PRIMALS.get(next);int current=WandApi.stored(wand,aspect);
        long amount=(long)current+VIS_RESTORED_PER_TICK;
        LOGGER.info("{}",amount);
        WandApi.set(wand,aspect,(int)Math.min(capacity,amount));
        return true;
    }
}
