package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DislocationRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static void aim(net.minecraft.server.level.ServerPlayer player,BlockPos pos){player.setPos(pos.getX()+.5,pos.getY()+.5-player.getEyeHeight(),pos.getZ()-3);player.setYRot(0);player.setXRot(0);}
    private static void fill(ItemStack wand){for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,WandApi.capacity(wand));}
    @GameTest(template="empty",timeoutTicks=100)
    public static void dislocation(GameTestHelper helper)throws Exception{
        VerificationReports.install();checks=0;var level=helper.getLevel();var peer=new DislocationServerRulesTest.Peer(helper,"TTDislocation");var player=peer.player;var item=FocusContent.DISLOCATION.get();
        var wand=FocusRulesTest.wand(player,new ItemStack(item));WandApi.parts(wand,new WandParts("silverwood","thaumium"));fill(wand);
        var anchor=helper.absolutePos(BlockPos.ZERO);var origin=new BlockPos((anchor.getX()>>4)*16+5,anchor.getY()+155,(anchor.getZ()>>4)*16+8);var support=origin.east(5);var destination=support.north();
        var changed=new ArrayList<BlockPos>();
        try{
            for(var pos:BlockPos.betweenClosed(origin.offset(-1,-2,-4),support.offset(1,2,1))){changed.add(pos.immutable());level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
            var focus=WandApi.focus(wand);
            check(item.getVisCost(focus).equals(Map.of(Aspects.ENTROPY,500,Aspects.ORDER,500,Aspects.EARTH,100))&&item.getFocusColor(focus)==16757248&&!item.continuous(focus),"original base cost, color and single activation");
            check(focus.getMaxDamage()==1&&!item.isRepairable(focus)&&item.getEnchantmentValue()==5&&item.getPossibleUpgradesByRank(focus,1).equals(List.of(FocusUpgradeType.treasure)),"original durability, no repair, enchantability and inherited treasure-only upgrade");
            check(ThaumcraftApi.portableHoleBlackList.contains(Blocks.RED_BED)&&ThaumcraftApi.portableHoleBlackList.contains(Blocks.PISTON)&&ThaumcraftApi.portableHoleBlackList.contains(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:infernal_furnace"))),"public live blacklist is populated from original native block families");
            var log=Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X);level.setBlock(origin,log,2);level.setBlock(support,Blocks.STONE.defaultBlockState(),2);aim(player,origin);
            var cost=WandApi.quote(wand,player,item.getVisCost(focus));var capacity=WandApi.capacity(wand);wand.getItem().use(level,player,InteractionHand.MAIN_HAND);
            focus=WandApi.focus(wand);
            check(level.getBlockState(origin).is(Blocks.AIR)&&item.captured(focus,level.registryAccess()).orElseThrow().equals(log),"actual native wand activation captures the full oriented block state");
            check(WandApi.stored(wand,Aspects.ORDER)==capacity-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.ENTROPY)==capacity-cost.get(Aspects.ENTROPY)&&WandApi.stored(wand,Aspects.EARTH)==capacity-cost.get(Aspects.EARTH),"ordinary block spends the actual quoted three-aspect cost once");
            check(peer.effects().size()==8&&peer.effects().stream().allMatch(e->e.kind()==ParticleApi.Kind.BURST&&e.size()==.2f),"capture requests eight original FXBurst particles");peer.fixture.packets.clear();
            check(item.getSortingHelper(focus).equals("DISLOCATIONblock.minecraft.oak_log-0"),"captured-block selector key uses the original tagless-stack hash rule");
            var originalData=ItemData.read(focus);check(originalData.getString("blockName").equals("minecraft:oak_log")&&originalData.contains("tileCmp",10)&&originalData.getBoolean("available"),"original storage names and availability flag are retained");
            wand=ItemStack.parse(level.registryAccess(),wand.save(level.registryAccess())).orElseThrow();player.setItemInHand(InteractionHand.MAIN_HAND,wand);
            check(item.captured(WandApi.focus(wand),level.registryAccess()).orElseThrow().equals(log),"full block state survives actual nested focus/wand serialization");
            for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);aim(player,support);
            check(item.cast(player,wand,WandApi.focus(wand))&&level.getBlockState(destination).equals(log),"captured block places against the clicked face with no available vis");
            check(item.captured(WandApi.focus(wand),level.registryAccess()).isEmpty()&&!ItemData.read(WandApi.focus(wand)).getBoolean("available"),"successful placement clears availability");
            check(ItemData.read(WandApi.focus(wand)).getString("blockName").equals("minecraft:oak_log")&&peer.effects().size()==8,"original stale capture data is retained and placement emits eight bursts");peer.fixture.packets.clear();
            level.setBlock(destination,Blocks.AIR.defaultBlockState(),2);
            var chest=Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST);level.setBlock(origin,chest,2);var chestTile=(ChestBlockEntity)level.getBlockEntity(origin);chestTile.setItem(0,new ItemStack(Items.DIAMOND,3));var chestData=chestTile.saveWithFullMetadata(level.registryAccess());chestData.putString("CustomName",net.minecraft.network.chat.Component.Serializer.toJson(net.minecraft.network.chat.Component.literal("Payload"),level.registryAccess()));chestTile.loadWithComponents(chestData,level.registryAccess());
            fill(wand);cost=WandApi.quote(wand,player,Map.of(Aspects.ENTROPY,2500,Aspects.ORDER,2500,Aspects.EARTH,500));aim(player,origin);
            check(item.cast(player,wand,WandApi.focus(wand))&&level.getBlockEntity(origin)==null,"block-entity capture removes the real source entity");
            check(WandApi.stored(wand,Aspects.ORDER)==capacity-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.EARTH)==capacity-cost.get(Aspects.EARTH),"block entity uses the original 2500/2500/500 price");
            check(level.getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(2)).isEmpty(),"removing the source block entity before its block prevents inventory drops");
            aim(player,support);check(item.cast(player,wand,WandApi.focus(wand)),"captured chest places at its new position");var restored=(ChestBlockEntity)level.getBlockEntity(destination);
            check(level.getBlockState(destination).equals(chest)&&restored.getItem(0).is(Items.DIAMOND)&&restored.getItem(0).getCount()==3&&restored.getCustomName().getString().equals("Payload"),"actual chest keeps orientation, inventory and custom name");
            check(restored.getBlockPos().equals(destination)&&restored.saveWithFullMetadata(level.registryAccess()).getInt("x")==destination.getX(),"restored tile uses destination coordinates in runtime and NBT");
            restored.clearContent();level.setBlock(destination,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(origin,Blocks.SPAWNER.defaultBlockState(),2);var spawnerTag=level.getBlockEntity(origin).saveWithFullMetadata(level.registryAccess());fill(wand);cost=WandApi.quote(wand,player,Map.of(Aspects.ENTROPY,10000,Aspects.ORDER,10000,Aspects.EARTH,5000));aim(player,origin);
            check(item.cast(player,wand,WandApi.focus(wand)),"mob spawner can be captured through the real wand focus data");
            check(WandApi.stored(wand,Aspects.ORDER)==capacity-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.EARTH)==capacity-cost.get(Aspects.EARTH),"mob spawner uses the original 10000/10000/5000 price");
            aim(player,support);check(item.cast(player,wand,WandApi.focus(wand))&&level.getBlockEntity(destination) instanceof SpawnerBlockEntity,"spawner creates its actual native block entity after placement");
            check(level.getBlockEntity(destination).saveWithFullMetadata(level.registryAccess()).getCompound("SpawnData").equals(spawnerTag.getCompound("SpawnData")),"spawner payload survives movement");
            level.setBlock(destination,Blocks.AIR.defaultBlockState(),2);
            for(var forbidden:List.of(Blocks.BEDROCK,Blocks.PISTON,Blocks.PISTON_HEAD)){
                level.setBlock(origin,forbidden.defaultBlockState(),2);aim(player,origin);fill(wand);
                check(!item.cast(player,wand,WandApi.focus(wand))&&level.getBlockState(origin).is(forbidden)&&WandApi.stored(wand,Aspects.ORDER)==capacity,"forbidden or unbreakable block is unchanged and free: "+forbidden);
            }
            level.setBlock(origin,Blocks.STONE.defaultBlockState(),2);ThaumcraftApi.portableHoleBlackList.add(Blocks.STONE);aim(player,origin);
            check(!item.cast(player,wand,WandApi.focus(wand))&&level.getBlockState(origin).is(Blocks.STONE),"addon sees live public blacklist additions");ThaumcraftApi.portableHoleBlackList.remove(Blocks.STONE);
            for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);
            check(!item.cast(player,wand,WandApi.focus(wand))&&level.getBlockState(origin).is(Blocks.STONE),"unaffordable capture neither removes nor stores its target");
            fill(wand);level.setBlock(origin,Blocks.WATER.defaultBlockState(),2);aim(player,origin);
            check(item.cast(player,wand,WandApi.focus(wand))&&item.captured(WandApi.focus(wand),level.registryAccess()).orElseThrow().is(Blocks.WATER),"original source-fluid ray can capture a block without a native BlockItem");
            aim(player,support);check(item.cast(player,wand,WandApi.focus(wand))&&level.getBlockState(destination).is(Blocks.WATER),"captured source fluid places without inventing an item");level.setBlock(destination,Blocks.AIR.defaultBlockState(),2);
            var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("focus_dislocation")).orElseThrow().value();
            check(recipe.instability()==8&&recipe.aspects().equals(Map.of("alienis",20,"tenebrae",10,"vacuos",25,"praecantatio",20,"vitium",5))&&recipe.components().size()==8,"original dislocation infusion costs and eight ordered components");
            var amber=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:amber")));
            check(recipe.components().subList(4,7).stream().allMatch(i->i.test(amber)),"original resource metadata 6 means three amber items");
            check(ResearchApi.exists(player,"thaumictinkerer:focus_dislocation")==thaumcraft.api.world.ContentRules.mirrorsEnabled(),"actual dislocation research follows the mirrors startup setting while its recipe remains loaded");
            var research=AddonRegistry.research().get("thaumictinkerer:focus_dislocation");check(research.secondary()&&research.concealed()&&research.column()==-5&&research.row()==-5&&research.pageOrder().size()==3&&research.hiddenParents().equals(List.of("INFUSION")),"original concealed secondary research, automatic INFUSION prerequisite and two text pages before its recipe");
            System.out.println("TT_DISLOCATION_PASS checks="+checks);helper.succeed();
        }finally{
            ThaumcraftApi.portableHoleBlackList.remove(Blocks.STONE);FocusRulesTest.stop(player,wand);peer.close();
            for(var pos:changed){if(level.getBlockEntity(pos) instanceof Container container)container.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);}
            level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(origin),Vec3.atLowerCornerOf(support)).inflate(4)).forEach(Entity::discard);
        }
    }
}
