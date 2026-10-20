package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumic.tinkerer.*;
import thaumic.tinkerer.fire.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ImbuedFireRulesTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    @GameTest(template="empty",timeoutTicks=80)
    public static void imbuedFire(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+280,(anchor.getZ()>>4)*16+8);
        var changed=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(pos.offset(-3,-2,-3),pos.offset(3,3,3)))changed.add(p.immutable());
        boolean fireTicks=level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,level.getServer());
        try{
            checks+=FireIdentityRulesTest.verify();
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            for(var kind:ImbuedFireKind.values()){
                var block=ImbuedFireContent.BLOCKS_BY_KIND.get(kind).get();level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(pos,block.defaultBlockState(),2);
                check(block.tickDelay()==(kind==ImbuedFireKind.PERDITIO?1:200),"original tick delay "+kind);
                check(level.getBlockState(pos).getLightEmission(level,pos)==0&&level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos).getShape(level,pos).isEmpty(),"original non-luminous, non-colliding, non-targetable fire "+kind);
                check(level.getBlockState(pos).isRandomlyTicking(),"legacy random ticks remain enabled "+kind);
                check(Block.getDrops(level.getBlockState(pos),level,pos,null).isEmpty(),"fire drops no item "+kind);
                level.getBlockState(pos).tick(level,pos,level.random);check(level.getBlockState(pos).is(block),"disabled doFireTick freezes existing fire "+kind);
                var id=ThaumicTinkerer.id(kind.path());var recipe=level.getRecipeManager().byKey(id);
                if(TinkererConfig.IMBUED_FIRE.get()){
                    check(recipe.isPresent()&&recipe.get().value() instanceof CrucibleRecipe,"native crucible recipe exists "+kind);
                    var crucible=(CrucibleRecipe)recipe.orElseThrow().value();var cost=kind==ImbuedFireKind.IGNIS?Map.of("ignis",10,"aer",5):Map.of("ignis",5,"praecantatio",5,kind.tag,5);
                    check(crucible.aspects().equals(cost)&&crucible.getRecipeOutput().is(block.asItem()),"extracted cost and output including ignis air quirk "+kind);
                    var research=AddonRegistry.research().get(id.toString());check(research!=null&&research.complexity()==kind.complexity&&research.column()==kind.x&&research.row()==kind.y&&research.parents().equals(List.of("thaumictinkerer:bright_nitor")),"registered original research coordinates and parent "+kind);
                    check(research.concealed()&&research.secondary()&&research.hiddenParents().isEmpty(),"concealed secondary without invented infusion prerequisite "+kind);
                }else check(recipe.isEmpty()&&!AddonRegistry.research().containsKey(id.toString()),"disabled fire retains blocks but removes recipes and research "+kind);
            }
            check(AddonRegistry.research().containsKey("thaumictinkerer:infused_potions"),"crop book is now connected to its six real fire dependencies");
            var aer=ImbuedFireContent.BLOCKS_BY_KIND.get(ImbuedFireKind.AER).get();var terra=ImbuedFireContent.BLOCKS_BY_KIND.get(ImbuedFireKind.TERRA).get();var ignis=ImbuedFireContent.BLOCKS_BY_KIND.get(ImbuedFireKind.IGNIS).get();var aqua=ImbuedFireContent.BLOCKS_BY_KIND.get(ImbuedFireKind.AQUA).get();
            check(aer.transformation(level,pos,Blocks.BIRCH_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X)).is(Blocks.SAND),"aer ignores old log metadata and axis");
            check(aer.transformation(level,pos,Blocks.ACACIA_LEAVES.defaultBlockState()).is(Blocks.SANDSTONE),"second original leaf block maps to sandstone");
            check(aer.transformation(level,pos,Blocks.CHERRY_LOG.defaultBlockState())==null,"modern wood is not silently added to old static transformation table");
            check(aer.transformation(level,pos,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,3))==null,"old stationary water identity excludes flowing water");
            check((aer.transformation(level,pos,Blocks.WATER.defaultBlockState())!=null)==TinkererConfig.IMBUED_FIRE_CAKE.get(),"original cake config controls stationary water target");
            check(ignis.transformation(level,pos,Blocks.GOLD_BLOCK.defaultBlockState()).is(Blocks.NETHER_QUARTZ_ORE)&&ignis.transformation(level,pos,Blocks.GOLD_ORE.defaultBlockState())==null,"release gold block, rather than gold ore, becomes quartz ore");
            check(ignis.transformation(level,pos,Blocks.DANDELION.defaultBlockState()).is(Blocks.DANDELION),"release dandelion self-transformation remains a target");
            check(terra.transformation(level,pos,Blocks.SPAWNER.defaultBlockState()).is(Blocks.IRON_BLOCK)&&terra.transformation(level,pos,Blocks.NETHER_BRICK_STAIRS.defaultBlockState()).is(Blocks.OAK_STAIRS),"earth spawner and stairs transformations");
            check(terra.transformation(level,pos,Blocks.NETHER_BRICK_STAIRS.defaultBlockState()).getValue(StairBlock.FACING)==Direction.EAST,"old stair metadata zero faces east");
            check(terra.transformation(level,pos,Blocks.SNOW.defaultBlockState()).is(ImbuedFireContent.DRY_SHRUB.get())&&!ImbuedFireContent.DRY_SHRUB.get().isValidBonemealTarget(level,pos,ImbuedFireContent.DRY_SHRUB.get().defaultBlockState()),"snow layers create old non-bonemealable tallgrass metadata zero, rather than a modern stick-dropping dead bush");
            check(aqua.transformation(level,pos,Blocks.NETHERRACK.defaultBlockState()).is(Blocks.SNOW_BLOCK)&&aqua.transformation(level,pos,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,7)).is(Blocks.OBSIDIAN),"water creates full snow block and converts flowing lava");
            var chaos=ImbuedFireContent.BLOCKS_BY_KIND.get(ImbuedFireKind.PERDITIO).get();for(var kind:ImbuedFireKind.values())if(kind!=ImbuedFireKind.PERDITIO)check(chaos.transformation(level,pos,ImbuedFireContent.BLOCKS_BY_KIND.get(kind).get().defaultBlockState().setValue(FireBlock.AGE,12)).is(Blocks.FIRE),"chaos accepts every age of other imbued fire "+kind);
            check(chaos.transformation(level,pos,chaos.defaultBlockState())==null,"chaos does not target itself");
            level.setBlock(pos,Blocks.OAK_LOG.defaultBlockState(),2);aer.setWithTransmutation(level,pos.getX(),pos.getZ(),pos.getY(),7,aer);check(level.getBlockState(pos).is(Blocks.SAND),"historical helper swaps Y/Z and transforms its destination");
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);aer.setWithTransmutation(level,pos.getX(),pos.getZ(),pos.getY(),7,aer);check(level.getBlockState(pos).is(aer)&&level.getBlockState(pos).getValue(FireBlock.AGE)==7,"non-target helper writes supplied age at swapped destination");
            check(FireLegacyStates.key(Blocks.RED_WOOL.defaultBlockState()).meta()==14&&FireLegacyStates.sameLegacyBlock(Blocks.RED_WOOL.defaultBlockState(),Blocks.WHITE_WOOL.defaultBlockState()),"flattened wool retains original shared block identity and metadata");
            check(FireLegacyStates.key(Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X)).meta()==5,"original log type and axis combine to metadata five");
            check(FireLegacyStates.key(Blocks.COPPER_ORE.defaultBlockState()).modern()!=null,"a new vanilla block uses modern state identity");
            // Original TileEnchanter accepts cosmetic-solid metadata 0 for its
            // totem pillars; metadata 1 is the ordinary obsidian tile cube.
            var totem=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:obsidian_totem")).defaultBlockState();
            var obsidianTile=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:obsidian_tile")).defaultBlockState();
            check(FireLegacyStates.key(totem).meta()==0&&FireLegacyStates.key(obsidianTile).meta()==1,"source totem and tile metadata are not reversed");
            check(FireLegacyStates.isMetadataZero(totem)&&!FireLegacyStates.isMetadataZero(obsidianTile)&&FireLegacyStates.matchesResult(totem,obsidianTile)&&!FireLegacyStates.matchesResult(obsidianTile,totem),"order fire preserves the source metadata-zero restriction for cosmetic stone");
            var airStone=BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:air_infused_stone"));
            check(FireLegacyStates.key(airStone.defaultBlockState()).meta()==1&&FireLegacyStates.key(airStone.defaultBlockState()).zero().meta()==0,"core flattened infused stone preserves old metadata restriction");
            level.setBlock(pos,Blocks.RED_WOOL.defaultBlockState(),2);level.setBlock(pos.east(),Blocks.WHITE_WOOL.defaultBlockState(),2);
            check(OrderFireRules.transformation(level,pos,Blocks.RED_WOOL.defaultBlockState())==null,"order caches actual colored metadata but initially queries only metadata zero");
            check(OrderFireRules.transformation(level,pos.east(),Blocks.WHITE_WOOL.defaultBlockState()).is(Blocks.GOLD_BLOCK),"real nine-slot fixture recipe computes metadata-zero packing result");
            check(OrderFireRules.transformation(level,pos,Blocks.RED_WOOL.defaultBlockState()).is(Blocks.GOLD_BLOCK),"old shared metadata-zero cache result overrides colored variant's own recipe");
            level.setBlock(pos,Blocks.DIAMOND_ORE.defaultBlockState(),2);check(OrderFireRules.transformation(level,pos,Blocks.DIAMOND_ORE.defaultBlockState()).is(Blocks.DIAMOND_BLOCK),"native diamond drops and native nine-item recipe drive order fire");
            for(var pair:java.util.List.of(java.util.Map.entry(Blocks.IRON_ORE,Blocks.IRON_BLOCK),java.util.Map.entry(Blocks.DEEPSLATE_GOLD_ORE,Blocks.GOLD_BLOCK),java.util.Map.entry(Blocks.COPPER_ORE,Blocks.COPPER_BLOCK),java.util.Map.entry(Blocks.RAW_IRON_BLOCK,Blocks.IRON_BLOCK),java.util.Map.entry(Blocks.RAW_GOLD_BLOCK,Blocks.GOLD_BLOCK),java.util.Map.entry(Blocks.RAW_COPPER_BLOCK,Blocks.COPPER_BLOCK))){
                level.setBlock(pos,pair.getKey().defaultBlockState(),2);
                check(java.util.Objects.equals(OrderFireRules.transformation(level,pos,level.getBlockState(pos)),pair.getValue().defaultBlockState()),"Ordo yields finished storage block: "+pair.getKey());
            }
            var amberOre=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:amber_ore"));
            var amberBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:amber_block"));
            level.setBlock(pos,amberOre.defaultBlockState(),2);check(OrderFireRules.transformation(level,pos,level.getBlockState(pos)).is(amberBlock),"amber ore becomes amber block");
            level.setBlock(pos,Blocks.DIAMOND_ORE.defaultBlockState(),2);
            check(OrderFireRules.decayChance(level,pos)==3,"single original drop has one-in-three transmutation chance");
            level.setBlock(pos,Blocks.REDSTONE_ORE.defaultBlockState(),2);check(OrderFireRules.decayChance(level,pos)==1,"merged native redstone stack expands to original three-or-more drop count");
            level.setBlock(pos,Blocks.GLASS.defaultBlockState(),2);check(OrderFireRules.transformation(level,pos,Blocks.GLASS.defaultBlockState())==null,"initial null blacklist survives later lookups");
            System.out.println("TT_IMBUED_FIRE_PASS checks="+checks);helper.succeed();
        }finally{for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(fireTicks,level.getServer());}
    }
}
