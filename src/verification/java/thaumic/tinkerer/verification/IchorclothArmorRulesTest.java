package thaumic.tinkerer.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class IchorclothArmorRulesTest {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static void check(boolean value,String label) {if(!value)throw new IllegalStateException(label);}
    private static void close(double actual,double expected,String label) {check(Math.abs(actual-expected)<1E-4,label+": "+actual);}
    private static void clear(net.minecraft.server.level.ServerPlayer player) {for(var slot:SLOTS)player.setItemSlot(slot,ItemStack.EMPTY);}
    private static void hurt(net.minecraft.server.level.ServerPlayer player,DamageSource source,float expected) {
        player.setHealth(100);player.invulnerableTime=0;
        check(player.hurt(source,10),"native player accepts the test hit");close(100-player.getHealth(),expected,"native health change after actual armor pipeline");
    }
    @GameTest(template="summon_space",batch="kami_armor",timeoutTicks=100)
    public static void armorStateProtectionAndEquip(GameTestHelper helper) {
        VerificationReports.install();
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            for(var name:List.of("helm","chest","legs","boots"))for(var suffix:List.of("","_gem"))
                check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id("ichorcloth_"+name+suffix)),"disabled KAMI leaves armor IDs unregistered");
            helper.succeed();return;
        }
        try(var nativePlayer=new NativePlayerFixture(helper,"TTIchorArmor",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=nativePlayer.player;var level=helper.getLevel();player.setGameMode(GameType.SURVIVAL);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            var items=List.of(KamiArmorContent.HELM.get(),KamiArmorContent.CHEST.get(),KamiArmorContent.LEGS.get(),KamiArmorContent.BOOTS.get(),KamiArmorContent.GEM_HELM.get(),KamiArmorContent.GEM_CHEST.get(),KamiArmorContent.GEM_LEGS.get(),KamiArmorContent.GEM_BOOTS.get());
            for(var item:items) {
                var stack=item.getDefaultInstance();
                check(!stack.has(DataComponents.MAX_DAMAGE)&&!stack.isDamageableItem()&&item.isEnchantable(stack),"source zero durability armor remains unbreakable and enchantable");
                check(stack.getMaxStackSize()==1&&stack.getRarity()==Rarity.EPIC&&item.getEnchantmentValue()==20,"source stack size, purple rarity and enchantability");
                check(item.getVisDiscount(stack,player,null)==(item.getType()==net.minecraft.world.item.ArmorItem.Type.BOOTS?3:4),"source per-piece vis discount");
                var slot=item.getType().getSlot();int index=slot.getIndex();int points=new int[]{3,8,6,3}[index];
                close(item.getProperties(player,stack,level.damageSources().magic(),10,index).absorbRatio,points*.0425,"source inventory-index coefficient even for bypass damage");
                check(item.getArmorDisplay(player,stack,index)==points,"source displayed points use the same inventory index");
                check(stack.is(switch(slot){case HEAD->ItemTags.HEAD_ARMOR;case CHEST->ItemTags.CHEST_ARMOR;case LEGS->ItemTags.LEG_ARMOR;case FEET->ItemTags.FOOT_ARMOR;default->throw new AssertionError(slot);}),"actual native slot tag supports armor enchantments");
                var texture=item.getArmorTexture(stack,player,slot,KamiArmorContent.MATERIAL.get().layers().getFirst(),slot==EquipmentSlot.LEGS);
                check(texture.getNamespace().equals(ThaumicTinkerer.ID)&&!texture.getPath().matches(".*[A-Z].*"),"ported armor textures use valid lowercase resource locations");
            }
            var equipped=List.of(KamiArmorContent.GEM_BOOTS.get().getDefaultInstance(),KamiArmorContent.GEM_LEGS.get().getDefaultInstance(),KamiArmorContent.GEM_CHEST.get().getDefaultInstance(),KamiArmorContent.GEM_HELM.get().getDefaultInstance());
            for(int i=0;i<4;i++)player.setItemSlot(SLOTS[i],equipped.get(i));
            check(player.getArmorValue()==20,"real display hook reports the original full-set points");
            hurt(player,level.damageSources().cactus(),1.5F);hurt(player,level.damageSources().magic(),1.5F);
            KamiArmorState.save(player,false);for(var stack:equipped)AwakenedIchorclothArmorItem.variant(stack,1);
            hurt(player,level.damageSources().magic(),1.5F);
            check(equipped.stream().allMatch(stack->stack.getCount()==1&&!stack.has(DataComponents.DAMAGE)),"neither disabled mode nor damage breaks the source indestructible armor");
            clear(player);player.setItemSlot(EquipmentSlot.CHEST,KamiArmorContent.CHEST.get().getDefaultInstance());
            check(player.getArmorValue()==6,"source chest display quirk is preserved for a partial set");hurt(player,level.damageSources().cactus(),7.45F);
            clear(player);player.setItemSlot(EquipmentSlot.LEGS,KamiArmorContent.LEGS.get().getDefaultInstance());
            check(player.getArmorValue()==8,"source leggings display quirk is preserved for a partial set");hurt(player,level.damageSources().cactus(),6.6F);
            var diamond=new ItemStack(Items.DIAMOND_CHESTPLATE);player.setItemSlot(EquipmentSlot.CHEST,diamond);
            check(player.getArmorValue()==16,"mixed ordinary and special display sums source contributions");hurt(player,level.damageSources().cactus(),3.4F);
            check(diamond.getDamageValue()==3,"ordinary armor durability uses its actual absorbed share, not modern damage divided by four");
            var fortressId=ResourceLocation.parse("thaumcraft:thaumium_fortress_chestplate");
            check(BuiltInRegistries.ITEM.containsKey(fortressId),"real core fortress item is present by its public registry ID");
            var fortress=new ItemStack(BuiltInRegistries.ITEM.get(fortressId));player.setItemSlot(EquipmentSlot.CHEST,fortress);
            check(player.getArmorValue()==15,"mixed core and KAMI display retains original seven plus eight points");
            hurt(player,level.damageSources().magic(),5.28F);check(fortress.getDamageValue()==2,"priority-one core magic absorption receives its own durability share");
            hurt(player,level.damageSources().inFire(),4.29F);check(fortress.getDamageValue()==5,"priority-one core fire absorption is applied once before KAMI");
            var held=KamiArmorContent.GEM_HELM.get().getDefaultInstance();ItemData.update(held,data->data.putInt("other",17));player.setItemInHand(InteractionHand.MAIN_HAND,held);player.setShiftKeyDown(true);
            KamiArmorContent.GEM_HELM.get().use(level,player,InteractionHand.MAIN_HAND);
            check(AwakenedIchorclothArmorItem.variant(held)==1&&!KamiArmorState.savedEnabled(player),"actual shift use changes only the individual item variant");
            var loaded=ItemStack.parse(level.registryAccess(),held.save(level.registryAccess())).orElseThrow();
            check(AwakenedIchorclothArmorItem.variant(loaded)==1&&ItemData.integer(loaded,"other")==17,"native stack codec retains variant and unrelated custom data");
            KamiArmorContent.GEM_HELM.get().use(level,player,InteractionHand.MAIN_HAND);check(AwakenedIchorclothArmorItem.variant(held)==0,"second shift use restores source zero variant");
            player.setShiftKeyDown(false);var occupied=new ItemStack(Items.IRON_HELMET);player.setItemSlot(EquipmentSlot.HEAD,occupied);
            KamiArmorContent.GEM_HELM.get().use(level,player,InteractionHand.MAIN_HAND);
            check(player.getItemBySlot(EquipmentSlot.HEAD)==occupied&&held.getCount()==1,"non-sneak use does not swap occupied armor, as in old ItemArmor");
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);player.setGameMode(GameType.CREATIVE);
            KamiArmorContent.GEM_HELM.get().use(level,player,InteractionHand.MAIN_HAND);
            check(held.isEmpty()&&player.getItemBySlot(EquipmentSlot.HEAD).is(KamiArmorContent.GEM_HELM.get()),"legacy equip consumes the held stack even in creative mode");
            System.out.println("TT_ICHOR_ARMOR_NATIVE_PASS items=8 native_damage=true mixed_durability=true slot_quirk=true variant_nbt=true source_equip=true");helper.succeed();
        }
    }
    private IchorclothArmorRulesTest() {}
}
