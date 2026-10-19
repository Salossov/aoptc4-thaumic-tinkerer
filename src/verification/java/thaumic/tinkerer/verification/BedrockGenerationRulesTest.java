package thaumic.tinkerer.verification;

import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.bedrock.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BedrockGenerationRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="bedrock_generation",timeoutTicks=100)
    public static void nativeSlabHeightmapsAndGeneratorCodec(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var generator=new BedrockChunkGenerator(registry.getHolderOrThrow(Biomes.BEACH));
        var bounds=LevelHeightAccessor.create(0,256);var chunk=new ProtoChunk(new ChunkPos(-1,2),UpgradeData.EMPTY,bounds,registry,null);
        // Actual native ProtoChunk; no loaded world terrain is replaced here.
        check(generator.fillFromNoise(null,null,null,chunk).getNow(null)==chunk,"native slab generation completes without waiting");
        var pos=new BlockPos.MutableBlockPos();
        for(int y=0;y<256;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++)check(chunk.getBlockState(pos.set(x,y,z)).is(Blocks.BEDROCK),"all65536 source slab voxels");
        check(chunk.getBlockState(pos.set(0,-1,0)).isAir()&&chunk.getBlockState(pos.set(0,256,0)).isAir(),"exact source height bounds");
        check(chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).getFirstAvailable(0,0)==256,"native source surface heightmap");
        check(generator.getSpawnHeight(bounds)==1&&generator.getMinY()==0&&generator.getGenDepth()==256,"source generator dimensions");
        var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var encoded=ChunkGenerator.CODEC.encodeStart(ops,generator).getOrThrow();
        var decoded=ChunkGenerator.CODEC.parse(ops,encoded).getOrThrow();check(decoded instanceof BedrockChunkGenerator,"registered native generator codec roundtrip");
        var bedrock=level.getServer().getLevel(BedrockContent.DIMENSION);
        if(TinkererConfig.KAMI_FORCE_ENABLED.get()&&TinkererConfig.KAMI_BEDROCK_ENABLED.get())check(bedrock!=null&&bedrock.getMinBuildHeight()==0&&bedrock.getHeight()==256,"conditional dimension resources loaded with approved saved key");
        else check(bedrock==null,"disabled KAMI/bedrock omits dimension");
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="bedrock_generation",timeoutTicks=100)
    public static void nativeOreTagsPreferredBlocksAndWeightedBoundaries(GameTestHelper helper){
        VerificationReports.install();var groups=BedrockOres.groups();check(groups.size()==33,"all original ordered ore groups loaded");
        check(groups.stream().mapToInt(BedrockOres.Group::weight).sum()==10671,"all original ore weights, once per group");
        check(groups.getFirst().name().equals("oreAluminum")&&groups.getLast().name().equals("oreVinteum"),"source enum ordering");
        var pool=BedrockOres.resolve(helper.getLevel());check(pool.totalWeight()>0,"native vanilla/core ore tags resolve");
        var iron=pool.entries().stream().filter(ore->ore.state().is(Blocks.IRON_ORE)).findFirst().orElseThrow();check(iron.weight()==1503,"preferred original iron, not first deepslate tag member");
        int start=0;
        for(var ore:pool.entries()){
            final int boundary=start;
            var first=pool.draw(new Random(){@Override public int nextInt(int bound){check(bound==pool.totalWeight(),"single total-weight draw");return boundary;}});
            final int end=start+ore.weight()-1;
            var last=pool.draw(new Random(){@Override public int nextInt(int bound){return end;}});
            check(first==ore.state()&&last==ore.state(),"strict original negative boundary, first and last roll of each interval");start+=ore.weight();
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="bedrock_generation",timeoutTicks=100)
    @SuppressWarnings("unchecked")
    public static void crossLevelPreparationKeepsOriginalOwnerAndExpires(GameTestHelper helper)throws ReflectiveOperationException {
        VerificationReports.install();var level=helper.getLevel();var destination=level.getServer().getLevel(BedrockContent.DIMENSION);
        if(destination==null){helper.succeed();return;}
        var pos=helper.absolutePos(new BlockPos(8,4,8));level.setBlockAndUpdate(pos,thaumic.tinkerer.block.AnimationTabletContent.BLOCK.get().defaultBlockState());
        var owner=level.getBlockEntity(pos);check(owner!=null,"actual registered owner stays in source level");
        var column=new ChunkPos(20000,-20000);
        try(var request=thaumcraft.api.world.ChunkPreparationApi.prepare(owner,destination,List.of(column,column))){
            check(request.total()==1&&!request.closed(),"cross-level request deduplicates destination columns");
            boolean hot=destination.getChunkSource().getChunkNow(column.x,column.z)!=null;
            check(request.advance(0)==hot,"zero cold budget polls destination, not source world");
            check(request.submitted()==(hot?1:0),"no cold generation submitted by staged lifetime test");
            level.removeBlock(pos,false);check(!request.advance(0)&&request.closed(),"removing source owner expires destination preparation");
        }
        try(var fixture=new NativePlayerFixture(helper,"TTChunkCrossOwner",pos)){
            var player=fixture.player;var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            var index=(Map<UUID,net.minecraft.server.level.ServerPlayer>)field.get(level.getServer().getPlayerList());check(index.putIfAbsent(player.getUUID(),player)==null,"unique indexed native peer");
            try(var request=thaumcraft.api.world.ChunkPreparationApi.prepare(player,destination,List.of(column))){
                check(!request.closed(),"connected player owns preparation while still in original world");
                player.setServerLevel(destination);check(!request.advance(0)&&request.closed(),"changing owner dimension expires original request");
            }finally{player.setServerLevel(level);index.remove(player.getUUID(),player);}
        }
        helper.succeed();
    }
    private BedrockGenerationRulesTest(){}
}
