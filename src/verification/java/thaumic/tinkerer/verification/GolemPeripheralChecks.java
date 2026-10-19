package thaumic.tinkerer.verification;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.IControllableGolem;
import thaumic.tinkerer.block.GolemConnectorBlockEntity;
import thaumic.tinkerer.compat.GolemConnectorPeripheral;

/** Loaded only in the verification profile containing the real ComputerCraft mod. */
final class GolemPeripheralChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Object[] call(IDynamicPeripheral peripheral, int index, Object... arguments) throws LuaException {
        var pending = new AtomicReference<LuaTask>();
        var result = peripheral.callMethod(null, task -> { pending.set(task); return 42; }, index, new ObjectArguments(arguments));
        if (result.getCallback() == null || pending.get() == null) throw new AssertionError("Every method must yield to a server-thread task");
        pending.get().execute();
        var values = result.getCallback().resume(new Object[]{"task_complete", 42L, true}).getResult();
        return values == null ? new Object[0] : values;
    }
    private static Map<?, ?> map(Object[] result) { return (Map<?, ?>)result[0]; }
    static int run(GolemConnectorBlockEntity tile, LivingEntity golem) throws LuaException {
        checks = 0; var level = tile.getLevel(); var pos = tile.getBlockPos();
        var peripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), pos, Direction.NORTH);
        check(peripheral != null && peripheral.getType().equals("tt_golemconnector") && peripheral.getTarget() == tile, "real ComputerCraft capability exposes the original peripheral type and target");
        var another = level.getCapability(PeripheralCapability.get(), pos, Direction.DOWN);
        check(peripheral.equals(another) && another.equals(peripheral), "stable identity equality replaces original recursive stack overflow");
        check(Arrays.equals(peripheral.getMethodNames(), new String[]{"getDecorations", "getPosition", "getType", "getHealth", "getCore", "getHome", "setHome", "getMarkers", "setMarkers", "newMarker", "addMarker", "saveMarker", "deleteMarker", "getMarker", "getMarkerCount"}), "all fifteen original names and method indices");
        tile.connect(null);
        check(call(peripheral, 1).length == 0 && call(peripheral, 3).length == 0 && call(peripheral, 14).length == 0, "unbound getters return no values");
        var blank = map(call(peripheral, 9));
        check(blank.get("posX").equals(pos.getX()) && blank.get("posY").equals(pos.getY()) && blank.get("side").equals(1) && blank.get("color").equals(-1) && blank.get("dim").equals(0), "newMarker works unbound and uses original position/default side/color/overworld ID");
        boolean arity = false;
        try { call(peripheral, 6, 1d); } catch (LuaException expected) { arity = expected.getMessage().equals("Invalid arguments"); }
        check(arity, "original arity check precedes unbound early return");
        tile.connect(golem.getUUID()); var control = (IControllableGolem)golem;
        golem.setPos(pos.getX() - .7, pos.getY() + .9, pos.getZ() + .2);
        check(Arrays.equals(call(peripheral, 1), new Object[]{(int)golem.getX(), (int)golem.getY(), (int)golem.getZ()}), "CC position uses truncation toward zero");
        check(call(peripheral, 2)[0].equals("Straw") && call(peripheral, 3)[0].equals(golem.getHealth()), "type and health read the live native entity");
        check(call(peripheral, 0).length == 0 && call(peripheral, 4).length == 1, "empty decorations return no table and blank core returns one localized string");
        var pending = new AtomicReference<LuaTask>(); var oldHome = control.home();
        var homeResult = peripheral.callMethod(null, task -> { pending.set(task); return 77; }, 6, new ObjectArguments(-2.8, -1.9, 4.7, 5d));
        check(control.home().equals(oldHome), "Lua thread scheduling does not immediately mutate home");
        pending.get().execute();
        check(Arrays.equals(homeResult.getCallback().resume(new Object[]{"task_complete", 77L, true}).getResult(), new Object[]{-2, -1, 4, 5}) && control.home().getY() == -1, "server task changes actual home and returns truncated coordinates/facing");
        var marker = new LinkedHashMap<String, Object>(); marker.put("posX", -3.9); marker.put("posY", -1d); marker.put("posZ", 8.8); marker.put("dim", -1d); marker.put("side", 2d); marker.put("color", 7d);
        check(map(call(peripheral, 10, marker)).get("dim").equals(-1) && control.golemMarkers().getFirst().dimension().equals(Level.NETHER), "legacy numeric Nether marker enters the actual golem configuration");
        check(control.golemMarkers().getFirst().position().getX() == -3 && control.golemMarkers().getFirst().position().getY() == -1, "marker coordinates preserve original truncation and negative height");
        var custom = new LinkedHashMap<>(marker); custom.put("dim", "example:another_world"); custom.put("color", -1d);
        check(map(call(peripheral, 10, custom)).get("dim").equals("example:another_world"), "custom dimension is emitted as its resource ID");
        check(map(call(peripheral, 7)).keySet().equals(java.util.Set.of(1, 2)) && call(peripheral, 14)[0].equals(2d), "marker collection is one-based while count is a Lua number");
        check(map(call(peripheral, 13, .75d)).get("dim").equals(-1), "getMarker uses zero-based indices and truncates fractional indices");
        marker.put("dim", "minecraft:the_end");
        check(map(call(peripheral, 11, 0d, marker)).get("dim").equals(1), "saveMarker accepts vanilla resource ID and emits its legacy numeric ID");
        call(peripheral, 12, 0d);
        check(control.golemMarkers().size() == 1 && map(call(peripheral, 13, 0d)).get("dim").equals("example:another_world"), "deleteMarker changes the live list using zero-based index");
        var input = new LinkedHashMap<Object, Object>(); input.put(90d, marker); input.put(2d, custom); input.put(1d, marker);
        call(peripheral, 8, input);
        check(control.golemMarkers().size() == 3 && control.golemMarkers().get(0).dimension().equals(Level.END)
                && control.golemMarkers().get(1).dimension().location().toString().equals("example:another_world"), "setMarkers preserves input table iteration order and duplicates without key sorting");
        boolean missing = false;
        try { call(peripheral, 13, 3d); } catch (LuaException expected) { missing = expected.getMessage().equals("marker 3 does not exist"); }
        check(missing, "original missing-index error");
        var before = control.golemMarkers(); var invalid = new LinkedHashMap<>(marker); invalid.put("dim", 71d);
        boolean badDimension = false;
        try { call(peripheral, 10, invalid); } catch (LuaException expected) { badDimension = true; }
        check(badDimension && before.equals(control.golemMarkers()), "unknown numeric dimension is rejected without mutating markers");
        boolean negative = false;
        try { call(peripheral, 12, -1d); } catch (LuaException expected) { negative = true; }
        check(negative && before.equals(control.golemMarkers()), "negative-index crash is replaced with a Lua error without mutation");
        var saved = new CompoundTag(); golem.saveWithoutId(saved); saved.getCompound("Configuration").putString("deco", "RHG"); saved.getCompound("Configuration").putInt("core", 8); golem.load(saved);
        var decoration = map(call(peripheral, 0));
        check(decoration.keySet().equals(java.util.Set.of(0d, 1d, 2d)) && ((String)decoration.get(0d)).contains("Dart"), "decorations retain zero-based numeric keys and original R lookup for dart gun");
        check(((String)call(peripheral, 4)[0]).contains("Use"), "core getter uses actual configured core and native localization");
        saved.getCompound("Configuration").putString("deco", "D"); golem.load(saved); boolean dartQuirk = false;
        try { call(peripheral, 0); } catch (LuaException expected) { dartQuirk = expected.getMessage().equals("Golem is wearing unknown accessory: D"); }
        check(dartQuirk, "original D enum versus R lookup mismatch retained");
        check(GolemConnectorPeripheral.dimensionValue(Level.OVERWORLD).equals(0) && GolemConnectorPeripheral.dimensionValue(Level.NETHER).equals(-1)
                && GolemConnectorPeripheral.dimensionValue(Level.END).equals(1), "all three vanilla IDs retain their legacy numbers");
        check(GolemConnectorPeripheral.parseDimension("minecraft:overworld").equals(Level.OVERWORLD)
                && GolemConnectorPeripheral.parseDimension("minecraft:the_nether").equals(Level.NETHER)
                && GolemConnectorPeripheral.parseDimension(1d).equals(Level.END), "both numeric and resource-ID input formats");
        tile.connect(null); System.out.println("TT_GOLEM_PERIPHERAL_PASS checks=" + checks); return checks;
    }
}
