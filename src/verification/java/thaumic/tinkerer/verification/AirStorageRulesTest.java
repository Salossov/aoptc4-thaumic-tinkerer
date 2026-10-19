package thaumic.tinkerer.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.GasBlock;
import thaumic.tinkerer.block.GasContent;

/** Regression for air-material content being discarded before insertion into an empty chunk section. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
@net.neoforged.fml.common.EventBusSubscriber(modid = ThaumicTinkerer.ID)
public final class AirStorageRulesTest {
    private static net.minecraft.world.level.block.Block unmarkedAir;
    @net.neoforged.bus.api.SubscribeEvent
    public static void register(net.neoforged.neoforge.registries.RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            unmarkedAir = new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().air());
            helper.register(ThaumicTinkerer.id("test_unmarked_air"), unmarkedAir);
        });
    }
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void isolatedGases(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 120, 2));
        var chunk = level.getChunkAt(pos); var section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        try {
            check(section.hasOnlyAir(), "isolated empty section fixture");
            check(!level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 2) && section.hasOnlyAir(), "vanilla air insertion shortcut remains intact");
            check(!level.setBlock(pos, unmarkedAir.defaultBlockState(), 2) && level.getBlockState(pos).is(Blocks.AIR), "unmarked custom air keeps its existing insertion behavior");
            for (var gas : List.of(GasContent.LIGHT.get(), GasContent.SHADOW.get(), GasContent.NITOR.get())) {
                var state = gas.defaultBlockState().setValue(GasBlock.SPREAD, 1);
                check(gas instanceof thaumcraft.api.world.IPersistentAir && state.isAir() && !state.isEmpty(), "air behavior is independent of NeoForge's permission to discard a state");
                check(level.setBlock(pos, state, 2) && level.getBlockState(pos).equals(state), "first gas block is actually stored in an empty section: " + gas);
                check(!section.hasOnlyAir() && section.isRandomlyTickingBlocks(), "section preserves stored and random-tick counts");
                section.recalcBlockCounts();
                check(!section.hasOnlyAir() && section.isRandomlyTickingBlocks(), "palette count reconstruction retains persistent air-like gas");
                var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    section.write(buffer);
                    var received = new LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME)); received.read(buffer);
                    check(received.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15).equals(state) && !received.hasOnlyAir(), "actual section network codec preserves gas state and nonempty count");
                } finally { buffer.release(); }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                check(section.hasOnlyAir() && !section.isRandomlyTickingBlocks(), "removal returns section to genuinely empty state");
            }
            System.out.println("TT_ISOLATED_GASES_PASS checks=" + checks); helper.succeed();
        } finally { level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); }
    }
}
