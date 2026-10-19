package thaumic.tinkerer.kami.bedrock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;

public final class BedrockContent {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,ThaumicTinkerer.id("bedrock"));
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,ThaumicTinkerer.ID);
    public static void register(IEventBus bus){GENERATORS.register("bedrock",()->BedrockChunkGenerator.CODEC);GENERATORS.register(bus);}
    private BedrockContent(){}
}
