package thaumic.tinkerer.kami.tool;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import thaumcraft.api.world.BlockMaterials;
import thaumcraft.api.world.IBlockStartBreakTool;
import static thaumic.tinkerer.kami.tool.AwakenedMiningShape.Kind;

public final class AwakenedDiggers {
    private static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand,String kind){
        var stack=player.getItemInHand(hand);
        if(!player.isShiftKeyDown())return InteractionResultHolder.pass(stack);
        AdvancedToolMode.change(player,stack,kind);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    private static void tooltip(ItemStack stack,List<Component> lines,String kind){lines.add(Component.translatable(AdvancedToolMode.key(kind,AdvancedToolMode.get(stack))));}
    public static final class Pick extends IchorTools.Pick implements IBlockStartBreakTool, thaumic.tinkerer.api.IAdvancedTool {
        @Override public String getToolType(){return "pick";}
        @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,ServerPlayer player){return AwakenedMining.start(stack,pos,player,Kind.PICK);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return AwakenedDiggers.use(level,player,hand,"pick");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){tooltip(stack,lines,"pick");}
        @Override public float getDestroySpeed(ItemStack stack,BlockState state){return state.is(Blocks.BEDROCK)?Float.MAX_VALUE:super.getDestroySpeed(stack,state);}
        @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return state.is(Blocks.BEDROCK)||super.isCorrectToolForDrops(stack,state);}
    }
    public static final class Shovel extends IchorTools.Shovel implements IBlockStartBreakTool, thaumic.tinkerer.api.IAdvancedTool {
        @Override public String getToolType(){return "shovel";}
        @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,ServerPlayer player){return AwakenedMining.start(stack,pos,player,Kind.SHOVEL);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return AwakenedDiggers.use(level,player,hand,"shovel");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){tooltip(stack,lines,"shovel");}
    }
    public static final class Axe extends IchorTools.Axe implements IBlockStartBreakTool, thaumic.tinkerer.api.IAdvancedTool {
        @Override public String getToolType(){return "axe";}
        @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,ServerPlayer player){return AwakenedMining.start(stack,pos,player,Kind.AXE);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return AwakenedDiggers.use(level,player,hand,"axe");}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){tooltip(stack,lines,"axe");}
        @Override public float getDestroySpeed(ItemStack stack,BlockState state){
            float shears=state.is(Blocks.COBWEB)||state.is(BlockMaterials.LEAVES)?15:
                    state.is(net.minecraft.tags.BlockTags.WOOL)?5:1;
            return Math.max(super.getDestroySpeed(stack,state),shears);
        }
    }
    private AwakenedDiggers(){}
}
