package thaumic.tinkerer.api;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;

/** Modern counterpart of the original IProjectile/ProjectileHelper extension points. */
public final class ProjectileHooks {
    private static final Map<Class<? extends Entity>, Function<Entity, Entity>> OWNERS = new IdentityHashMap<>();
    public static final List<Class<?>> DEFLECT_BLACKLIST = new ArrayList<>(List.of(ThrownExperienceBottle.class));
    static {
        register(AbstractArrow.class, entity -> ((AbstractArrow) entity).getOwner());
        // Current core frost shards and other original EntityThrowable families inherit this class.
        register(ThrowableProjectile.class, entity -> ((ThrowableProjectile) entity).getOwner());
    }
    public static void register(Class<? extends Entity> type, Function<Entity, Entity> owner) { OWNERS.put(type, owner); }
    public static boolean isProjectile(Entity entity) { return OWNERS.keySet().stream().anyMatch(type -> type.isInstance(entity)); }
    public static Entity owner(Entity entity) {
        var exact = OWNERS.get(entity.getClass());
        if (exact != null) return exact.apply(entity);
        for (var entry : OWNERS.entrySet()) if (entry.getKey().isInstance(entity)) return entry.getValue().apply(entity);
        return null;
    }
    public static boolean blacklisted(Entity entity) {
        if (DEFLECT_BLACKLIST.contains(entity.getClass())) return true;
        if(net.neoforged.fml.ModList.get().isLoaded("botania"))return thaumic.tinkerer.integration.BotaniaCompatibility.harmless(entity);
        for (var type : DEFLECT_BLACKLIST) if (type.isInterface() && type.isInstance(entity)) return true;
        return false;
    }
    private ProjectileHooks() { }
}
