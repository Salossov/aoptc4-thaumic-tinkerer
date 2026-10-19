package thaumic.tinkerer.kami.bedrock;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import thaumic.tinkerer.TinkererConfig;

/** The original full 256-block bedrock slab, without vanilla surface/features.
 * Decoration writes through the already-prepared WorldGenRegion, never through
 * a live ServerLevel chunk lookup or synchronous chunk generation.
 */
public final class BedrockChunkGenerator extends ChunkGenerator {
    public static final MapCodec<BedrockChunkGenerator> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(generator->generator.biome)).apply(instance,BedrockChunkGenerator::new));
    private final Holder<Biome> biome;
    public BedrockChunkGenerator(Holder<Biome> biome){super(new FixedBiomeSource(biome),ignored->BiomeGenerationSettings.EMPTY);this.biome=biome;}
    @Override protected MapCodec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> lookup,RandomState random,long seed){return ChunkGeneratorStructureState.createForFlat(random,seed,biomeSource,Stream.empty());}
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        var state=Blocks.BEDROCK.defaultBlockState();var pos=new BlockPos.MutableBlockPos();
        var floor=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);var surface=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for(int y=Math.max(0,chunk.getMinBuildHeight());y<Math.min(256,chunk.getMaxBuildHeight());y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            chunk.setBlockState(pos.set(x,y,z),state,false);floor.update(x,y,z,state);surface.update(x,y,z,state);
        }
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public void applyBiomeDecoration(WorldGenLevel region,ChunkAccess chunk,StructureManager structures){
        var ores=BedrockOres.resolve(region);
        // Approved empty-table adaptation: retain solid bedrock, no nextInt(0).
        if(ores.totalWeight()==0)return;
        var pos=new BlockPos.MutableBlockPos();
        BedrockOreShape.populate(region.getSeed(),chunk.getPos().x,chunk.getPos().z,TinkererConfig.KAMI_BEDROCK_DENSITY.get(),ores::draw,(ore,x,y,z)->{
            pos.set(x,y,z);
            if(!region.isOutsideBuildHeight(pos)&&region.getBlockState(pos).is(Blocks.BEDROCK))region.setBlock(pos,ore,2);
        });
    }
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving stage){}
    @Override public void spawnOriginalMobs(WorldGenRegion region){}
    @Override public int getSpawnHeight(LevelHeightAccessor level){return 1;}
    @Override public int getMinY(){return 0;}
    @Override public int getGenDepth(){return 256;}
    @Override public int getSeaLevel(){return 63;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor level,RandomState random){return level.getMaxBuildHeight()<=0||level.getMinBuildHeight()>=256?level.getMinBuildHeight():Math.min(256,level.getMaxBuildHeight());}
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor level,RandomState random){
        var states=new BlockState[level.getHeight()];for(int i=0;i<states.length;i++){int y=level.getMinBuildHeight()+i;states[i]=(y>=0&&y<256?Blocks.BEDROCK:Blocks.AIR).defaultBlockState();}
        return new NoiseColumn(level.getMinBuildHeight(),states);
    }
    @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos pos){}
}
