package thaumic.tinkerer.verification;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import thaumic.tinkerer.fire.FireLegacyStates;

/** Native-state assertions called only after the real NeoForge test loader initializes. */
public final class FireIdentityRulesTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static void identity(BlockState state,String block,int meta){var actual=FireLegacyStates.key(state);check(actual.block().equals("minecraft:"+block)&&actual.meta()==meta&&actual.modern()==null,"old identity of "+state+": "+actual);}
    public static int verify(){
        checks=0;
        validatePatterns();
        identity(Blocks.WHITE_WOOL.defaultBlockState(),"wool",0);identity(Blocks.RED_WOOL.defaultBlockState(),"wool",14);
        identity(Blocks.OAK_LOG.defaultBlockState(),"log",0);identity(Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X),"log",5);
        identity(Blocks.OAK_WOOD.defaultBlockState(),"log",12);identity(Blocks.ACACIA_LOG.defaultBlockState(),"log2",0);identity(Blocks.DARK_OAK_LOG.defaultBlockState(),"log2",1);
        identity(Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,false).setValue(LeavesBlock.DISTANCE,7),"leaves",0);identity(Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),"leaves",5);
        identity(Blocks.CAULDRON.defaultBlockState(),"cauldron",0);identity(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3),"cauldron",3);
        identity(Blocks.MELON.defaultBlockState(),"melon_block",0);identity(Blocks.SPAWNER.defaultBlockState(),"mob_spawner",0);identity(Blocks.NETHER_PORTAL.defaultBlockState(),"portal",1);
        identity(Blocks.SAND.defaultBlockState(),"sand",0);identity(Blocks.RED_SAND.defaultBlockState(),"sand",1);
        identity(Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST).setValue(StairBlock.SHAPE,StairsShape.INNER_LEFT),"oak_stairs",0);
        identity(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP).setValue(SlabBlock.WATERLOGGED,true),"wooden_slab",8);
        identity(Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH,true),"fence",0);
        identity(Blocks.GRASS_BLOCK.defaultBlockState().setValue(BlockStateProperties.SNOWY,true),"grass",0);
        identity(Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,ChestType.LEFT),"chest",2);
        identity(Blocks.FIRE.defaultBlockState().setValue(FireBlock.AGE,5).setValue(FireBlock.NORTH,true),"fire",5);
        identity(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),"wheat",7);
        identity(Blocks.SHORT_GRASS.defaultBlockState(),"tallgrass",1);identity(Blocks.FERN.defaultBlockState(),"tallgrass",2);identity(Blocks.DEAD_BUSH.defaultBlockState(),"deadbush",0);
        identity(Blocks.WATER.defaultBlockState(),"water",0);
        check(FireLegacyStates.key(Blocks.COPPER_ORE.defaultBlockState()).modern()!=null,"new copper ore uses modern identity");
        check(FireLegacyStates.key(Blocks.GRANITE.defaultBlockState()).modern()!=null,"granite did not exist as a separate variant in 1.7.10");
        check(FireLegacyStates.key(Blocks.RED_WOOL.defaultBlockState()).zero().equals(FireLegacyStates.key(Blocks.WHITE_WOOL.defaultBlockState())),"query zero preserves the original shared wool block");
        return checks;
    }
    private static void validatePatterns(){
        var problems=new java.util.TreeSet<String>();
        try(var stream=java.util.Objects.requireNonNull(FireIdentityRulesTest.class.getResourceAsStream("/assets/thaumictinkerer/fire/legacy_states.json"));var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
            var entries=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("vanilla");
            for(var entry:entries){
                var pattern=entry.getAsJsonObject();var id=net.minecraft.resources.ResourceLocation.parse(pattern.get("block").getAsString());
                if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)){problems.add("missing block "+id);continue;}
                var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
                for(var property:pattern.getAsJsonObject("properties").entrySet()){
                    var nativeProperty=block.getStateDefinition().getProperty(property.getKey());
                    if(nativeProperty==null||nativeProperty.getValue(property.getValue().getAsString()).isEmpty())problems.add(id+" invalid property "+property.getKey()+"="+property.getValue().getAsString());
                }
            }
            check(problems.isEmpty(),"all extracted fire identities match actual modern native registry: "+problems);
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
}
