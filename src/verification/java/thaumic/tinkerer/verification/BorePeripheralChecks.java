package thaumic.tinkerer.verification;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.world.IArcaneBore;

final class BorePeripheralChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Object value(IDynamicPeripheral peripheral, int method) throws LuaException {
        var task = new AtomicReference<LuaTask>();
        var result = peripheral.callMethod(null, next -> { task.set(next); return 19; }, method, new ObjectArguments());
        if (result.getCallback() == null || task.get() == null) throw new AssertionError("Bore peripheral must yield to server task");
        task.get().execute(); return result.getCallback().resume(new Object[]{"task_complete", 19L, true}).getResult()[0];
    }
    static int run(net.minecraft.server.level.ServerLevel level, BlockPos origin) throws LuaException {
        checks = 0; var pos = origin.offset(3, 15, 3); var power = pos.west();
        try {
            level.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:arcane_bore")).defaultBlockState(), 3);
            var bore = (IArcaneBore)level.getBlockEntity(pos); var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            var peripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), pos, Direction.UP);
            check(!bore.base() && peripheral != null && peripheral.getType().equals("tt_arcanebore"), "actual bore head exposes the distinct original peripheral");
            check(List.of(peripheral.getMethodNames()).equals(List.of("hasPickaxe", "hasFocus", "isPickaxeBroken", "isWorking", "getRadius", "getSpeed", "hasNativeClusters", "getFortune", "hasSilkTouch")), "all nine original bore method names and indices");
            check(value(peripheral, 0).equals(false) && value(peripheral, 1).equals(false) && value(peripheral, 2).equals(false) && value(peripheral, 3).equals(false), "empty head has no pickaxe/focus and is not working");
            check(value(peripheral, 4).equals(5) && value(peripheral, 5).equals(0) && value(peripheral, 7).equals(0) && value(peripheral, 8).equals(false), "original getRadius returns diameter five at baseline, speed/enchants zero");
            var pick = new ItemStack(Items.DIAMOND_PICKAXE); var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            pick.enchant(enchantments.getOrThrow(Enchantments.EFFICIENCY), 2); pick.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 1); pick.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
            check(items.insertItem(1, pick, false).isEmpty() && value(peripheral, 0).equals(true) && value(peripheral, 1).equals(false), "actual item capability installs a real enchanted pickaxe");
            var focus = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:focus_excavation")));
            CustomData.update(DataComponents.CUSTOM_DATA, focus, tag -> tag.putIntArray("FocusUpgrades", new int[]{FocusUpgradeType.enlarge.id(), FocusUpgradeType.potency.id(), FocusUpgradeType.treasure.id(), FocusUpgradeType.treasure.id(), -1}));
            check(items.insertItem(0, focus, false).isEmpty() && value(peripheral, 1).equals(true), "actual capability installs native excavation focus");
            check(value(peripheral, 4).equals(7) && value(peripheral, 5).equals(3), "diameter reflects area while speed sums focus potency and pickaxe efficiency");
            check(value(peripheral, 7).equals(1) && value(peripheral, 8).equals(true) && value(peripheral, 6).equals(false), "fortune getter reads only pickaxe enchantment despite higher focus treasure; silk reads pickaxe");
            check(!value(peripheral, 3).equals(true), "valid tools alone do not count as working without redstone");
            level.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            check(value(peripheral, 3).equals(true), "original peripheral working predicate needs power/tools but no target or base entity");
            items.getStackInSlot(1).setDamageValue(pick.getMaxDamage() - 1);
            check(value(peripheral, 2).equals(true) && value(peripheral, 3).equals(false), "exact one-durability-left boundary is reported broken and not working");
            items.getStackInSlot(1).setDamageValue(pick.getMaxDamage());
            check(value(peripheral, 2).equals(false) && value(peripheral, 3).equals(true), "original equality-only near-broken quirk survives an over-worn stored pickaxe");
            items.extractItem(1, 1, false); var elemental = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:elemental_pickaxe")));
            check(items.insertItem(1, elemental, false).isEmpty() && value(peripheral, 6).equals(true), "native cluster flag recognizes actual elemental pickaxe");
            items.extractItem(1, 1, false); var thaumium = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumium_pickaxe")));
            check(items.insertItem(1, thaumium, false).isEmpty() && value(peripheral, 6).equals(false), "ordinary thaumium tool sharing the modern class does not gain native clusters");
            level.setBlock(power, Blocks.AIR.defaultBlockState(), 3);
            check(value(peripheral, 3).equals(false) && bore.inspectBore().hasPickaxe(), "cached peripheral observes power removal while retaining installed tools");
            check(peripheral.equals(level.getCapability(PeripheralCapability.get(), pos, Direction.DOWN)), "bore identity is stable across sides");
            System.out.println("TT_BORE_PERIPHERAL_PASS checks=" + checks); return checks;
        } finally {
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            if (items != null) { items.extractItem(0, Integer.MAX_VALUE, false); items.extractItem(1, Integer.MAX_VALUE, false); }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); level.setBlock(power, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
