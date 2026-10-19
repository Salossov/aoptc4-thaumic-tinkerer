package thaumic.tinkerer.integration;

import mcp.mobius.waila.api.*;
import thaumic.tinkerer.block.*;

public final class WthitClientCompatibility implements IWailaClientPlugin {
    public static final Provider PROVIDER=new Provider();
    @Override public void register(IClientRegistrar registrar) {
        for(var type:java.util.List.of(AnimationTabletBlock.class,TransvectorInterfaceBlock.class,RepairerBlock.class,MagnetBlock.class))
            registrar.body(PROVIDER,type);
    }
    public static final class Provider implements IBlockComponentProvider {
        @Override public void appendBody(ITooltip tooltip,IBlockAccessor accessor,IPluginConfig config) {
            DeviceTooltip.readOrLocal(accessor.getData().raw(),accessor.getBlockEntity(),accessor.getWorld().registryAccess()).forEach(tooltip::addLine);
        }
    }
}
