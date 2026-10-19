package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.entities.IControllableGolem;
import thaumic.tinkerer.ThaumicTinkerer;

/** Uses only the shipped public API and registry; no internal core classes. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class GolemApiRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void liveGolemConfiguration(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel();
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("thaumcraft:golem"));
        var entity = (LivingEntity)type.create(level);
        var restored = (LivingEntity)type.create(level);
        try {
            check(entity instanceof IControllableGolem, "real registered golem exposes the public control contract");
            var golem = (IControllableGolem)entity;
            check(golem.golemMaterial().equals("straw") && golem.core() == -1 && golem.decoration().isEmpty(), "actual native golem defaults");
            var home = helper.absolutePos(new BlockPos(2, 3, 2)).mutable();
            var expectedHome = home.immutable(); golem.setGolemHome(home, Direction.WEST); home.move(9, 0, 0);
            check(golem.home().equals(expectedHome) && golem.homeFace() == Direction.WEST, "public home mutation changes actual native fields and copies mutable positions");
            var saved = new CompoundTag(); entity.saveWithoutId(saved);
            check(BlockPos.of(saved.getLong("Home")).equals(expectedHome) && saved.getByte("HomeFace") == Direction.WEST.get3DDataValue(), "home mutation enters real entity persistence");
            var markerPos = new BlockPos.MutableBlockPos(4, -1, 7);
            var marker = new IControllableGolem.Marker(level.dimension(), markerPos, Direction.DOWN, -1); markerPos.move(100, 0, 0);
            var nether = new IControllableGolem.Marker(Level.NETHER, new BlockPos(-8, 30, 5), Direction.EAST, 15);
            var input = new ArrayList<>(List.of(marker, nether, marker)); golem.replaceGolemMarkers(input); input.clear();
            check(golem.golemMarkers().equals(List.of(marker, nether, marker)) && marker.position().equals(new BlockPos(4, -1, 7)), "marker order, duplicates, dimension, color, negative height and defensive copies");
            var snapshot = golem.golemMarkers(); boolean immutable = false;
            try { snapshot.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
            check(immutable, "returned marker snapshot cannot mutate the native list");
            golem.replaceGolemMarkers(List.of(nether));
            check(snapshot.equals(List.of(marker, nether, marker)) && golem.golemMarkers().equals(List.of(nether)), "earlier snapshot remains independent after a replacement");
            var invalid = new ArrayList<IControllableGolem.Marker>(); invalid.add(marker); invalid.add(null); boolean rejected = false;
            try { golem.replaceGolemMarkers(invalid); } catch (NullPointerException expected) { rejected = true; }
            check(rejected && golem.golemMarkers().equals(List.of(nether)), "invalid marker replacement is atomic");
            boolean invalidHome = false;
            try { golem.setGolemHome(BlockPos.ZERO, null); } catch (NullPointerException expected) { invalidHome = true; }
            check(invalidHome && golem.home().equals(expectedHome), "invalid home mutation does not partially alter home");
            boolean wrongThread = CompletableFuture.supplyAsync(() -> {
                try { golem.setGolemHome(BlockPos.ZERO, Direction.UP); return false; } catch (IllegalStateException expected) { return true; }
            }).join();
            check(wrongThread && golem.home().equals(expectedHome), "off-thread mutation is rejected before changing live AI configuration");
            golem.replaceGolemMarkers(List.of(marker, nether, marker)); entity.saveWithoutId(saved); restored.load(saved);
            var loaded = (IControllableGolem)restored;
            check(loaded.home().equals(expectedHome) && loaded.homeFace() == Direction.WEST && loaded.golemMarkers().equals(golem.golemMarkers()), "home and ordered markers survive the actual entity save/load path");
            check(saved.getCompound("Configuration").getList("Markers", 10).size() == 3, "public API uses the existing native Configuration/Markers schema");
            golem.replaceGolemMarkers(List.of());
            check(golem.golemMarkers().isEmpty(), "clearing markers changes the same native configuration");
            System.out.println("TT_GOLEM_API_PASS checks=" + checks); helper.succeed();
        } finally { if (entity != null) entity.discard(); if (restored != null) restored.discard(); }
    }
}
