package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class RepairerRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(net.minecraft.server.level.ServerLevel level,BlockPos pos,int count){
        var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);
        BlockEntityTicker ticker=((EntityBlock)state.getBlock()).getTicker(level,state,tile.getType());
        for(int i=0;i<count;i++)ticker.tick(level,pos,state,tile);
    }
    private static void fill(BlockEntity source,Aspect contents,Aspect suction,int amount){
        ((IAspectContainer)source).setAspects(new AspectList().add(contents,1));((IEssentiaTransport)source).setSuction(suction,amount);
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void repairer(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+230,(anchor.getZ()>>4)*16+8);var sourcePos=pos.north();var player=FocusRulesTest.player(helper,"[TTRepairer]");var changed=new ArrayList<BlockPos>();
        try{
            for(var p:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))){changed.add(p.immutable());level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            var state=RepairerContent.BLOCK.get().defaultBlockState();level.setBlock(pos,state,3);var tile=(RepairerBlockEntity)level.getBlockEntity(pos);
            check(tile!=null&&tile.isEmpty()&&tile.getMaxStackSize()==1&&tile.getContainerSize()==1,"actual repairer has the original single-item inventory");
            check(state.getDestroySpeed(level,pos)==5&&state.getBlock().getExplosionResistance()==6&&!state.canOcclude()&&state.getCollisionShape(level,pos).bounds().maxY==1,"original hardness, effective blast resistance and nonopaque collision cube");
            check(Arrays.stream(Direction.values()).noneMatch(side->state.isFaceSturdy(level,pos,side)),"nonopaque powered repairer has no sturdy attachment face");
            for(int i=0;i<4;i++){
                player.setYRot(i*90);var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(RepairerContent.BLOCK.get()),new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
                check(RepairerContent.BLOCK.get().getStateForPlacement(context).getValue(RepairerBlock.FACING)==new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[i],"original yaw quadrant "+i);
            }
            check(!RepairerBlockEntity.accepts(new ItemStack(Items.STONE))&&!RepairerBlockEntity.accepts(new ItemStack(thaumic.tinkerer.item.FocusContent.DISLOCATION.get()))&&RepairerBlockEntity.accepts(new ItemStack(Items.IRON_PICKAXE)),"actual repairable predicate rejects blocks and explicitly nonrepairable durable foci");
            var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(25);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Kept tool"));player.setItemInHand(InteractionHand.MAIN_HAND,tool);
            var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);player.getAbilities().instabuild=true;
            check(state.useItemOn(tool,level,player,InteractionHand.MAIN_HAND,hit).consumesAction()&&tool.isEmpty()&&tile.getItem(0).getDamageValue()==25,"manual insertion splits the real item even in creative mode");player.getAbilities().instabuild=false;
            for(var side:Direction.values()){
                var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side);
                check(handler!=null&&handler.getSlots()==1&&handler.extractItem(0,1,true).getDamageValue()==25&&tile.canPlaceItemThroughFace(0,tile.getItem(0),side)&&!tile.canPlaceItemThroughFace(0,new ItemStack(Items.STONE),side),"all faces allow tool insertion and extraction: "+side);
            }
            check(state.isSignalSource()&&Arrays.stream(Direction.values()).allMatch(side->state.getDirectSignal(level,pos,side)==15&&state.getSignal(level,pos,side)==0),"damage emits strong 15 on every face and no weak power");
            var transport=level.getCapability(EssentiaCapabilities.TRANSPORT,pos,Direction.NORTH);
            check(transport==tile&&tile.isConnectable(Direction.NORTH)&&!tile.isConnectable(Direction.SOUTH)&&!tile.isConnectable(null)&&tile.canOutputTo(Direction.NORTH)&&!tile.canInputFrom(Direction.NORTH),"original facing-only transport and intentionally reversed output/input declarations");
            check(tile.getSuctionType(Direction.NORTH)==Aspects.TOOL&&tile.getSuctionAmount(Direction.NORTH)==128&&tile.getSuctionType(null)==null&&tile.getSuctionAmount(Direction.SOUTH)==0,"original tool suction only on the oriented face");
            check(tile.addEssentia(Aspects.TOOL,1,Direction.NORTH)==0&&tile.takeEssentia(Aspects.TOOL,1,Direction.NORTH)==0&&tile.getEssentiaType(null)==null&&tile.getEssentiaAmount(null)==0&&tile.getMinimumSuction()==0&&!tile.renderExtendedTube(),"original non-storing transport operations");
            level.setBlock(sourcePos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:essentia_tube")).defaultBlockState(),2);var source=level.getBlockEntity(sourcePos);
            fill(source,Aspects.TOOL,Aspects.TOOL,127);tick(level,pos,9);
            check(tile.getItem(0).getDamageValue()==25&&((IAspectContainer)source).getAspects().getAmount(Aspects.TOOL)==1,"first nine actual ticks consume no essentia");tick(level,pos,1);
            check(tile.getItem(0).getDamageValue()==17&&((IAspectContainer)source).getAspects().visSize()==0,"tenth actual tick spends one tool for eight durability");
            for(var aspect:new Aspect[]{Aspects.CRAFT,Aspects.ORDER}){
                tile.getItem(0).setDamageValue(20);fill(source,aspect,aspect,127);tick(level,pos,10);
                check(tile.getItem(0).getDamageValue()==20-(aspect==Aspects.CRAFT?5:3)&&((IAspectContainer)source).getAspects().visSize()==0,"original repair value and exact one-unit consumption for "+aspect.getTag());
            }
            tile.getItem(0).setDamageValue(20);fill(source,Aspects.TOOL,Aspects.WATER,127);tick(level,pos,10);
            check(tile.getItem(0).getDamageValue()==20&&((IAspectContainer)source).getAspects().getAmount(Aspects.TOOL)==1,"stored tool essence alone cannot bypass the original source suction-type predicate");
            fill(source,Aspects.ORDER,Aspects.TOOL,127);tick(level,pos,10);
            check(tile.getItem(0).getDamageValue()==20&&((IAspectContainer)source).getAspects().getAmount(Aspects.ORDER)==1,"different stored essence is not consumed under tool suction");
            fill(source,Aspects.TOOL,Aspects.TOOL,128);tick(level,pos,10);
            check(tile.getItem(0).getDamageValue()==20&&((IAspectContainer)source).getAspects().getAmount(Aspects.TOOL)==1,"equal suction blocks transfer");
            var data=source.saveWithFullMetadata(level.registryAccess());data.putInt("OpenSides",63&~(1<<Direction.SOUTH.ordinal()));source.loadWithComponents(data,level.registryAccess());((IEssentiaTransport)source).setSuction(Aspects.TOOL,127);tick(level,pos,10);
            check(tile.getItem(0).getDamageValue()==20&&((IAspectContainer)source).getAspects().getAmount(Aspects.TOOL)==1,"closed source output face blocks repair");data.putInt("OpenSides",63);source.loadWithComponents(data,level.registryAccess());
            tile.getItem(0).setDamageValue(2);fill(source,Aspects.TOOL,Aspects.TOOL,127);tick(level,pos,10);
            check(tile.getItem(0).getDamageValue()==0&&tile.getAspects().size()==1&&tile.getAspects().getAmount(Aspects.ENTROPY)==0&&state.getDirectSignal(level,pos,Direction.UP)==0,"repair clamps at zero, retains original zero entropy display and turns strong signal off");
            fill(source,Aspects.TOOL,Aspects.TOOL,127);tick(level,pos,10);
            check(((IAspectContainer)source).getAspects().getAmount(Aspects.TOOL)==1&&!tile.tookLastTick,"undamaged item consumes nothing and becomes visually inactive");
            check(!tile.doesContainerAccept(Aspects.ENTROPY)&&tile.addToContainer(Aspects.ENTROPY,1)==0&&!tile.takeFromContainer(Aspects.ENTROPY,1)&&!tile.doesContainerContainAmount(Aspects.ENTROPY,1)&&tile.containerContains(Aspects.ENTROPY)==0,"original damage display remains a read-only pseudo-container");
            tile.getItem(0).setDamageValue(11);var saved=tile.saveWithFullMetadata(level.registryAccess());var reloaded=new RepairerBlockEntity(pos,state);reloaded.loadWithComponents(saved,level.registryAccess());
            check(reloaded.getItem(0).getDamageValue()==11&&reloaded.getItem(0).getHoverName().getString().equals("Kept tool")&&reloaded.ticksExisted==0&&reloaded.tookLastTick,"real NBT retains item components but not original transient counters");
            check(tile.getUpdateTag(level.registryAccess()).getList("Items",10).size()==1&&tile.getUpdatePacket()!=null,"actual network update contains the tool and its damage");
            var hopperPos=pos.below();level.setBlock(hopperPos,Blocks.HOPPER.defaultBlockState(),2);HopperBlockEntity.pushItemsTick(level,hopperPos,level.getBlockState(hopperPos),(HopperBlockEntity)level.getBlockEntity(hopperPos));
            check(tile.isEmpty()&&((HopperBlockEntity)level.getBlockEntity(hopperPos)).getItem(0).getDamageValue()==11,"actual vanilla hopper can extract the repairer's item through its bottom");
            ((Container)level.getBlockEntity(hopperPos)).clearContent();tile.setItem(0,reloaded.getItem(0).copy());player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);state.useWithoutItem(level,player,hit);
            check(tile.isEmpty()&&player.getInventory().items.stream().anyMatch(stack->stack.getHoverName().getString().equals("Kept tool")&&stack.getDamageValue()==11),"empty hand retrieves the real damaged named tool");
            tile.setItem(0,reloaded.getItem(0).copy());level.destroyBlock(pos,true);
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
            check(drops.stream().anyMatch(entity->entity.getItem().is(Items.IRON_PICKAXE)&&entity.getItem().getDamageValue()==11&&entity.getItem().getHoverName().getString().equals("Kept tool"))&&drops.stream().anyMatch(entity->entity.getItem().is(RepairerContent.BLOCK.get().asItem())),"breaking drops the device and its exact stored item");
            var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("repairer")).orElseThrow().value();
            check(recipe.instability()==8&&recipe.aspects().equals(Map.of("instrumentum",15,"fabrico",20,"ordo",10,"praecantatio",15))&&recipe.components().size()==8,"original infusion instability and aspect costs");
            var thaumium=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_block")));
            check(recipe.input().test(thaumium)&&!recipe.input().test(new ItemStack(Blocks.IRON_BLOCK)),"pinned cosmetic metadata 4 is the real thaumium block");
            check(recipe.components().get(4).test(new ItemStack(Blocks.OAK_PLANKS))&&!recipe.components().get(4).test(new ItemStack(Blocks.BIRCH_PLANKS))&&recipe.components().get(6).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:enchanted_fabric"))))&&recipe.components().get(7).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_ingot")))),"original oak metadata, enchanted fabric metadata 7 and thaumium metadata 2 ingredients");
            var research=AddonRegistry.research().get("thaumictinkerer:repairer");
            check(research.concealed()&&!research.secondary()&&research.complexity()==3&&research.column()==-1&&research.row()==-9&&research.parents().equals(List.of("thaumictinkerer:funnel"))&&research.hiddenParents().equals(List.of("INFUSION","THAUMIUM","ENCHFABRIC")),"original research flags, grid and visible/hidden parents including TTResearchItem's automatic INFUSION parent");
            System.out.println("TT_REPAIRER_PASS checks="+checks);helper.succeed();
        }finally{
            player.getAbilities().instabuild=false;player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(var p:changed){if(level.getBlockEntity(p) instanceof Container container)container.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).forEach(Entity::discard);
        }
    }
}
