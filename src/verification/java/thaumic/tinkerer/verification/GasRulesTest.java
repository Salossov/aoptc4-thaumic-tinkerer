package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.GasBlock;
import thaumic.tinkerer.block.GasContent;
import thaumic.tinkerer.block.NitorGasBlock;
import thaumic.tinkerer.item.BrightNitorItem;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class GasRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void gases(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel();
        // Keep this volume separate from the other tests and clean even outside the tiny template.
        var anchor=helper.absolutePos(BlockPos.ZERO);
        BlockPos origin=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+30,(anchor.getZ()>>4)*16+8);
        var modified = new ArrayList<BlockPos>();
        var player = FakePlayerFactory.get(level, new GameProfile(java.util.UUID.fromString("cf2c9f1d-c4a8-4b83-a107-bcc344998108"), "[TTGasTest]"));
        try {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -5, -5), origin.offset(5, 5, 5))) {
                modified.add(pos.immutable()); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            var light = GasContent.LIGHT.get(); var shadow = GasContent.SHADOW.get(); var nitor = GasContent.NITOR.get();
            var state = light.defaultBlockState();
            check(state.isAir() && state.canBeReplaced(), "gas remains air-like and replaceable");
            check(state.getShape(level, origin).isEmpty() && state.getCollisionShape(level, origin).isEmpty(), "gas has no hit or collision box");
            check(state.getLightEmission(level, origin) == 12 && state.getLightBlock(level, origin) == 0, "original light .85 truncates to 12");
            check(shadow.defaultBlockState().getLightEmission(level, origin) == 0 && shadow.defaultBlockState().getLightBlock(level, origin) == 15
                    && !shadow.defaultBlockState().propagatesSkylightDown(level, origin), "legacy opacity 215 blocks all modern block/sky light");
            check(!state.canEntityDestroy(level, origin, player) && state.getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, origin.getCenter())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)).isEmpty(), "gas cannot be destroyed by an entity and drops nothing");
            level.setBlock(origin, state.setValue(GasBlock.SPREAD, 4), 2);
            for (int step = 0; step < 5; step++) {
                var active = new ArrayList<BlockPos>();
                for (BlockPos pos : modified) if (level.getBlockState(pos).is(light) && level.getBlockState(pos).getValue(GasBlock.SPREAD) > 0) active.add(pos);
                for (BlockPos pos : active) level.getBlockState(pos).tick(level, pos, level.random);
            }
            int count = 0;
            for (BlockPos pos : modified) if (level.getBlockState(pos).is(light)) {
                count++; check(pos.distManhattan(origin) <= 4 && level.getBlockState(pos).getValue(GasBlock.SPREAD) == 0, "spread radius and exhausted propagation metadata");
            }
            check(count == 129, "strength four fills exactly the 129-cell Manhattan ball");
            level.setBlock(origin, state.setValue(GasBlock.SPREAD, 1), 2);
            level.setBlock(origin.east(), shadow.defaultBlockState(), 2);
            level.setBlock(origin.west(), Blocks.STONE.defaultBlockState(), 2);
            level.getBlockState(origin).tick(level, origin, level.random);
            check(level.getBlockState(origin.east()).is(light), "one gas replaces a different air-like gas");
            check(level.getBlockState(origin.west()).is(Blocks.STONE), "solid obstacles stop propagation");
            player.setPos(origin.getX(), origin.getY() - 1, origin.getZ());
            var flask = new ItemStack(GasContent.SHADOW_ITEM.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, flask);
            level.setBlock(origin, Blocks.STONE.defaultBlockState(), 2);
            GasContent.SHADOW_ITEM.get().use(level, player, InteractionHand.MAIN_HAND);
            check(flask.getCount() == 1 && level.getBlockState(origin).is(Blocks.STONE), "occupied origin still consumes a gas flask");
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 2);
            GasContent.SHADOW_ITEM.get().use(level, player, InteractionHand.MAIN_HAND);
            check(flask.isEmpty() && level.getBlockState(origin).is(shadow) && level.getBlockState(origin).getValue(GasBlock.SPREAD) == 4, "flask places spread four in air");
            player.getAbilities().instabuild = true;
            flask = new ItemStack(GasContent.LIGHT_ITEM.get()); player.setItemInHand(InteractionHand.MAIN_HAND, flask);
            GasContent.LIGHT_ITEM.get().use(level, player, InteractionHand.MAIN_HAND);
            check(flask.getCount() == 1 && level.getBlockState(origin).is(light), "creative consumption exemption and replacement of another gas");
            player.getAbilities().instabuild = false;
            BrightNitorItem.light(level, origin, false);
            check(level.getBlockState(origin).is(light), "bright nitor requires literal air rather than another gas");
            level.setBlock(origin, Blocks.AIR.defaultBlockState(), 2);
            GasContent.BRIGHT_NITOR.get().inventoryTick(new ItemStack(GasContent.BRIGHT_NITOR.get()), level, player, 17, false);
            check(level.getBlockState(origin).is(nitor) && level.getBlockState(origin).getLightEmission(level, origin) == 12, "nitor works in an unselected inventory slot");
            check(nitor.defaultBlockState().setValue(GasBlock.SPREAD, 1).getLightEmission(level, origin) == 15, "KAMI light variant emits 15");
            check(NitorGasBlock.interval(level) == 20, "ordinary dimension maintenance interval");
            var nether = level.getServer().getLevel(Level.NETHER);
            check(nether != null && NitorGasBlock.interval(nether) == 60, "Nether maintenance interval is sixty ticks");
            level.getBlockState(origin).tick(level, origin, level.random);
            check(level.getBlockState(origin).isAir() && !level.getBlockState(origin).is(nitor), "temporary light expires without a nearby holder");
            player.getInventory().clearContent(); player.getInventory().setItem(5, new ItemStack(GasContent.BRIGHT_NITOR.get()));
            player.getInventory().getItem(5).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Nitor with custom data"));
            level.addNewPlayer(player);
            check(level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,player.getBoundingBox().inflate(1)).contains(player), "nitor fixture player is visible to the actual world entity query");
            BrightNitorItem.light(level, origin, false);
            level.getBlockState(origin).tick(level, origin, level.random);
            check(level.getBlockState(origin).is(nitor), "nearby player maintains light with nitor regardless of its item components");
            player.getInventory().clearContent();
            level.getBlockState(origin).tick(level, origin, level.random);
            check(!level.getBlockState(origin).is(nitor), "dropping nitor stops maintenance");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GasContent.REMOVER.get()));
            player.setPos(origin.getX(), origin.getY(), origin.getZ());
            level.setBlock(origin, light.defaultBlockState(), 2);
            level.setBlock(origin.offset(-3, -3, -3), nitor.defaultBlockState(), 2);
            level.setBlock(origin.offset(2, 2, 2), shadow.defaultBlockState(), 2);
            level.setBlock(origin.offset(3, 3, 3), shadow.defaultBlockState(), 2);
            GasContent.REMOVER.get().use(level, player, InteractionHand.MAIN_HAND);
            check(level.getBlockState(origin).is(light), "remover requires sneaking");
            player.setShiftKeyDown(true); GasContent.REMOVER.get().use(level, player, InteractionHand.MAIN_HAND);
            check(level.getBlockState(origin).is(Blocks.AIR) && level.getBlockState(origin.offset(-3, -3, -3)).is(Blocks.AIR)
                    && level.getBlockState(origin.offset(2, 2, 2)).is(Blocks.AIR), "remover clears light, shadow and nitor in inclusive minus-three through plus-two cube");
            check(level.getBlockState(origin.offset(3, 3, 3)).is(shadow), "positive-three boundary is deliberately excluded");
            check(player.getMainHandItem().getDamageValue() == 0, "remover is reusable without durability");
            for (String id : List.of("gaseous_light", "gaseous_shadow", "bright_nitor")) {
                var recipe = (CrucibleRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id(id)).orElseThrow().value();
                check(recipe.getRecipeOutput().is(switch (id) { case "gaseous_light" -> GasContent.LIGHT_ITEM.get(); case "gaseous_shadow" -> GasContent.SHADOW_ITEM.get(); default -> GasContent.BRIGHT_NITOR.get(); }), "registered crucible output " + id);
                check(recipe.catalyst().test(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:" + (id.equals("bright_nitor") ? "nitor" : "empty_phial"))))), "exact catalyst " + id);
                if (id.equals("bright_nitor")) check(!recipe.catalyst().test(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:tallow")))), "original resource metadata 1 is nitor, not tallow");
                check(recipe.aspects().equals(id.equals("bright_nitor") ? Map.of("potentia", 25, "lux", 25, "aer", 10, "ignis", 10)
                        : Map.of(id.equals("gaseous_light") ? "lux" : "tenebrae", 16, "aer", 10, "motus", 8)), "exact release costs " + id);
            }
            var arcane = (IArcaneRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("gas_remover")).orElseThrow().value();
            var quartz = thaumic.tinkerer.block.DarkQuartzContent.QUARTZ.get();
            var input = CraftingInput.of(3, 3, List.of(new ItemStack(quartz), new ItemStack(quartz), new ItemStack(quartz),
                    new ItemStack(GasContent.SHADOW_ITEM.get()), ItemStack.EMPTY, new ItemStack(GasContent.LIGHT_ITEM.get()),
                    new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ)));
            check(arcane.matches(input, level) && arcane.vis().equals(Map.of("aer", 2, "ordo", 2)), "remover recipe retains default-shadow/special-light registration order and vis costs");
            for (String id : List.of("gaseous_light", "gaseous_shadow", "bright_nitor", "gas_remover")) {
                var research = AddonRegistry.research().get("thaumictinkerer:" + id);
                check(research != null && research.recipePages().contains("thaumictinkerer:" + id), "registered book recipe " + id);
            }
            check(AddonRegistry.research().get("thaumictinkerer:bright_nitor").concealed() && AddonRegistry.research().get("thaumictinkerer:bright_nitor").secondary(), "bright nitor original flags");
            System.out.println("TT_GAS_PASS checks=" + checks); helper.succeed();
        } finally {
            player.getInventory().clearContent(); player.setShiftKeyDown(false); player.getAbilities().instabuild = false;
            if (player.isAddedToLevel()) level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            for (BlockPos pos : modified) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
