package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.wands.FocusBehaviors;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.item.FocusContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class FocusRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    static ServerPlayer player(GameTestHelper helper, String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(java.util.UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
    }
    static ItemStack wand(ServerPlayer player, ItemStack focus) {
        var wand = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:casting_wand")));
        WandApi.focus(wand, focus);
        for (var aspect : Aspects.PRIMALS) WandApi.set(wand, aspect, 1000);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        return wand;
    }
    static void stop(ServerPlayer player, ItemStack wand) {
        wand.getItem().releaseUsing(wand, player.level(), player, 0); player.stopUsingItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.setShiftKeyDown(false);
    }
    @GameTest(template = "empty", batch="smelting_focus", timeoutTicks = 1200)
    public static void smeltingFocus(GameTestHelper helper)throws Exception {
        var target=helper.absolutePos(new BlockPos(4,4,8));var columns=new ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=(target.getX()-20)>>4;x<=(target.getX()+20)>>4;x++)for(int z=(target.getZ()-24)>>4;z<=(target.getZ()+20)>>4;z++)columns.add(new net.minecraft.world.level.ChunkPos(x,z));
        ReplacementApiRulesTest.prepared(helper,"TTSmelt",columns,peer->{
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = peer.player; thaumic.tinkerer.item.TinkererFocusItem.playerLogout(player.getGameProfile().getName());
        var item = FocusContent.SMELT.get(); var focus = new ItemStack(item);
        for (int rank = 1; rank <= 3; rank++) check(item.applyUpgrade(focus, FocusUpgradeType.potency, rank), "focus potency rank " + rank);
        var wand = wand(player, focus); var pos = helper.absolutePos(new BlockPos(4, 4, 8));
        var changed = new ArrayList<BlockPos>();
        try {
            for (int z = -4; z <= 0; z++) { var p = pos.offset(0, 0, z); changed.add(p); level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); }
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 2);
            player.setPos(pos.getX() + .5, pos.getY() + .5 - player.getEyeHeight(), pos.getZ() - 3.5); player.setYRot(0); player.setXRot(0);
            var cost = WandApi.quote(wand, player, item.getVisCost(focus));
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
            check(player.isUsingItem(), "native wand begins continuous smelting despite per-activation tooltip");
            for (int tick = 0; tick < 10; tick++) wand.getItem().onUseTick(level, player, wand, 72000 - tick);
            check(level.getBlockState(pos).is(Blocks.SAND) && WandApi.stored(wand, Aspects.FIRE) == 1000, "charging does not consume vis each tick");
            WandApi.set(wand, Aspects.FIRE, 0);
            wand.getItem().onUseTick(level, player, wand, 71990);
            check(player.isUsingItem() && level.getBlockState(pos).is(Blocks.SAND), "insufficient vis pauses progress without ending original use session");
            WandApi.set(wand, Aspects.FIRE, 1000);
            // Release and re-use; the original name/coordinate progress survives.
            wand.getItem().releaseUsing(wand, level, player, 71990); player.stopUsingItem();
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
            for (int tick = 10; tick < 20; tick++) wand.getItem().onUseTick(level, player, wand, 72000 - tick);
            check(level.getBlockState(pos).is(Blocks.SAND), "twenty calls initialize plus decrement nineteen times, even with attached potency upgrades");
            wand.getItem().onUseTick(level, player, wand, 71980);
            check(level.getBlockState(pos).is(Blocks.GLASS), "twenty-first charged call smelts a block in place");
            check(WandApi.stored(wand, Aspects.FIRE) == 1000 - cost.get(Aspects.FIRE)
                    && WandApi.stored(wand, Aspects.ENTROPY) == 1000 - cost.get(Aspects.ENTROPY), "one discounted vis payment only on completion");
            check(item.getFocusColor(focus) == 16711680 && !item.isVisCostPerTick(focus), "original red color and activation-cost tooltip");
            var smeltRecipe=(thaumcraft.api.crafting.IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("focus_smelt")).orElseThrow().value();
            check(smeltRecipe.getIngredients().stream().anyMatch(i->i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:nitor"))))) && smeltRecipe.getIngredients().stream().noneMatch(i->i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:tallow"))))), "original smelting focus resource metadata 1 is nitor");
            var research = AddonRegistry.research().get("thaumictinkerer:focus_smelt");
            check(research.concealed() && research.parents().equals(List.of("FOCUSEXCAVATION")) && !research.secondary(), "smelting original research prerequisites and flags");
            check(FocusBehaviors.get(focus).ornament().equals(ThaumicTinkerer.id("item/focus_smelt_ornament")), "native ornament model registered before client model loading");
            System.out.println("TT_SMELT_FOCUS_PASS checks=" + checks);
        } finally { stop(player, wand); for (BlockPos p : changed) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2); }
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void flightFocus(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var player = player(helper, "[TTFlight]"); var item = FocusContent.FLIGHT.get(); var focus = new ItemStack(item);
        for (int rank = 1; rank <= 3; rank++) item.applyUpgrade(focus, FocusUpgradeType.potency, rank);
        var wand = wand(player, focus); boolean enabled = TinkererConfig.FLIGHT.get();
        try {
            player.setXRot(-30); player.setYRot(20); player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 12;
            WandApi.set(wand, Aspects.AIR, 0); wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player.getDeltaMovement().equals(Vec3.ZERO) && player.fallDistance == 12, "unaffordable launch neither moves player nor clears fall damage");
            WandApi.set(wand, Aspects.AIR, 1000); var cost = WandApi.quote(wand, player, item.getVisCost(focus)).get(Aspects.AIR);
            wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player.getDeltaMovement().distanceTo(player.getLookAngle().scale(2d / 3)) < 1e-8, "original 2/3 impulse; focus potency is not read from the attached focus by the original bug");
            check(player.fallDistance == 0 && WandApi.stored(wand, Aspects.AIR) == 1000 - cost, "native player-motion bridge and one launch payment");
            check(!player.getAbilities().mayfly && !player.isUsingItem(), "impulse launch grants no creative-flight ability or channelled session");
            TinkererConfig.FLIGHT.set(false); var before = WandApi.stored(wand, Aspects.AIR);
            check(!item.begin(player, wand, focus, InteractionHand.MAIN_HAND) && !item.cast(player, wand, focus), "original flight config disables beginning and executing the effect");
            check(WandApi.stored(wand, Aspects.AIR) == before, "disabled flight spends no vis");
            check(item.getFocusColor(focus) == 10416895 && item.getVisCost(focus).equals(Map.of(Aspects.AIR, 15)), "exact flight color and centivis");
            var recipe = (InfusionRecipe) player.level().getRecipeManager().byKey(ThaumicTinkerer.id("focus_flight")).orElseThrow().value();
            check(recipe.instability() == 3 && recipe.components().size() == 7 && recipe.aspects().equals(Map.of("aer", 15, "motus", 20, "iter", 10)), "original flight infusion values");
            var research = AddonRegistry.research().get("thaumictinkerer:focus_flight");
            check(research.concealed() && research.parents().equals(List.of("thaumictinkerer:focus_smelt")) && research.hiddenParents().equals(List.of("INFUSION")), "flight research uses real preceding focus and implicit infusion requirement");
            System.out.println("TT_FLIGHT_FOCUS_PASS checks=" + checks); helper.succeed();
        } finally { TinkererConfig.FLIGHT.set(enabled); stop(player, wand); player.setDeltaMovement(Vec3.ZERO); player.fallDistance = 0; }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void telekinesisFocus(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = player(helper, "[TTTelekinesis]");
        var item = FocusContent.TELEKINESIS.get(); var focus = new ItemStack(item); var wand = wand(player, focus);
        // Keep entities in the template's resident chunk instead of relying on
        // adjacent test layouts to make an out-of-template chunk visible.
        var anchor = helper.absolutePos(BlockPos.ZERO);
        var base = new BlockPos((anchor.getX() >> 4) * 16 + 8, anchor.getY() + 70, (anchor.getZ() >> 4) * 16 + 8);
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5); player.setYRot(0); player.setXRot(0);
        Vec3 target = player.position().add(0, player.getBbHeight() / 2d + .5, 5);
        var spawned = new ArrayList<ItemEntity>();
        try {
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND);
            wand.getItem().onUseTick(level, player, wand, 72000);
            check(player.isUsingItem() && WandApi.stored(wand, Aspects.AIR) == 1000, "no nearby items means no charge while native wand keeps channeling");
            for (Vec3 point : List.of(target.add(5, 0, 0), target.add(.5, -.125, 0))) {
                var entity = new ItemEntity(level, point.x, point.y, point.z, new ItemStack(Items.DIRT));
                entity.setDeltaMovement(Vec3.ZERO); level.addFreshEntity(entity); spawned.add(entity);
            }
            var cost = WandApi.quote(wand, player, item.getVisCost(focus));
            wand.getItem().onUseTick(level, player, wand, 71999);
            var distant = spawned.get(0); var close = spawned.get(1);
            check(level.getEntitiesOfClass(ItemEntity.class, distant.getBoundingBox().inflate(1)).contains(distant), "telekinesis fixture item is visible to the actual world query");
            check(Math.abs(distant.getDeltaMovement().length() - .3333f) < 1e-8, "far item receives normalized original speed .3333; actual=" + distant.getDeltaMovement() + " loaded=" + level.hasChunkAt(distant.blockPosition()) + " entities=" + level.getEntitiesOfClass(ItemEntity.class, distant.getBoundingBox().inflate(1)).size() + " vis=" + WandApi.stored(wand, Aspects.AIR));
            check(close.getDeltaMovement().distanceTo(new Vec3(-.5 * .3333f, 0, 0)) < 1e-8, "near item retains scaled non-normalized approach");
            check(WandApi.stored(wand, Aspects.AIR) == 1000 - cost.get(Aspects.AIR)
                    && WandApi.stored(wand, Aspects.ENTROPY) == 1000 - cost.get(Aspects.ENTROPY), "one payment for the whole item group");
            WandApi.set(wand, Aspects.AIR, 0); var motion = distant.getDeltaMovement();
            check(!item.cast(player, wand, focus) && distant.getDeltaMovement().equals(motion), "unaffordable effect does not move entities");
            WandApi.set(wand, Aspects.AIR, 1000); player.setShiftKeyDown(true);
            check(item.cast(player, wand, focus), "sneaking effect still finds the nearby group");
            Vec3 sneakTarget = player.position().add(0, player.getBbHeight() / 2d + .5, 0);
            Vec3 delta = sneakTarget.subtract(close.position().add(0, close.getBbHeight() / 2d, 0));
            check(close.getDeltaMovement().distanceTo(delta.normalize().scale(.3333f)) < 1e-8, "sneaking attracts to the player instead of five blocks ahead");
            check(item.getPossibleUpgradesByRank(focus, 1).isEmpty() && !item.applyUpgrade(focus, FocusUpgradeType.potency, 1), "original telekinesis has no focus upgrades");
            check(item.isVisCostPerTick(focus) && item.getFocusColor(focus) == 10223806, "original per-tick tooltip and color");
            var recipe = (InfusionRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("focus_telekinesis")).orElseThrow().value();
            check(recipe.instability() == 5 && recipe.components().size() == 7
                    && recipe.aspects().equals(Map.of("motus", 10, "aer", 20, "perditio", 20, "cognitio", 10)), "exact telekinesis infusion");
            var research = AddonRegistry.research().get("thaumictinkerer:focus_telekinesis");
            check(research.secondary() && research.parents().equals(List.of("thaumictinkerer:focus_flight")), "telekinesis research follows the real flight focus");
            System.out.println("TT_TELEKINESIS_FOCUS_PASS checks=" + checks); helper.succeed();
        } finally { stop(player, wand); spawned.forEach(ItemEntity::discard); }
    }
}
