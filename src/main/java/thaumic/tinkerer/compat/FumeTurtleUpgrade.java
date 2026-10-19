package thaumic.tinkerer.compat;

import dan200.computercraft.api.turtle.*;
import dan200.computercraft.api.upgrades.UpgradeType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.GasBlock;

/** Release-164 FumeTool (legacy upgrade ID 171), now a named data-pack upgrade. */
public final class FumeTurtleUpgrade implements ITurtleUpgrade {
    private static final DeferredRegister<UpgradeType<? extends ITurtleUpgrade>> TYPES = DeferredRegister.create(ITurtleUpgrade.typeRegistry(), ThaumicTinkerer.ID);
    public static final FumeTurtleUpgrade INSTANCE = new FumeTurtleUpgrade();
    public static final DeferredHolder<UpgradeType<? extends ITurtleUpgrade>, UpgradeType<FumeTurtleUpgrade>> TYPE = TYPES.register("gas_remover", () -> UpgradeType.simple(INSTANCE));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    @Override public UpgradeType<FumeTurtleUpgrade> getType() { return TYPE.get(); }
    @Override public TurtleUpgradeType getUpgradeType() { return TurtleUpgradeType.TOOL; }
    @Override public Component getAdjective() { return Component.translatable("upgrade.thaumictinkerer.gas_remover.adjective"); }
    @Override public ItemStack getCraftingItem() { return new ItemStack(BuiltInRegistries.ITEM.get(ThaumicTinkerer.id("gas_remover"))); }
    @Override public TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, Direction direction) {
        if (verb != TurtleVerb.DIG) return TurtleCommandResult.failure();
        var level = turtle.getLevel(); var origin = turtle.getPosition();
        if (level.isClientSide() || level.getServer() == null || !level.getServer().isSameThread()) return TurtleCommandResult.failure();
        for (int x = origin.getX() - 3; x < origin.getX() + 3; x++)
            for (int y = origin.getY() - 3; y < origin.getY() + 3; y++)
                for (int z = origin.getZ() - 3; z < origin.getZ() + 3; z++) {
                    var pos = new net.minecraft.core.BlockPos(x, y, z);
                    if (level.getBlockState(pos).getBlock() instanceof GasBlock gas) {
                        gas.placeParticle(level, pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
        return TurtleCommandResult.success();
    }
}
