package thaumic.tinkerer.compat;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import thaumic.tinkerer.block.GolemConnectorContent;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

/** Loaded only after the ComputerCraft presence check. */
public final class ComputerCraftCompat {
    // Modern TC shares a common container superclass for its transport blocks.
    // These explicit registry mappings preserve the original Java-interface priority.
    private static final Set<String> NATIVE_TRANSPORTS = Set.of("essentia_tube", "essentia_valve", "restricted_essentia_tube", "directional_essentia_tube", "arcane_bore_base", "lamp_of_growth", "lamp_of_fertility", "flux_scrubber");
    private static final Set<String> NATIVE_CONTAINERS = Set.of("arcane_alembic", "warded_jar", "void_jar", "filtered_essentia_tube", "essentia_buffer", "alchemical_centrifuge", "essentia_crystallizer", "essentia_reservoir", "crucible", "mana_pod", "thaumatorium", "advanced_alchemy_furnace", "essentia_mirror");
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), GolemConnectorContent.TILE.get(), (tile, side) -> new GolemConnectorPeripheral(tile));
        event.registerBlock(PeripheralCapability.get(), (level, pos, state, tile, side) -> {
            if (tile == null) {
                // The original thaumatorium top had its own delegating tile. Modern
                // TC exposes that same view as a public capability on the upper half.
                if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumatorium"))) {
                    var main = level.getCapability(thaumcraft.api.aspects.EssentiaCapabilities.CONTAINER, pos);
                    if (main instanceof net.minecraft.world.level.block.entity.BlockEntity entity) return new EssentiaPeripheral(entity, true);
                }
                return null;
            }
            if (tile instanceof thaumic.tinkerer.block.AnimationTabletBlockEntity tablet) return new AnimationTabletPeripheral(tablet);
            if (tile instanceof thaumic.tinkerer.block.AspectAnalyzerBlockEntity analyzer) return new AspectAnalyzerPeripheral(analyzer);
            if (tile instanceof thaumcraft.api.world.IBrainInAJar || tile instanceof thaumcraft.api.world.IDeconstructionTable || tile instanceof thaumcraft.api.world.IArcaneEar
                    || tile instanceof thaumcraft.api.world.IArcaneBore bore && !bore.base()) return new DevicePeripheral(tile);
            if (tile instanceof thaumcraft.api.aspects.IAspectDisplay) return new EssentiaPeripheral(tile, true);
            var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id.getNamespace().equals("thaumcraft")) {
                if (id.getPath().equals("infernal_furnace") && level.getCapability(thaumcraft.api.aspects.EssentiaCapabilities.TRANSPORT, pos, side) != null) return new EssentiaPeripheral(tile, false);
                if (NATIVE_TRANSPORTS.contains(id.getPath()) && tile instanceof IEssentiaTransport) return new EssentiaPeripheral(tile, false);
                return NATIVE_CONTAINERS.contains(id.getPath()) && tile instanceof IAspectContainer ? new EssentiaPeripheral(tile, true) : null;
            }
            if (tile instanceof IAspectContainer) return new EssentiaPeripheral(tile, true);
            return tile instanceof IEssentiaTransport ? new EssentiaPeripheral(tile, false) : null;
        }, BuiltInRegistries.BLOCK.stream().toArray(Block[]::new));
    }
    private ComputerCraftCompat() { }
}
