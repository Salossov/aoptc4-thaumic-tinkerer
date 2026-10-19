package thaumic.tinkerer.verification;

import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.GolemConnectorBlockEntity;
import thaumic.tinkerer.block.GolemConnectorContent;
import thaumic.tinkerer.block.TransvectorContent;
import thaumic.tinkerer.item.ConnectorItem;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class GolemConnectorRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void golemConnector(GameTestHelper helper) throws Exception {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var id = ThaumicTinkerer.id("golem_connector");
        var player = FocusRulesTest.player(helper, "[TTGolemConnector]");
        if (!GolemConnectorContent.enabled()) {
            check(!BuiltInRegistries.BLOCK.containsKey(id) && !BuiltInRegistries.ITEM.containsKey(id), "connector block and item absent without ComputerCraft");
            check(level.getRecipeManager().byKey(id).isEmpty() && !ResearchApi.exists(player, id.toString()), "recipe and research absent without ComputerCraft");
            check(!ResearchApi.exists(player, "thaumictinkerer:peripherals"), "original peripheral overview absent without ComputerCraft");
            System.out.println("TT_GOLEM_CONNECTOR_ABSENT_PASS checks=" + checks); helper.succeed(); return;
        }
        var pos = helper.absolutePos(new BlockPos(2, 170, 2)); var outside = pos.offset(35, 0, 0);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("thaumcraft:golem"));
        var golem = (LivingEntity)type.create(level); var second = (LivingEntity)type.create(level);
        var held = new ItemStack(TransvectorContent.CONNECTOR.get());
        try {
            level.setBlock(pos, GolemConnectorContent.BLOCK.get().defaultBlockState(), 3);
            var tile = (GolemConnectorBlockEntity)level.getBlockEntity(pos); var state = level.getBlockState(pos);
            check(tile != null && tile.connectedId() == null && tile.golem() == null, "conditional block creates its real native block entity");
            check(state.canOcclude() && state.getDestroySpeed(level, pos) == 0 && state.getBlock().getExplosionResistance() == 0 && state.ignitedByLava(), "original opaque wood cube, lava ignition and default zero hardness/resistance");
            golem.setPos(Vec3.atCenterOf(pos.above())); ((net.minecraft.world.entity.Mob)golem).setNoAi(true); level.addFreshEntity(golem);
            second.setPos(Vec3.atCenterOf(outside)); ((net.minecraft.world.entity.Mob)second).setNoAi(true); level.addFreshEntity(second);
            player.setPos(Vec3.atCenterOf(pos)); player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            check(ConnectorItem.selected(held).orElseThrow().equals(pos), "actual binder selects golem connector");
            check(!held.getItem().interactLivingEntity(held, player, golem, InteractionHand.MAIN_HAND).consumesAction() && ConnectorItem.selected(held).isPresent(), "ordinary entity click preserves selection");
            player.setShiftKeyDown(true);
            var decoy = new ItemStack(TransvectorContent.CONNECTOR.get());
            check(held.getItem().interactLivingEntity(decoy, player, golem, InteractionHand.MAIN_HAND).consumesAction(), "sneaking entity use completes the binding");
            check(golem.getUUID().equals(tile.connectedId()) && tile.golem() == golem && tile.control() != null && ConnectorItem.selected(held).isEmpty(), "binder updates live entity cache and clears the actual held selection");
            check(ConnectorItem.selected(decoy).isEmpty() && !decoy.has(DataComponents.CUSTOM_DATA), "original held-stack quirk does not modify the decoy argument");
            tile.camouflage(Blocks.STONE.defaultBlockState()); var saved = tile.saveWithFullMetadata(level.registryAccess());
            check(saved.getLong("UUIDMost") == golem.getUUID().getMostSignificantBits() && saved.getLong("UUIDLeast") == golem.getUUID().getLeastSignificantBits(), "original UUIDMost/UUIDLeast persistence");
            var restored = new GolemConnectorBlockEntity(pos, state); restored.setLevel(level); restored.loadWithComponents(saved, level.registryAccess()); restored.verifyGolem();
            check(restored.golem() == golem && restored.camouflage().is(Blocks.STONE), "actual NBT reload restores binding and camouflage");
            var noId = saved.copy(); noId.remove("UUIDMost"); noId.remove("UUIDLeast"); restored.loadWithComponents(noId, level.registryAccess());
            check(restored.connectedId().equals(golem.getUUID()), "original missing UUID tags retain an existing binding");
            tile.connect(second.getUUID());
            check(tile.golem() == golem && tile.connectedId().equals(second.getUUID()), "new UUID outside the 30-block box retains the previous cached golem");
            second.setPos(Vec3.atCenterOf(pos.east(4))); tile.verifyGolem();
            check(tile.golem() == second, "verification resolves the new UUID once its golem enters the original box");
            second.setPos(Vec3.atCenterOf(outside)); tile.verifyGolem();
            check(tile.golem() == second, "leaving the box does not clear original cached entity");
            tile.connect(null);
            check(tile.golem() == null && tile.connectedId() == null, "explicit disconnect clears UUID and cached entity");
            ConnectorItem.select(held, pos); held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            check(ConnectorItem.selected(held).isEmpty(), "block-to-block use from a golem connector clears the invalid origin");
            check(held.getItem().interactLivingEntity(held, player, golem, InteractionHand.MAIN_HAND).consumesAction() && tile.connectedId() == null, "unselected sneaking golem click is handled without binding");
            ConnectorItem.select(held, pos.east(8));
            check(!held.getItem().interactLivingEntity(held, player, golem, InteractionHand.MAIN_HAND).consumesAction() && ConnectorItem.selected(held).isEmpty(), "missing selected connector clears selection and returns unhandled");
            var recipe = (IArcaneRecipe)level.getRecipeManager().byKey(id).orElseThrow().value();
            check(recipe.research().equals(id.toString()) && recipe.vis().equals(Map.of("aer", 20, "ordo", 5, "perditio", 15)), "actual conditional recipe has exact original research and vis");
            check(ResearchApi.exists(player, id.toString()) && ResearchApi.exists(player, "thaumictinkerer:peripherals"), "actual research catalog includes both original ComputerCraft branches");
            GolemPeripheralChecks.run(tile, golem);
            EssentiaPeripheralChecks.run(level, pos);
            DevicePeripheralChecks.run(level, pos);
            BorePeripheralChecks.run(level, pos);
            AspectDisplayChecks.run(level, pos);
            FumeTurtleChecks.run(level, pos);
            System.out.println("TT_GOLEM_CONNECTOR_PASS checks=" + checks); helper.succeed();
        } finally { player.setShiftKeyDown(false); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); golem.discard(); second.discard(); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
    }
}
