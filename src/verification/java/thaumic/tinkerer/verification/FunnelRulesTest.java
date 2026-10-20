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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class FunnelRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static void label(BlockEntity tile,Aspect aspect,net.minecraft.server.level.ServerLevel level){var data=tile.saveWithFullMetadata(level.registryAccess());data.putString("AspectFilter",aspect.getTag());tile.loadWithComponents(data,level.registryAccess());}
    @GameTest(template="empty",timeoutTicks=100)
    public static void funnel(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+180,(anchor.getZ()>>4)*16+8);var hopperPos=pos.below();var targetPos=hopperPos.east();var west=hopperPos.west();
        var player=FocusRulesTest.player(helper,"[TTFunnel]");var changed=new ArrayList<BlockPos>();
        try{
            for(var p:BlockPos.betweenClosed(pos.offset(-3,-2,-2),pos.offset(3,3,2))){changed.add(p.immutable());level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            level.setBlock(hopperPos,Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST),2);
            var state=FunnelContent.BLOCK.get().defaultBlockState();level.setBlock(pos,state,3);var tile=(FunnelBlockEntity)level.getBlockEntity(pos);
            var targetBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar"));level.setBlock(targetPos,targetBlock.defaultBlockState(),2);var target=(IAspectContainer)level.getBlockEntity(targetPos);
            check(tile!=null&&tile.isEmpty()&&tile.getContainerSize()==1&&tile.getMaxStackSize()==1,"actual funnel block entity has the original one singleton slot");
            check(state.getShape(level,pos).bounds().maxY==.125&&state.getCollisionShape(level,pos).bounds().maxY==1&&!state.canOcclude(),"original thin selection shape and full-block physical collision remain distinct");
            check(Arrays.stream(Direction.values()).noneMatch(side->state.isFaceSturdy(level,pos,side)),"nonopaque original funnel provides no solid attachment face despite its collision cube");
            check(state.getDestroySpeed(level,pos)==3&&Math.abs(state.getBlock().getExplosionResistance()-4.8f)<1e-6,"original hardness and effective blast resistance");
            check(state.canSurvive(level,pos)&&!state.canSurvive(level,pos.east(2)),"support requires the actual vanilla hopper");
            var invalid=pos.south(2);var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(FunnelContent.BLOCK.get()),new BlockHitResult(invalid.getCenter(),Direction.UP,invalid,false));
            check(FunnelContent.BLOCK.get().getStateForPlacement(context)==null,"actual placement context rejects an unsupported position");
            var jar=new ItemStack(targetBlock);check(jar.getItem() instanceof IEssentiaJarItem,"native jar item exposes its real public read/write contract");var jarItem=(IEssentiaJarItem)jar.getItem();
            check(jarItem.getAspects(jar)==null,"native empty item jar reports null contents as in the original");
            jarItem.setAspects(jar,new AspectList().add(Aspects.FIRE,3));jar.set(DataComponents.CUSTOM_NAME,Component.literal("Kept jar"));
            CustomData.update(DataComponents.BLOCK_ENTITY_DATA,jar,data->data.putString("AspectFilter","ignis"));
            jarItem.getAspects(jar).add(Aspects.FIRE,99);
            check(jarItem.getAspects(jar).getAmount(Aspects.FIRE)==3,"item aspect snapshot cannot mutate native contents");
            var placedJar=pos.south(2);level.setBlock(placedJar.below(),Blocks.STONE.defaultBlockState(),2);player.setItemInHand(InteractionHand.MAIN_HAND,jar.copyWithCount(1));
            var placeHit=new BlockHitResult(placedJar.below().getCenter(),Direction.UP,placedJar.below(),false);
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,placeHit));
            check(level.getBlockEntity(placedJar) instanceof IAspectContainer placed&&placed.getAspects().getAmount(Aspects.FIRE)==3&&placed.getAspectFilter()==Aspects.FIRE,"public item setter writes the actual contents and label consumed by native jar placement");
            var held=jar.copyWithCount(2);player.setItemInHand(InteractionHand.MAIN_HAND,held);var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
            check(state.useItemOn(held,level,player,InteractionHand.MAIN_HAND,hit).consumesAction()&&held.getCount()==1&&tile.getItem(0).getCount()==1,"actual manual insertion splits exactly one jar from the held stack");
            check(tile.getAspects().getAmount(Aspects.FIRE)==3&&target.getJarType()==IAspectContainer.JarType.NORMAL,"funnel reads held-item essentia and native target jar identity");
            var top=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);var bottom=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.DOWN);var internal=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,null);
            check(top.getSlots()==1&&bottom.getSlots()==0&&internal.getSlots()==1,"actual item capabilities hide every slot from the hopper below");
            check(tile.canPlaceItemThroughFace(0,jar,Direction.UP)&&!tile.canPlaceItemThroughFace(0,new ItemStack(Items.STONE),Direction.UP)&&!tile.canTakeItemThroughFace(0,jar,Direction.DOWN),"jar-only sided insertion and no downward extraction");
            var stored=tile.removeItemNoUpdate(0);
            check(top.insertItem(0,new ItemStack(Items.STONE),false).is(Items.STONE)&&tile.isEmpty(),"actual sided adapter rejects stone even with an empty slot");
            check(top.insertItem(0,stored.copyWithCount(2),false).getCount()==1&&tile.getItem(0).getCount()==1,"actual sided adapter inserts only one real jar and returns the remainder");
            HopperBlockEntity.pushItemsTick(level,hopperPos,level.getBlockState(hopperPos),(HopperBlockEntity)level.getBlockEntity(hopperPos));
            check(((HopperBlockEntity)level.getBlockEntity(hopperPos)).isEmpty()&&!tile.getItem(0).isEmpty(),"real vanilla hopper cannot extract the held jar through the funnel's bottom");
            FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==2&&target.getAspects().getAmount(Aspects.FIRE)==1,"one real update transfers exactly one unit to the hopper's output jar");
            var saved=tile.saveWithFullMetadata(level.registryAccess());var reloaded=new FunnelBlockEntity(pos,state);reloaded.loadWithComponents(saved,level.registryAccess());
            check(reloaded.getAspects().getAmount(Aspects.FIRE)==2&&reloaded.getItem(0).getHoverName().getString().equals("Kept jar"),"transferred amount and unrelated item components survive real funnel NBT");
            check(tile.getUpdateTag(level.registryAccess()).getList("Items",10).size()==1&&tile.getUpdatePacket()!=null,"actual synchronization includes the held jar");
            level.setBlock(hopperPos,level.getBlockState(hopperPos).setValue(HopperBlock.ENABLED,false),2);FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==1&&target.getAspects().getAmount(Aspects.FIRE)==2,"powered/disabled hopper does not stop the original essence transfer");
            target.setAspects(new AspectList().add(Aspects.FIRE,64));FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==1&&target.getAspects().getAmount(Aspects.FIRE)==64,"full ordinary destination jar blocks transfer");
            target.setAspects(new AspectList().add(Aspects.WATER,2));FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==1&&target.getAspects().getAmount(Aspects.WATER)==2,"different nonempty aspect blocks transfer");
            target.setAspects(new AspectList());label((BlockEntity)target,Aspects.WATER,level);FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==1&&target.getAspects().visSize()==0,"different empty label rejects transfer without the original empty-array crash");
            level.setBlock(targetPos,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:void_jar")).defaultBlockState(),2);target=(IAspectContainer)level.getBlockEntity(targetPos);target.setAspects(new AspectList().add(Aspects.FIRE,64));FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects()==null&&target.getAspects().getAmount(Aspects.FIRE)==64&&target.getJarType()==IAspectContainer.JarType.VOID,"full void jar consumes the final source unit while retaining its sixty-four units");
            check(tile.getItem(0).getHoverName().getString().equals("Kept jar")&&tile.getItem(0).getOrDefault(DataComponents.BLOCK_ENTITY_DATA,CustomData.EMPTY).copyTag().getString("AspectFilter").equals("ignis"),"emptying preserves the original jar label and all other components");
            jarItem.setAspects(tile.getItem(0),new AspectList().add(Aspects.FIRE,3));level.setBlock(west,targetBlock.defaultBlockState(),2);level.setBlock(hopperPos,level.getBlockState(hopperPos).setValue(HopperBlock.FACING,Direction.WEST),2);FunnelBlockEntity.tick(level,pos,state,tile);
            check(((IAspectContainer)level.getBlockEntity(west)).getAspects().getAmount(Aspects.FIRE)==1&&tile.getAspects().getAmount(Aspects.FIRE)==2,"changing hopper direction retargets the next transfer immediately");
            var linked=pos.offset(-2,-1,0);changed.add(linked);level.setBlock(linked,TransvectorContent.INTERFACE.get().defaultBlockState(),2);((TransvectorInterfaceBlockEntity)level.getBlockEntity(linked)).bind(west);
            level.setBlock(hopperPos,level.getBlockState(hopperPos).setValue(HopperBlock.FACING,Direction.NORTH),2);
            var north=hopperPos.north();changed.add(north);level.setBlock(north,TransvectorContent.INTERFACE.get().defaultBlockState(),2);((TransvectorInterfaceBlockEntity)level.getBlockEntity(north)).bind(west);FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==2,"forwarded aspect container is not mistaken for a concrete native jar");
            var phial=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:essentia_phial")));CustomData.update(DataComponents.CUSTOM_DATA,phial,data->data.putString("Aspect","ignis"));tile.setItem(0,phial);FunnelBlockEntity.tick(level,pos,state,tile);
            check(tile.getAspects().getAmount(Aspects.FIRE)==8&&!tile.canPlaceItemThroughFace(0,phial,Direction.UP),"a directly assigned phial retains its real read-only contents but cannot be inserted or drained as a jar");
            check(!tile.doesContainerAccept(Aspects.FIRE)&&tile.addToContainer(Aspects.FIRE,5)==0&&!tile.takeFromContainer(Aspects.FIRE,1)&&!tile.doesContainerContainAmount(Aspects.FIRE,1)&&tile.containerContains(Aspects.FIRE)==0,"original pseudo-container mutators and queries remain non-storing stubs");
            tile.setItem(0,jar.copyWithCount(1));player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);state.useWithoutItem(level,player,hit);
            check(tile.isEmpty()&&player.getInventory().items.stream().anyMatch(stack->stack.is(jar.getItem())&&stack.getHoverName().getString().equals("Kept jar")),"empty-hand interaction returns the actual labelled jar to the player");
            tile.setItem(0,jar.copyWithCount(1));level.setBlock(hopperPos,Blocks.AIR.defaultBlockState(),3);
            check(level.getBlockState(pos).is(Blocks.AIR),"removing its hopper support destroys the funnel");
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
            check(drops.stream().anyMatch(entity->entity.getItem().is(jar.getItem())&&jarItem.getAspects(entity.getItem()).getAmount(Aspects.FIRE)==3)&&drops.stream().anyMatch(entity->entity.getItem().is(FunnelContent.BLOCK.get().asItem())),"support loss drops both the funnel and its full component-preserving stored jar");
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("funnel")).orElseThrow().value();check(recipe.vis().equals(Map.of("ordo",1,"perditio",1))&&recipe.getResultItem(level.registryAccess()).is(FunnelContent.BLOCK.get().asItem()),"exact original arcane price and output");
            var research=AddonRegistry.research().get("thaumictinkerer:funnel");check(research.concealed()&&research.secondary()&&research.complexity()==1&&research.column()==0&&research.row()==-7&&research.parents().equals(List.of("thaumictinkerer:bright_nitor"))&&research.hiddenParents().equals(List.of("DISTILESSENTIA")),"original research flags, grid and visible/hidden parents");
            System.out.println("TT_FUNNEL_PASS checks="+checks);helper.succeed();
        }finally{
            player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(var p:changed){if(level.getBlockEntity(p) instanceof Container container)container.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).forEach(Entity::discard);
        }
    }
}
