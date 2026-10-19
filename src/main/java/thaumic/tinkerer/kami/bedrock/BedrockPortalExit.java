package thaumic.tinkerer.kami.bedrock;

import net.minecraft.BlockUtil;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.*;
import net.minecraft.world.phys.Vec3;

/** Modern NetherPortalBlock exit calculation for a source block without AXIS.
 * BedrockPortalBlock has no directional state: native entry defaults to X and
 * relative offset(.5,0,0). Destination rectangle/collision checks remain native.
 */
public final class BedrockPortalExit {
    public static DimensionTransition transition(ServerLevel target,BlockPos anchor,Entity entity){
        var state=target.getBlockState(anchor);var axis=state.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        var rectangle=BlockUtil.getLargestRectangleAround(anchor,axis,21,Direction.Axis.Y,21,pos->target.getBlockState(pos)==state);
        var dimensions=entity.getDimensions(entity.getPose());
        double along=(double)dimensions.width()/2+((double)rectangle.axis1Size-(double)dimensions.width())*.5;
        var bottom=rectangle.minCorner;boolean xAxis=axis==Direction.Axis.X;
        var position=new Vec3(bottom.getX()+(xAxis?along:.5),bottom.getY(),bottom.getZ()+(xAxis?.5:along));
        var safe=PortalShape.findCollisionFreePosition(position,target,entity,dimensions);
        var speed=entity.getDeltaMovement();
        if(!xAxis)speed=new Vec3(speed.z,speed.y,-speed.x);
        return new DimensionTransition(target,safe,speed,entity.getYRot()+(xAxis?0:90),entity.getXRot(),DimensionTransition.DO_NOTHING);
    }
    private BedrockPortalExit(){}
}
