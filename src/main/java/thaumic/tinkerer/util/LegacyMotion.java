package thaumic.tinkerer.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Original MiscHelper.setEntityMotionFromVector, including unnormalised nearby movement. */
public final class LegacyMotion {
    public static void towards(Entity entity, Vec3 target, float modifier) {
        entity.setDeltaMovement(velocity(entity.position(),entity.getBbHeight(),target,modifier));
    }
    /** Original Vector3 uses reciprocal multiplication, not Vec3.normalize's divisions. */
    public static Vec3 velocity(Vec3 position,float height,Vec3 target,float modifier){
        var delta=new Vec3(target.x-position.x,target.y-(position.y+(double)(height/2f)),target.z-position.z);
        double length=delta.length();if(length>1)delta=delta.scale(1.0/length);
        return delta.scale(modifier);
    }
    private LegacyMotion() { }
}
