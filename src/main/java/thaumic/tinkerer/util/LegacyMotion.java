package thaumic.tinkerer.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Original MiscHelper.setEntityMotionFromVector, including unnormalised nearby movement. */
public final class LegacyMotion {
    /** EntityLivingBase.getLook(1), retaining the original float operation order. */
    public static Vec3 look(float yaw, float pitch) {
        float cos = Mth.cos(-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float sin = Mth.sin(-yaw * ((float) Math.PI / 180) - (float) Math.PI);
        float horizontal = -Mth.cos(-pitch * ((float) Math.PI / 180));
        float vertical = Mth.sin(-pitch * ((float) Math.PI / 180));
        // The old Vec3 constructor canonicalized negative zero.
        return Vec3.ZERO.add((double) (sin * horizontal), (double) vertical, (double) (cos * horizontal));
    }
    public static void towards(Entity entity, Vec3 target, float modifier) {
        entity.setDeltaMovement(velocity(entity.position(),entity.getBbHeight(),target,modifier));
    }
    /** Original Vector3 uses reciprocal multiplication, not Vec3.normalize's divisions. */
    public static Vec3 velocity(Vec3 position,float height,Vec3 target,float modifier){
        var delta=new Vec3(target.x-position.x,target.y-(position.y+(double)(height/2f)),target.z-position.z);
        double length=delta.length();if(length>1)delta=delta.scale(1.0/length);
        return delta.scale(modifier);
    }
    /** Original deflection advances along the existing velocity without changing it. */
    public static Vec3 deflection(Vec3 position, Vec3 velocity, Vec3 source) {
        double length = velocity.length();
        if (length != 0.0) velocity = velocity.scale(1.0 / length);
        double x = position.x - source.x, y = position.y - source.y, z = position.z - source.z;
        return position.add(velocity.scale(Math.sqrt(x * x + y * y + z * z) * 2.0));
    }
    private LegacyMotion() { }
}
