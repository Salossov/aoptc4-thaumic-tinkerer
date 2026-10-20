package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.crop.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfusedCropRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static InfusedCropBlockEntity plant(net.minecraft.server.level.ServerLevel level,BlockPos pos,Aspect aspect,int age){level.setBlock(pos.below(),InfusedContent.SOIL.get().defaultBlockState(),2);level.setBlock(pos,InfusedContent.CROP.get().getStateForAge(age),2);var tile=(InfusedCropBlockEntity)level.getBlockEntity(pos);tile.aspect=aspect;tile.primalTendencies=new AspectList();tile.changed();return tile;}
    private static List<ItemEntity> drops(net.minecraft.server.level.ServerLevel level,BlockPos pos){return level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void step(net.minecraft.server.level.ServerLevel level,BlockPos pos,int count){for(int i=0;i<count;i++){var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);BlockEntityTicker ticker=((EntityBlock)state.getBlock()).getTicker(level,state,tile.getType());ticker.tick(level,pos,state,tile);}}
    @GameTest(template="empty",timeoutTicks=100)
    public static void infusedCrops(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+260,(anchor.getZ()>>4)*16+8);var player=FocusRulesTest.player(helper,"[TTCrops]");player.setPos(pos.getCenter());player.getAbilities().instabuild=false;player.getAbilities().mayBuild=true;
        var changed=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(pos.offset(-4,-3,-4),pos.offset(4,5,4)))changed.add(p.immutable());
        try{
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            var blank=new ItemStack(InfusedContent.SEEDS.get());player.setItemInHand(InteractionHand.MAIN_HAND,blank);level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);var blankHit=new BlockHitResult(pos.below().getCenter(),Direction.UP,pos.below(),false);
            check(blank.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,blankHit))==InteractionResult.PASS&&blank.getCount()==1&&level.getBlockState(pos.below()).is(Blocks.FARMLAND)&&level.getBlockState(pos).isAir(),"approved missing-aspect seed does not plant, mutate soil or consume itself");
            var emptyCrop=plant(level,pos,null,0);step(level,pos,100);check(emptyCrop.aspect==null&&level.getBlockState(pos).getValue(InfusedCropBlock.AGE)==0,"approved missing-aspect crop pauses without throwing or inventing an aspect");level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);drops(level,pos).forEach(Entity::discard);
            var seed=InfusedSeedsItem.of(Aspects.AIR);InfusedSeedsItem.tendencies(seed,new AspectList().add(Aspects.WATER,9).add(Aspects.FIRE,0));seed.setCount(2);player.setItemInHand(InteractionHand.MAIN_HAND,seed);level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState(),2);level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            var hit=new BlockHitResult(pos.below().getCenter(),Direction.UP,pos.below(),false);check(seed.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction()&&seed.getCount()==1,"actual seed use replaces the occupied upper cell and consumes one as in source");
            var tile=(InfusedCropBlockEntity)level.getBlockEntity(pos);var soil=(InfusedFarmlandBlockEntity)level.getBlockEntity(pos.below());
            check(tile.aspect==Aspects.AIR&&tile.primalTendencies.getAmount(Aspects.WATER)==9&&tile.primalTendencies.size()==2&&tile.primalTendencies.getAmount(Aspects.FIRE)==0,"planted native tile retains main aspect, tendencies and discovered zero");
            check(level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).bounds().maxY==1&&level.getBlockState(pos).getShape(level,pos).bounds().maxY==.25&&level.getBlockState(pos).getCollisionShape(level,pos).isEmpty(),"source full soil cube, fixed quarter-block crop outline and no crop collision");
            for(var side:Direction.values())check(level.getBlockState(pos.below()).canSustainPlant(level,pos.below(),side,InfusedContent.CROP.get().defaultBlockState()).isTrue()&&level.getBlockState(pos.below()).canSustainPlant(level,pos.below(),side,Blocks.WHEAT.defaultBlockState()).isFalse(),"source farmland sustains only infused grain on "+side);
            soil.aspectList=new AspectList().add(Aspects.FIRE,2).add(Aspects.AIR,0);var saved=soil.saveWithFullMetadata(level.registryAccess());var restored=(InfusedFarmlandBlockEntity)BlockEntity.loadStatic(pos.below(),soil.getBlockState(),saved,level.registryAccess());
            check(restored.aspectList.size()==2&&restored.aspectList.getAmount(Aspects.FIRE)==2&&saved.getCompound("aspectList").getList("Aspects",10).size()==2,"actual native soil BlockEntity NBT uses source schema and retains zero");
            var loaded=(InfusedCropBlockEntity)BlockEntity.loadStatic(pos,tile.getBlockState(),tile.saveWithFullMetadata(level.registryAccess()),level.registryAccess());check(loaded.aspect==Aspects.AIR&&loaded.primalTendencies.size()==2&&loaded.primalTendencies.getAmount(Aspects.WATER)==9,"actual native crop BlockEntity NBT retains aspect and tendencies");
            check(!soil.doesContainerAccept(Aspects.FIRE)&&soil.addToContainer(Aspects.FIRE,1)==0&&!soil.takeFromContainer(Aspects.FIRE,1)&&!tile.doesContainerContainAmount(Aspects.AIR,1)&&tile.containerContains(Aspects.AIR)==0,"source pseudo-container methods remain non-storing stubs");
            soil.aspectList=new AspectList().add(Aspects.WATER,21).add(Aspects.AIR,0);soil.reduceSaturatedAspects();check(soil.aspectList.visSize()==20,"actual soil object enforces approved strict cap");
            var animal=new Cow(EntityType.COW,level);float health=animal.getHealth();InfusedContent.SOIL.get().fallOn(level,soil.getBlockState(),pos.below(),animal,100);check(animal.getHealth()==health&&level.getBlockState(pos.below()).is(InfusedContent.SOIL.get()),"source soil fall callback does not trample or inflict falling damage");animal.discard();
            for(int i=0;i<100;i++)soil.getBlockState().randomTick(level,pos.below(),level.random);check(level.getBlockState(pos.below()).is(InfusedContent.SOIL.get()),"source soil random tick never dries to dirt");
            var before=tile;tile.primalTendencies=new AspectList().add(Aspects.WATER,50);level.setBlock(pos.above(),Blocks.SEA_LANTERN.defaultBlockState(),2);step(level,pos,6000);
            check(level.getBlockState(pos).getValue(InfusedCropBlock.AGE)==7&&level.getBlockEntity(pos)==before&&tile.aspect==Aspects.AIR,"registered native ticker grows through all seven ages without replacing BlockEntity identity");
            level.setBlock(pos,InfusedContent.CROP.get().getStateForAge(0),2);tile.primalTendencies=new AspectList();soil.aspectList=new AspectList();drops(level,pos).forEach(Entity::discard);level.destroyBlock(pos,true,player);
            var outputs=drops(level,pos);check(outputs.size()==1&&outputs.getFirst().getItem().is(InfusedContent.SEEDS.get())&&InfusedSeedsItem.aspect(outputs.getFirst().getItem())==Aspects.AIR&&soil.aspectList.visSize()==0,"actual immature removal produces only the original seed, no soil or bonus loot");
            check(outputs.getFirst().getItem().getCount()==1&&outputs.getFirst().saveWithoutId(new net.minecraft.nbt.CompoundTag()).getShort("PickupDelay")==10,"source seed count and pickup delay ten");outputs.forEach(Entity::discard);
            tile=plant(level,pos,Aspects.AIR,7);soil=(InfusedFarmlandBlockEntity)level.getBlockEntity(pos.below());soil.aspectList=new AspectList().add(Aspects.FIRE,1);tile.primalTendencies.add(Aspects.FIRE,9);level.destroyBlock(pos,true,player);outputs=drops(level,pos);
            check(outputs.stream().anyMatch(e->e.getItem().is(InfusedContent.SEEDS.get())&&InfusedSeedsItem.aspect(e.getItem())==Aspects.LIGHT)&&outputs.stream().anyMatch(e->e.getItem().is(InfusedContent.FRUITS.getFirst().get()))&&outputs.size()==2,"actual mature harvest breeds lux seed but takes bonus fruit from original aer aspect");
            check(soil.aspectList.getAmount(Aspects.FIRE)==1&&soil.aspectList.getAmount(Aspects.AIR)==1,"mature harvest adds original crop aspect once to real soil");outputs.forEach(Entity::discard);
            tile=plant(level,pos,Aspects.FLIGHT,7);soil=(InfusedFarmlandBlockEntity)level.getBlockEntity(pos.below());level.destroyBlock(pos,true,player);outputs=drops(level,pos);check(outputs.size()==1&&InfusedSeedsItem.aspect(outputs.getFirst().getItem())==Aspects.FLIGHT&&soil.aspectList.getAmount(Aspects.FLIGHT)==1,"approved empty volatus pool retains seed and soil without bonus or exception");outputs.forEach(Entity::discard);
            tile=plant(level,pos,Aspects.LIGHT,7);var north=plant(level,pos.north(),Aspects.AIR,7);var south=plant(level,pos.south(),Aspects.WATER,7);for(int i=0;i<5;i++)tile.learn();check(tile.primalTendencies.getAmount(Aspects.AIR)==5&&tile.primalTendencies.getAmount(Aspects.WATER)==0,"source directional preference learns primal north only up to five");tile.learn();check(tile.primalTendencies.getAmount(Aspects.WATER)==1,"at primal limit, source continues to next neighbour");
            north.aspect=Aspects.LIGHT;north.primalTendencies=new AspectList();tile.primalTendencies=new AspectList();tile.learn();check(tile.primalTendencies.size()==0,"first empty compound neighbour blocks later primal neighbour");
            north.primalTendencies.add(Aspects.WATER,0);tile.learn();check(tile.primalTendencies.getAmount(Aspects.WATER)==1&&north.primalTendencies.size()==1&&north.primalTendencies.getAmount(Aspects.WATER)==0,"source zero-donor quirk adds one while failed reduce retains donor zero");tile.learn();check(tile.primalTendencies.getAmount(Aspects.WATER)==1,"failed compound comparison returns without trying later primal neighbour");
            north.primalTendencies=new AspectList().add(Aspects.FIRE,3);tile.primalTendencies=new AspectList().add(Aspects.FIRE,1);tile.learn();check(tile.primalTendencies.getAmount(Aspects.FIRE)==2&&north.primalTendencies.getAmount(Aspects.FIRE)==2,"compound neighbour transfers one selected tendency");
            var beasts=CropLoot.pool(level,Aspects.BEAST);check(beasts.entries()==24&&beasts.totalWeight()==24&&beasts.outputs().stream().map(s->thaumic.tinkerer.util.ItemData.integer(s,"LegacyEntityId")).collect(java.util.stream.Collectors.toSet()).size()==24,"native loot table retains all 24 distinct inert egg identities and weights");
            var minds=CropLoot.pool(level,Aspects.MIND);check(minds.entries()==27&&minds.totalWeight()==54&&minds.outputs().stream().filter(s->s.is(Items.ENCHANTED_BOOK)).count()==24,"original mind weights and 24-book snapshot exclude modern/new enchantments");
            check(CropLoot.pool(level,Aspects.FLESH).entries()==2&&CropLoot.pool(level,Aspects.FLESH).totalWeight()==5,"identical flesh stacks remain separate identity keys, weights four and one");
            check(CropLoot.pool(level,Aspects.TREE).entries()==35&&CropLoot.pool(level,Aspects.METAL).totalWeight()==110&&CropLoot.pool(level,Aspects.MAGIC).entries()==12,"source double string scans and fixed wildcard weights survive native group expansion");
            var essence=AspectApi.wispEssence(Aspects.FIRE);check(!essence.isEmpty()&&AspectApi.aspects(level,essence).getOrDefault("ignis",0)==2&&CropLoot.pool(level,Aspects.AURA).entries()==Aspect.all().size(),"native public essence factory and dynamic aura pool include two contained units per aspect");
            var seedOrder=List.of(Aspects.AIR,Aspects.FIRE,Aspects.EARTH,Aspects.WATER,Aspects.ORDER,Aspects.ENTROPY);
            for(int i=0;i<6;i++){var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("infused_seeds_"+i)).orElseThrow().value();check(recipe.instability()==5&&recipe.aspects().equals(Map.of("messis",32,"meto",32))&&recipe.input().test(new ItemStack(Items.WHEAT_SEEDS))&&recipe.components().size()==4&&InfusedSeedsItem.aspect(recipe.getRecipeOutput())==seedOrder.get(i),"native infusion recipe result components and exact source cost "+i);}
            var definition=InfusedResearch.definition();check(definition.concealed()&&!definition.secondary()&&definition.hiddenParents().equals(List.of("INFUSION"))&&definition.recipeVariants("thaumictinkerer:infused_seed_recipes").size()==4&&definition.textPages().size()==6&&definition.pageOrder().size()==11,"source concealed research has four cyclic seed variants despite six craftable recipes");
            System.out.println("TT_INFUSED_CROPS_PASS checks="+checks);helper.succeed();
        }finally{player.getInventory().clearContent();player.getAbilities().mayBuild=true;player.getAbilities().instabuild=false;for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);drops(level,pos).forEach(Entity::discard);}
    }
}
