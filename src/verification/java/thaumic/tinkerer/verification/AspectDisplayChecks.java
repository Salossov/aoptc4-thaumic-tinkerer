package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.*;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.wands.WandApi;

final class AspectDisplayChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Object value(IDynamicPeripheral peripheral, int method, Object... args) throws LuaException {
        var task = new AtomicReference<LuaTask>();
        var result = peripheral.callMethod(null, next -> { task.set(next); return 83; }, method, new ObjectArguments(args));
        if (task.get() == null || result.getCallback() == null) throw new AssertionError("Display peripheral must yield");
        task.get().execute(); return result.getCallback().resume(new Object[]{"task_complete", 83L, true}).getResult()[0];
    }
    static int run(net.minecraft.server.level.ServerLevel level, BlockPos origin) throws LuaException {
        checks = 0; var changed = new ArrayList<BlockPos>();
        try {
            var jarPos = origin.offset(3, 20, 3); changed.add(jarPos);
            level.setBlock(jarPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:node_jar")).defaultBlockState(), 2);
            var jar = level.getBlockEntity(jarPos); var jarPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), jarPos, Direction.UP);
            check(jar instanceof IAspectDisplay && jarPeripheral != null && jarPeripheral.getType().equals("tt_aspectContainer"), "actual jarred node exposes original aspect-container peripheral through read-only API");
            var saved = jar.saveWithFullMetadata(level.registryAccess()); var vis = new CompoundTag(); vis.putInt("ignis", 17); vis.putInt("aer", 0); vis.putInt("lux", 9); saved.put("vis", vis); jar.loadWithComponents(saved, level.registryAccess());
            check(value(jarPeripheral, 0).equals(Map.of(1d, "aer", 2d, "ignis", 3d, "lux")), "jarred node lists original aspect tags in order, including a depleted zero entry");
            check(value(jarPeripheral, 1, "ignis").equals(17) && value(jarPeripheral, 1, "lux").equals(9), "node view preserves whole node units and compound aspects");
            ((IAspectDisplay)jar).displayedAspects().add(Aspects.FIRE, 100);
            check(value(jarPeripheral, 1, "ignis").equals(17), "display snapshots cannot mutate the live jarred node");
            var matrixPos = origin.offset(6, 20, 3); changed.add(matrixPos);
            level.setBlock(matrixPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:infusion_matrix")).defaultBlockState(), 2);
            var matrix = level.getBlockEntity(matrixPos); var matrixPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), matrixPos, Direction.UP);
            check(matrix instanceof IAspectDisplay && matrixPeripheral != null && matrixPeripheral.getType().equals("tt_aspectContainer"), "infusion matrix exposes original recipe-cost view through public API");
            var matrixTag = matrix.saveWithFullMetadata(level.registryAccess()); var remaining = new CompoundTag(); remaining.putInt("aer", 0); remaining.putInt("ignis", 7); matrixTag.put("RemainingEssentia", remaining); matrix.loadWithComponents(matrixTag, level.registryAccess());
            check(value(matrixPeripheral, 0).equals(Map.of(1d, "aer", 2d, "ignis")) && value(matrixPeripheral, 1, "ignis").equals(7), "matrix reports remaining ritual requirements with retained zero entries");
            matrix.loadWithComponents(matrix.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            check(value(matrixPeripheral, 1, "aer").equals(0) && ((Map<?, ?>)value(matrixPeripheral, 0)).size() == 2, "zero ritual entries survive the existing native save/load path");
            var energizedPos = origin.offset(9, 20, 3); changed.add(energizedPos);
            level.setBlock(energizedPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:energized_aura_node")).defaultBlockState(), 2);
            var energized = level.getBlockEntity(energizedPos); var energizedPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), energizedPos, Direction.UP);
            check(energized instanceof IAspectDisplay && energizedPeripheral != null && energizedPeripheral.getType().equals("tt_aspectContainer"), "energized node exposes original capacity view");
            check(((Map<?, ?>)value(energizedPeripheral, 0)).size() == 6 && value(energizedPeripheral, 1, "aer").equals(4), "default internal energized node exposes square-root capacity for six primals");
            check(((TileVisNode)energized).consumeVis(Aspects.AIR, 1) == 1 && value(energizedPeripheral, 1, "aer").equals(4), "energized peripheral reports original visBase capacity rather than spent per-tick budget");
            var pedestalPos = origin.offset(12, 20, 3); changed.add(pedestalPos);
            level.setBlock(pedestalPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:wand_recharge_pedestal")).defaultBlockState(), 2);
            var pedestal = (Container)level.getBlockEntity(pedestalPos); var pedestalPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), pedestalPos, Direction.UP);
            check(pedestal instanceof IAspectDisplay && pedestalPeripheral != null && pedestalPeripheral.getType().equals("tt_aspectContainer"), "recharge pedestal exposes original aspect view");
            check(((Map<?, ?>)value(pedestalPeripheral, 0)).isEmpty() && value(pedestalPeripheral, 1, "aer").equals(0), "empty pedestal has no visible stored vis");
            var wand = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:casting_wand"))); WandApi.set(wand, Aspects.AIR, 199); pedestal.setItem(0, wand);
            check(((Map<?, ?>)value(pedestalPeripheral, 0)).size() == 6 && value(pedestalPeripheral, 1, "aer").equals(1), "pedestal lists all six wand primals and truncates centivis to whole vis");
            WandApi.set(pedestal.getItem(0), Aspects.AIR, 299);
            check(value(pedestalPeripheral, 1, "aer").equals(2), "cached peripheral reads changes to the actual held wand");
            var amulet = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:vis_amulet"))); WandApi.set(amulet, Aspects.FIRE, 1234); pedestal.setItem(0, amulet);
            check(value(pedestalPeripheral, 1, "ignis").equals(12) && ((Map<?, ?>)value(pedestalPeripheral, 0)).size() == 6, "native vis amulet follows the original whole-vis branch");
            pedestal.setItem(0, new ItemStack(Items.STONE));
            check(((Map<?, ?>)value(pedestalPeripheral, 0)).isEmpty(), "invalid stored item is not treated as a wand or amulet");
            pedestal.clearContent();
            System.out.println("TT_ASPECT_DISPLAYS_PASS checks=" + checks); return checks;
        } finally {
            for (var pos : changed) if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
            for (var pos : changed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
