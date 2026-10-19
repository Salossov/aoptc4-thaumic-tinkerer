package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;

/** Core regressions needed by the tablet; reflection stays outside the released addon. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class FireBatRulesTest {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    private static Bat bat(ServerLevel level,BlockPos pos){
        var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("thaumcraft:firebat"));
        var bat=(Bat)java.util.Objects.requireNonNull(type.create(level));bat.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);return bat;
    }
    private static void ai(Bat bat){
        try{var method=bat.getClass().getDeclaredMethod("customServerAiStep");method.setAccessible(true);method.invoke(bat);}
        catch(ReflectiveOperationException error){throw new AssertionError("native firebat AI invocation",error);}
    }
    private static void summon(Bat bat,Player owner,LivingEntity target,int potency,boolean devil,boolean explosive,boolean vampire){
        try{bat.getClass().getMethod("summon",Player.class,LivingEntity.class,int.class,boolean.class,boolean.class,boolean.class).invoke(bat,owner,target,potency,devil,explosive,vampire);}
        catch(ReflectiveOperationException error){throw new AssertionError("native focus summon invocation",error);}
    }
    private static CompoundTag data(Bat bat){var tag=new CompoundTag();bat.addAdditionalSaveData(tag);return tag;}
    @GameTest(template="summon_space",batch="firebat",timeoutTicks=100)
    public static void wildSurvivalAndFocusLoss(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));var wild=bat(level,pos);
        check(wild.isResting()&&wild.getMaxHealth()==5&&wild.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"native wild defaults are hanging, five health and one attack");
        wild.setResting(false);for(int i=0;i<100;i++)ai(wild);
        check(wild.isAlive()&&wild.getHealth()==5&&wild.getDeltaMovement().lengthSqr()>0,"wild bat wanders without owner or target and never self-damages");
        try(var fixture=new NativePlayerFixture(helper,"TTBatOwner",pos.east(5))){
            var target=new Cow(EntityType.COW,level);target.moveTo(pos.getX()+3,pos.getY(),pos.getZ(),0,0);level.addFreshEntity(target);
            var focus=bat(level,pos);summon(focus,fixture.player,target,2,true,false,false);
            check(data(focus).getBoolean("Summoned")&&!focus.isResting()&&focus.getMaxHealth()==15&&focus.getAttributeValue(Attributes.ATTACK_DAMAGE)==5,"focus preserves mode, devil health and potency attack");
            ai(focus);check(focus.getHealth()==15&&focus.getDeltaMovement().x>0,"focus still chases its live assigned target");
            var restored=bat(level,pos);restored.readAdditionalSaveData(data(focus));check(data(restored).getBoolean("Summoned")&&restored.getHealth()==15&&restored.getAttributeValue(Attributes.ATTACK_DAMAGE)==5,"summoned mode and upgrades survive native entity NBT");
            target.discard();ai(restored);check(restored.getHealth()==13,"only summoned bat loses two health after its target disappears");
            var legacy=data(focus);legacy.remove("Summoned");legacy.putInt("Flags",1);var upgraded=bat(level,pos);upgraded.readAdditionalSaveData(legacy);
            check(data(upgraded).getBoolean("Summoned")&&upgraded.getMaxHealth()==15,"previous port saves infer summoned mode from owner/target without losing upgrade bits");
            var original=new CompoundTag();original.putByte("BatFlags",(byte)30);original.putByte("damBonus",(byte)3);original.putFloat("Health",15);
            var old=bat(level,pos);old.readAdditionalSaveData(original);var oldData=data(old);
            check(oldData.getBoolean("Summoned")&&oldData.getInt("Flags")==15&&old.getMaxHealth()==15&&old.getAttributeValue(Attributes.ATTACK_DAMAGE)==6,"TC4's five packed flags and signed damage bonus migrate to explicit modern mode");
            var roundtrip=bat(level,pos);roundtrip.readAdditionalSaveData(data(wild));check(!data(roundtrip).getBoolean("Summoned")&&roundtrip.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"wild save stays wild instead of becoming a broken focus summon");
            wild.discard();focus.discard();restored.discard();upgraded.discard();old.discard();roundtrip.discard();
        }
        System.out.println("TT_FIREBAT_MODES_NBT_PASS");helper.succeed();
    }
    @GameTest(template="summon_space",batch="firebat",timeoutTicks=100)
    public static void wildPlayerAggressionAndResting(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));
        try(var fixture=new NativePlayerFixture(helper,"TTWildBatTarget",pos)){
            var player=fixture.player;level.addNewPlayer(player);
            try{
                var wild=bat(level,pos.east(6));level.setBlock(wild.blockPosition().above(),Blocks.STONE.defaultBlockState(),2);
                ai(wild);check(wild.getTarget()==player&&wild.isResting(),"wild bat acquires vulnerable player within twelve blocks but remains hanging beyond wake range four");
                player.getAbilities().invulnerable=true;wild.setTarget(null);ai(wild);check(wild.getTarget()==null,"wild bat ignores invulnerable players");
                player.getAbilities().invulnerable=false;wild.setPos(player.getX()+1,player.getY(),player.getZ());wild.setResting(false);ai(wild);
                check(wild.getTarget()==player&&(player.getHealth()<20||player.isOnFire()||wild.isRemoved()),"wild bat attacks a native vulnerable player without requiring a focus owner");
                wild.discard();level.setBlock(pos.east(6).above(),Blocks.AIR.defaultBlockState(),2);
            }finally{level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);}
        }
        System.out.println("TT_FIREBAT_WILD_AGGRESSION_PASS");helper.succeed();
    }
    @GameTest(template="summon_space",batch="firebat",timeoutTicks=100)
    public static void focusVampireImmunityAndDrops(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));
        try(var fixture=new NativePlayerFixture(helper,"TTVampireOwner",pos.east(6))){
            var owner=fixture.player;level.addNewPlayer(owner);
            try{
                var target=new Cow(EntityType.COW,level);target.moveTo(pos.getX()+1,pos.getY(),pos.getZ()+.5,0,0);level.addFreshEntity(target);
                var focus=bat(level,pos);summon(focus,owner,target,0,true,false,true);focus.setHealth(10);ai(focus);
                check(focus.getHealth()==11&&owner.hasEffect(MobEffects.REGENERATION)&&owner.getEffect(MobEffects.REGENERATION).getAmplifier()==1&&target.getHealth()==7,"vampire focus heals itself, grants owner regeneration II and keeps potency damage");
                check(!focus.hurt(level.damageSources().inFire(),2)&&!focus.hurt(level.damageSources().explosion(focus,focus),2)&&focus.getHealth()==11,"both fire and explosion immunity remain intact");
                focus.setHealth(1);focus.hurt(level.damageSources().genericKill(),100);
                check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"summoned bat does not drop wild gunpowder");
                int gunpowder=0;
                for(int seed=0;seed<12;seed++){
                    var wild=bat(level,pos);wild.getRandom().setSeed(seed);wild.hurt(level.damageSources().genericKill(),100);
                    for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2))){check(drop.getItem().is(Items.GUNPOWDER),"wild bat's source drop remains gunpowder");gunpowder+=drop.getItem().getCount();drop.discard();}wild.discard();
                }
                check(gunpowder>0,"native deaths exercise the wild drop branch");target.discard();focus.discard();
            }finally{level.removePlayerImmediately(owner,Entity.RemovalReason.DISCARDED);}
        }
        System.out.println("TT_FIREBAT_FOCUS_COMBAT_LOOT_PASS");helper.succeed();
    }
    private FireBatRulesTest(){}
}
