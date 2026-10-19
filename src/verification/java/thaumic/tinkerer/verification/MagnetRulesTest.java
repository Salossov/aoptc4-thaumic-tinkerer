package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.block.MagnetBlock;
import thaumic.tinkerer.block.MagnetBlockEntity;
import thaumic.tinkerer.block.MagnetContent;
import thaumic.tinkerer.block.MobMagnetBlockEntity;
import thaumic.tinkerer.crafting.OreMetalIngredient;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.menu.MobMagnetMenu;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class MagnetRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static ItemStack core(String path) { return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:" + path))); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void magnetMotion(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 20, 2));
        var centre = Vec3.atCenterOf(pos); var entities = new ArrayList<Entity>();
        try {
            level.setBlock(pos, MagnetContent.MAGNET.get().defaultBlockState(), 3);
            var tile = (MagnetBlockEntity)level.getBlockEntity(pos);
            check(tile != null && tile.isPulling() && tile.signal() == 0, "new item magnet is blue and unpowered");
            check(level.getBlockState(pos).getDestroySpeed(level, pos) == 1.7f && MagnetContent.MAGNET.get().getExplosionResistance() == .6f, "original hardness and effective blast resistance");
            var shape = level.getBlockState(pos).getShape(level, pos).bounds();
            check(shape.minX == 1d / 16 && shape.maxX == 15d / 16 && shape.maxY == 2d / 16, "collision remains the original thin base despite tall visible model");
            var item = new ItemEntity(level, centre.x + 3, centre.y, centre.z, new ItemStack(Items.DIAMOND));
            level.addFreshEntity(item); entities.add(item); item.setDeltaMovement(.1, .2, .3); tile.tick();
            check(item.getDeltaMovement().equals(new Vec3(.1, .2, .3)), "no signal does not alter movement");
            level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            check(tile.signal() == 15 && tile.effectBox(15).getXsize() == 14 && tile.effectBox(14).equals(tile.effectBox(15)), "signal fifteen and fourteen share integer range seven");
            check(tile.effectBox(1).getXsize() == 0 && tile.effectBox(1).minY == pos.getY() && tile.effectBox(15).maxY == pos.getY() + 7.5, "signal one has zero horizontal width and Y box starts at the block floor");
            tile.tick();
            var expected = new Vec3(-3, -item.getBbHeight() / 2d, 0).normalize().scale(.25f);
            check(item.getDeltaMovement().distanceTo(expected) < 1e-10 && item.getDeltaMovement().x < 0, "blue magnet replaces velocity with original quarter-speed vector to physical centre");
            var near = new ItemEntity(level, centre.x + .4, centre.y, centre.z, new ItemStack(Items.STICK));
            level.addFreshEntity(near); entities.add(near); near.setDeltaMovement(.12, .23, .34); tile.tick();
            check(near.getDeltaMovement().equals(new Vec3(.12, .23, .34)), "pull leaves existing velocity untouched inside distance one");
            tile.setPulling(false); tile.tick();
            check(item.getDeltaMovement().distanceTo(expected.scale(-1)) < 1e-10 && near.getDeltaMovement().x > 0, "red magnet reverses movement and pushes even inside distance one");
            var outside = new ItemEntity(level, centre.x + 7.6, centre.y, centre.z, new ItemStack(Items.STICK));
            var below = new ItemEntity(level, centre.x + 2, pos.getY() - 1, centre.z, new ItemStack(Items.STICK));
            var above = new ItemEntity(level, centre.x + 2, pos.getY() + 7.6, centre.z, new ItemStack(Items.STICK));
            for (var entity : List.of(outside, below, above)) { level.addFreshEntity(entity); entities.add(entity); entity.setDeltaMovement(.13, .24, .35); }
            tile.tick();
            check(outside.getDeltaMovement().equals(new Vec3(.13, .24, .35)) && below.getDeltaMovement().equals(outside.getDeltaMovement()) && above.getDeltaMovement().equals(outside.getDeltaMovement()), "entities outside asymmetric effect box remain untouched");
            var cow = EntityType.COW.create(level); entities.add(cow); cow.setPos(centre.x + 2, centre.y, centre.z); level.addFreshEntity(cow); cow.setDeltaMovement(Vec3.ZERO); tile.tick();
            check(cow.getDeltaMovement().equals(Vec3.ZERO) && !tile.accepts(cow), "item magnet does not move living entities");
            var player = FocusRulesTest.player(helper, "[TTMagnet]"); var hit = new BlockHitResult(centre, Direction.UP, pos, false);
            var wand = FocusRulesTest.wand(player, new ItemStack(FocusContent.TELEKINESIS.get()));
            level.getBlockState(pos).useItemOn(wand, level, player, InteractionHand.MAIN_HAND, hit);
            check(!tile.isPulling() && player.containerMenu == player.inventoryMenu, "wand on item magnet is handled without toggling or opening a menu");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.getBlockState(pos).useWithoutItem(level, player, hit);
            check(tile.isPulling(), "ordinary activation toggles polarity");
            check(MagnetContent.MAGNET.get().asItem().getDefaultInstance().getMaxStackSize() == 64, "original block item stacking");
            System.out.println("TT_MAGNET_MOTION_PASS checks=" + checks); helper.succeed();
        } finally {
            for (var entity : entities) if (entity != null) entity.discard();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3);
        }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void mobMagnet(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var anchor = helper.absolutePos(new BlockPos(2, 40, 2));
        // Keep the complete seven-block query radius and spawned mobs inside the loaded test chunk.
        var pos = new BlockPos((anchor.getX() >> 4) * 16 + 8, anchor.getY(), (anchor.getZ() >> 4) * 16 + 8); var centre = Vec3.atCenterOf(pos);
        var stub = FocusRulesTest.player(helper, "[TTMobMagnet]");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, stub.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override public BlockPos adjustSpawnLocation(net.minecraft.server.level.ServerLevel world, BlockPos spawn) { return spawn; }
        };
        var packets = new ArrayList<net.minecraft.network.protocol.Packet<?>>();
        // The built-in fake handler has no channel, so NeoForge's DataSlot negotiation
        // cannot query it. Capture normal menu packets and explicitly use the vanilla
        // data-packet path without creating a socket or pretending a client logged in.
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), stub.connection.getConnection(), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public boolean hasChannel(ResourceLocation id) { return false; }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { packets.add(packet); }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { packets.add(packet); }
        };
        player.setPos(centre); var entities = new ArrayList<Entity>();
        try {
            level.setBlock(pos, MagnetContent.MOB_MAGNET.get().defaultBlockState(), 3);
            var tile = (MobMagnetBlockEntity)level.getBlockEntity(pos); check(tile != null && tile.searchesAdults(), "fresh mob magnet defaults to adults");
            var adult = EntityType.COW.create(level); var baby = EntityType.COW.create(level); baby.setBaby(true); var zombie = EntityType.ZOMBIE.create(level); zombie.setBaby(true);
            var item = new ItemEntity(level, centre.x, centre.y, centre.z, new ItemStack(Items.STICK));
            entities.addAll(List.of(adult, baby, zombie, item));
            check(tile.accepts(adult) && !tile.accepts(baby) && tile.accepts(zombie), "only AgeableMob is restricted by age; baby zombies remain eligible");
            check(!tile.accepts(player) && !tile.accepts(item), "players and item entities are excluded");
            tile.setSearchAdults(false);
            check(!tile.accepts(adult) && tile.accepts(baby) && tile.accepts(zombie), "child setting reverses ageable filter and retains other living entities");
            var mould = new ItemStack(ThaumicTinkerer.SOUL_MOULD.get()); CustomData.update(DataComponents.CUSTOM_DATA, mould, tag -> tag.putString("pattern", "minecraft:cow"));
            tile.setItem(0, mould);
            check(tile.accepts(baby) && !tile.accepts(zombie), "pattern and age filters combine");
            for (var entity : List.of(adult, baby, zombie)) { entity.setPos(centre.add(3, 0, 0)); entity.setDeltaMovement(Vec3.ZERO); level.addFreshEntity(entity); }
            level.setBlock(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3); tile.tick();
            check(baby.getDeltaMovement().x < 0 && adult.getDeltaMovement().equals(Vec3.ZERO) && zombie.getDeltaMovement().equals(Vec3.ZERO), "actual mob tick moves only the matching age and pattern");
            tile.setPulling(false); tile.tick();
            check(baby.getDeltaMovement().x > 0, "actual mob repulsion uses the same selected filter");
            tile.setPulling(true);
            tile.setItem(0, new ItemStack(Items.STONE));
            check(!tile.accepts(baby) && !tile.accepts(zombie) && tile.canPlaceItem(0, new ItemStack(Items.STONE)), "arbitrary inventory input is permitted but an untagged wrong item matches no mobs");
            CustomData.update(DataComponents.CUSTOM_DATA, tile.getItem(0), tag -> tag.putString("pattern", "minecraft:cow"));
            check(tile.accepts(baby), "original static pattern reader also honors pattern data on an arbitrary item");
            tile.setItem(0, mould);
            var saved = tile.saveWithFullMetadata(level.registryAccess()); var restored = new MobMagnetBlockEntity(pos, tile.getBlockState()); restored.loadWithComponents(saved, level.registryAccess());
            check(!restored.searchesAdults() && ItemStack.matches(restored.getItem(0), mould) && restored.accepts(baby), "age and filter persist through the real block-entity serialization");
            restored.loadWithComponents(new CompoundTag(), level.registryAccess());
            check(!restored.searchesAdults() && restored.isEmpty(), "original missing adultCheck loads false rather than the fresh-instance default");
            var update = tile.getUpdateTag(level.registryAccess()); restored.loadWithComponents(update, level.registryAccess());
            check(restored.getItem(0).is(ThaumicTinkerer.SOUL_MOULD.get()) && !restored.searchesAdults() && tile.getUpdatePacket() != null, "actual block update carries filter and age state");
            tile.clearContent(); var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.NORTH);
            check(handler != null && handler.getSlots() == 1 && handler.getSlotLimit(0) == 1, "automation sees one slot with original one-item capacity");
            var remainder = handler.insertItem(0, new ItemStack(Items.STONE, 3), false);
            check(remainder.getCount() == 2 && tile.getItem(0).is(Items.STONE), "automation deliberately accepts a non-mould item");
            tile.clearContent(); var wand = FocusRulesTest.wand(player, new ItemStack(FocusContent.TELEKINESIS.get()));
            var hit = new BlockHitResult(centre, Direction.UP, pos, false); level.getBlockState(pos).useItemOn(wand, level, player, InteractionHand.MAIN_HAND, hit);
            check(player.containerMenu instanceof MobMagnetMenu && tile.isPulling(), "actual core wand opens the real mob menu without toggling");
            var menu = (MobMagnetMenu)player.containerMenu;
            check(menu.slots.size() == 37 && menu.getSlot(0).x == 53 && menu.getSlot(0).y == 37, "original slot count, order and filter position");
            check(menu.getSlot(0).mayPlace(mould) && !menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "GUI filter slot only allows a soul mould");
            check(menu.clickMenuButton(player, 0) && tile.searchesAdults() && menu.searchesAdults(), "adult radio button updates actual persisted tile state");
            check(menu.clickMenuButton(player, 1) && !tile.searchesAdults() && !menu.clickMenuButton(player, 2), "child radio button and invalid-button rejection");
            check(packets.stream().anyMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket data && data.getValue() == 1)
                    && packets.stream().anyMatch(packet -> packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket data && data.getValue() == 0), "real menu synchronizer sends both radio states as container data packets");
            player.getInventory().setItem(9, mould.copy());
            check(menu.quickMoveStack(player, 1).is(ThaumicTinkerer.SOUL_MOULD.get()) && tile.getItem(0).is(ThaumicTinkerer.SOUL_MOULD.get()) && player.getInventory().getItem(9).isEmpty(), "shift-click moves the soul mould from inventory to filter");
            check(menu.quickMoveStack(player, 0).is(ThaumicTinkerer.SOUL_MOULD.get()) && tile.isEmpty(), "shift-click moves the filter back to player inventory");
            player.getInventory().setItem(9, new ItemStack(Items.STONE));
            check(menu.quickMoveStack(player, 1).isEmpty() && tile.isEmpty(), "shift-click ignores non-mould items");
            player.setPos(centre.add(9, 0, 0));
            check(!menu.stillValid(player) && !menu.clickMenuButton(player, 0), "menu range and button authorization share original distance-eight check");
            player.closeContainer(); player.setPos(centre); tile.setItem(0, mould.copy());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1)); entities.addAll(drops);
            check(drops.size() == 1 && drops.getFirst().getItem().is(ThaumicTinkerer.SOUL_MOULD.get()) && drops.getFirst().getItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("pattern").equals("minecraft:cow"), "breaking the device returns its filter with pattern intact");
            System.out.println("TT_MOB_MAGNET_PASS checks=" + checks); helper.succeed();
        } finally {
            player.closeContainer(); player.getInventory().clearContent(); player.discard();
            if (level.getBlockEntity(pos) instanceof MobMagnetBlockEntity tile) tile.clearContent();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), 3); for (var entity : entities) entity.discard();
        }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void magnetRecipes(GameTestHelper helper) {
        VerificationReports.install(); checks = 0; var level = helper.getLevel();
        boolean metalOption = TinkererConfig.ORE_DICT_METAL.get();
        try {
            var empty = new OreMetalIngredient(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ThaumicTinkerer.id("absent_test_metal")), Items.GOLD_INGOT);
            TinkererConfig.ORE_DICT_METAL.set(true);
            check(empty.test(new ItemStack(Items.GOLD_INGOT)) && !empty.test(new ItemStack(Items.COPPER_INGOT)), "original enabled option falls back to gold only for an empty metal list");
            TinkererConfig.ORE_DICT_METAL.set(false);
            check(!empty.test(new ItemStack(Items.GOLD_INGOT)) && empty.getItems().findAny().isEmpty(), "original disabled option retains the empty tag instead of restoring gold");
            var copper = new OreMetalIngredient(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:ingots/copper")), Items.GOLD_INGOT);
            check(copper.test(new ItemStack(Items.COPPER_INGOT)) && !copper.test(new ItemStack(Items.GOLD_INGOT)), "nonempty copper tag remains copper even with option disabled, as actual old expression specifies");
            TinkererConfig.ORE_DICT_METAL.set(true);
            check(copper.test(new ItemStack(Items.COPPER_INGOT)), "default setting accepts normal copper");
            var recipe = (IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("magnet")).orElseThrow().value();
            var focus = new ItemStack(FocusContent.TELEKINESIS.get());
            var ingredients = List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                    core("air_shard"), new ItemStack(Items.IRON_INGOT), core("earth_shard"), core("greatwood_log"), focus, core("greatwood_log"));
            check(recipe.matches(CraftingInput.of(3, 3, ingredients), level), "original kinetic magnet pattern and core public IDs");
            focus.setDamageValue(1); check(!recipe.matches(CraftingInput.of(3, 3, ingredients), level), "focus damage zero is required by original metadata matching"); focus.setDamageValue(0);
            check(recipe.vis().equals(Map.of("aer", 20, "ordo", 5, "terra", 15, "perditio", 5)), "original four-aspect arcane cost");
            var mobRecipe = (IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("mob_magnet")).orElseThrow().value();
            var mobIngredients = new ArrayList<>(ingredients); mobIngredients.set(1, new ItemStack(Items.COPPER_INGOT)); mobIngredients.set(4, new ItemStack(Items.COPPER_INGOT));
            check(mobRecipe.matches(CraftingInput.of(3, 3, mobIngredients), level), "corporeal recipe resolves real copper tag");
            mobIngredients.set(1, new ItemStack(Items.GOLD_INGOT)); mobIngredients.set(4, new ItemStack(Items.GOLD_INGOT));
            check(!mobRecipe.matches(CraftingInput.of(3, 3, mobIngredients), level), "gold is not an alternate when copper exists");
            var research = thaumic.tinkerer.research.TinkererResearch.magnets();
            check(research.parents().equals(List.of("thaumictinkerer:interface")) && research.hiddenParents().equals(List.of("thaumictinkerer:focus_telekinesis")) && research.concealed() && research.complexity() == 3 && research.column() == -6 && research.row() == 3, "original research parents, hidden parent, flags, coordinates and difficulty");
            check(research.pageOrder().getLast().equals("thaumictinkerer:soul_mould"), "soul mould remains the last original book recipe page");
            System.out.println("TT_MAGNET_RECIPES_PASS checks=" + checks); helper.succeed();
        } finally { TinkererConfig.ORE_DICT_METAL.set(metalOption); }
    }
}
