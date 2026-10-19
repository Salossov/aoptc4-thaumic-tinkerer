package thaumic.tinkerer.api;

/**
 * Common contract for awakened KAMI tools (pickaxe, shovel, axe, sword)
 * allowing utility items like Protoclay to dynamically detect tool types
 * and perform automated tool swapping when interacting with world materials.
 */
public interface IAdvancedTool {
    /**
     * Returns the tool kind: "pick", "shovel", "axe", or "sword".
     */
    String getToolType();
}
