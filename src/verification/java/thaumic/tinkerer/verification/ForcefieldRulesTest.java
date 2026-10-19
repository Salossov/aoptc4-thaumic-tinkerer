package thaumic.tinkerer.verification;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.client.ParticleApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.ForcefieldBlockEntity;
import thaumic.tinkerer.block.ForcefieldContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ForcefieldRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void forcefield(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 70, 2));
        var particles = new ArrayList<ParticleApi.BlockSparkle>();
        ParticleApi.installBlockSparkles((source, effect) -> { if (source == level && effect.pos().equals(pos)) particles.add(effect); });
        try {
            var block = ForcefieldContent.BLOCK.get(); level.setBlock(pos, block.defaultBlockState(), 3);
            var state = level.getBlockState(pos); var tile = (ForcefieldBlockEntity)level.getBlockEntity(pos);
            check(tile != null && tile.ticks == 60, "original fresh field lifetime counter");
            check(state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE && !state.canOcclude(), "field is invisible and nonopaque");
            check(state.getCollisionShape(level, pos).bounds().equals(new AABB(0, 0, 0, 1, 1, 1)), "original inherited full collision cube");
            check(!level.noCollision(new AABB(pos).deflate(.01)), "actual world collision query sees an isolated field in an otherwise empty section");
            check(state.getDestroySpeed(level, pos) == 0 && block.getExplosionResistance() == 0, "original zero hardness and resistance");
            var ticker = block.getTicker(level, state, ForcefieldContent.TILE.get());
            check(ticker != null, "block connects the real ticker");
            for (int tick = 0; tick < 60; tick++) ticker.tick(level, pos, state, tile);
            check(tile.ticks == 0 && level.getBlockState(pos).is(block), "field remains after sixty updates");
            var saved = tile.saveWithFullMetadata(level.registryAccess()); var restored = new ForcefieldBlockEntity(pos, state); restored.loadWithComponents(saved, level.registryAccess());
            check(!saved.contains("ticks") && restored.ticks == 60, "original timer is not serialized and reload starts at sixty");
            ticker.tick(level, pos, state, tile);
            check(tile.ticks == -1 && level.getBlockState(pos).is(block), "sixty-first update only decrements zero to minus one");
            ticker.tick(level, pos, state, tile);
            check(level.getBlockState(pos).is(Blocks.AIR) && tile.ticks == -2, "sixty-second update removes the field before decrementing again");
            check(particles.size() == 62 && particles.stream().allMatch(effect -> effect.color() == 255 && effect.count() == 1), "original blue RGB particle request occurs on every tick including removal");
            System.out.println("TT_FORCEFIELD_PASS checks=" + checks); helper.succeed();
        } finally {
            ParticleApi.installBlockSparkles((source, effect) -> { }); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
