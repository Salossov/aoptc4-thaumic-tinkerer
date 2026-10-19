package thaumic.tinkerer.client;

import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import net.neoforged.bus.api.IEventBus;
import thaumic.tinkerer.compat.FumeTurtleUpgrade;

/** Explicitly registered only on a client with ComputerCraft present. */
public final class FumeTurtleModels {
    public static void register(IEventBus bus) { bus.addListener(FumeTurtleModels::modellers); }
    private static void modellers(RegisterTurtleModellersEvent event) { event.register(FumeTurtleUpgrade.TYPE.get(), TurtleUpgradeModeller.flatItem()); }
    private FumeTurtleModels() { }
}
