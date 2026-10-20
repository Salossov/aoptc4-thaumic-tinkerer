package thaumic.tinkerer.compat;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.*;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;

/** Release-164 eight-method peripheral; negative rotations are rejected by user approval. */
public final class AnimationTabletPeripheral implements IDynamicPeripheral {
    private static final String[] METHODS={"getRedstone","setRedstone","getLeftClick","setLeftClick","getRotation","setRotation","hasItem","trigger"};
    private final AnimationTabletBlockEntity tile;
    public AnimationTabletPeripheral(AnimationTabletBlockEntity tile){this.tile=tile;}
    @Override public String getType(){return "tt_animationTablet";}
    @Override public String[] getMethodNames(){return METHODS.clone();}
    @Override public Object getTarget(){return tile;}
    @Override public boolean equals(IPeripheral other){return other instanceof AnimationTabletPeripheral peripheral&&peripheral.tile==tile;}
    @Override public MethodResult callMethod(IComputerAccess computer,ILuaContext context,int method,IArguments args)throws LuaException{
        args.escapes();
        return context.executeMainThreadTask(()->{
            var level=tile.getLevel();if(level==null||level.isClientSide()||level.getServer()==null||!level.getServer().isSameThread())throw new LuaException("Server thread required");
            if(tile.isRemoved())throw new LuaException("Peripheral is no longer present");
            return switch(method){
                case 0->new Object[]{tile.redstone};
                case 1->{tile.redstone=args.getBoolean(0);tile.sync();yield new Object[0];}
                case 2->new Object[]{tile.leftClick};
                case 3->{tile.leftClick=args.getBoolean(0);tile.sync();yield new Object[0];}
                case 4->new Object[]{tile.reportedRotation()};
                case 5->{double value=args.getDouble(0);int rotation=(int)value;if(value<0||rotation>3)throw new LuaException("Invalid value: "+rotation+".");tile.rotation(rotation);yield new Object[0];}
                case 6->new Object[]{!tile.isEmpty()};
                case 7->new Object[]{tile.trigger()};
                default->new Object[0];
            };
        });
    }
}
