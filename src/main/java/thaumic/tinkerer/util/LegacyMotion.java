package thaumic.tinkerer.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Original MiscHelper.setEntityMotionFromVector, including unnormalised nearby movement. */
public final class LegacyMotion {
    public static void towards(Entity entity, Vec3 target, float modifier) {
        var delta = target.subtract(entity.position().add(0, entity.getBbHeight() / 2d, 0));
        if (delta.length() > 1) delta = delta.normalize();
        entity.setDeltaMovement(delta.scale(modifier));
    }
    private LegacyMotion() { }
}
