package thaumic.tinkerer.verification;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.block.AspectAnalyzerBlockEntity;

final class AspectAnalyzerPeripheralChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Object[] call(IDynamicPeripheral peripheral,int method,Object... args)throws LuaException{
        var task=new AtomicReference<LuaTask>();var result=peripheral.callMethod(null,next->{task.set(next);return 73;},method,new ObjectArguments(args));
        if(result.getCallback()==null||task.get()==null)throw new AssertionError("Analyzer must yield to the server thread");task.get().execute();
        var values=result.getCallback().resume(new Object[]{"task_complete",73L,true}).getResult();return values==null?new Object[0]:values;
    }
    static void run(AspectAnalyzerBlockEntity tile)throws LuaException{
        checks=0;var level=tile.getLevel();var peripheral=(IDynamicPeripheral)level.getCapability(PeripheralCapability.get(),tile.getBlockPos(),Direction.NORTH);
        check(peripheral!=null&&peripheral.getType().equals("tt_aspectanalyzer")&&List.of(peripheral.getMethodNames()).equals(List.of("hasItem","itemHasAspects","getAspects","getAspectCount")),"real optional analyzer exposes the original type and all four methods in order");
        tile.clearContent();check(call(peripheral,0)[0].equals(false)&&call(peripheral,1)[0].equals(false)&&((Map<?,?>)call(peripheral,2)[0]).isEmpty()&&((Map<?,?>)call(peripheral,3)[0]).isEmpty(),"empty analyzer returns false and empty tables");
        tile.setItem(0,new ItemStack(ClothTestMod.PROTECTED));check(call(peripheral,0)[0].equals(true)&&call(peripheral,1)[0].equals(false)&&((Map<?,?>)call(peripheral,3)[0]).isEmpty(),"unrestricted automation can provide an actual item without aspects");
        tile.setItem(0,new ItemStack(Items.IRON_INGOT,23));var before=tile.getItem(0).copy();var expected=AspectApi.aspects(level,before);
        check(call(peripheral,0)[0].equals(true)&&call(peripheral,1)[0].equals(true),"real tagged item is reported without requiring scan or research knowledge");
        var names=(Map<?,?>)call(peripheral,2)[0];var sorted=new TreeSet<>(expected.keySet());double index=1;for(var tag:sorted)check(names.get(index++).equals(tag),"alphabetical one-based Lua aspect entry "+tag);
        var counts=(Map<?,?>)call(peripheral,3,"ignored original argument")[0];check(counts.size()==expected.size()&&expected.entrySet().stream().allMatch(e->counts.get(e.getKey()).equals(e.getValue().doubleValue())),"getAspectCount returns the original complete tag-to-double mapping, not a single aspect count");
        check(ItemStack.isSameItemSameComponents(before,tile.getItem(0))&&tile.getItem(0).getCount()==23,"computer inspection neither changes nor consumes the stack");
        check(peripheral.equals(level.getCapability(PeripheralCapability.get(),tile.getBlockPos(),Direction.DOWN))&&peripheral.getTarget()==tile,"peripheral identity remains stable across sides");
        tile.clearContent();System.out.println("TT_ASPECT_ANALYZER_PERIPHERAL_PASS checks="+checks);
    }
}
