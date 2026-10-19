package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.AspectApi;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.menu.AspectAnalyzerMenu;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class AspectAnalyzerRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void aspectAnalyzer(GameTestHelper helper)throws Exception{
        VerificationReports.install();checks=0;var level=helper.getLevel();var id=ThaumicTinkerer.id("aspect_analyzer");var player=FocusRulesTest.player(helper,"[TTAnalyzer]");
        if(!GolemConnectorContent.enabled()){
            check(!BuiltInRegistries.BLOCK.containsKey(id)&&!BuiltInRegistries.ITEM.containsKey(id),"original block and item require ComputerCraft");
            check(!BuiltInRegistries.MENU.containsKey(id),"analyzer menu absent without ComputerCraft");
            check(level.getRecipeManager().byKey(id).isEmpty()&&!ResearchApi.exists(player,id.toString()),"analyzer recipe and research absent without ComputerCraft");
            System.out.println("TT_ASPECT_ANALYZER_ABSENT_PASS checks="+checks);helper.succeed();return;
        }
        // FakePlayer overrides openMenu to do nothing. Keep only its inert packet
        // connection; execute the real ServerPlayer menu creation/synchronization.
        var connection=player.connection;
        player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,player.getGameProfile(),net.minecraft.server.level.ClientInformation.createDefault()){
            @Override public BlockPos adjustSpawnLocation(net.minecraft.server.level.ServerLevel world,BlockPos spawn){return spawn;}
        };
        player.connection=connection;
        var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+260,(anchor.getZ()>>4)*16+8);var hopperPos=pos.below();
        try{
            level.setBlock(pos,AspectAnalyzerContent.BLOCK.get().defaultBlockState(),3);var state=level.getBlockState(pos);var tile=(AspectAnalyzerBlockEntity)level.getBlockEntity(pos);
            check(tile!=null&&tile.isEmpty()&&tile.getContainerSize()==1&&tile.getMaxStackSize()==64,"actual analyzer has the original one full-size inventory slot");
            check(state.canOcclude()&&state.ignitedByLava()&&Math.abs(state.getDestroySpeed(level,pos)-1.7f)<1e-6&&Math.abs(state.getBlock().getExplosionResistance()-.6f)<1e-6,"original opaque wood cube, hardness and effective blast resistance");
            var iron=new ItemStack(Items.IRON_INGOT,12);iron.set(DataComponents.CUSTOM_NAME,Component.literal("Analyzed stack"));
            check(!AspectApi.aspects(level,iron).isEmpty()&&AspectApi.aspects(level,new ItemStack(ClothTestMod.PROTECTED)).isEmpty(),"native composition and registered untagged fixture are available to the real slot predicate");
            player.setPos(pos.getCenter());var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);state.useWithoutItem(level,player,hit);
            check(player.containerMenu instanceof AspectAnalyzerMenu,"real block activation opens the native server menu");var menu=(AspectAnalyzerMenu)player.containerMenu;
            check(menu.slots.size()==37&&menu.slots.getFirst().x==20&&menu.slots.getFirst().y==30&&menu.slots.get(1).x==8&&menu.slots.get(1).y==84&&menu.slots.get(28).y==142,"original machine/player slot coordinates and count");
            check(menu.slots.getFirst().mayPlace(iron)&&!menu.slots.getFirst().mayPlace(new ItemStack(ClothTestMod.PROTECTED)),"GUI slot uses real effective aspects and rejects untagged items");
            player.getInventory().clearContent();player.getInventory().setItem(9,iron);var moved=menu.quickMoveStack(player,1);
            check(moved.getCount()==12&&player.getInventory().getItem(9).isEmpty()&&tile.getItem(0).getCount()==12,"actual shift-click moves the full stack into the analyzer");
            check(tile.aspects().equals(AspectApi.aspects(level,tile.getItem(0)))&&tile.aspects().equals(AspectApi.aspects(level,tile.getItem(0).copyWithCount(1))),"analyzer reads effective composition per item, without multiplying by stack count");
            var saved=tile.saveWithFullMetadata(level.registryAccess());var restored=new AspectAnalyzerBlockEntity(pos,state);restored.setLevel(level);restored.loadWithComponents(saved,level.registryAccess());
            check(restored.getItem(0).getCount()==12&&restored.getItem(0).getHoverName().getString().equals("Analyzed stack")&&restored.aspects().equals(tile.aspects()),"real NBT retains count, item components and composition");
            check(menu.quickMoveStack(player,0).getCount()==12&&tile.isEmpty()&&player.getInventory().getItem(9).getCount()==12,"original forward shift-click returns the analyzed stack to player storage");
            player.getInventory().setItem(10,new ItemStack(ClothTestMod.PROTECTED));check(menu.quickMoveStack(player,2).isEmpty()&&tile.isEmpty(),"untagged player item does not shift-move between either inventory");
            var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
            check(handler.getSlots()==1&&handler.insertItem(0,new ItemStack(ClothTestMod.PROTECTED),false).isEmpty()&&!tile.isEmpty()&&tile.aspects().isEmpty(),"original unrestricted inventory automation differs from the aspect-restricted GUI slot");tile.clearContent();
            for(var side:Direction.values())check(level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side).getSlots()==1,"automation available from "+side);
            tile.setItem(0,restored.getItem(0).copy());level.setBlock(hopperPos,Blocks.HOPPER.defaultBlockState(),2);HopperBlockEntity.pushItemsTick(level,hopperPos,level.getBlockState(hopperPos),(HopperBlockEntity)level.getBlockEntity(hopperPos));
            check(tile.getItem(0).getCount()==11&&((HopperBlockEntity)level.getBlockEntity(hopperPos)).getItem(0).getCount()==1,"actual hopper extracts one item without consuming it as a research operation");
            var order=new LinkedHashMap<String,Integer>();order.put("aer",2);order.put("aqua",7);order.put("terra",7);order.put("ordo",1);
            check(AspectAnalyzerMenu.sortedAmounts(order).stream().map(Map.Entry::getKey).toList().equals(List.of("aqua","terra","aer","ordo")),"original GUI amount sort is descending and stable for equal amounts");
            order.clear();order.put("aer",2);order.put("ordo",0);order.put("terra",7);
            check(AspectAnalyzerMenu.sortedAmounts(order).stream().map(Map.Entry::getKey).toList().equals(List.of("aer","ordo","terra")),"original zero entry remains a barrier to the positive-only bubble sort");
            player.setPos(pos.getCenter().add(8,0,0));check(menu.stillValid(player),"original exact eight-block menu boundary");player.setPos(pos.getCenter().add(8.001,0,0));check(!menu.stillValid(player),"menu closes beyond eight blocks");player.setPos(pos.getCenter());
            AspectAnalyzerPeripheralChecks.run(tile);
            tile.setItem(0,restored.getItem(0).copy());var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(id).orElseThrow().value();
            check(recipe.vis().equals(Map.of("ordo",1,"perditio",1))&&recipe.getResultItem(level.registryAccess()).is(AspectAnalyzerContent.BLOCK.get().asItem()),"original conditional recipe price and output");
            var research=AddonRegistry.research().get(id.toString());
            check(research.concealed()&&research.round()&&!research.secondary()&&research.complexity()==2&&research.column()==0&&research.row()==1&&research.parents().equals(List.of("thaumictinkerer:peripherals"))&&research.hiddenParents().equals(List.of("GOGGLES","THAUMIUM")),"original analyzer research flags, visible/hidden parents and grid");
            level.destroyBlock(pos,true);check(!menu.stillValid(player),"removing the actual block invalidates its menu");
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
            check(drops.stream().filter(e->e.getItem().is(Items.IRON_INGOT)).mapToInt(e->e.getItem().getCount()).sum()==12&&drops.stream().anyMatch(e->e.getItem().is(AspectAnalyzerContent.BLOCK.get().asItem())),"break preserves all twelve analyzed items and drops the analyzer");
            System.out.println("TT_ASPECT_ANALYZER_PASS checks="+checks);helper.succeed();
        }finally{
            player.closeContainer();player.getInventory().clearContent();
            for(var p:List.of(pos,hopperPos)){if(level.getBlockEntity(p) instanceof Container container)container.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).forEach(Entity::discard);
        }
    }
}
