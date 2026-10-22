package thaumic.tinkerer;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original startup option; additional options are connected as their systems are ported. */
public final class TinkererConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FLIGHT;
    public static final ModConfigSpec.BooleanValue DEFLECT_DESTROY_PROJECTILES;
    public static final ModConfigSpec.BooleanValue DEBUG_COMMANDS;
    public static final ModConfigSpec.BooleanValue TOOLTIP_INDICATORS;
    public static final ModConfigSpec.BooleanValue EASY_RESEARCH;
    public static final ModConfigSpec.BooleanValue ORE_DICT_METAL;
    public static final ModConfigSpec.BooleanValue SURVIVAL_SHARE_TOME;
    public static final ModConfigSpec.BooleanValue IMBUED_FIRE;
    public static final ModConfigSpec.BooleanValue IMBUED_FIRE_CAKE;
    public static final ModConfigSpec.BooleanValue CROPS_ALLOW_BONEMEAL;
    public static final ModConfigSpec.BooleanValue KAMI_FORCE_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> KAMI_NETHER_DIMENSION;
    public static final ModConfigSpec.ConfigValue<String> KAMI_END_DIMENSION;
    public static final ModConfigSpec.BooleanValue KAMI_BEDROCK_ENABLED;
    public static final ModConfigSpec.BooleanValue PLACEMENT_MIRROR_PREVIEW;
    public static final ModConfigSpec.IntValue KAMI_BEDROCK_DENSITY;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> KAMI_BEDROCK_BLACKLIST;
    static {
        var builder = new ModConfigSpec.Builder();
        KAMI_FORCE_ENABLED=builder.gameRestart().comment("Force-enable KAMI content, as in the release 164 default.")
                .define("general.kami.forceenabled",true);
        KAMI_NETHER_DIMENSION=builder.gameRestart().comment("Dimension for nether-shard drops; namespaced dimension ID replaces the old numeric ID.")
                .define("general.kami.dimension.nether.id","minecraft:the_nether",TinkererConfig::dimensionName);
        KAMI_END_DIMENSION=builder.gameRestart().comment("Dimension for ender-shard drops; namespaced dimension ID replaces the old numeric ID.")
                .define("general.kami.dimension.end.id","minecraft:the_end",TinkererConfig::dimensionName);
        KAMI_BEDROCK_ENABLED=builder.gameRestart().comment("Enable thaumictinkerer:bedrock; replaces the old numeric dimension ID/zero switch.")
                .define("general.kami.bedrock.enabled",true);
        PLACEMENT_MIRROR_PREVIEW=builder.gameRestart().comment("Show the phantom blocks displayed by the Worldshaper's Seeing Glass.")
                .define("general.kami.placementMirror.blocks.show",true);
        KAMI_BEDROCK_DENSITY=builder.gameRestart().comment("Original number of vertical ore columns per chunk. Nonpositive values produce no columns.")
                .defineInRange("general.bedrock.oreDensity",1,Integer.MIN_VALUE,Integer.MAX_VALUE);
        KAMI_BEDROCK_BLACKLIST=builder.gameRestart().comment("Original OreDictionary group names excluded from bedrock ore generation.")
                .defineListAllowEmpty("general.kami.bedrock.oreBlacklist",java.util.List.of("oreFirestone"),()->"",value->value instanceof String);
        DEBUG_COMMANDS=builder.gameRestart().comment("Enable original debug commands for every player (original permission behavior).")
                .define("general.debugCommands.enabled",false);
        TOOLTIP_INDICATORS=builder.gameRestart().comment("Show the original [TT] and [TTKami] prefixes in research descriptions.")
                .define("general.tooltipIndicators.enabled",true);
        EASY_RESEARCH=builder.gameRestart().comment("Original global easy research mode: buy research with aspect points instead of creating notes. Requires restart.")
                .define("general.research.easymode.enabled",false);
        FLIGHT = builder.gameRestart().comment("Enable the mod's magical flight (original modFlight.enabled).")
                .define("general.modFlight.enabled", true);
        DEFLECT_DESTROY_PROJECTILES=builder.gameRestart().comment("Destroy hostile projectiles with the Distortion focus. False restores original displacement. Does not change KAMI armor. Requires restart.")
                .define("general.focusDeflect.destroyProjectiles",true);
        ORE_DICT_METAL = builder.gameRestart().comment("Original oreDictMetal.enabled, including its inverted empty-tag fallback quirk.")
                .define("general.oreDictMetal.enabled", true);
        SURVIVAL_SHARE_TOME=builder.gameRestart().comment("Set to false to disable the crafting recipe for the Tome of Research Sharing.")
                .define("general.shareTome.survival.enabled",true);
        IMBUED_FIRE=builder.gameRestart().define("general.imbuedFire.enabled",true);
        IMBUED_FIRE_CAKE=builder.gameRestart().define("general.imbuedFire.cake.enabled",true);
        CROPS_ALLOW_BONEMEAL=builder.gameRestart().comment("Allows crops to be grown using bonemeal. Useful for debug purposes.")
                .define("general.cropsAllowBonemeal.enabled",false);
        SPEC = builder.build();
    }
    private TinkererConfig() { }
    private static boolean dimensionName(Object value) {return value instanceof String name&&name.contains(":")&&net.minecraft.resources.ResourceLocation.tryParse(name)!=null;}
}
