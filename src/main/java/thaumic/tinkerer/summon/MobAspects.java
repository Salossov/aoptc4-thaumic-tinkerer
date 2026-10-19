package thaumic.tinkerer.summon;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import thaumcraft.api.aspects.Aspect;

/** Extracted EnumMobAspect order and exact-class matching, including flattened vanilla families. */
public final class MobAspects {
    public record Row(String legacyName, ResourceLocation entity, List<ResourceLocation> harvestTypes, List<Aspect> aspects, float scale, float offset) { }
    public static final List<Aspect> ASPECTS;
    public static final List<Row> ROWS;
    private static final Map<EntityType<?>, Class<?>> CLASSES = new IdentityHashMap<>();
    static {
        try (var input = Objects.requireNonNull(MobAspects.class.getResourceAsStream("/data/thaumictinkerer/thaumictinkerer/mob_aspects.json")); var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            var data = JsonParser.parseReader(reader).getAsJsonObject(); var aspects = new ArrayList<Aspect>();
            for (var value : data.getAsJsonArray("aspects")) aspects.add(Objects.requireNonNull(Aspect.getAspect(value.getAsString())));
            ASPECTS = List.copyOf(aspects); var rows = new ArrayList<Row>();
            for (var value : data.getAsJsonArray("mobs")) {
                var row = value.getAsJsonObject(); var tags = new ArrayList<Aspect>(); var types = new ArrayList<ResourceLocation>();
                for (var tag : row.getAsJsonArray("aspects")) tags.add(Objects.requireNonNull(Aspect.getAspect(tag.getAsString())));
                for (var type : row.getAsJsonArray("harvest_types")) types.add(ResourceLocation.parse(type.getAsString()));
                rows.add(new Row(row.get("legacy_name").getAsString(), ResourceLocation.parse(row.get("entity").getAsString()), List.copyOf(types), List.copyOf(tags), row.get("scale").getAsFloat(), row.get("offset").getAsFloat()));
            }
            ROWS = List.copyOf(rows);
        } catch (java.io.IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    public static List<Aspect> forEntity(Entity entity) {
        for (var row : ROWS) for (var id : row.harvestTypes()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null); if (type == null) continue;
            var actual = CLASSES.computeIfAbsent(type, key -> { var prototype = key.create(entity.level()); return prototype == null ? Void.class : prototype.getClass(); });
            if (entity.getClass() == actual) return row.aspects();
        }
        return List.of();
    }
    public static Row matching(List<Aspect> aspects) {
        for (var row : ROWS) if (row.aspects().containsAll(aspects) && aspects.containsAll(row.aspects())) return row;
        return null;
    }
    private MobAspects() { }
}
