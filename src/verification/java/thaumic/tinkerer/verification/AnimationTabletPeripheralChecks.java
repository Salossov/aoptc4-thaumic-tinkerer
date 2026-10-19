package thaumic.tinkerer.verification;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import thaumic.tinkerer.block.*;

final class AnimationTabletPeripheralChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Object[] call(IDynamicPeripheral peripheral,int method,Object... args)throws LuaException{
        var task=new AtomicReference<LuaTask>();var result=peripheral.callMethod(null,next->{task.set(next);return 77;},method,new ObjectArguments(args));
        if(result.getCallback()==null||task.get()==null)throw new AssertionError("Tablet must yield to server thread");task.get().execute();var values=result.getCallback().resume(new Object[]{"task_complete",77L,true}).getResult();return values==null?new Object[0]:values;
    }
    static void run(AnimationTabletBlockEntity tile)throws LuaException{
        checks=0;var level=tile.getLevel();var peripheral=(IDynamicPeripheral)level.getCapability(PeripheralCapability.get(),tile.getBlockPos(),Direction.UP);
        check(peripheral!=null&&peripheral.getType().equals("tt_animationTablet")&&List.of(peripheral.getMethodNames()).equals(List.of("getRedstone","setRedstone","getLeftClick","setLeftClick","getRotation","setRotation","hasItem","trigger")),"actual peripheral preserves type case and eight-method order");
        check(peripheral.getTarget()==tile&&peripheral.equals(level.getCapability(PeripheralCapability.get(),tile.getBlockPos(),Direction.DOWN)),"source bytecode Object.equals identity retained through wrappers");
        check(call(peripheral,1,true).length==0&&call(peripheral,0)[0].equals(true)&&tile.redstone,"setRedstone returns no Lua values and updates actual tile");
        check(call(peripheral,3,false).length==0&&call(peripheral,2)[0].equals(false)&&!tile.leftClick,"setLeftClick returns no Lua values and updates actual tile");
        for(int i=0;i<4;i++)check(call(peripheral,5,(double)i).length==0&&((Number)call(peripheral,4)[0]).intValue()==i&&tile.rotation()==i,"source direction index "+i);
        level.setBlock(tile.getBlockPos(),tile.getBlockState().setValue(AnimationTabletBlock.POWERED,true),2);check(((Number)call(peripheral,4)[0]).intValue()==11,"source rotation getter includes powered metadata bit");
        call(peripheral,5,2.9);check(tile.rotation()==2&&!tile.getBlockState().getValue(AnimationTabletBlock.POWERED),"source positive fraction truncation and setter clearing powered bit");
        for(double bad:new double[]{-1,-.25,4}){boolean rejected=false;try{call(peripheral,5,bad);}catch(LuaException expected){rejected=expected.getMessage().startsWith("Invalid value: ");}check(rejected&&tile.rotation()==2,"approved rejection without state mutation "+bad);}
        tile.clearContent();check(call(peripheral,6)[0].equals(false),"hasItem false when empty");tile.setItem(0,new ItemStack(Items.STONE,32));check(call(peripheral,6)[0].equals(true),"hasItem accepts arbitrary item stack");
        tile.swingProgress=0;check(call(peripheral,7)[0].equals(true)&&tile.swingProgress==1&&call(peripheral,7)[0].equals(false),"trigger starts source swing once and rejects busy trigger");
        tile.clearContent();System.out.println("TT_ANIMATION_TABLET_PERIPHERAL_PASS checks="+checks);
    }
}
