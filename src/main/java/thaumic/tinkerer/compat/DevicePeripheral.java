package thaumic.tinkerer.compat;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.world.*;

/** Original brain-jar, deconstruction-table and arcane-ear peripherals. */
public final class DevicePeripheral implements IDynamicPeripheral {
    private final BlockEntity tile;
    public DevicePeripheral(BlockEntity tile) { this.tile = tile; }
    @Override public String getType() { return tile instanceof IBrainInAJar ? "tt_braininajar" : tile instanceof IDeconstructionTable ? "tt_deconstructor" : tile instanceof IArcaneBore ? "tt_arcanebore" : "tt_arcaneear"; }
    @Override public String[] getMethodNames() { return tile instanceof IBrainInAJar ? new String[]{"getXP"} : tile instanceof IDeconstructionTable ? new String[]{"hasAspect", "hasItem", "getAspect"} : tile instanceof IArcaneBore ? new String[]{"hasPickaxe", "hasFocus", "isPickaxeBroken", "isWorking", "getRadius", "getSpeed", "hasNativeClusters", "getFortune", "hasSilkTouch"} : new String[]{"getNote", "setNote"}; }
    @Override public Object getTarget() { return tile; }
    @Override public boolean equals(IPeripheral other) { return other instanceof DevicePeripheral device && device.tile == tile; }
    @Override public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments args) throws LuaException {
        args.escapes();
        return context.executeMainThreadTask(() -> {
            var level = tile.getLevel();
            if (level == null || level.isClientSide() || level.getServer() == null || !level.getServer().isSameThread()) throw new LuaException("Server thread required");
            if (tile.isRemoved()) throw new LuaException("Peripheral is no longer present");
            if (tile instanceof IBrainInAJar brain) return new Object[]{brain.experience()};
            if (tile instanceof IArcaneBore bore) {
                var state = bore.inspectBore();
                return switch (method) {
                    case 0 -> new Object[]{state.hasPickaxe()}; case 1 -> new Object[]{state.hasFocus()};
                    case 2 -> new Object[]{state.pickaxeBroken()}; case 3 -> new Object[]{state.working()};
                    case 4 -> new Object[]{state.diameter()}; case 5 -> new Object[]{state.speed()};
                    case 6 -> new Object[]{state.nativeClusters()}; case 7 -> new Object[]{state.fortune()};
                    case 8 -> new Object[]{state.silkTouch()}; default -> new Object[0];
                };
            }
            if (tile instanceof IDeconstructionTable table) return switch (method) {
                case 0 -> new Object[]{table.pendingAspect() != null};
                case 1 -> new Object[]{!table.getItem(0).isEmpty()};
                case 2 -> table.pendingAspect() == null ? new Object[0] : new Object[]{table.pendingAspect().getTag()};
                default -> new Object[0];
            };
            if (tile instanceof IArcaneEar ear) {
                if (method == 0) return new Object[]{ear.note()};
                // The original casts Double directly to byte, preserving narrowing/wrap.
                ear.setNote((byte)args.getDouble(0)); return new Object[0];
            }
            return new Object[0];
        });
    }
}
