package thaumic.tinkerer.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.IControllableGolem;
import thaumic.tinkerer.block.GolemConnectorBlockEntity;

/** Release-164 Lua interface, with all world access marshalled onto the server. */
public final class GolemConnectorPeripheral implements IDynamicPeripheral {
    private static final String[] METHODS = {"getDecorations", "getPosition", "getType", "getHealth", "getCore", "getHome", "setHome",
            "getMarkers", "setMarkers", "newMarker", "addMarker", "saveMarker", "deleteMarker", "getMarker", "getMarkerCount"};
    private static final String[] CORES = {"fill", "empty", "gather", "harvest", "guard", "decant", "alchemy", "chop", "use", "butcher", "sorting", "fishing"};
    private static final Map<Character, String> DECORATIONS = Map.of('H', "tophat", 'G', "glasses", 'B', "bowtie", 'F', "fez", 'R', "dart_launcher", 'V', "visor", 'P', "armor", 'M', "mace");
    private final GolemConnectorBlockEntity tile;
    public GolemConnectorPeripheral(GolemConnectorBlockEntity tile) { this.tile = tile; }
    @Override public String getType() { return "tt_golemconnector"; }
    @Override public String[] getMethodNames() { return METHODS.clone(); }
    @Override public Object getTarget() { return tile; }
    @Override public boolean equals(IPeripheral other) { return other instanceof GolemConnectorPeripheral peripheral && peripheral.tile == tile; }
    @Override public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException {
        arguments.escapes();
        return context.executeMainThreadTask(() -> invoke(method, arguments));
    }
    private Object[] invoke(int method, IArguments args) throws LuaException {
        var level = tile.getLevel();
        if (level == null || level.isClientSide() || level.getServer() == null || !level.getServer().isSameThread()) throw new LuaException("Server thread required");
        if (tile.isRemoved()) throw new LuaException("Golem connector is no longer present");
        // Original validates wrapper arity even when there is no connected golem.
        switch (method) {
            case 6 -> arity(args, 4, "Invalid arguments");
            case 8 -> arity(args, 1, "setMarkers takes 1 argument");
            case 10 -> arity(args, 1, "addMarker must have 1 argument");
            case 11 -> arity(args, 2, "saveMarker must have 2 arguments");
            case 12 -> arity(args, 1, "deleteMarker must have 1 argument");
            case 13 -> arity(args, 1, "getMarker must have 1 argument");
        }
        if (method == 9) return new Object[]{markerTable(new IControllableGolem.Marker(level.dimension(), tile.getBlockPos(), Direction.UP, -1))};
        var golem = tile.golem(); var control = tile.control();
        if (golem == null) return new Object[0];
        return switch (method) {
            case 0 -> decorations(control.decoration());
            case 1 -> new Object[]{(int)golem.getX(), (int)golem.getY(), (int)golem.getZ()};
            case 2 -> {
                String material = control.golemMaterial();
                if (!Set.of("clay", "flesh", "iron", "stone", "straw", "tallow", "thaumium", "wood").contains(material)) throw new LuaException("Unknown Golem Type: " + material);
                yield new Object[]{Character.toUpperCase(material.charAt(0)) + material.substring(1)};
            }
            case 3 -> new Object[]{golem.getHealth()};
            case 4 -> {
                int core = control.core(); String name;
                if (core == -1 || core == 100) name = "blank";
                else if (core >= 0 && core < CORES.length) name = CORES[core];
                else throw new LuaException("Golem has Unknown core: " + core);
                yield new Object[]{Component.translatable("item.thaumcraft.golem_core_" + name).getString()};
            }
            case 5 -> home(control);
            case 6 -> {
                control.setGolemHome(new BlockPos((int)args.getDouble(0), (int)args.getDouble(1), (int)args.getDouble(2)), face((int)args.getDouble(3)));
                yield home(control);
            }
            case 7 -> markers(control);
            case 8 -> {
                var list = new ArrayList<IControllableGolem.Marker>();
                // Deliberately iterate table values; the old implementation did not sort keys.
                for (Object value : args.getTable(0).values()) list.add(parseMarker(table(value)));
                control.replaceGolemMarkers(list); yield markers(control);
            }
            case 10 -> {
                var list = new ArrayList<>(control.golemMarkers()); var marker = parseMarker(args.getTable(0)); list.add(marker);
                control.replaceGolemMarkers(list); yield new Object[]{markerTable(marker)};
            }
            case 11, 12, 13 -> {
                var list = new ArrayList<>(control.golemMarkers()); double requested = args.getDouble(0); int index = (int)requested;
                if (!Double.isFinite(requested) || requested < 0 || list.size() <= requested) throw new LuaException("marker " + index + " does not exist");
                if (method == 12) { list.remove(index); control.replaceGolemMarkers(list); yield new Object[0]; }
                if (method == 11) { list.set(index, parseMarker(args.getTable(1))); control.replaceGolemMarkers(list); }
                yield new Object[]{markerTable(list.get(index))};
            }
            case 14 -> new Object[]{(double)control.golemMarkers().size()};
            default -> new Object[0];
        };
    }
    private static Object[] home(IControllableGolem golem) { var pos = golem.home(); return new Object[]{pos.getX(), pos.getY(), pos.getZ(), golem.homeFace().get3DDataValue()}; }
    private static Object[] decorations(String value) throws LuaException {
        if (value.isEmpty()) return new Object[0];
        var result = new LinkedHashMap<Double, String>();
        for (int i = 0; i < value.length(); i++) {
            var name = DECORATIONS.get(value.charAt(i));
            if (name == null) throw new LuaException("Golem is wearing unknown accessory: " + value.charAt(i));
            result.put((double)i, Component.translatable("item.thaumcraft.golem_" + name).getString());
        }
        return new Object[]{result};
    }
    private static Object[] markers(IControllableGolem golem) {
        var result = new LinkedHashMap<Integer, Map<String, Object>>(); int i = 1;
        for (var marker : golem.golemMarkers()) result.put(i++, markerTable(marker));
        return new Object[]{result};
    }
    private static void arity(IArguments args, int count, String error) throws LuaException { if (args.count() != count) throw new LuaException(error); }
    private static Map<?, ?> table(Object value) throws LuaException { if (value instanceof Map<?, ?> map) return map; throw new LuaException("Expected marker table"); }
    private static double number(Map<?, ?> map, String name) throws LuaException {
        if (map.get(name) instanceof Number value && Double.isFinite(value.doubleValue())) return value.doubleValue();
        throw new LuaException("Marker field '" + name + "' must be a finite number");
    }
    private static Direction face(int value) throws LuaException { if (value < 0 || value > 5) throw new LuaException("Side must be 0..5"); return Direction.from3DDataValue(value); }
    private static IControllableGolem.Marker parseMarker(Map<?, ?> map) throws LuaException {
        int color = (byte)number(map, "color"); if (color < -1 || color > 15) throw new LuaException("Color must be -1..15");
        return new IControllableGolem.Marker(parseDimension(map.get("dim")),
                new BlockPos((int)number(map, "posX"), (int)number(map, "posY"), (int)number(map, "posZ")), face((byte)number(map, "side")), color);
    }
    public static Object dimensionValue(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) return 0;
        if (dimension.equals(Level.NETHER)) return -1;
        if (dimension.equals(Level.END)) return 1;
        return dimension.location().toString();
    }
    public static ResourceKey<Level> parseDimension(Object value) throws LuaException {
        if (value instanceof Number number) {
            double raw = number.doubleValue(); if (!Double.isFinite(raw)) throw new LuaException("Invalid dimension");
            return switch ((int)raw) { case 0 -> Level.OVERWORLD; case -1 -> Level.NETHER; case 1 -> Level.END;
                default -> throw new LuaException("Use the dimension's resource ID for a non-vanilla dimension"); };
        }
        if (value instanceof String text) {
            var id = ResourceLocation.tryParse(text);
            if (id != null) return ResourceKey.create(Registries.DIMENSION, id);
        }
        throw new LuaException("Dimension must be 0, -1, 1, or a resource ID");
    }
    private static Map<String, Object> markerTable(IControllableGolem.Marker marker) {
        var result = new LinkedHashMap<String, Object>(); var pos = marker.position();
        result.put("posX", pos.getX()); result.put("posY", pos.getY()); result.put("posZ", pos.getZ());
        result.put("dim", dimensionValue(marker.dimension())); result.put("color", marker.color()); result.put("side", marker.face().get3DDataValue());
        return result;
    }
}
