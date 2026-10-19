package thaumic.tinkerer.kami;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import thaumcraft.api.entities.EffectDurationApi;
import thaumcraft.api.entities.LivingEquipmentTickEvent;
import thaumcraft.api.entities.PlayerAirControlEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.item.BrightNitorItem;

/** Release-164 callbacks. Keep the different source metadata tests (==0 versus !=1)
 * and the passive effects which do not depend on the global toggle.
 */
public final class KamiArmorEffects {
    private static final Set<String> FLIGHT=ConcurrentHashMap.newKeySet();
    private static final Set<String> HIGH_STEP=ConcurrentHashMap.newKeySet();
    private static final ResourceLocation STEP=ThaumicTinkerer.id("kami_step_height");
    public static void register() {
        LivingEquipmentTickEvent.BUS.addListener(EventPriority.HIGH,KamiArmorEffects::stepBookkeeping);
        LivingEquipmentTickEvent.BUS.addListener(KamiArmorEffects::tick);
        PlayerAirControlEvent.BUS.addListener(KamiArmorEffects::airControl);
        NeoForge.EVENT_BUS.addListener(KamiArmorEffects::jump);
        NeoForge.EVENT_BUS.addListener(KamiArmorEffects::fall);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,KamiArmorEffects::fireDamage);
    }
    private static boolean wears(Player player,EquipmentSlot slot,Item item) {return player.getItemBySlot(slot).is(item);}
    private static int variant(Player player,EquipmentSlot slot) {return AwakenedIchorclothArmorItem.variant(player.getItemBySlot(slot));}
    private static String flightKey(Player player) {return player.getGameProfile().getName()+":"+player.level().isClientSide();}
    public static void logout(Player player) {
        thaumic.tinkerer.item.DeflectionActions.cancelArmor(player);
        String name=player.getGameProfile().getName();FLIGHT.remove(name+":false");FLIGHT.remove(name+":true");HIGH_STEP.remove(name);
        var step=player.getAttribute(Attributes.STEP_HEIGHT);if(step!=null)step.removeModifier(STEP);
    }
    private static void tick(LivingEquipmentTickEvent event) {
        if(!(event.entity() instanceof Player player))return;
        boolean enabled=KamiArmorState.enabled(player);
        if(wears(player,EquipmentSlot.HEAD,KamiArmorContent.GEM_HELM.get()))helmet(player,enabled);
        boolean chest=wears(player,EquipmentSlot.CHEST,KamiArmorContent.GEM_CHEST.get());
        if(chest&&enabled&&variant(player,EquipmentSlot.CHEST)!=1) {
            // Both the inherited listener and the chest's own listener call this.
            thaumic.tinkerer.item.DeflectionActions.armor(player);
            thaumic.tinkerer.item.DeflectionActions.armor(player);
        }
        flight(player,chest&&enabled&&variant(player,EquipmentSlot.CHEST)==0&&TinkererConfig.FLIGHT.get());
        if(wears(player,EquipmentSlot.LEGS,KamiArmorContent.GEM_LEGS.get())&&enabled&&variant(player,EquipmentSlot.LEGS)!=1)leggings(player);
        if(wears(player,EquipmentSlot.FEET,KamiArmorContent.GEM_BOOTS.get())&&enabled&&variant(player,EquipmentSlot.FEET)!=1)boots(player);
    }
    private static void extend(Player player,Holder<MobEffect> effect,int amplifier) {
        var current=player.getEffect(effect);
        if(current!=null&&current.getDuration()<=202)EffectDurationApi.setDuration(current,202);
        else player.addEffect(new MobEffectInstance(effect,202,amplifier,true,true));
    }
    private static void helmet(Player player,boolean enabled) {
        if(enabled&&variant(player,EquipmentSlot.HEAD)==0) {
            if(player.isEyeInFluid(FluidTags.WATER)) {player.setAirSupply(300);extend(player,MobEffects.NIGHT_VISION,0);}
            if(player.isEyeInFluid(FluidTags.LAVA)) {player.setAirSupply(300);extend(player,MobEffects.BLINDNESS,0);}
        }
        int food=player.getFoodData().getFoodLevel();
        if(food>0&&food<18&&player.getHealth()>0&&player.getHealth()<player.getMaxHealth()&&player.tickCount%80==0)player.heal(1);
    }
    private static void flight(Player player,boolean eligible) {
        var abilities=player.getAbilities();boolean oldMayFly=abilities.mayfly,oldFlying=abilities.flying,oldInvulnerable=abilities.invulnerable;
        String key=flightKey(player);
        if(FLIGHT.contains(key)) {
            if(eligible)abilities.mayfly=true;
            else {
                if(!abilities.instabuild) {abilities.mayfly=false;abilities.flying=false;abilities.invulnerable=false;}
                FLIGHT.remove(key);
            }
        } else if(eligible) {FLIGHT.add(key);abilities.mayfly=true;}
        if(player instanceof ServerPlayer server&&(oldMayFly!=abilities.mayfly||oldFlying!=abilities.flying||oldInvulnerable!=abilities.invulnerable))server.onUpdateAbilities();
    }
    private static void leggings(Player player) {
        extend(player,MobEffects.FIRE_RESISTANCE,10);
        int x=Mth.floor(player.getX()),y=(int)player.getY()+1,z=Mth.floor(player.getZ());
        BrightNitorItem.light(player.level(),new BlockPos(x,y,z),true);
        float yaw=Mth.wrapDegrees(player.getYRot()+90F)*(float)Math.PI/180F;
        double dx=Math.cos(yaw),dz=Math.sin(yaw),length=Math.sqrt(dx*dx+dz*dz);
        if(length!=0) {double reciprocal=1D/length;dx*=reciprocal;dz*=reciprocal;}
        double aheadX=dx,aheadZ=dz;
        for(int i=0;i<5;i++) {
            aheadX+=dx;aheadZ+=dz;
            // ItemBrightNitor.meta is reset before the forward row in the release.
            BrightNitorItem.light(player.level(),new BlockPos(x+(int)aheadX,y,z+(int)aheadZ),false);
        }
    }
    private static void boots(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,2,1,true,true));
        if(player.level().isClientSide())stepHeight(player,player.isShiftKeyDown()?.5:1);
        if((player.onGround()||player.getAbilities().flying)&&player.zza>0)player.moveRelative(player.getAbilities().flying?.075F:.15F,new Vec3(0,0,1));
        player.fallDistance=0;
        var pos=new BlockPos((int)player.getX(),(int)player.getY()-1,(int)player.getZ());
        if(!player.level().isClientSide()&&player.level().getBlockState(pos).is(Blocks.DIRT))player.level().setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),2);
    }
    private static void airControl(PlayerAirControlEvent event) {
        var player=event.player();
        if((!player.getAbilities().flying||player.isPassenger())&&KamiArmorState.enabled(player)
                &&wears(player,EquipmentSlot.FEET,KamiArmorContent.GEM_BOOTS.get())&&variant(player,EquipmentSlot.FEET)!=1)
            event.speed(player.isSprinting()?.05F:.04F);
    }
    private static void stepBookkeeping(LivingEquipmentTickEvent event) {
        if(!(event.entity() instanceof Player player)||!player.level().isClientSide())return;
        String name=player.getGameProfile().getName();boolean listed=HIGH_STEP.contains(name);
        boolean wears=wears(player,EquipmentSlot.FEET,KamiArmorContent.GEM_BOOTS.get()),enabled=KamiArmorState.enabled(player);
        if(!listed&&wears&&enabled&&variant(player,EquipmentSlot.FEET)==0)HIGH_STEP.add(name);
        if(listed&&(!wears||!enabled||variant(player,EquipmentSlot.FEET)==1)) {
            HIGH_STEP.remove(name);player.getAttribute(Attributes.STEP_HEIGHT).removeModifier(STEP);
        }
    }
    private static void stepHeight(Player player,double target) {
        var attribute=player.getAttribute(Attributes.STEP_HEIGHT);attribute.removeModifier(STEP);
        double raw=attribute.getBaseValue(),baseMultiplier=1,totalMultiplier=1;
        for(var modifier:attribute.getModifiers())switch(modifier.operation()) {
            case ADD_VALUE->raw+=modifier.amount();
            case ADD_MULTIPLIED_BASE->baseMultiplier+=modifier.amount();
            case ADD_MULTIPLIED_TOTAL->totalMultiplier*=1+modifier.amount();
        }
        // Offset before other mods' multipliers. Removal restores their value and
        // the modern 0.6 base. A zero multiplier from another mod remains effective.
        double gain=baseMultiplier*totalMultiplier;
        if(gain!=0&&Double.isFinite(gain))attribute.addTransientModifier(new AttributeModifier(STEP,target/gain-raw,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void jump(LivingJumpEvent event) {
        if(event.getEntity() instanceof Player player&&KamiArmorState.enabled(player)
                &&wears(player,EquipmentSlot.FEET,KamiArmorContent.GEM_BOOTS.get())&&variant(player,EquipmentSlot.FEET)==0)
            player.setDeltaMovement(player.getDeltaMovement().add(0,.3,0));
    }
    private static void fall(LivingFallEvent event) {
        if(event.getEntity() instanceof Player player&&wears(player,EquipmentSlot.FEET,KamiArmorContent.GEM_BOOTS.get()))event.setDistance(0);
    }
    private static void fireDamage(LivingIncomingDamageEvent event) {
        if(event.getEntity() instanceof Player player&&KamiArmorState.enabled(player)
                &&wears(player,EquipmentSlot.LEGS,KamiArmorContent.GEM_LEGS.get())&&event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);player.heal(event.getAmount());
        }
    }
    private KamiArmorEffects() {}
}
