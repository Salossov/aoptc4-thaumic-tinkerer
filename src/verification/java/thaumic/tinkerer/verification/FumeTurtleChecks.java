package thaumic.tinkerer.verification;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import dan200.computercraft.api.turtle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.GasContent;
import thaumic.tinkerer.compat.FumeTurtleUpgrade;

final class FumeTurtleChecks {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    static void run(ServerLevel level, BlockPos origin) {
        checks = 0;
        var tool = level.registryAccess().registryOrThrow(ITurtleUpgrade.REGISTRY).get(ThaumicTinkerer.id("gas_remover"));
        check(tool == FumeTurtleUpgrade.INSTANCE && tool.getType() == FumeTurtleUpgrade.TYPE.get(), "real CC data registry decodes the registered upgrade codec");
        check(tool.getUpgradeType() == TurtleUpgradeType.TOOL && tool.getCraftingItem().is(GasContent.REMOVER.get()), "original fume tool type and crafting item");
        check(tool.getAdjective().getString().length() > 0, "upgrade has an adjective component");
        var center = origin.offset(4, 35, 4);
        var turtle = (ITurtleAccess)Proxy.newProxyInstance(ITurtleAccess.class.getClassLoader(), new Class<?>[]{ITurtleAccess.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLevel" -> level;
            case "getPosition" -> center;
            default -> throw new AssertionError("Fume tool unexpectedly uses " + method.getName());
        });
        check(tool.createPeripheral(turtle, TurtleSide.LEFT) == null, "original active FumeTool has no peripheral");
        var positions = new ArrayList<BlockPos>();
        try {
            var low = center.offset(-3, -3, -3); var high = center.offset(2, 2, 2); var outside = center.offset(3, 0, 0); var solid = center.east();
            positions.addAll(java.util.List.of(low, high, outside, solid));
            level.setBlock(low, GasContent.LIGHT.get().defaultBlockState(), 2);
            level.setBlock(high, GasContent.SHADOW.get().defaultBlockState(), 2);
            level.setBlock(outside, GasContent.LIGHT.get().defaultBlockState(), 2);
            level.setBlock(solid, Blocks.STONE.defaultBlockState(), 2);
            check(!tool.useTool(turtle, TurtleSide.LEFT, TurtleVerb.ATTACK, Direction.DOWN).isSuccess() && level.getBlockState(low).is(GasContent.LIGHT.get()), "attack fails without removing gas");
            check(tool.useTool(turtle, TurtleSide.RIGHT, TurtleVerb.DIG, Direction.UP).isSuccess(), "dig succeeds for either mounting side and direction");
            check(level.getBlockState(low).is(Blocks.AIR) && level.getBlockState(high).is(Blocks.AIR), "both inclusive corners of the original asymmetric six-block range clear");
            check(level.getBlockState(outside).is(GasContent.LIGHT.get()) && level.getBlockState(solid).is(Blocks.STONE), "positive third coordinate and solid blocks remain");
            check(tool.useTool(turtle, TurtleSide.LEFT, TurtleVerb.DIG, Direction.NORTH).isSuccess(), "dig still succeeds without any gas in range");
            System.out.println("TT_FUME_TURTLE_PASS checks=" + checks);
        } finally { for (var pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
    }
}
