package thaumic.tinkerer.kami.bedrock;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** Logical dimension keys have no native tag registry. Use the native tag loader
 * for their data-pack tag files, including replace, nested tags and removals.
 */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class BedrockPortalWorlds {
    public static final ResourceLocation SURFACE_WORLDS=ThaumicTinkerer.id("bedrock_portal_surface_worlds");
    private static volatile Set<ResourceKey<Level>> surfaceWorlds=Set.of();
    public static boolean isSurfaceWorld(Level level){return surfaceWorlds.contains(level.dimension());}
    private static final class Loader extends SimplePreparableReloadListener<Set<ResourceKey<Level>>> {
        @Override protected Set<ResourceKey<Level>> prepare(ResourceManager manager,ProfilerFiller profiler){
            var tags=new TagLoader<ResourceKey<Level>>(id->Optional.of(ResourceKey.create(Registries.DIMENSION,id)),"tags/dimension");
            return Set.copyOf(tags.loadAndBuild(manager).getOrDefault(SURFACE_WORLDS,List.of()));
        }
        @Override protected void apply(Set<ResourceKey<Level>> prepared,ResourceManager manager,ProfilerFiller profiler){surfaceWorlds=prepared;}
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event){event.addListener(new Loader());}
    private BedrockPortalWorlds(){}
}
