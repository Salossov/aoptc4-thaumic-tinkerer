package thaumic.tinkerer.enchantment;

import java.util.Random;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.util.ItemData;

/** Release-164 event formulas. No random enchanting-table availability is added. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class TinkererEnchantments {
    private static final DeferredRegister<HolderSetType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.HOLDER_SET_TYPES,ThaumicTinkerer.ID);
    public static final DeferredHolder<HolderSetType,LegacyEnchantmentItems.Type> ITEM_TYPES=TYPES.register("legacy_enchantment_items",LegacyEnchantmentItems.Type::new);
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static ResourceKey<Enchantment> key(String name){return ResourceKey.create(Registries.ENCHANTMENT,ThaumicTinkerer.id(name));}
    public static int level(LivingEntity wearer,ItemStack stack,String name){
        if(stack.isEmpty())return 0;
        var holder=wearer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key(name));
        return holder.isEmpty()?0:stack.getEnchantmentLevel(holder.get());
    }
    private static int armor(Player player,String name){int result=0;for(var stack:player.getArmorSlots())result=Math.max(result,level(player,stack,name));return result;}
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event){
        if(!(event.getSource().getEntity() instanceof LivingEntity attacker))return;
        var held=attacker.getMainHandItem();if(held.isEmpty())return;
        float amount=event.getAmount();
        if(attacker instanceof Player player){int pounce=level(player,player.getItemBySlot(EquipmentSlot.LEGS),"pounce");
            if(pounce>0&&player.level().getBlockState(new BlockPos((int)Math.floor(player.getX()),(int)Math.floor(player.getY())-1,(int)Math.floor(player.getZ()))).is(Blocks.AIR))amount=(float)(amount*(1.0+.25*pounce));
        }
        int finalStrike=level(attacker,held,"final_strike");if(finalStrike>0&&new Random().nextInt(20-finalStrike)==0)amount*=3;
        int valiance=level(attacker,held,"valiance");if(valiance>0&&attacker.getHealth()/attacker.getMaxHealth()<.5f)amount=(float)(amount*(1.0+.1*valiance));
        int focused=level(attacker,held,"focused_strike"),dispersed=level(attacker,held,"dispersed_strike");
        if(focused>0||dispersed>0){
            var data=ItemData.read(held);int last=data.getInt("TTEnchantLastTarget"),successive=data.getInt("TTEnchantSuccessiveStrike"),target=event.getEntity().getId();byte repeated=(byte)(last==target?1:0);
            ItemData.update(held,tag->{tag.putInt("TTEnchantSuccessiveStrike",last==target?successive+1:0);tag.putInt("TTEnchantLastTarget",target);});
            if(focused>0){amount/=2;amount=(float)(amount+.5*repeated*amount*focused);}
            if(dispersed>0){amount*=1+repeated/5;amount/=1+repeated*2;}
        }
        int vampirism=level(attacker,held,"vampirism");if(vampirism>0){attacker.heal(vampirism);var victim=event.getEntity();victim.level().playSound(null,victim.getX(),victim.getY(),victim.getZ(),BuiltInRegistries.SOUND_EVENT.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:zap")),SoundSource.NEUTRAL,.6f,1);}
        event.setAmount(amount);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void tick(PlayerTickEvent.Pre event){
        var player=event.getEntity();
        int slowfall=armor(player,"slow_fall");var motion=player.getDeltaMovement();
        if(slowfall>0&&!player.isShiftKeyDown()&&motion.y<-.0784000015258789&&player.fallDistance>=2.9){
            player.setDeltaMovement(motion.x,motion.y/(1f+slowfall*.33f),motion.z);player.fallDistance=Math.max(2.9f,player.fallDistance-slowfall/3f);
            // Legacy client player Y included 1.62: source Y-1 therefore means feet+.62.
            player.level().addParticle(ParticleTypes.CLOUD,player.getX()+.25,player.getY()+.62,player.getZ()+.25,-motion.x,player.getDeltaMovement().y,-motion.z);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(LivingEntityUseItemEvent.Tick event){
        if(!(event.getEntity() instanceof Player player)||event.getHand()!=InteractionHand.MAIN_HAND||!(event.getItem().getItem() instanceof BowItem))return;
        int quick=level(player,player.getMainHandItem(),"quick_draw");int time=event.getDuration();
        if(quick>0&&(event.getItem().getUseDuration(player)-time)%(6-quick)==0)event.setDuration(time-1);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){
        if(event.getEntity() instanceof Player player){int boost=armor(player,"ascent_boost");if(boost>=1&&!player.isShiftKeyDown())player.setDeltaMovement(player.getDeltaMovement().multiply(1,(boost+2)/2.0,1));}
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void fall(LivingFallEvent event){
        if(!(event.getEntity() instanceof Player player)||player.level().isClientSide())return;
        int shock=level(player,player.getItemBySlot(EquipmentSlot.FEET),"shockwave");
        if(shock>0)for(var target:player.level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(player.getX()-10,player.getY()-10,player.getZ()-10,player.getX()+10,player.getY()+10,player.getZ()+10)))if(target!=player&&event.getDistance()>3)target.hurt(player.damageSources().fall(),.1f*shock*event.getDistance());
    }
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event){
        var held=event.getPlayer().getMainHandItem();if(level(event.getPlayer(),held,"tunnel")>0)ItemData.update(held,data->data.putFloat("TTEnchantTunnelDir",event.getPlayer().getYRot()));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void speed(PlayerEvent.BreakSpeed event){
        var player=event.getEntity();var held=player.getMainHandItem();if(held.isEmpty())return;
        int shatter=level(player,held,"shatter");
        if(shatter>0)event.setNewSpeed(event.getState().getDestroySpeed(player.level(),BlockPos.ZERO)>20?event.getNewSpeed()*(3*shatter):(float)(event.getNewSpeed()*.8));
        int tunnel=level(player,held,"tunnel");var data=ItemData.read(held);
        if(tunnel>0&&data.contains("TTEnchantTunnelDir")){float difference=Math.abs(data.getFloat("TTEnchantTunnelDir")-player.getYRot());event.setNewSpeed((float)(event.getNewSpeed()*(difference<50?1.0+.2*tunnel:.3)));}
        int desintegrate=level(player,held,"desintegrate"),autoSmelt=level(player,held,"auto_smelt");
        if(desintegrate>0||autoSmelt>0){
            boolean applies=desintegrate>0&&event.getState().getDestroySpeed(player.level(),BlockPos.ZERO)<=1.5f||autoSmelt>0&&event.getState().is(thaumcraft.api.world.BlockMaterials.WOOD);
            if(applies){held.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);event.setNewSpeed(Float.MAX_VALUE);}else event.setCanceled(true);
        }
    }
    private TinkererEnchantments(){}
}
