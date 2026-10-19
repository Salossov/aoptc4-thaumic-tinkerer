package thaumic.tinkerer;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original startup option; additional options are connected as their systems are ported. */
public final class TinkererConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FLIGHT;
    public static final ModConfigSpec.BooleanValue DEBUG_COMMANDS;
    public static final ModConfigSpec.BooleanValue ORE_DICT_METAL;
    public static final ModConfigSpec.BooleanValue SURVIVAL_SHARE_TOME;
    public static final ModConfigSpec.BooleanValue IMBUED_FIRE;
    public static final ModConfigSpec.BooleanValue IMBUED_FIRE_CAKE;
    public static final ModConfigSpec.BooleanValue CROPS_ALLOW_BONEMEAL;
    static {
        var builder = new ModConfigSpec.Builder();
        DEBUG_COMMANDS=builder.comment("Enable original debug commands for every player (original permission behavior).")
                .define("general.debugCommands.enabled",false);
        FLIGHT = builder.comment("Enable the mod's magical flight (original modFlight.enabled).")
                .define("general.modFlight.enabled", true);
        ORE_DICT_METAL = builder.comment("Original oreDictMetal.enabled, including its inverted empty-tag fallback quirk.")
                .define("general.oreDictMetal.enabled", true);
        SURVIVAL_SHARE_TOME=builder.comment("Set to false to disable the crafting recipe for the Tome of Research Sharing.")
                .define("general.shareTome.survival.enabled",true);
        IMBUED_FIRE=builder.define("general.imbuedFire.enabled",true);
        IMBUED_FIRE_CAKE=builder.define("general.imbuedFire.cake.enabled",true);
        CROPS_ALLOW_BONEMEAL=builder.comment("Allows crops to be grown using bonemeal. Useful for debug purposes.")
                .define("general.cropsAllowBonemeal.enabled",false);
        SPEC = builder.build();
    }
    private TinkererConfig() { }
}
