package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.CamoBlock;
import thaumic.tinkerer.block.TransvectorContent;
import thaumic.tinkerer.block.TransvectorInterfaceBlockEntity;
import thaumic.tinkerer.item.ConnectorItem;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class TransvectorRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(label); }
    private static void click(net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void transvectorBinding(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        var pos = new BlockPos(anchor.getX(), -1, anchor.getZ()); var chestPos = pos.offset(4, 0, 4); var secondPos = pos.offset(-2, 0, 1);
        var otherPos = pos.east(); var fixturePos = pos.offset(1, 0, -2); var jarPos = pos.offset(-2, 0, -2);
        var changed = new ArrayList<>(List.of(pos, chestPos, secondPos, otherPos, fixturePos, jarPos));
        var player = FocusRulesTest.player(helper, "[TTTransvector]"); var binder = new ItemStack(TransvectorContent.CONNECTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, binder); player.setPos(Vec3.atCenterOf(pos));
        try {
            level.setBlock(pos, TransvectorContent.INTERFACE.get().defaultBlockState(), 3);
            var tile = (TransvectorInterfaceBlockEntity)level.getBlockEntity(pos);
            check(tile != null && !tile.hasBinding() && ConnectorItem.selected(binder).isEmpty(), "new interface and binder are unbound");
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH);
            check(items != null && items.getSlots() == 0, "actual unbound capability has no inventory");
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3); var chest = (Container)level.getBlockEntity(chestPos);
            check(level.getBlockState(pos).useItemOn(binder, level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)) == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                    "camouflage does not consume the binder's interaction");
            click(player, pos);
            check(ConnectorItem.selected(binder).orElseThrow().equals(pos), "binder selects the interface at Y minus one");
            click(player, chestPos);
            check(tile.hasBinding() && tile.targetPosition().equals(chestPos) && ConnectorItem.selected(binder).isEmpty(), "binding at Y minus one succeeds at the four-by-four diagonal range corner");
            check(tile.resolve() == chest && items.getSlots() == 27 && tile.getContainerSize() == 27, "same cached capability and native Container resolve the actual chest");
            check(items.insertItem(0, new ItemStack(Items.DIAMOND, 5), false).isEmpty() && chest.getItem(0).getCount() == 5, "real capability writes conserve linked chest items");
            tile.setItem(1, new ItemStack(Items.GOLD_INGOT, 3));
            check(chest.getItem(1).getCount() == 3 && tile.removeItem(1, 2).getCount() == 2 && chest.getItem(1).getCount() == 1, "direct Container setters/removal delegate to target");
            check(tile.removeItemNoUpdate(1).is(Items.GOLD_INGOT) && chest.getItem(1).isEmpty(), "native no-update removal empties the actual target slot");
            check(tile.getSlotsForFace(Direction.DOWN).length == 27 && tile.canPlaceItemThroughFace(0, new ItemStack(Items.STONE), Direction.UP), "ordinary inventory exposes original unsided slots through all sides");
            var saved = tile.saveWithFullMetadata(level.registryAccess()); var restored = new TransvectorInterfaceBlockEntity(pos, tile.getBlockState()); restored.loadWithComponents(saved, level.registryAccess()); restored.setLevel(level);
            check(saved.getBoolean("bound") && saved.getInt("yt") == -1 && restored.hasBinding() && restored.resolve() == chest, "explicit bound flag persists a real Y minus one target");
            var legacy = saved.copy(); legacy.remove("bound"); restored.loadWithComponents(legacy, level.registryAccess());
            check(!restored.hasBinding(), "old sentinel-only NBT with yt minus one remains unbound");
            legacy.putInt("yt", 0); restored.loadWithComponents(legacy, level.registryAccess());
            check(restored.hasBinding(), "old non-sentinel target data remains readable");
            level.setBlock(secondPos, Blocks.CHEST.defaultBlockState(), 3); var second = (Container)level.getBlockEntity(secondPos); second.setItem(0, new ItemStack(Items.EMERALD, 2));
            click(player, pos); click(player, secondPos);
            check(items.getStackInSlot(0).is(Items.EMERALD) && tile.getItem(0).getCount() == 2, "a cached capability observes retargeting without reconnecting");
            level.setBlock(otherPos, TransvectorContent.INTERFACE.get().defaultBlockState(), 3);
            click(player, pos); click(player, otherPos);
            check(ConnectorItem.selected(binder).isPresent() && tile.targetPosition().equals(secondPos), "interface-to-interface binding is rejected and selection retained");
            click(player, pos.offset(5, 0, 0));
            check(ConnectorItem.selected(binder).isPresent() && tile.targetPosition().equals(secondPos), "out-of-range binding is rejected without losing selection or old target");
            var airPos = pos.offset(0, 0, 3); changed.add(airPos); level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3); click(player, airPos);
            check(tile.hasBinding() && ConnectorItem.selected(binder).isEmpty(), "original connector accepts a nearby air target before any capability resolution");
            check(items.getSlots() == 0 && !tile.hasBinding(), "first resolution clears a loaded missing tile");
            var unloaded = pos.offset(30000, 0, 0); check(!level.hasChunkAt(unloaded), "unloaded-target fixture"); tile.bind(unloaded);
            check(tile.resolve() == null && tile.hasBinding() && !level.hasChunkAt(unloaded), "unloaded target retains its binding and is not force-loaded");
            var farPos = pos.offset(6, 0, 0); changed.add(farPos); level.setBlock(farPos, Blocks.CHEST.defaultBlockState(), 3); tile.bind(farPos);
            check(tile.resolve() == null && !tile.hasBinding(), "loaded out-of-range target is cleared on resolution");
            var cheaty = tile.saveWithFullMetadata(level.registryAccess()); cheaty.putBoolean("bound", true); cheaty.putBoolean("cheatyMode", true); tile.loadWithComponents(cheaty, level.registryAccess());
            check(tile.resolve() == level.getBlockEntity(farPos) && tile.hasBinding(), "original cheatyMode NBT bypasses range without changing normal binder range checks");
            cheaty.putInt("xt", airPos.getX()); cheaty.putInt("yt", airPos.getY()); cheaty.putInt("zt", airPos.getZ()); tile.loadWithComponents(cheaty, level.registryAccess());
            check(tile.resolve() == null && tile.hasBinding(), "original cheatyMode also allows a link with no tile");
            cheaty.putBoolean("cheatyMode", false); tile.loadWithComponents(cheaty, level.registryAccess());
            tile.bind(fixturePos); level.setBlock(fixturePos, CapabilityFixture.BLOCK.defaultBlockState(), 3); var fixture = (CapabilityFixture)level.getBlockEntity(fixturePos);
            var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.NORTH); var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH);
            check(fluids != null && fluids.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE) == 200 && fixture.northFluid.getFluidAmount() == 200, "actual interface fluid provider forwards to the original side");
            check(energy != null && energy.receiveEnergy(300, false) == 300 && fixture.northEnergy.getEnergyStored() == 300, "actual interface energy provider charges the target");
            var jar = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar")); level.setBlock(jarPos, jar.defaultBlockState(), 3); tile.bind(jarPos);
            var aspects = level.getCapability(EssentiaCapabilities.CONTAINER, pos); var directAspects = level.getCapability(EssentiaCapabilities.CONTAINER, jarPos);
            check(aspects != null && directAspects != null, "real aspect capability is exposed at the interface");
            directAspects.setAspects(new AspectList().add(Aspects.AIR, 12));
            check(aspects.containerContains(Aspects.AIR) == 12 && aspects.takeFromContainer(Aspects.AIR, 3) && directAspects.containerContains(Aspects.AIR) == 9, "actual aspect operations preserve target contents");
            check(tile.containerContains(Aspects.AIR) == 9 && tile.isConnectable(Direction.DOWN), "original direct public aspect/transport interfaces remain available alongside capabilities");
            check(level.getCapability(EssentiaCapabilities.TRANSPORT, pos, Direction.UP).isConnectable(Direction.UP), "actual essentia transport provider keeps original connectability");
            var other = (TransvectorInterfaceBlockEntity)level.getBlockEntity(otherPos); tile.bind(otherPos); other.bind(pos);
            check(tile.getContainerSize() == 0 && items.getSlots() == 0, "malformed persisted interface cycle is bounded in Container and capability paths"); tile.setChanged();
            tile.bind(chestPos); check(items.getStackInSlot(0).is(Items.DIAMOND), "interface recovers after invalid cycle without stale guard state");
            chest.clearContent(); level.setBlock(chestPos, Blocks.AIR.defaultBlockState(), 3);
            check(items.getSlots() == 0 && !tile.hasBinding(), "cached capability detects removal of a previously live target");
            click(player, pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); click(player, chestPos);
            check(ConnectorItem.selected(binder).isEmpty(), "missing origin clears the binder selection");
            System.out.println("TT_TRANSVECTOR_BINDING_PASS checks=" + checks); helper.succeed();
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (var p : changed) if (!(level.getBlockEntity(p) instanceof TransvectorInterfaceBlockEntity) && level.getBlockEntity(p) instanceof Container container) container.clearContent();
            for (var p : changed) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void transvectorCamouflageAndRecipes(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 150, 2)); var player = FocusRulesTest.player(helper, "[TTCamo]");
        try {
            level.setBlock(pos, TransvectorContent.INTERFACE.get().defaultBlockState(), 3); var state = level.getBlockState(pos); var tile = (TransvectorInterfaceBlockEntity)level.getBlockEntity(pos);
            check(state.getDestroySpeed(level, pos) == 3 && state.getBlock().getExplosionResistance() == 6 && !state.canOcclude(), "original interface hardness/resistance/nonopacity");
            var stone = new ItemStack(Items.STONE, 3); player.setItemInHand(InteractionHand.MAIN_HAND, stone);
            state.useItemOn(stone, level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
            check(tile.camouflage().is(Blocks.STONE) && stone.getCount() == 3, "standard camouflage is applied without consuming the held block");
            var log = new ItemStack(Items.SPRUCE_LOG); log.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("axis", "x")));
            state.useItemOn(log, level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            check(tile.camouflage().is(Blocks.SPRUCE_LOG) && tile.camouflage().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "log variant and orientation components are preserved");
            var pumpkin = new ItemStack(Items.CARVED_PUMPKIN); state.useItemOn(pumpkin, level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.WEST, pos, false));
            check(tile.camouflage().getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST, "original directional camouflage faces the clicked horizontal side");
            var saved = tile.saveWithFullMetadata(level.registryAccess()); var restored = new TransvectorInterfaceBlockEntity(pos, state); restored.loadWithComponents(saved, level.registryAccess());
            check(restored.camouflage().equals(tile.camouflage()) && tile.getUpdateTag(level.registryAccess()).contains("camoState", 10) && tile.getUpdatePacket() != null, "camouflage properties persist and enter actual block synchronization");
            check(tile.getModelData().get(thaumic.tinkerer.block.CamoModelData.CAMOUFLAGE).equals(tile.camouflage()), "actual block model-data path carries the selected camouflage state");
            check(CamoBlock.validCamouflage(Blocks.QUARTZ_PILLAR.defaultBlockState(), level, pos) && CamoBlock.validCamouflage(Blocks.OAK_SLAB.defaultBlockState(), level, pos), "original quartz and half-slab renderer families are accepted");
            check(!CamoBlock.validCamouflage(Blocks.OAK_STAIRS.defaultBlockState(), level, pos) && !CamoBlock.validCamouflage(state, level, pos), "stair rendering and nested camouflage are rejected");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); state.useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            check(tile.camouflage().is(Blocks.AIR), "empty-hand activation clears camouflage");
            var recipe = (IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("interface")).orElseThrow().value();
            var arcane = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:arcane_stone"))); var redstone = new ItemStack(Items.REDSTONE);
            check(recipe.matches(CraftingInput.of(3, 3, List.of(arcane, redstone, arcane, new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.LAPIS_LAZULI), arcane, redstone, arcane)), level)
                    && recipe.vis().equals(Map.of("ordo", 12, "perditio", 16)), "original metadata-six arcane stone recipe and vis costs");
            var binderRecipe = (IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("connector")).orElseThrow().value();
            var orderShard = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:order_shard"))); var iron = new ItemStack(Items.IRON_INGOT);
            check(binderRecipe.matches(CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, iron, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK), iron, orderShard, ItemStack.EMPTY, ItemStack.EMPTY)), level) && binderRecipe.vis().equals(Map.of("ordo", 2)), "original binder recipe and order cost");
            var research = AddonRegistry.research().get("thaumictinkerer:interface");
            check(research.pageOrder().equals(List.of("page.thaumictinkerer.interface.0", "thaumictinkerer:interface", "page.thaumictinkerer.interface.1", "thaumictinkerer:connector", "page.thaumictinkerer.interface.2")), "original five-page research ordering");
            check(ResearchApi.exists(player, "thaumictinkerer:interface") && ResearchApi.exists(player, "thaumictinkerer:magnets"), "real server catalog now contains the original interface and magnet branches");
            System.out.println("TT_TRANSVECTOR_CAMO_RECIPES_PASS checks=" + checks); helper.succeed();
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
    }
}
