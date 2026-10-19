package thaumic.tinkerer.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@WailaPlugin(ThaumicTinkerer.ID)
public final class JadeCompatibility implements IWailaPlugin {
    public static final Provider PROVIDER=new Provider();
    @Override public void register(IWailaCommonRegistration registration) {
        for(var type:DeviceTooltip.TYPES)registration.registerBlockDataProvider(PROVIDER,type);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        for(var type:java.util.List.of(AnimationTabletBlock.class,TransvectorInterfaceBlock.class,RepairerBlock.class,MagnetBlock.class))
            registration.registerBlockComponent(PROVIDER,type);
    }
    public static final class Provider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        @Override public ResourceLocation getUid() {return ThaumicTinkerer.id("device_details");}
        @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor) {DeviceTooltip.write(tag,accessor.getBlockEntity());}
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config) {
            DeviceTooltip.readOrLocal(accessor.getServerData(),accessor.getBlockEntity(),accessor.getLevel().registryAccess()).forEach(tooltip::add);
        }
    }
}
