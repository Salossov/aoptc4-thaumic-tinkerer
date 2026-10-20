package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.entities.EffectDurationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.block.GasBlock;
import thaumic.tinkerer.block.GasContent;
import thaumic.tinkerer.block.NitorGasBlock;
import thaumic.tinkerer.kami.*;

/** Native scenarios: compiling these is not evidence that the mixins executed. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiArmorEffectsRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static void close(double actual,double expected,String label) {check(Math.abs(actual-expected)<1E-5,label+": "+actual);}
    private static boolean disabled(GameTestHelper helper) {
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {helper.succeed();return true;}return false;
    }
    @GameTest(template="summon_space",batch="kami_effects",timeoutTicks=100)
    public static void helmetAtActualFluidEyes(GameTestHelper helper) {
        if(disabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTKamiBreathing",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var level=helper.getLevel();var origin=helper.absolutePos(new BlockPos(8,4,8));
            player.setGameMode(GameType.SURVIVAL);KamiArmorState.save(player,true);
            var helm=KamiArmorContent.GEM_HELM.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.HEAD,helm);
            player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)level.setBlock(origin.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);
            player.baseTick();check(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"native fluid-eye setup");
            player.setAirSupply(20);player.aiStep();check(player.getAirSupply()==300,"water restores source air supply");
            var effect=player.getEffect(MobEffects.NIGHT_VISION);check(effect!=null&&effect.getDuration()==202&&effect.getAmplifier()==0&&effect.isAmbient(),"water grants source night vision");
            player.removeEffect(MobEffects.NIGHT_VISION);AwakenedIchorclothArmorItem.variant(helm,1);player.setAirSupply(20);player.aiStep();
            check(player.getAirSupply()==20&&!player.hasEffect(MobEffects.NIGHT_VISION),"dormant helmet does not grant breathing or vision");
            AwakenedIchorclothArmorItem.variant(helm,0);player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
            // Remove all water before adding lava: native fluid placement would
            // otherwise solidify the first lava cells against the old fixture.
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            for(int x=-1;x<=1;x++)for(int y=0;y<3;y++)for(int z=-1;z<=1;z++)level.setBlock(origin.offset(x,y,z),Blocks.LAVA.defaultBlockState(),2);
            player.baseTick();check(player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA),"native lava-eye setup");
            player.setAirSupply(20);player.aiStep();check(player.getAirSupply()==300,"lava restores source air supply");
            effect=player.getEffect(MobEffects.BLINDNESS);check(effect!=null&&effect.getDuration()==202&&effect.getAmplifier()==0&&effect.isAmbient(),"lava grants original blindness, preserving its surprising effect type");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="kami_effects",timeoutTicks=100)
    public static void exactEquipmentTickAndDuration(GameTestHelper helper) {
        if(disabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTKamiEffects",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;player.setGameMode(GameType.SURVIVAL);
            var helm=KamiArmorContent.GEM_HELM.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.HEAD,helm);
            KamiArmorState.save(player,false);AwakenedIchorclothArmorItem.variant(helm,1);
            player.getFoodData().setFoodLevel(17);player.setHealth(10);player.tickCount=80;player.aiStep();
            close(player.getHealth(),11,"actual aiStep invokes healing even with both switches off");
            player.tickCount=79;player.aiStep();close(player.getHealth(),11,"healing requires the original 80 tick boundary");
            player.tickCount=80;player.getFoodData().setFoodLevel(18);player.aiStep();close(player.getHealth(),11,"food 18 does not heal");
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);KamiArmorState.save(player,true);
            var legs=KamiArmorContent.GEM_LEGS.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.LEGS,legs);
            check(legs.is(NitorGasBlock.LEGGINGS),"generated retention tag contains the real KAMI leggings");
            var effect=new MobEffectInstance(MobEffects.FIRE_RESISTANCE,-1,3,false,false,false);
            player.addEffect(effect);var same=player.getEffect(MobEffects.FIRE_RESISTANCE);
            var origin=helper.absolutePos(new BlockPos(8,4,8));player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setYRot(-90);
            var centre=new BlockPos(origin.getX(),origin.getY()+1,origin.getZ());
            for(int x=0;x<=6;x++)helper.getLevel().setBlock(centre.offset(x,0,0),Blocks.AIR.defaultBlockState(),2);
            player.aiStep();
            check(player.getEffect(MobEffects.FIRE_RESISTANCE)==same&&same.getDuration()==202,"infinite effect is edited on the same object at actual equipment phase");
            check(same.getAmplifier()==3&&!same.isAmbient()&&!same.isVisible()&&!same.showIcon(),"duration edit preserves all existing effect flags and strength");
            var state=helper.getLevel().getBlockState(centre);
            check(state.is(GasContent.NITOR.get())&&state.getValue(GasBlock.SPREAD)==1,"light at player has the source KAMI retention range");
            for(int x=2;x<=6;x++) {
                state=helper.getLevel().getBlockState(centre.offset(x,0,0));
                check(state.is(GasContent.NITOR.get())&&state.getValue(GasBlock.SPREAD)==0,"forward light uses reset metadata at distance "+x);
            }
            EffectDurationApi.setDuration(same,201);player.aiStep();check(same.getDuration()==202,"finite existing effect is extended directly");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="kami_effects",timeoutTicks=100)
    public static void sourceFireJumpAndFallGates(GameTestHelper helper) {
        if(disabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTKamiDamage",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;player.setGameMode(GameType.SURVIVAL);KamiArmorState.save(player,true);
            var legs=KamiArmorContent.GEM_LEGS.get().getDefaultInstance();AwakenedIchorclothArmorItem.variant(legs,1);player.setItemSlot(EquipmentSlot.LEGS,legs);
            player.setHealth(10);player.invulnerableTime=0;player.hurt(helper.getLevel().damageSources().inFire(),4);
            close(player.getHealth(),14,"dormant leggings still turn fire damage into healing while global mode is enabled");
            KamiArmorState.save(player,false);player.invulnerableTime=0;player.hurt(helper.getLevel().damageSources().inFire(),4);
            check(player.getHealth()<14,"global off disables fire healing");player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);
            var boots=KamiArmorContent.GEM_BOOTS.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.FEET,boots);KamiArmorState.save(player,true);
            player.jumpFromGround();close(player.getDeltaMovement().y,.42F+.3,"real jump callback adds original 0.3 boost");
            AwakenedIchorclothArmorItem.variant(boots,2);player.jumpFromGround();close(player.getDeltaMovement().y,.42F,"raw metadata 2 does not grant jump boost");
            AwakenedIchorclothArmorItem.variant(boots,1);KamiArmorState.save(player,false);player.invulnerableTime=0;float health=player.getHealth();
            player.causeFallDamage(30,1,helper.getLevel().damageSources().fall());close(player.getHealth(),health,"fall immunity remains when both switches are off");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="kami_effects",timeoutTicks=100)
    public static void flightAirSpeedAndStepRestoration(GameTestHelper helper) throws Exception {
        if(disabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTKamiMovement",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;player.setGameMode(GameType.SURVIVAL);KamiArmorState.save(player,true);
            var boots=KamiArmorContent.GEM_BOOTS.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.FEET,boots);
            var speed=Player.class.getDeclaredMethod("getFlyingSpeed");speed.setAccessible(true);
            close((float)speed.invoke(player),.04F,"actual Player return hook changes ordinary air control");
            player.setSprinting(true);close((float)speed.invoke(player),.05F,"sprinting air control");
            player.getAbilities().flying=true;close((float)speed.invoke(player),player.getAbilities().getFlyingSpeed()*2F,"ability flight keeps its own speed");
            player.getAbilities().flying=false;player.setSprinting(false);AwakenedIchorclothArmorItem.variant(boots,1);
            close((float)speed.invoke(player),.02F,"dormant boots retain modern ordinary speed");
            var chest=KamiArmorContent.GEM_CHEST.get().getDefaultInstance();player.setItemSlot(EquipmentSlot.CHEST,chest);player.aiStep();
            check(player.getAbilities().mayfly==TinkererConfig.FLIGHT.get(),"flight follows configured eligibility at actual equipment phase");
            var arrow=new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW,helper.getLevel());
            double start=player.getX();arrow.setPos(start+.5,player.getY(),player.getZ());arrow.setDeltaMovement(1,0,0);
            check(helper.getLevel().addFreshEntity(arrow),"actual projectile enters the test level");
            player.aiStep();close(arrow.getX(),start+(net.neoforged.fml.ModList.get().isLoaded("botania")?.5:4.5),"source chest callbacks respect the optional Botania projectile filter");
            close(arrow.getDeltaMovement().x,1,"deflection changes position without changing trajectory");arrow.discard();
            if(TinkererConfig.FLIGHT.get()) {
                player.getAbilities().flying=true;player.getAbilities().invulnerable=true;
                AwakenedIchorclothArmorItem.variant(chest,1);player.aiStep();
                check(!player.getAbilities().mayfly&&!player.getAbilities().flying&&!player.getAbilities().invulnerable,"losing survival flight clears all three source flags");
            }
            // Client-only phase needs a real client; exercise its actual attribute
            // calculation here without pretending this verifies client dispatch.
            var step=player.getAttribute(Attributes.STEP_HEIGHT);step.setBaseValue(.6);
            var other=ThaumicTinkerer.id("verification_other_step");step.addTransientModifier(new AttributeModifier(other,.4,AttributeModifier.Operation.ADD_VALUE));
            var multiplier=ThaumicTinkerer.id("verification_other_step_multiplier");step.addTransientModifier(new AttributeModifier(multiplier,.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            var apply=KamiArmorEffects.class.getDeclaredMethod("stepHeight",Player.class,double.class);apply.setAccessible(true);
            apply.invoke(null,player,1D);close(step.getValue(),1,"active height is exact with another mod's modifiers");
            apply.invoke(null,player,.5D);close(step.getValue(),.5,"sneaking height is exact");
            KamiArmorEffects.logout(player);close(step.getValue(),1.5,"cleanup restores modern base and other modifiers");
            step.removeModifier(other);step.removeModifier(multiplier);close(step.getValue(),.6,"modern base survives removal");
        }
        helper.succeed();
    }
}
