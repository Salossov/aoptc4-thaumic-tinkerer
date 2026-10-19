package thaumic.tinkerer.kami;

import java.util.function.DoubleSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumic.tinkerer.TinkererConfig;

/** Original player-caused drops: inclusive thresholds, no looting bonus and no
 * entity/level RNG draw. The dimension check precedes the single Math.random call.
 */
public final class DimensionalShardDrops {
    public static void register() {NeoForge.EVENT_BUS.addListener(DimensionalShardDrops::drops);}
    public static ItemStack rollDrop(LivingEntity victim,DamageSource source,DoubleSupplier random) {
        if(victim.level().isClientSide()||!(source.getEntity() instanceof Player))return ItemStack.EMPTY;
        String dimension=victim.level().dimension().location().toString();
        if(victim instanceof EnderMan&&dimension.equals(TinkererConfig.KAMI_END_DIMENSION.get())) {
            if(random.getAsDouble()<=.03125D)return KamiMaterials.ENDER_SHARD.get().getDefaultInstance();
        }
        if(victim instanceof ZombifiedPiglin&&dimension.equals(TinkererConfig.KAMI_NETHER_DIMENSION.get())) {
            if(random.getAsDouble()<=.0625D)return KamiMaterials.NETHER_SHARD.get().getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }
    private static void drops(LivingDropsEvent event) {
        append(event,Math::random);
    }
    public static void append(LivingDropsEvent event,DoubleSupplier random) {
        var victim=event.getEntity();var stack=rollDrop(victim,event.getSource(),random);
        if(!stack.isEmpty())event.getDrops().add(new ItemEntity(victim.level(),victim.getX(),victim.getY(),victim.getZ(),stack));
    }
    private DimensionalShardDrops() {}
}
