package thaumic.tinkerer.kami.focus;

import net.minecraft.world.entity.player.Player;

/**
 * Player application of the selected modern curve, with integer-point truncation.
 */
public final class ExperienceHelper {

    public static int getPlayerXP(Player player) {
        return ExperienceMath.total(player.experienceLevel,player.experienceProgress,player.getXpNeededForNextLevel());
    }

    public static boolean drainPlayerXP(Player player, int amount) {
        return addPlayerXP(player, -amount);
    }

    public static boolean addPlayerXP(Player player, int amount) {
        long changed=(long)getPlayerXP(player)+amount;
        if (changed < 0) {
            return false;
        }
        int experience=(int)Math.min(Integer.MAX_VALUE,changed);
        player.totalExperience = experience;
        int level = getLevelForExperience(experience);
        player.experienceLevel = level;
        int expForLevel = getExperienceForLevel(level);
        int needed = player.getXpNeededForNextLevel();
        player.experienceProgress = ExperienceMath.progressForPoints(experience-expForLevel,needed);
        return true;
    }

    public static int getExperienceForLevel(int level) {
        return ExperienceMath.pointsForLevel(level);
    }

    public static int getLevelForExperience(int experience) {
        return ExperienceMath.levelForPoints(experience);
    }

    private ExperienceHelper() {}
}
