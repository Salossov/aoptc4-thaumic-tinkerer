package thaumic.tinkerer.client;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.CamoBlock;
import thaumic.tinkerer.block.CamoBlockEntity;
import thaumic.tinkerer.block.CamoModelData;
import thaumic.tinkerer.block.TransvectorContent;

@EventBusSubscriber(modid = ThaumicTinkerer.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CamoModels {
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event) {
        var key = new ModelResourceLocation(ThaumicTinkerer.id("interface"), "");
        event.getModels().put(key, new Cube(Objects.requireNonNull(event.getModels().get(key))));
        for (boolean powered : new boolean[]{false, true}) {
            var dislocator = new ModelResourceLocation(ThaumicTinkerer.id("dislocator"), "powered=" + powered);
            event.getModels().put(dislocator, new Cube(Objects.requireNonNull(event.getModels().get(dislocator)), true));
        }
        var platform=new ModelResourceLocation(ThaumicTinkerer.id("platform"), "");
        event.getModels().put(platform,new Cube(Objects.requireNonNull(event.getModels().get(platform))));
        if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) {
            var golem = new ModelResourceLocation(ThaumicTinkerer.id("golem_connector"), "");
            event.getModels().put(golem, new Cube(Objects.requireNonNull(event.getModels().get(golem))));
        }
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, index) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof CamoBlockEntity tile)) return 0xffffff;
            var camo = tile.camouflage();
            if (camo.isAir() || camo.getBlock() instanceof CamoBlock) return 0xffffff;
            // Original BlockCamo supplies colorMultiplier to every face, even the
            // otherwise untinted bottom/side icons of a grass-block camouflage.
            return Minecraft.getInstance().getBlockColors().getColor(camo, level, pos, 0);
        }, TransvectorContent.INTERFACE.get(), TransvectorContent.DISLOCATOR.get(), thaumic.tinkerer.block.PlatformContent.BLOCK.get());
        if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) event.register((state, level, pos, index) -> {
            if (level == null || pos == null || !(level.getBlockEntity(pos) instanceof CamoBlockEntity tile)) return 0xffffff;
            var camo = tile.camouflage();
            return camo.isAir() || camo.getBlock() instanceof CamoBlock ? 0xffffff : Minecraft.getInstance().getBlockColors().getColor(camo, level, pos, 0);
        }, thaumic.tinkerer.block.GolemConnectorContent.BLOCK.get());
    }
    private static BlockState camouflage(ModelData data) {
        var state = data.get(CamoModelData.CAMOUFLAGE);
        return state == null || state.isAir() || state.getBlock() instanceof CamoBlock ? null : state;
    }
    private static final class Cube extends BakedModelWrapper<BakedModel> {
        private final Map<BlockState, Map<Direction, List<BakedQuad>>> cubes = new ConcurrentHashMap<>();
        private final Map<Integer, Map<Direction, List<BakedQuad>>> arrows = new ConcurrentHashMap<>();
        private final boolean oriented;
        Cube(BakedModel base) { this(base, false); }
        Cube(BakedModel base, boolean oriented) { super(base); this.oriented = oriented; }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            var camo = camouflage(data);
            if (camo == null) {
                if (!oriented) return super.getQuads(state, side, random, data, renderType);
                if (side == null) return List.of();
                Integer value = data.get(CamoModelData.ORIENTATION); int direction = value == null ? 0 : value;
                if (direction < 0 || direction > 5) direction = 6;
                return arrows.computeIfAbsent(direction, orientation -> bakeArrow(state, orientation)).get(side);
            }
            if (side == null) return List.of();
            return cubes.computeIfAbsent(camo, Cube::bake).get(side);
        }
        @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
            return camouflage(data) == null ? super.getRenderTypes(state, random, data) : ChunkRenderTypeSet.of(RenderType.cutout());
        }
        @Override public TextureAtlasSprite getParticleIcon(ModelData data) {
            var camo = camouflage(data); return camo == null ? super.getParticleIcon(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(camo).getParticleIcon(ModelData.EMPTY);
        }
        private static Map<Direction, List<BakedQuad>> bake(BlockState state) {
            var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state); var result = new EnumMap<Direction, List<BakedQuad>>(Direction.class);
            for (var side : Direction.values()) {
                var quads = model.getQuads(state, side, RandomSource.create(42), ModelData.EMPTY, null);
                var sprite = quads.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : quads.getFirst().getSprite();
                var face = new BlockElementFace(side, 0, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0));
                var quad = new FaceBakery().bakeQuad(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), face, sprite, side, BlockModelRotation.X0_Y0, null, true);
                result.put(side, List.of(quad));
            }
            return Map.copyOf(result);
        }
        private Map<Direction, List<BakedQuad>> bakeArrow(BlockState state, int orientation) {
            var base = originalModel.getQuads(state, Direction.UP, RandomSource.create(42), ModelData.EMPTY, null).getFirst().getSprite();
            var arrow = originalModel.getQuads(state, Direction.DOWN, RandomSource.create(42), ModelData.EMPTY, null).getFirst().getSprite();
            var result = new EnumMap<Direction, List<BakedQuad>>(Direction.class);
            for (var side : Direction.values()) {
                var face = new BlockElementFace(side, -1, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0));
                result.put(side, List.of(new FaceBakery().bakeQuad(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), face,
                        side.get3DDataValue() == orientation ? arrow : base, side, BlockModelRotation.X0_Y0, null, true)));
            }
            return Map.copyOf(result);
        }
    }
    private CamoModels() { }
}
