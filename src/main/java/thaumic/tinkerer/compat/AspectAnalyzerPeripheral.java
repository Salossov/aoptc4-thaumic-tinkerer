package thaumic.tinkerer.compat;

import java.util.*;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import thaumic.tinkerer.block.AspectAnalyzerBlockEntity;

public final class AspectAnalyzerPeripheral implements IDynamicPeripheral {
    private static final String[] METHODS={"hasItem","itemHasAspects","getAspects","getAspectCount"};
    private final AspectAnalyzerBlockEntity tile;
    public AspectAnalyzerPeripheral(AspectAnalyzerBlockEntity tile){this.tile=tile;}
    @Override public String getType(){return "tt_aspectanalyzer";}
    @Override public String[] getMethodNames(){return METHODS.clone();}
    @Override public Object getTarget(){return tile;}
    @Override public boolean equals(IPeripheral other){return other instanceof AspectAnalyzerPeripheral peripheral&&peripheral.tile==tile;}
    @Override public MethodResult callMethod(IComputerAccess computer,ILuaContext context,int method,IArguments args)throws LuaException{
        return context.executeMainThreadTask(()->{
            var level=tile.getLevel();if(level==null||level.isClientSide()||level.getServer()==null||!level.getServer().isSameThread())throw new LuaException("Server thread required");
            if(tile.isRemoved())throw new LuaException("Peripheral is no longer present");
            if(method==0)return new Object[]{!tile.isEmpty()};
            var aspects=tile.aspects();
            if(method==1)return new Object[]{!aspects.isEmpty()};
            if(method==2){var result=new LinkedHashMap<Double,String>();double index=1;for(var tag:new TreeSet<>(aspects.keySet()))result.put(index++,tag);return new Object[]{result};}
            if(method==3){var result=new LinkedHashMap<String,Double>();for(var tag:new TreeSet<>(aspects.keySet()))result.put(tag,aspects.get(tag).doubleValue());return new Object[]{result};}
            return new Object[0];
        });
    }
}
