package thaumic.tinkerer.kami.construction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import thaumic.tinkerer.kami.ToolModeNotification;
import thaumic.tinkerer.kami.blocktalisman.BlockTalismanItem;
import top.theillusivec4.curios.api.CuriosApi;

public class PlacementMirrorItem extends Item {

    public PlacementMirrorItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static BlockState getState(ItemStack stack){return MirrorBinding.state(stack);}
    public static Block getBlock(ItemStack stack){return getState(stack).getBlock();}
    public static void setState(ItemStack stack,BlockState state){MirrorBinding.state(stack,state);}
    public static void setBlock(ItemStack stack,Block block){setState(stack,block==null?Blocks.AIR.defaultBlockState():block.defaultBlockState());}
    public static int getSize(ItemStack stack){return MirrorBinding.size(stack);}
    public static void setSize(ItemStack stack,int size){MirrorBinding.size(stack,size);}
    public static List<BlockPos> getBlocksToPlace(ItemStack stack,Player player){
        var target=thaumic.tinkerer.kami.tool.AwakenedMining.target(player,true,5.0);
        if(!target.missing().isEmpty()||target.hit().getType()!=HitResult.Type.BLOCK)return List.of();
        var hit=target.hit();var origin=hit.getBlockPos();var level=player.level();
        if(level.getBlockState(origin).canBeReplaced())origin=origin.below();
        var face=hit.getDirection();int rotation=Mth.floor(player.getYRot()*4F/360F+.5D)&3;
        var result=new ArrayList<BlockPos>();
        for(var cell:PlacementMirrorShape.candidates(origin.getX(),origin.getY(),origin.getZ(),face.getStepX(),face.getStepY(),face.getStepZ(),getSize(stack),rotation,player.getXRot())){
            var pos=new BlockPos(cell.x(),cell.y(),cell.z());
            if(!level.isInWorldBounds(pos))continue;
            boolean loaded=level instanceof net.minecraft.server.level.ServerLevel server?server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null:level.hasChunkAt(pos);
            if(!loaded){if(level.isClientSide())result.add(pos);continue;}
            var state=level.getBlockState(pos);if(state.isAir()||state.canBeReplaced())result.add(pos);
        }
        return List.copyOf(result);
    }

    private record Stock(ItemStack stack,boolean talisman) {
        int count(){return talisman?BlockTalismanItem.getBlockCount(stack):stack.getCount();}
        void take(int amount){if(talisman)BlockTalismanItem.remove(stack,amount);else stack.shrink(amount);}
    }
    private static List<Stock> stock(Player player,Block bound){
        var normal=new ArrayList<Stock>();var talismans=new ArrayList<Stock>();
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemStack,Boolean>());
        var item=bound.asItem();if(item==Items.AIR)return List.of();
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var stack=inventory.getItem(i);if(stack.isEmpty()||!seen.add(stack))continue;
            if(stack.is(item))normal.add(new Stock(stack,false));
            else if(stack.getItem() instanceof BlockTalismanItem&&BlockTalismanItem.getBlock(stack).asItem()==item)talismans.add(new Stock(stack,true));
        }
        CuriosApi.getCuriosInventory(player).ifPresent(handler->{
            var equipped=handler.getEquippedCurios();
            for(int i=0;i<equipped.getSlots();i++){
                var stack=equipped.getStackInSlot(i);
                if(!stack.isEmpty()&&seen.add(stack)&&stack.getItem() instanceof BlockTalismanItem&&BlockTalismanItem.getBlock(stack).asItem()==item)talismans.add(new Stock(stack,true));
            }
        });
        normal.addAll(talismans);return normal;
    }
    public static boolean hasBlocks(ItemStack mirror,Player player,long required){
        if(player.getAbilities().instabuild)return true;
        long count=0;
        for(var entry:stock(player,getBlock(mirror))){count+=entry.count();if(count>=required)return true;}
        return false;
    }
    static boolean consumeBlocks(ItemStack mirror,Player player,int required){
        if(required<1)return false;
        if(player.getAbilities().instabuild)return true;
        var entries=stock(player,getBlock(mirror));long count=0;
        for(var entry:entries)count+=entry.count();
        if(count<required)return false;
        int remaining=required;
        for(var entry:entries){int take=Math.min(remaining,entry.count());entry.take(take);remaining-=take;if(remaining==0)break;}
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();return true;
    }
    public record Placement(BlockPos pos,thaumcraft.api.world.BlockPlacementApi.Plan plan){}
    public static List<Placement> plans(ItemStack mirror,Player player){
        var selected=getState(mirror);if(selected.isAir())return List.of();
        var target=thaumic.tinkerer.kami.tool.AwakenedMining.target(player,true,5.0);
        if(!target.missing().isEmpty()||target.hit().getType()!=HitResult.Type.BLOCK)return List.of();
        var hand=player.getOffhandItem()==mirror?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        var result=new ArrayList<Placement>();var occupied=new java.util.HashSet<BlockPos>();
        for(var pos:getBlocksToPlace(mirror,player)){
            if(occupied.contains(pos))continue;
            var plan=thaumcraft.api.world.BlockPlacementApi.plan(player,hand,pos,target.hit().getDirection(),selected);
            if(plan==null||plan.parts().stream().anyMatch(part->occupied.contains(part.pos())))continue;
            result.add(new Placement(pos,plan));for(var part:plan.parts())occupied.add(part.pos());
        }
        return List.copyOf(result);
    }
    public static long cost(List<Placement> plans){long cost=0;for(var placement:plans)cost+=placement.plan().cost();return cost;}
    public static boolean placeAllBlocks(ItemStack stack,Player player){
        var state=getState(stack);if(state.isAir()||state.getBlock().asItem()==Items.AIR)return false;
        var plans=plans(stack,player);if(!hasBlocks(stack,player,cost(plans)))return false;
        if(player.level().isClientSide())return true;
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return false;
        var target=thaumic.tinkerer.kami.tool.AwakenedMining.target(player,true,5.0);
        if(!target.missing().isEmpty()||target.hit().getType()!=HitResult.Type.BLOCK)return false;
        var hand=player.getOffhandItem()==stack?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        for(var placement:plans)MirrorPlacement.place(player,hand,placement.pos(),target.hit().getDirection(),state,cost->consumeBlocks(stack,player,cost));
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:wand")),SoundSource.PLAYERS,1F,1F);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int currentSize = getSize(stack);
            int newSize = currentSize >= 11 ? 3 : currentSize + 2;
            setSize(stack, newSize);
            if (level.isClientSide()) {
                NeoForge.EVENT_BUS.post(new ToolModeNotification(newSize + " x " + newSize));
            }
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.1F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        var preparation=thaumic.tinkerer.kami.blocktalisman.TalismanActions.prepare(context,player.isShiftKeyDown()?2:getSize(context.getItemInHand())/2+4);
        if(preparation!=null)return preparation;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            BlockState target = level.getBlockState(pos);
            if (!target.isAir() && !target.liquid() && target.getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL && target.getBlock().asItem() != Items.AIR) {
                setState(stack,target);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            placeAllBlocks(stack,player);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int size = getSize(stack);
        lines.add(Component.literal(size + " x " + size));
        Block block = getBlock(stack);
        if (block != Blocks.AIR) {
            lines.add(Component.translatable(block.getDescriptionId()));
        }
    }
}
