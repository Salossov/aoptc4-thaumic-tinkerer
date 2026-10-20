package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.CleansingTalismanItem;
import thaumic.tinkerer.item.XpTalismanItem;
import thaumic.tinkerer.util.ItemData;
import top.theillusivec4.curios.api.SlotContext;

@net.neoforged.neoforge.gametest.GameTestHolder(ThaumicTinkerer.ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class TalismanRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new net.minecraft.gametest.framework.GameTestAssertException(label); }
    private static int orbCount(ExperienceOrb orb) { var tag = new CompoundTag(); orb.addAdditionalSaveData(tag); return tag.getInt("Count"); }
    private static ExperienceOrb orb(GameTestHelper helper, net.minecraft.world.entity.LivingEntity wearer, int value, int count, double offset) {
        var orb = new ExperienceOrb(helper.getLevel(), wearer.getX() + offset, wearer.getY(), wearer.getZ(), value);
        orb.setNoGravity(true);orb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (count > 1) { var tag = new CompoundTag(); orb.addAdditionalSaveData(tag); tag.putInt("Count", count); orb.readAdditionalSaveData(tag); }
        helper.getLevel().addFreshEntity(orb);
        return orb;
    }
    private static InfusionRecipe infusion(GameTestHelper helper, String id) {
        return (InfusionRecipe) helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id(id)).orElseThrow().value();
    }
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void experienceTalisman(GameTestHelper helper) {
        VerificationReports.install();
        checks = 0;
        var level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTXpTalisman"));
        var created = new ArrayList<ExperienceOrb>();
        var item = ThaumicTinkerer.XP_TALISMAN.get();
        var stack = new ItemStack(item);
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX() + .5, pos.getY()+100, pos.getZ() + .5);
        player.getInventory().clearContent();
        player.getAbilities().instabuild = false;
        boolean[] awaiting={false};
        Runnable cleanup=()->{created.forEach(ExperienceOrb::discard);player.setShiftKeyDown(false);player.getInventory().clearContent();};
        try {
            check(XpTalismanItem.xp(stack) == 0 && !XpTalismanItem.absorbing(stack), "new talisman is empty and inactive");
            ItemData.update(stack, tag -> tag.putString("unrelated", "retained"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setShiftKeyDown(true);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(XpTalismanItem.absorbing(stack), "sneak use activates");
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(!XpTalismanItem.absorbing(stack), "second sneak use deactivates");
            XpTalismanItem.xp(stack, 1500);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(!XpTalismanItem.absorbing(stack), "cannot toggle at capacity");
            XpTalismanItem.xp(stack, 9);
            player.setShiftKeyDown(false);
            player.getInventory().setItem(1, new ItemStack(Items.GLASS_BOTTLE, 2));
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(XpTalismanItem.xp(stack) == 9 && player.getInventory().getItem(1).getCount() == 2, "less than ten XP consumes no bottle");
            XpTalismanItem.xp(stack, 25);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(XpTalismanItem.xp(stack) == 15 && player.getInventory().getItem(1).getCount() == 1,
                    "bottling consumes exactly ten stored XP and one glass bottle");
            check(player.getInventory().items.stream().filter(s -> s.is(Items.EXPERIENCE_BOTTLE)).mapToInt(ItemStack::getCount).sum() == 1,
                    "bottling creates exactly one experience bottle");
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(XpTalismanItem.xp(stack) == 5 && player.getInventory().getItem(1).isEmpty(), "second bottle debits exactly once");
            XpTalismanItem.xp(stack, 50);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(XpTalismanItem.xp(stack) == 50, "no glass bottle leaves stored XP untouched");
            check(ItemData.read(stack).getString("unrelated").equals("retained"), "state changes retain unrelated custom data");

            XpTalismanItem.xp(stack, 0);
            var near = orb(helper, player, 7, 1, 0); created.add(near);
            item.wornTick(stack, player);
            check(near.isAlive() && XpTalismanItem.xp(stack) == 0, "inactive talisman ignores experience orbs");
            // FULL chunks can precede native entity-section visibility. Wait for
            // the real orb to become queryable instead of assuming addFreshEntity is immediate.
            awaiting[0]=true;
            helper.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener(){public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo t){}public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestInfo n,net.minecraft.gametest.framework.GameTestRunner r){}public void testPassed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){cleanup.run();}public void testFailed(net.minecraft.gametest.framework.GameTestInfo t,net.minecraft.gametest.framework.GameTestRunner r){cleanup.run();}});
            helper.onEachTick(new Runnable(){boolean done;public void run(){
                if(done)return;
                check(near.isAlive(),"isolated orb remains alive while its native section is loading");
                if(!level.getEntitiesOfClass(ExperienceOrb.class,new net.minecraft.world.phys.AABB(near.blockPosition()).inflate(1)).contains(near))return;
                done=true;
                try{
            XpTalismanItem.absorbing(stack, true);
            var far = orb(helper, player, 3, 1, 4); created.add(far);
            item.curioTick(new SlotContext("necklace", player, 0, false, true), stack);
            check(!near.isAlive() && XpTalismanItem.xp(stack) == 7 && far.isAlive(), "worn talisman absorbs within original cube and ignores distant orbs");
            far.discard();

            XpTalismanItem.xp(stack, 1490);
            var three = List.of(orb(helper, player, 3, 1, 0), orb(helper, player, 3, 1, 0), orb(helper, player, 3, 1, 0));
            created.addAll(three);
            item.wornTick(stack, player);
            check(XpTalismanItem.xp(stack) == 1496 && three.stream().filter(ExperienceOrb::isAlive).count() == 1,
                    "legacy cumulative-capacity subtraction accepts only two of three 3-XP orbs with ten room");
            three.forEach(ExperienceOrb::discard);

            XpTalismanItem.xp(stack, 1490);
            var merged = orb(helper, player, 3, 3, 0); created.add(merged);
            item.wornTick(stack, player);
            check(XpTalismanItem.xp(stack) == 1496 && merged.isAlive() && orbCount(merged) == 1 && merged.getValue() == 3,
                    "modern merged units preserve original capacity quirk without deleting unconsumed XP");
            merged.discard();

            XpTalismanItem.xp(stack, 1499);
            var last = orb(helper, player, 1, 1, 0); created.add(last);
            item.wornTick(stack, player);
            check(XpTalismanItem.xp(stack) == 1500 && XpTalismanItem.absorbing(stack) && !last.isAlive(), "reaching capacity does not deactivate until the next worn tick");
            item.wornTick(stack, player);
            check(!XpTalismanItem.absorbing(stack), "next worn tick deactivates a full talisman");
            var stored = stack.save(level.registryAccess());
            var restored = ItemStack.parse(level.registryAccess(), stored).orElseThrow();
            check(XpTalismanItem.xp(restored) == 1500 && !XpTalismanItem.absorbing(restored)
                    && ItemData.read(restored).getString("unrelated").equals("retained"), "experience state survives real item-component serialization");
            check(stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse("curios:necklace"))), "necklace slot tag is loaded");
            var curioInventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            check(curioInventory.getCurios().containsKey("necklace")
                    && curioInventory.getCurios().get("necklace").getStacks().isItemValid(0, stack), "real Curios handler accepts the talisman");
            var recipe = infusion(helper, "xp_talisman");
            check(recipe.instability() == 6 && recipe.aspects().equals(Map.of("lucrum", 20, "permutatio", 10, "bestia", 10, "machina", 5)), "original XP infusion costs and instability");
            check(recipe.input().test(new ItemStack(Items.GOLD_INGOT)) && recipe.components().size() == 4
                    && recipe.components().get(0).test(new ItemStack(Items.QUARTZ))
                    && recipe.components().get(1).test(new ItemStack(Items.QUARTZ))
                    && recipe.components().get(2).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:zombie_brain"))))
                    && recipe.components().get(3).test(new ItemStack(Items.DIAMOND)), "release-164 components, not the differing published-source recipe");
            var research = AddonRegistry.research().get("thaumictinkerer:xp_talisman");
            check(research.secondary() && research.concealed() && research.parents().equals(List.of("JARBRAIN", "thaumictinkerer:spell_cloth"))
                    && research.hiddenParents().equals(List.of("INFUSION")), "original XP research flags, parents and implicit infusion requirement");
            System.out.println("TT_XP_TALISMAN_PASS checks=" + checks);
            helper.succeed();
                }finally{cleanup.run();}
            }});
        } finally {
            if(!awaiting[0])cleanup.run();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cleansingTalisman(GameTestHelper helper) {
        VerificationReports.install();
        checks = 0;
        var level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var item = ThaumicTinkerer.CLEANSING_TALISMAN.get();
        var stack = new ItemStack(item);
        var context = new SlotContext("necklace", player, 0, false, true);
        int oldTick = player.tickCount;
        player.getAbilities().instabuild = false;
        player.removeAllEffects();
        player.clearFire();
        try {
            check(!CleansingTalismanItem.enabled(stack) && stack.getMaxDamage() == 100, "original default state and durability");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setShiftKeyDown(true);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(CleansingTalismanItem.enabled(stack), "sneak use toggles cleansing");
            player.setShiftKeyDown(false);
            item.use(level, player, InteractionHand.MAIN_HAND);
            check(CleansingTalismanItem.enabled(stack), "plain use does not change cleansing state");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
            player.tickCount = 19;
            item.curioTick(context, stack);
            check(player.hasEffect(MobEffects.POISON) && stack.getDamageValue() == 0, "cleansing waits for the twentieth entity tick");
            player.tickCount = 20;
            player.igniteForSeconds(5);
            item.curioTick(context, stack);
            check(!player.isOnFire() && player.hasEffect(MobEffects.POISON) && stack.getDamageValue() == 1,
                    "fire has priority over harmful effects and costs one durability");
            item.curioTick(context, stack);
            check(!player.hasEffect(MobEffects.POISON) && stack.getDamageValue() == 2, "one ordinary bad effect costs one durability");
            var warp = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("thaumcraft:blurred_vision")).orElseThrow();
            var ward = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("thaumcraft:warp_ward")).orElseThrow();
            player.addEffect(new MobEffectInstance(warp, 600));
            player.addEffect(new MobEffectInstance(ward, 600));
            item.curioTick(context, stack);
            check(!player.hasEffect(warp) && player.hasEffect(ward) && stack.getDamageValue() == 12,
                    "warp affliction costs ten; Warp Ward remains");
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
            item.curioTick(context, stack);
            check(player.hasEffect(MobEffects.MOVEMENT_SPEED) && stack.getDamageValue() == 12, "beneficial effects are retained without charge");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
            item.curioTick(context, stack);
            check(player.hasEffect(MobEffects.POISON) != player.hasEffect(MobEffects.WEAKNESS), "one activation removes only one harmful effect");
            check(stack.getDamageValue() == 13, "one durability for the single effect removed");
            player.removeAllEffects();
            CleansingTalismanItem.enabled(stack, false);
            player.igniteForSeconds(5);
            item.curioTick(context, stack);
            check(player.isOnFire() && stack.getDamageValue() == 13, "disabled talisman neither extinguishes nor spends durability");
            CleansingTalismanItem.enabled(stack, true);
            stack.setDamageValue(99);
            item.curioTick(context, stack);
            check(!stack.isEmpty() && stack.getDamageValue() == 100, "legacy damage equal to maximum remains usable");
            player.igniteForSeconds(5);
            item.curioTick(context, stack);
            check(stack.isEmpty() && !player.isOnFire(), "next one-point use breaks only above the original maximum");
            var fresh = new ItemStack(item);
            CleansingTalismanItem.enabled(fresh, true);
            player.getAbilities().instabuild = true;
            player.igniteForSeconds(5);
            item.curioTick(context, fresh);
            check(!player.isOnFire() && fresh.getDamageValue() == 0, "original creative-mode durability exemption");
            check(fresh.is(TagKey.create(Registries.ITEM, ResourceLocation.parse("curios:necklace"))), "cleansing talisman is accepted by the necklace tag");
            var restored = ItemStack.parse(level.registryAccess(), fresh.save(level.registryAccess())).orElseThrow();
            check(CleansingTalismanItem.enabled(restored), "enabled state survives real item-component serialization");
            var recipe = infusion(helper, "cleansing_talisman");
            check(recipe.instability() == 5 && recipe.aspects().equals(Map.of("sano", 10, "instrumentum", 10, "humanus", 20, "victus", 10)), "original cleansing infusion costs");
            check(recipe.components().size() == 6 && recipe.input().test(new ItemStack(Items.ENDER_PEARL)), "original infusion input and component count");
            var originalNitor=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:nitor")));
            var wrongTallow=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:tallow")));
            check(recipe.components().stream().anyMatch(i -> i.test(originalNitor)) && recipe.components().stream().noneMatch(i -> i.test(wrongTallow)), "original cleansing component resource metadata 1 is nitor");
            var research = AddonRegistry.research().get("thaumictinkerer:cleansing_talisman");
            check(research.secondary() && research.parents().equals(List.of("thaumictinkerer:dark_quartz"))
                    && research.hiddenParents().equals(List.of("INFUSION")), "cleansing research retains quartz parent and implicit infusion prerequisite");
            System.out.println("TT_CLEANSING_TALISMAN_PASS checks=" + checks);
            helper.succeed();
        } finally {
            player.removeAllEffects();
            player.clearFire();
            player.setShiftKeyDown(false);
            player.getAbilities().instabuild = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.tickCount = oldTick;
        }
    }
}
