package thaumic.tinkerer.compat;

import java.util.LinkedHashMap;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.*;

/** Original aspect-container and essentia-transport ComputerCraft interfaces. */
public final class EssentiaPeripheral implements IDynamicPeripheral {
    private static final String[] CONTAINER_METHODS = {"getAspects", "getAspectCount"};
    private static final String[] TRANSPORT_METHODS = {"isConnectable", "canInputFrom", "canOutputTo", "getSuctionType", "getSuctionAmount", "getEssentiaType", "getEssentiaAmount", "getMinimumSuction"};
    private final BlockEntity tile;
    private final boolean container;
    public EssentiaPeripheral(BlockEntity tile, boolean container) { this.tile = tile; this.container = container; }
    @Override public String getType() { return container ? "tt_aspectContainer" : "tt_aspectTransport"; }
    @Override public String[] getMethodNames() { return (container ? CONTAINER_METHODS : TRANSPORT_METHODS).clone(); }
    @Override public Object getTarget() { return tile; }
    @Override public boolean equals(IPeripheral other) { return other instanceof EssentiaPeripheral peripheral && peripheral.tile == tile && peripheral.container == container; }
    @Override public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments args) throws LuaException {
        args.escapes();
        return context.executeMainThreadTask(() -> {
            var level = tile.getLevel();
            if (level == null || level.isClientSide() || level.getServer() == null || !level.getServer().isSameThread()) throw new LuaException("Server thread required");
            if (tile.isRemoved()) throw new LuaException("Peripheral is no longer present");
            if (container) {
                var target = tile instanceof IAspectContainer actual ? actual : null;
                if (method == 0) {
                    var result = new LinkedHashMap<Double, String>(); var filter = target == null ? null : target.getAspectFilter();
                    if (filter != null) result.put(1d, filter.getTag());
                    else {
                        var aspects = target == null ? ((IAspectDisplay)tile).displayedAspects() : target.getAspects(); double i = 1;
                        if (aspects != null) for (var aspect : aspects.getAspectsSorted()) result.put(i++, aspect.getTag());
                    }
                    return new Object[]{result};
                }
                if (method == 1) { var name = args.getString(0); var aspects = target == null ? ((IAspectDisplay)tile).displayedAspects() : target.getAspects(); return new Object[]{aspects == null ? 0 : aspects.getAmount(Aspect.getAspect(name))}; }
                return new Object[0];
            }
            var target = (IEssentiaTransport)tile;
            if (method == 7) return new Object[]{target.getMinimumSuction()};
            if (method < 0 || method > 6) return new Object[0];
            int index = (int)args.getDouble(0); var side = index >= 0 && index < 6 ? Direction.from3DDataValue(index) : null;
            return switch (method) {
                case 0 -> new Object[]{target.isConnectable(side)};
                case 1 -> new Object[]{target.canInputFrom(side)};
                case 2 -> new Object[]{target.canOutputTo(side)};
                case 3 -> new Object[]{tag(target.getSuctionType(side))};
                case 4 -> new Object[]{target.getSuctionAmount(side)};
                case 5 -> new Object[]{tag(target.getEssentiaType(side))};
                case 6 -> new Object[]{target.getEssentiaAmount(side)};
                default -> new Object[0];
            };
        });
    }
    private static String tag(Aspect aspect) throws LuaException { if (aspect == null) throw new LuaException("No aspect on the requested side"); return aspect.getTag(); }
}
