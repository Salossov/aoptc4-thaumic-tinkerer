package thaumic.tinkerer.verification;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.world.BlockMaterials;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class MaterialClassificationRulesTest {
    private static void material(String name,TagKey<Block> material,boolean noTool) {
        var id=ResourceLocation.parse(name);
        if(!BuiltInRegistries.BLOCK.containsKey(id))throw new IllegalStateException("Missing block: "+name);
        var state=BuiltInRegistries.BLOCK.get(id).defaultBlockState();
        if(!state.is(material)||state.is(BlockMaterials.TOOL_NOT_REQUIRED)!=noTool)
            throw new IllegalStateException("Source material / tool requirement: "+name);
    }
    @GameTest(template="empty",batch="material_classification",timeoutTicks=100)
    public static void sourceMaterialsAndAddonContributions(GameTestHelper helper) {
        VerificationReports.install();
        material("minecraft:stone",BlockMaterials.ROCK,false);
        material("minecraft:infested_stone",BlockMaterials.CLAY,true);
        material("minecraft:glass",BlockMaterials.GLASS,true);
        material("minecraft:packed_ice",BlockMaterials.PACKED_ICE,true);
        material("minecraft:snow",BlockMaterials.SNOW,false);
        material("minecraft:snow_block",BlockMaterials.CRAFTED_SNOW,false);
        material("minecraft:oak_log",BlockMaterials.WOOD,true);
        material("minecraft:sticky_piston",BlockMaterials.PISTON,true);
        material("thaumcraft:warded_glass",BlockMaterials.ROCK,false);
        material("thaumcraft:air_crystal_cluster",BlockMaterials.GLASS,true);
        material("thaumcraft:loot_crate",BlockMaterials.WOOD,true);
        // The custom original material subclasses keep requiresNoTool=true.
        for(var name:java.util.List.of("thaumcraft:nitor","thaumcraft:tainted_soil","thaumcraft:fibrous_taint")) {
            var state=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name)).defaultBlockState();
            if(!state.is(BlockMaterials.TOOL_NOT_REQUIRED))throw new IllegalStateException("Custom source material: "+name);
        }
        material("thaumictinkerer:platform",BlockMaterials.WOOD,true);
        material("thaumictinkerer:repairer",BlockMaterials.IRON,false);
        material("thaumictinkerer:dark_quartz",BlockMaterials.ROCK,false);
        material("thaumictinkerer:forcefield",BlockMaterials.AIR,true);
        material("thaumictinkerer:infused_farmland",BlockMaterials.GROUND,true);
        material("thaumictinkerer:infused_grain_block",BlockMaterials.PLANTS,true);
        material("thaumictinkerer:legacy_dry_shrub",BlockMaterials.VINE,true);
        helper.succeed();
    }
    private MaterialClassificationRulesTest() {}
}
