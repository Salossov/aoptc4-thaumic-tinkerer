package thaumic.tinkerer.integration;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Release 164's sixteen materials. Optional library types stay behind the presence guard. */
public final class MultipartCompatibility {
    public record MaterialSpec(String legacy_block, int metadata, String block, String render_block,
                               float hardness, float explosion_resistance, String transparency, int light) {
        public BlockState sourceState() { return state(block); }
        public BlockState renderState() { return state(render_block); }
        private static BlockState state(String name) {
            return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(name))
                    .orElseThrow(() -> new IllegalStateException("Missing micro material block: " + name)).defaultBlockState();
        }
    }

    private static final List<MaterialSpec> MATERIALS = load();
    private static List<MaterialSpec> load() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(MultipartCompatibility.class.getResourceAsStream(
                "/assets/thaumictinkerer/compat/multipart_materials.json")), StandardCharsets.UTF_8)) {
            var specs = new Gson().fromJson(reader, MaterialSpec[].class);
            if (specs.length != 16) throw new IllegalStateException("Release 164 must register sixteen micro materials");
            return List.of(specs);
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }

    public static List<MaterialSpec> materials() { return MATERIALS; }
    public static void register(IEventBus bus) {
        if (ModList.get().isLoaded("cb_microblock")) Backend.register(bus);
    }

    private static final class Backend {
        static void register(IEventBus bus) {
            bus.addListener((RegisterEvent event) -> event.register(codechicken.microblock.api.MicroMaterial.MULTIPART_TYPES,
                    helper -> MATERIALS.forEach(spec -> helper.register(
                            codechicken.microblock.api.BlockMicroMaterial.makeMaterialKey(spec.sourceState()), new LegacyMaterial(spec)))));
        }
    }

    private static final class LegacyMaterial extends codechicken.microblock.api.BlockMicroMaterial {
        private final MaterialSpec spec;
        private final BlockState source;
        LegacyMaterial(MaterialSpec spec) {
            // Static getIcon(1) on the old totem returned obsidian tile on all six faces.
            super(spec.renderState());
            this.spec = spec;
            source = spec.sourceState();
        }
        @Override public ItemStack getItem() { return new ItemStack(source.getBlock()); }
        @Override public int getLightEmission() { return spec.light(); }
        @Override public float getStrength(Player player) {
            // Legacy BlockMultipart divided the material's result by 30; CB Multipart no longer does.
            return player.getDigSpeed(source, new BlockPos(0, -1, 0)) / spec.hardness() / 30f;
        }
        @Override public boolean isTransparent() {
            return switch (spec.transparency()) {
                case "transparent" -> true;
                case "leaves" -> FMLEnvironment.dist.isClient() && LeafGraphics.fancy();
                default -> false;
            };
        }
        @Override public float getExplosionResistance(BlockGetter level, BlockPos pos, Explosion explosion) {
            return spec.explosion_resistance();
        }
    }

    private static final class LeafGraphics {
        static boolean fancy() { return net.minecraft.client.Minecraft.useFancyGraphics(); }
    }
    private MultipartCompatibility() {}
}
