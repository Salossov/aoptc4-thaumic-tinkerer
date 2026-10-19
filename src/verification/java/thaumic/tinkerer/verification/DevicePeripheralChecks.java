package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.world.*;

final class DevicePeripheralChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Object[] call(IDynamicPeripheral peripheral, int method, Object... args) throws LuaException {
        var task = new AtomicReference<LuaTask>();
        var result = peripheral.callMethod(null, next -> { task.set(next); return 32; }, method, new ObjectArguments(args));
        if (result.getCallback() == null || task.get() == null) throw new AssertionError("Device peripheral must yield to server task");
        task.get().execute(); var values = result.getCallback().resume(new Object[]{"task_complete", 32L, true}).getResult();
        return values == null ? new Object[0] : values;
    }
    static int run(net.minecraft.server.level.ServerLevel level, BlockPos origin) throws LuaException {
        checks = 0; var changed = new ArrayList<BlockPos>();
        try {
            var brainPos = origin.offset(3, 10, 3); changed.add(brainPos);
            level.setBlock(brainPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:brain_in_a_jar")).defaultBlockState(), 3);
            var brainTile = level.getBlockEntity(brainPos); var brain = (IBrainInAJar)brainTile;
            var brainPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), brainPos, Direction.UP);
            check(brainPeripheral != null && brainPeripheral.getType().equals("tt_braininajar") && List.of(brainPeripheral.getMethodNames()).equals(List.of("getXP")), "brain jar exposes original type and getXP name");
            check(call(brainPeripheral, 0)[0].equals(0) && brain.experience() == 0, "empty native brain jar reports zero XP");
            var savedBrain = brainTile.saveWithFullMetadata(level.registryAccess()); savedBrain.putInt("XP", 357); brainTile.loadWithComponents(savedBrain, level.registryAccess());
            check(call(brainPeripheral, 0)[0].equals(357) && brain.experience() == 357, "same peripheral reads live XP from the actual native save schema");
            var tablePos = origin.offset(6, 10, 3); changed.add(tablePos);
            level.setBlock(tablePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:deconstruction_table")).defaultBlockState(), 3);
            var tableTile = level.getBlockEntity(tablePos); var table = (IDeconstructionTable)tableTile;
            var tablePeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), tablePos, Direction.UP);
            check(tablePeripheral != null && tablePeripheral.getType().equals("tt_deconstructor") && List.of(tablePeripheral.getMethodNames()).equals(List.of("hasAspect", "hasItem", "getAspect")), "deconstructor exposes all three original methods");
            check(call(tablePeripheral, 0)[0].equals(false) && call(tablePeripheral, 1)[0].equals(false) && call(tablePeripheral, 2).length == 0, "empty deconstructor has no aspect/item and returns no getAspect values");
            table.setItem(0, new ItemStack(Items.STONE));
            check(call(tablePeripheral, 1)[0].equals(true) && call(tablePeripheral, 0)[0].equals(false), "inventory presence is independent of a pending research aspect");
            var savedTable = tableTile.saveWithFullMetadata(level.registryAccess()); savedTable.putString("Aspect", "aer"); tableTile.loadWithComponents(savedTable, level.registryAccess());
            check(call(tablePeripheral, 0)[0].equals(true) && call(tablePeripheral, 2)[0].equals("aer") && table.pendingAspect() == Aspects.AIR, "pending aspect is read from the real native table state");
            table.setItem(0, ItemStack.EMPTY);
            check(call(tablePeripheral, 1)[0].equals(false) && call(tablePeripheral, 0)[0].equals(true), "pending result remains visible after input is removed");
            var earPos = origin.offset(9, 10, 3); changed.add(earPos);
            level.setBlock(earPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:arcane_ear")).defaultBlockState(), 3);
            var earTile = level.getBlockEntity(earPos); var ear = (IArcaneEar)earTile;
            var earPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), earPos, Direction.UP);
            check(earPeripheral != null && earPeripheral.getType().equals("tt_arcaneear") && List.of(earPeripheral.getMethodNames()).equals(List.of("getNote", "setNote")), "arcane ear exposes original type and both methods");
            check(call(earPeripheral, 0)[0].equals(0) && call(earPeripheral, 1, 13.9).length == 0 && ear.note() == 13, "setNote truncates its Lua number and returns no values");
            check(call(earPeripheral, 0)[0].equals(13), "getNote observes the native ear field");
            var queued = new AtomicReference<LuaTask>();
            earPeripheral.callMethod(null, task -> { queued.set(task); return 93; }, 1, new ObjectArguments(255.9));
            check(ear.note() == 13, "ear mutation waits for its server-thread task"); queued.get().execute();
            check(ear.note() == -1, "original byte narrowing wraps 255.9 to minus one");
            var savedEar = earTile.saveWithFullMetadata(level.registryAccess()); ear.setNote((byte)1); earTile.loadWithComponents(savedEar, level.registryAccess());
            check(ear.note() == -1 && earTile.getUpdateTag(level.registryAccess()).getByte("note") == -1, "user-approved raw signed-byte pitch survives NBT and synchronization");
            call(earPeripheral, 1, -129d); check(ear.note() == 127, "negative overflow follows original byte narrowing");
            call(earPeripheral, 1, -128d); check(ear.note() == -128, "minimum signed-byte pitch is accepted");
            savedEar = earTile.saveWithFullMetadata(level.registryAccess()); ear.setNote((byte)0); earTile.loadWithComponents(savedEar, level.registryAccess());
            check(call(earPeripheral, 0)[0].equals(-128), "minimum pitch remains visible after actual native reload");
            check(CompletableFuture.supplyAsync(() -> { try { ear.setNote((byte)2); return false; } catch (IllegalStateException expected) { return true; } }).join()
                    && ear.note() == -128, "direct public ear mutation rejects a foreign thread without partial change");
            check(brainPeripheral.equals(level.getCapability(PeripheralCapability.get(), brainPos, Direction.DOWN)) && !brainPeripheral.equals(tablePeripheral), "device identity equality is stable across sides and distinct for other devices");
            level.setBlock(earPos, Blocks.AIR.defaultBlockState(), 3); boolean removed = false;
            try { call(earPeripheral, 1, 20d); } catch (LuaException expected) { removed = true; }
            check(removed, "cached peripheral cannot mutate a removed device");
            System.out.println("TT_DEVICE_PERIPHERALS_PASS checks=" + checks); return checks;
        } finally {
            for (var pos : changed) if (level.getBlockEntity(pos) instanceof IDeconstructionTable table) table.clearContent();
            for (var pos : changed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
