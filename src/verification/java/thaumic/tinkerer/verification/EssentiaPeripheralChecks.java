package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.block.TransvectorContent;
import thaumic.tinkerer.block.TransvectorInterfaceBlockEntity;

final class EssentiaPeripheralChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static Object[] call(IDynamicPeripheral peripheral, int method, Object... args) throws LuaException {
        var task = new AtomicReference<LuaTask>();
        var result = peripheral.callMethod(null, next -> { task.set(next); return 21; }, method, new ObjectArguments(args));
        if (result.getCallback() == null || task.get() == null) throw new AssertionError("Essentia peripheral must yield to server task");
        task.get().execute(); var values = result.getCallback().resume(new Object[]{"task_complete", 21L, true}).getResult();
        return values == null ? new Object[0] : values;
    }
    private static Map<?, ?> aspects(IDynamicPeripheral peripheral) throws LuaException { return (Map<?, ?>)call(peripheral, 0)[0]; }
    static int run(net.minecraft.server.level.ServerLevel level, BlockPos origin) throws LuaException {
        checks = 0; var changed = new ArrayList<BlockPos>();
        try {
            var jarPos = origin.offset(9, 0, 8); changed.add(jarPos);
            level.setBlock(jarPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar")).defaultBlockState(), 3);
            var jar = level.getBlockEntity(jarPos); var contents = (IAspectContainer)jar;
            var peripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), jarPos, Direction.UP);
            check(peripheral != null && peripheral.getType().equals("tt_aspectContainer") && List.of(peripheral.getMethodNames()).equals(List.of("getAspects", "getAspectCount")), "actual warded jar exposes original aspect-container methods");
            check(aspects(peripheral).isEmpty() && call(peripheral, 1, "aer")[0].equals(0), "empty unlabelled jar has no listed aspects and zero count");
            var saved = jar.saveWithFullMetadata(level.registryAccess()); saved.putString("AspectFilter", "aer"); jar.loadWithComponents(saved, level.registryAccess());
            check(contents.getAspectFilter() == Aspects.AIR && aspects(peripheral).equals(Map.of(1d, "aer")), "real empty jar label remains visible through public filter contract");
            check(call(peripheral, 1, "aer")[0].equals(0) && call(peripheral, 1, "unknown_aspect")[0].equals(0), "label does not manufacture contents and unknown tags return zero");
            contents.setAspects(new AspectList().add(Aspects.AIR, 12));
            check(call(peripheral, 1, "aer")[0].equals(12), "count reads real current jar contents without cached snapshots");
            contents.setAspects(new AspectList());
            var interfacePos = jarPos.west(); changed.add(interfacePos); level.setBlock(interfacePos, TransvectorContent.INTERFACE.get().defaultBlockState(), 3);
            ((TransvectorInterfaceBlockEntity)level.getBlockEntity(interfacePos)).bind(jarPos);
            var forwarded = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), interfacePos, Direction.UP);
            check(forwarded != null && forwarded.getType().equals("tt_aspectContainer") && aspects(forwarded).isEmpty(), "transvector forwards actual contents but does not pretend to be a labelled native jar");
            var bufferPos = jarPos.east(); changed.add(bufferPos); level.setBlock(bufferPos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:essentia_buffer")).defaultBlockState(), 3);
            var buffer = (IAspectContainer)level.getBlockEntity(bufferPos); buffer.setAspects(new AspectList().add(Aspects.WATER, 5).add(Aspects.AIR, 3));
            var bufferPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), bufferPos, Direction.UP);
            check(aspects(bufferPeripheral).equals(Map.of(1d, "aer", 2d, "aqua")) && call(bufferPeripheral, 1, "aqua")[0].equals(5), "multiaspect buffer preserves alphabetical listing, one-based keys and actual counts");
            String[] transports = {"essentia_tube", "essentia_valve", "restricted_essentia_tube", "directional_essentia_tube", "arcane_bore_base", "lamp_of_growth", "lamp_of_fertility", "flux_scrubber"};
            int i = 0;
            for (String name : transports) {
                var pos = jarPos.offset(i++, 3, 0); changed.add(pos); level.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:" + name)).defaultBlockState(), 3);
                var target = (IEssentiaTransport)level.getBlockEntity(pos); var pipe = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), pos, Direction.UP);
                check(pipe != null && pipe.getType().equals("tt_aspectTransport") && pipe.getMethodNames().length == 8, name + " uses original transport interface despite modern container superclass");
                check(call(pipe, 0, 2.9)[0].equals(target.isConnectable(Direction.NORTH)) && call(pipe, 1, 1d)[0].equals(target.canInputFrom(Direction.UP))
                        && call(pipe, 2, 0d)[0].equals(target.canOutputTo(Direction.DOWN)), name + " delegates side booleans and fractional direction truncation");
                check(call(pipe, 7)[0].equals(target.getMinimumSuction()), name + " minimum suction takes no direction argument");
            }
            String[] containers = {"arcane_alembic", "void_jar", "filtered_essentia_tube", "alchemical_centrifuge", "essentia_crystallizer", "essentia_reservoir", "crucible", "mana_pod", "essentia_mirror"};
            for (String name : containers) {
                var pos = jarPos.offset(i++, 5, 0); changed.add(pos); level.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:" + name)).defaultBlockState(), 3);
                var value = level.getCapability(PeripheralCapability.get(), pos, Direction.UP);
                check(value != null && value.getType().equals("tt_aspectContainer"), name + " preserves original container-before-transport priority");
            }
            var advanced = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:advanced_alchemy_furnace"));
            var part = (net.minecraft.world.level.block.state.properties.IntegerProperty)advanced.defaultBlockState().getProperties().stream().filter(p -> p.getName().equals("part")).findFirst().orElseThrow();
            for (int nozzle : new int[]{1, 3, 5, 7}) {
                var pos = jarPos.offset(i++, 8, 0); changed.add(pos); level.setBlock(pos, advanced.defaultBlockState().setValue(part, nozzle), 2);
                var value = level.getCapability(PeripheralCapability.get(), pos, Direction.UP);
                check(value != null && value.getType().equals("tt_aspectContainer"), "advanced furnace nozzle " + nozzle + " exposes its original aspect container");
            }
            var center = jarPos.offset(i++, 8, 0); changed.add(center); level.setBlock(center, advanced.defaultBlockState().setValue(part, 4), 2);
            check(level.getCapability(PeripheralCapability.get(), center, Direction.UP) == null, "advanced furnace center does not inherit a nozzle peripheral");
            var infernal = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:infernal_furnace"));
            var infernalPart = (net.minecraft.world.level.block.state.properties.IntegerProperty)infernal.defaultBlockState().getProperties().stream().filter(p -> p.getName().equals("part")).findFirst().orElseThrow();
            var infernalPos = jarPos.offset(i++, 8, 3); changed.add(infernalPos);
            level.setBlock(infernalPos, infernal.defaultBlockState().setValue(infernalPart, 4), 2);
            var infernalPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), infernalPos, Direction.UP);
            check(infernalPeripheral != null && infernalPeripheral.getType().equals("tt_aspectTransport"), "infernal furnace shell nozzle exposes the original transport peripheral through its real public capability");
            check(call(infernalPeripheral, 3, 99d)[0].equals("ignis") && call(infernalPeripheral, 6, 0d)[0].equals(0), "infernal nozzle reports actual fire suction type and zero stored essentia");
            var infernalCore = infernalPos.offset(3, 0, 0); changed.add(infernalCore); level.setBlock(infernalCore, infernal.defaultBlockState(), 2);
            check(level.getCapability(PeripheralCapability.get(), infernalCore, Direction.UP) == null, "infernal furnace lava core does not inherit a shell peripheral");
            var lowerPos = jarPos.offset(2, 12, 3); var support = lowerPos.below(); var upperPos = lowerPos.above(); changed.add(support); changed.add(lowerPos); changed.add(upperPos);
            level.setBlock(support, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:crucible")).defaultBlockState(), 2);
            var thaumatorium = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:thaumatorium"));
            level.setBlock(lowerPos, thaumatorium.defaultBlockState(), 2);
            level.setBlock(upperPos, thaumatorium.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
            var lowerPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), lowerPos, Direction.UP);
            var upperPeripheral = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), upperPos, Direction.UP);
            check(lowerPeripheral != null && lowerPeripheral.getType().equals("tt_aspectContainer"), "native thaumatorium lower half exposes original container");
            check(level.getBlockEntity(upperPos) == null && upperPeripheral != null && upperPeripheral.getType().equals("tt_aspectContainer"), "upper half without a modern tile resolves the original view through public container capability");
            ((IAspectContainer)level.getBlockEntity(lowerPos)).setAspects(new AspectList().add(Aspects.AIR, 5));
            check(call(lowerPeripheral, 1, "aer")[0].equals(5) && call(upperPeripheral, 1, "aer")[0].equals(5), "both physical thaumatorium halves read the same live native contents");
            var ordinaryMirror = jarPos.offset(i++, 9, 0); changed.add(ordinaryMirror); level.setBlock(ordinaryMirror, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:magic_mirror")).defaultBlockState(), 2);
            check(level.getCapability(PeripheralCapability.get(), ordinaryMirror, Direction.UP) == null, "ordinary item mirror does not gain an essentia peripheral from its modern shared class");
            var furnace = jarPos.offset(i++, 9, 0); changed.add(furnace); level.setBlock(furnace, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:alchemy_furnace")).defaultBlockState(), 2);
            check(level.getCapability(PeripheralCapability.get(), furnace, Direction.UP) == null, "ordinary alchemy furnace does not gain a container peripheral from its modern superclass");
            var tubePos = jarPos.above(3); var tube = (IEssentiaTransport)level.getBlockEntity(tubePos); var tubeContainer = (IAspectContainer)tube;
            tubeContainer.addToContainer(Aspects.AIR, 1); tube.setSuction(Aspects.AIR, 23);
            var pipe = (IDynamicPeripheral)level.getCapability(PeripheralCapability.get(), tubePos, Direction.UP);
            check(call(pipe, 3, 0d)[0].equals("aer") && call(pipe, 4, 0d)[0].equals(23), "transport exposes actual suction type/amount");
            check(call(pipe, 5, 0d)[0].equals("aer") && call(pipe, 6, 0d)[0].equals(1), "transport exposes actual essentia type/amount");
            check(call(pipe, 0, 99d)[0].equals(tube.isConnectable(null)), "old ForgeDirection.UNKNOWN maps to the public null/internal side");
            check(pipe.equals(level.getCapability(PeripheralCapability.get(), tubePos, Direction.DOWN)) && !pipe.equals(peripheral), "identity equality is stable across queried sides and distinguishes devices");
            var funnelPos=jarPos.offset(3,15,4);changed.add(funnelPos.below());changed.add(funnelPos);
            level.setBlock(funnelPos.below(),Blocks.HOPPER.defaultBlockState(),2);level.setBlock(funnelPos,thaumic.tinkerer.block.FunnelContent.BLOCK.get().defaultBlockState(),2);
            var funnel=(thaumic.tinkerer.block.FunnelBlockEntity)level.getBlockEntity(funnelPos);
            var heldJar=new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:warded_jar")));
            ((IEssentiaJarItem)heldJar.getItem()).setAspects(heldJar,new AspectList().add(Aspects.FIRE,4));funnel.setItem(0,heldJar);
            var funnelPeripheral=(IDynamicPeripheral)level.getCapability(PeripheralCapability.get(),funnelPos,Direction.UP);
            check(funnelPeripheral!=null&&funnelPeripheral.getType().equals("tt_aspectContainer"),"actual funnel keeps original container peripheral priority");
            check(aspects(funnelPeripheral).equals(Map.of(1d,"ignis"))&&call(funnelPeripheral,1,"ignis")[0].equals(4),"funnel peripheral reads the actual jar item's contents");
            check(level.getCapability(EssentiaCapabilities.TRANSPORT,funnelPos,Direction.UP)==null,"funnel does not invent an essentia transport capability");
            funnel.clearContent();
            var repairerPos=funnelPos.east(2);changed.add(repairerPos);level.setBlock(repairerPos,thaumic.tinkerer.block.RepairerContent.BLOCK.get().defaultBlockState(),2);
            var repairer=(thaumic.tinkerer.block.RepairerBlockEntity)level.getBlockEntity(repairerPos);var tool=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);tool.setDamageValue(17);repairer.setItem(0,tool);
            var repairerPeripheral=(IDynamicPeripheral)level.getCapability(PeripheralCapability.get(),repairerPos,Direction.UP);
            check(repairerPeripheral!=null&&repairerPeripheral.getType().equals("tt_aspectContainer")&&level.getCapability(EssentiaCapabilities.TRANSPORT,repairerPos,Direction.NORTH)==repairer,"repairer keeps original container peripheral priority despite also implementing transport");
            check(aspects(repairerPeripheral).equals(Map.of(1d,"perditio"))&&call(repairerPeripheral,1,"perditio")[0].equals(17),"repairer peripheral reads real current tool damage");
            tool.setDamageValue(0);check(aspects(repairerPeripheral).equals(Map.of(1d,"perditio"))&&call(repairerPeripheral,1,"perditio")[0].equals(0),"repairer preserves the original zero-amount entropy entry");repairer.clearContent();
            System.out.println("TT_ESSENTIA_PERIPHERALS_PASS checks=" + checks); return checks;
        } finally { for (var pos : changed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
    }
}
