package thaumic.tinkerer.integration;

import mcp.mobius.waila.api.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** WTHIT discovers this optional entry point through waila_plugins.json. */
public final class WthitCompatibility implements IWailaCommonPlugin {
    public static final Provider PROVIDER=new Provider();
    @Override public void register(ICommonRegistrar registrar) {
        for(var type:DeviceTooltip.TYPES)registrar.blockData(PROVIDER,type);
    }
    public static final class Provider implements IDataProvider<BlockEntity> {
        @Override public void appendData(IDataWriter data,IServerAccessor<BlockEntity> accessor,IPluginConfig config) {
            DeviceTooltip.write(data.raw(),accessor.getTarget());
        }
    }
}
