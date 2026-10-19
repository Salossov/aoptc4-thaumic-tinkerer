package thaumic.tinkerer.summon;

import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.*;

/** Approved equivalents for old single-class horse and skeleton metadata variants. */
public final class SummonSpawns {
    public record Prepared(Mob entity, boolean witherBow) {
        public void initialize(ServerLevel level) {
            entity.finalizeSpawn(level,level.getCurrentDifficultyAt(entity.blockPosition()),MobSpawnType.MOB_SUMMONED,null);
            if(witherBow){entity.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));((AbstractSkeleton)entity).reassessWeaponGoal();}
            entity.playAmbientSound();
        }
    }
    /** Creates the replacement before world insertion; other spawn initialization remains native. */
    public static Prepared prepare(ServerLevel level,Mob original) {
        if(original.getType()==EntityType.HORSE&&original.getRandom().nextInt(10)==0){original.discard();return new Prepared(Objects.requireNonNull(EntityType.DONKEY.create(level)),false);}
        if(original.getType()==EntityType.SKELETON&&level.dimensionType().ultraWarm()){
            boolean bow=original.getRandom().nextInt(5)==0;original.discard();return new Prepared(Objects.requireNonNull(EntityType.WITHER_SKELETON.create(level)),bow);
        }
        return new Prepared(original,false);
    }
    private SummonSpawns(){ }
}
