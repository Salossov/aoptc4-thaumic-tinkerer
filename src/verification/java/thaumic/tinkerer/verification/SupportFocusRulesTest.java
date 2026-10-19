package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ContentRules;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.ProjectileHooks;
import thaumic.tinkerer.item.FocusContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class SupportFocusRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void healingFocus(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var player = FocusRulesTest.player(helper, "[TTHeal]"); var item = FocusContent.HEAL.get(); var focus = new ItemStack(item);
        for (int rank = 1; rank <= 3; rank++) item.applyUpgrade(focus, FocusUpgradeType.potency, rank);
        var wand = FocusRulesTest.wand(player, focus); float health = player.getMaxHealth() - 4; player.setHealth(health);
        try {
            var cost = WandApi.quote(wand, player, item.getVisCost(focus));
            wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            for (int i = 0; i < 10; i++) wand.getItem().onUseTick(player.level(), player, wand, 72000 - i);
            check(player.getHealth() == health && WandApi.stored(wand, Aspects.EARTH) == 1000, "healing preparation does not pay every tick");
            WandApi.set(wand, Aspects.EARTH, 0); wand.getItem().onUseTick(player.level(), player, wand, 71990);
            check(player.isUsingItem() && player.getHealth() == health, "insufficient vis pauses healing while holding the wand");
            WandApi.set(wand, Aspects.EARTH, 1000);
            wand.getItem().releaseUsing(wand, player.level(), player, 71990); player.stopUsingItem();
            wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            for (int i = 0; i < 29; i++) wand.getItem().onUseTick(player.level(), player, wand, 72000 - i);
            check(player.getHealth() == health, "unlike smelting, stopping healing resets progress; attached potency retains original ineffective lookup");
            wand.getItem().onUseTick(player.level(), player, wand, 71971);
            check(player.getHealth() == health + 1, "thirtieth active tick heals exactly one health point");
            check(WandApi.stored(wand, Aspects.EARTH) == 1000 - cost.get(Aspects.EARTH)
                    && WandApi.stored(wand, Aspects.WATER) == 1000 - cost.get(Aspects.WATER), "single completion payment");
            player.setHealth(player.getMaxHealth()); var before = WandApi.stored(wand, Aspects.WATER);
            check(!item.cast(player, wand, focus) && WandApi.stored(wand, Aspects.WATER) == before, "full health consumes no vis");
            var recipe = (InfusionRecipe) player.level().getRecipeManager().byKey(ThaumicTinkerer.id("focus_heal")).orElseThrow().value();
            check(recipe.instability() == 4 && recipe.components().size() == 4 && recipe.aspects().equals(Map.of("sano", 10, "spiritus", 10, "victus", 15)), "exact healing infusion");
            check(item.getFocusColor(focus) == 16580822 && !item.isVisCostPerTick(focus), "original color and cost presentation");
            System.out.println("TT_HEAL_FOCUS_PASS checks=" + checks); helper.succeed();
        } finally { FocusRulesTest.stop(player, wand); player.setHealth(player.getMaxHealth()); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void deflectFocus(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        var level = helper.getLevel(); var player = FocusRulesTest.player(helper, "[TTDeflect]"); var other = FocusRulesTest.player(helper, "[TTArcher]");
        var item = FocusContent.DEFLECT.get(); var focus = new ItemStack(item); var wand = FocusRulesTest.wand(player, focus);
        var anchor = helper.absolutePos(BlockPos.ZERO);
        var origin = new BlockPos((anchor.getX() >> 4) * 16 + 8, anchor.getY() + 100, (anchor.getZ() >> 4) * 16 + 8);
        player.setPos(origin.getX(), origin.getY(), origin.getZ());
        var spawned = new ArrayList<Entity>();
        try {
            var hostile = new Arrow(EntityType.ARROW, level); hostile.setPos(player.position().add(-2, 0, 0)); hostile.setDeltaMovement(1, 0, 0); hostile.setOwner(other);
            var own = new Arrow(EntityType.ARROW, level); own.setPos(player.position().add(0, 0, 2)); own.setDeltaMovement(0, 0, -1); own.setOwner(player);
            var tiny = new Arrow(EntityType.ARROW, level); tiny.setPos(player.position().add(-1, 0, 1)); tiny.setDeltaMovement(.000001, 0, 0);
            var outside = new Arrow(EntityType.ARROW, level); outside.setPos(player.position().add(3.5, 0, 0)); outside.setDeltaMovement(-1, 0, 0);
            var negative = new Arrow(EntityType.ARROW, level); negative.setPos(player.position().add(-3.5, 0, 0)); negative.setDeltaMovement(-1, 0, 0);
            var bottle = new ThrownExperienceBottle(EntityType.EXPERIENCE_BOTTLE, level); bottle.setPos(player.position().add(1, 0, 0)); bottle.setDeltaMovement(-1, 0, 0);
            var fireball = new LargeFireball(EntityType.FIREBALL, level); fireball.setPos(player.position().add(0, 0, 1)); fireball.setDeltaMovement(0, 0, -1);
            for (var entity : List.of(hostile, own, tiny, outside, negative, bottle, fireball)) { level.addFreshEntity(entity); spawned.add(entity); }
            check(level.getEntities(player, player.getBoundingBox().inflate(5)).contains(hostile), "deflection fixture arrow is visible to the actual world query");
            boolean botania=net.neoforged.fml.ModList.get().isLoaded("botania");
            Vec3 oldHostile=hostile.position(),oldNegative=negative.position();
            Vec3 oldOwn = own.position(), oldBottle = bottle.position(), oldOutside = outside.position(), oldFireball = fireball.position(), oldTiny = tiny.position();
            var cost = WandApi.quote(wand, player, item.getVisCost(focus));
            wand.getItem().use(level, player, InteractionHand.MAIN_HAND); wand.getItem().onUseTick(level, player, wand, 72000);
            check(botania?hostile.position().equals(oldHostile):hostile.position().distanceTo(player.position().add(2, 0, 0)) < 1e-8, "original deflection teleports along incoming motion by twice its distance, passing over the protected player; actual=" + hostile.position() + " player=" + player.position() + " loaded=" + level.hasChunkAt(hostile.blockPosition()) + " entities=" + level.getEntities(player, player.getBoundingBox().inflate(5)).size() + " vis=" + WandApi.stored(wand, Aspects.ORDER));
            check(hostile.getDeltaMovement().equals(new Vec3(1, 0, 0)) && hostile.getOwner() == other, "trajectory and projectile owner remain unchanged");
            check(own.position().equals(oldOwn), "original owner registry excludes the player's own arrow");
            check(bottle.position().equals(oldBottle), "experience bottles remain blacklisted");
            check(outside.position().equals(oldOutside) && (botania?negative.position().equals(oldNegative):negative.getX() < player.getX() - 9), "asymmetric minus-four/plus-three search box is preserved");
            check(fireball.position().equals(oldFireball) && !ProjectileHooks.isProjectile(fireball), "vanilla fireballs do not gain legacy IProjectile membership");
            check(botania?tiny.position().equals(oldTiny):Math.abs(tiny.getX() - oldTiny.x - Math.sqrt(2) * 2) < 1e-8, "old normalization keeps a very small nonzero velocity");
            check(WandApi.stored(wand, Aspects.ORDER) == 1000 - cost.get(Aspects.ORDER) && WandApi.stored(wand, Aspects.AIR) == 1000 - cost.get(Aspects.AIR), "one per-tick payment for the whole protection field");
            ProjectileHooks.DEFLECT_BLACKLIST.add(AbstractArrow.class);
            check(ProjectileHooks.blacklisted(hostile)==botania, "blacklist class entries match exact classes; source Botania early return takes precedence over interface entries");
            ProjectileHooks.DEFLECT_BLACKLIST.add(net.minecraft.world.level.entity.EntityAccess.class);
            check(ProjectileHooks.blacklisted(hostile), "interface blacklist entries match implementing projectile classes");
            WandApi.set(wand, Aspects.ORDER, 0); var before = hostile.position();
            check(!item.cast(player, wand, focus) && hostile.position().equals(before), "no vis means no deflection");
            var recipe = (InfusionRecipe) level.getRecipeManager().byKey(ThaumicTinkerer.id("focus_deflect")).orElseThrow().value();
            check(recipe.instability() == 5 && recipe.aspects().equals(Map.of("aer", 15, "tutamen", 5, "ordo", 20)) && recipe.components().size() == 4, "exact deflection infusion");
            var damagedFocus = new ItemStack(FocusContent.FLIGHT.get()); damagedFocus.setDamageValue(1);
            check(!recipe.input().test(damagedFocus), "default metadata-zero flight focus ingredient does not accept a damaged focus");
            check(item.getPossibleUpgradesByRank(focus, 1).equals(List.of(FocusUpgradeType.treasure)), "only default treasure upgrade is supported");
            System.out.println("TT_DEFLECT_FOCUS_PASS checks=" + checks); helper.succeed();
        } finally {
            ProjectileHooks.DEFLECT_BLACKLIST.remove(AbstractArrow.class); ProjectileHooks.DEFLECT_BLACKLIST.remove(net.minecraft.world.level.entity.EntityAccess.class);
            FocusRulesTest.stop(player, wand); spawned.forEach(Entity::discard);
        }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void enderChestFocus(GameTestHelper helper) {
        VerificationReports.install(); checks = 0;
        // NeoForge FakePlayer.openMenu is deliberately a no-op. Use the actual server
        // player implementation with its no-network handler, without firing a login event.
        var networkStub = FocusRulesTest.player(helper, "[TTEnderChest]");
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), networkStub.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = networkStub.connection;
        var item = FocusContent.ENDER_CHEST.get(); var focus = new ItemStack(item); var wand = FocusRulesTest.wand(player, focus);
        try {
            player.getEnderChestInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
            WandApi.set(wand, Aspects.ORDER, 0); wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player.containerMenu == player.inventoryMenu, "unaffordable cast does not open inventory");
            WandApi.set(wand, Aspects.ORDER, 1000); var cost = WandApi.quote(wand, player, item.getVisCost(focus));
            wand.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player.containerMenu instanceof ChestMenu && player.containerMenu.slots.size() == 63, "native wand opens a three-row ender chest menu");
            check(player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND) && player.containerMenu.getSlot(0).getItem().getCount() == 3, "remote menu reads the actual player's ender inventory");
            player.containerMenu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT, 2));
            check(player.getEnderChestInventory().getItem(0).is(Items.GOLD_INGOT) && player.containerMenu.stillValid(player), "remote menu writes to persistent ender storage without a nearby chest block");
            check(WandApi.stored(wand, Aspects.ORDER) == 1000 - cost.get(Aspects.ORDER)
                    && WandApi.stored(wand, Aspects.ENTROPY) == 1000 - cost.get(Aspects.ENTROPY), "one activation payment for opening the menu");
            check(!player.isUsingItem() && item.getFocusColor(focus) == 1253923, "original single-use behavior and color");
            System.out.println("TT_ENDER_CHEST_FOCUS_PASS checks=" + checks); helper.succeed();
        } finally { player.closeContainer(); player.getEnderChestInventory().clearContent(); FocusRulesTest.stop(player, wand); }
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void mirrorContentRules(GameTestHelper helper) throws java.io.IOException {
        VerificationReports.install(); checks = 0;
        boolean enabled = ContentRules.mirrorsEnabled(); var player = FocusRulesTest.player(helper, "[TTRules]");
        for (String id : List.of("MIRROR", "MIRRORHAND", "MIRRORESSENTIA", "thaumictinkerer:focus_deflect", "thaumictinkerer:focus_heal", "thaumictinkerer:focus_ender_chest"))
            check(ResearchApi.exists(player, id) == enabled, "server research catalog follows actual mirrors setting: " + id);
        for (var id : List.of(net.minecraft.resources.ResourceLocation.parse("thaumcraft:mirror"), net.minecraft.resources.ResourceLocation.parse("thaumcraft:mirrorhand"),
                net.minecraft.resources.ResourceLocation.parse("thaumcraft:mirroressentia"), ThaumicTinkerer.id("focus_ender_chest")))
            check(helper.getLevel().getRecipeManager().byKey(id).isPresent() == enabled, "real conditional recipe loading: " + id);
        check(helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("focus_heal")).isPresent()
                && helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("focus_deflect")).isPresent(), "original healing/deflect recipes remain registered even with absent research gates");
        check(AddonRegistry.research().containsKey("thaumictinkerer:focus_deflect"), "disabled addon keeps its startup model/book identity");
        check(AddonRegistry.disabledResearch().contains("thaumictinkerer:focus_deflect") != enabled, "server catalog condition is reevaluated, not frozen at mod initialization");
        var report = java.nio.file.Path.of(System.getProperty("thaumictinkerer.testReport") + ".mode.json");
        java.nio.file.Files.createDirectories(report.getParent());
        java.nio.file.Files.writeString(report, "{\"mirrors\":" + enabled + ",\"computercraft\":"+net.neoforged.fml.ModList.get().isLoaded("computercraft")+",\"ae2\":"+net.neoforged.fml.ModList.get().isLoaded("ae2")+",\"botania\":"+net.neoforged.fml.ModList.get().isLoaded("botania")+",\"enderstorage\":"+net.neoforged.fml.ModList.get().isLoaded("enderstorage")+",\"bloodmagic\":"+net.neoforged.fml.ModList.get().isLoaded("bloodmagic")+",\"multipart\":"+net.neoforged.fml.ModList.get().isLoaded("cb_microblock")+",\"jade\":"+net.neoforged.fml.ModList.get().isLoaded("jade")+",\"wthit\":"+net.neoforged.fml.ModList.get().isLoaded("wthit")+"}\n");
        System.out.println("TT_MIRROR_RULES_PASS enabled=" + enabled + " checks=" + checks); helper.succeed();
    }
}
