package thaumic.tinkerer.summon;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumcraft.api.IRepairable;
import thaumcraft.api.entities.LivingAttackAttemptEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.util.ItemData;

@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class BloodSwordItem extends SwordItem implements IRepairable {
    private static int handleNext;
    private static final Tier TIER = new Tier() {
        public int getUses() { return 950; } public float getSpeed() { return 0; } public float getAttackDamageBonus() { return 0; }
        public int getEnchantmentValue() { return 22; } public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_WOODEN_TOOL; }
    };
    public BloodSwordItem() { super(TIER, new Properties().attributes(ItemAttributeModifiers.builder()
            .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,10,AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
            .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,.25,AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND).build())); }
    public static boolean active(ItemStack stack) { return ItemData.integer(stack,"Activated")==1; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);player.startUsingItem(hand);
        if(player.isShiftKeyDown()&&!level.isClientSide()) {
            boolean activate=ItemData.integer(stack,"Activated")==0;ItemData.update(stack,tag->tag.putInt("Activated",activate?1:0));
            player.displayClientMessage(Component.translatable("message.thaumictinkerer.blood_sword."+(activate?"activate":"deactivate")),false);
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) { return ability!=net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP&&super.canPerformAction(stack,ability); }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if(!event.getSource().getMsgId().equals("player")||!(event.getSource().getEntity() instanceof Player player))return;
        var stack=player.getMainHandItem();if(!(stack.getItem() instanceof BloodSwordItem)||!active(stack))return;
        var aspects=MobAspects.forEntity(event.getEntity());if(aspects.isEmpty())return;
        event.getDrops().clear();
        for(var aspect:aspects) {var entity=new ItemEntity(event.getEntity().level(),event.getEntity().getX(),event.getEntity().getY(),event.getEntity().getZ(),MobAspectItem.of(aspect));entity.setPickUpDelay(10);event.getDrops().add(entity);}
    }
    public static void attack(LivingAttackAttemptEvent event) {
        if(event.target().level().isClientSide())return;
        boolean handle=handleNext==0;if(!handle)handleNext--;
        if(event.target() instanceof Player player&&handle&&player.isUsingItem()&&player.getUseItem().getItem() instanceof BloodSwordItem) {
            event.setCanceled(true);handleNext=3;player.hurt(player.damageSources().magic(),3);
        }
        if(handle&&event.source().getDirectEntity() instanceof LivingEntity attacker&&attacker.getMainHandItem().getItem() instanceof BloodSwordItem)attacker.hurt(attacker.damageSources().magic(),2);
    }
}
