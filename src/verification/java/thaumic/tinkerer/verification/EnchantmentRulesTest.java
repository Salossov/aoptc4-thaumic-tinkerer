package thaumic.tinkerer.verification;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.BlockMaterials;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.enchantment.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void close(double actual,double expected,String label){check(Math.abs(actual-expected)<1e-5,label+" actual="+actual+" expected="+expected);}
    private static Holder.Reference<Enchantment> enchant(ServerLevel level,String id){return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse(id.contains(":")?id:"thaumictinkerer:"+id)));}
    private static ItemStack stack(ServerLevel level,Item item,String id,int amount){var result=new ItemStack(item);result.enchant(enchant(level,id),amount);return result;}
    private static float damage(Cow victim,ServerPlayer player,float amount){victim.setHealth(200);victim.invulnerableTime=0;check(victim.hurt(victim.damageSources().playerAttack(player),amount),"actual living attack accepted");return 200-victim.getHealth();}
    private static Cow cow(ServerLevel level,Vec3 pos){var result=new Cow(EntityType.COW,level);result.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);result.setHealth(200);result.setNoAi(true);result.setPos(pos);level.addFreshEntity(result);return result;}
    private static void bare(ServerPlayer player){for(var slot:EquipmentSlot.values())player.setItemSlot(slot,ItemStack.EMPTY);player.setShiftKeyDown(false);player.getAbilities().instabuild=false;player.getAbilities().mayfly=false;player.getAbilities().flying=false;}
    @GameTest(template="empty",timeoutTicks=100)
    public static void enchantments(GameTestHelper helper)throws Exception{
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+270,(anchor.getZ()>>4)*16+8);
        var transport=FocusRulesTest.player(helper,"[TTEnchantments]");var player=new ServerPlayer(level.getServer(),level,transport.getGameProfile(),ClientInformation.createDefault()){@Override public BlockPos adjustSpawnLocation(ServerLevel world,BlockPos spawn){return spawn;}};player.connection=transport.connection;player.setPos(Vec3.atCenterOf(pos));bare(player);
        var entities=new ArrayList<Entity>();var victim=cow(level,pos.getCenter().add(2,0,0));entities.add(victim);var anvilPos=pos.below(2);
        try{
            var levels=new LinkedHashMap<String,Integer>();levels.put("ascent_boost",4);levels.put("slow_fall",3);levels.put("auto_smelt",1);levels.put("desintegrate",1);levels.put("quick_draw",2);levels.put("vampirism",2);for(var id:List.of("dispersed_strike","final_strike","focused_strike","pounce","shatter","shockwave","tunnel","valiance"))levels.put(id,5);
            for(var entry:levels.entrySet()){
                var holder=enchant(level,entry.getKey());var value=holder.value();
                check(value.getMaxLevel()==entry.getValue()&&value.getMinCost(1)==11&&value.getMaxCost(2)==26&&value.getAnvilCost()==0,"original levels, bytecode enchantability and zero-weight anvil cost for "+entry.getKey());
                check(!holder.is(EnchantmentTags.IN_ENCHANTING_TABLE)&&!holder.is(EnchantmentTags.TRADEABLE)&&!holder.is(EnchantmentTags.ON_RANDOM_LOOT)&&!holder.is(EnchantmentTags.ON_TRADED_EQUIPMENT)&&!holder.is(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT),"original non-random availability for "+entry.getKey());
            }
            check(enchant(level,"ascent_boost").value().canEnchant(new ItemStack(Items.IRON_LEGGINGS))&&!enchant(level,"ascent_boost").value().canEnchant(new ItemStack(Items.IRON_BOOTS)),"source leggings class predicate");
            check(enchant(level,"slow_fall").value().canEnchant(new ItemStack(Items.IRON_BOOTS))&&!enchant(level,"slow_fall").value().canEnchant(new ItemStack(Items.IRON_LEGGINGS)),"source feet type checked separately from broad slow-fall canApply override");
            check(enchant(level,"quick_draw").value().canEnchant(new ItemStack(ClothTestMod.ENCHANTMENT_BOW))&&!enchant(level,"quick_draw").value().canEnchant(new ItemStack(Items.CROSSBOW)),"registered addon bow accepted by class without a vanilla item tag");
            check(enchant(level,"vampirism").value().canEnchant(new ItemStack(ClothTestMod.ENCHANTMENT_SWORD))&&!enchant(level,"vampirism").value().canEnchant(new ItemStack(Items.IRON_AXE)),"legacy weapon is a sword, including registered addon swords");
            var digger=enchant(level,"shatter").value();check(digger.canEnchant(new ItemStack(ClothTestMod.ENCHANTMENT_PICKAXE))&&digger.canEnchant(new ItemStack(Items.IRON_SHOVEL))&&digger.canEnchant(new ItemStack(Items.IRON_AXE))&&!digger.canEnchant(new ItemStack(Items.IRON_HOE)),"legacy ItemTool compatibility does not accidentally include modern hoes");
            check(!enchant(level,"auto_smelt").value().canEnchant(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:elemental_axe")))),"original Auto Smelt exception rejects the actual native elemental axe");
            var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var encoded=Enchantment.DIRECT_CODEC.encodeStart(ops,digger).getOrThrow();var decoded=Enchantment.DIRECT_CODEC.parse(ops,encoded).getOrThrow();
            check(decoded.canEnchant(new ItemStack(ClothTestMod.ENCHANTMENT_PICKAXE))&&!decoded.canEnchant(new ItemStack(Items.IRON_HOE)),"custom item predicate survives the actual native data codec");
            var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
            try{var codec=net.minecraft.network.codec.ByteBufCodecs.holderSet(Registries.ITEM);codec.encode(wire,digger.definition().supportedItems());var received=codec.decode(wire);check(received.contains(ClothTestMod.ENCHANTMENT_PICKAXE.builtInRegistryHolder())&&!received.contains(Items.IRON_HOE.builtInRegistryHolder()),"custom holder set survives its actual registry-aware network codec");}finally{wire.release();}
            check(!Enchantment.areCompatible(enchant(level,"slow_fall"),enchant(level,"minecraft:feather_falling"))&&!Enchantment.areCompatible(enchant(level,"quick_draw"),enchant(level,"minecraft:punch")),"source slow-fall and quick-draw incompatibilities");
            check(!Enchantment.areCompatible(enchant(level,"shockwave"),enchant(level,"slow_fall"))&&!Enchantment.areCompatible(enchant(level,"tunnel"),enchant(level,"shatter"))&&!Enchantment.areCompatible(enchant(level,"shatter"),enchant(level,"minecraft:efficiency")),"source symmetric combined compatibility rules");
            check(Enchantment.areCompatible(enchant(level,"auto_smelt"),enchant(level,"minecraft:unbreaking"))&&Enchantment.areCompatible(enchant(level,"auto_smelt"),enchant(level,"thaumcraft:repair"))&&!Enchantment.areCompatible(enchant(level,"auto_smelt"),enchant(level,"minecraft:mending")),"whitelist complement includes enchantments introduced after the pinned release");
            check(Enchantment.areCompatible(enchant(level,"valiance"),enchant(level,"minecraft:smite"))&&!Enchantment.areCompatible(enchant(level,"valiance"),enchant(level,"minecraft:bane_of_arthropods")),"source repeated-smite typo does not silently allow bane of arthropods");
            check(!Enchantment.areCompatible(enchant(level,"focused_strike"),enchant(level,"dispersed_strike"))&&Enchantment.areCompatible(enchant(level,"ascent_boost"),enchant(level,"pounce")),"original weapon exclusions and compatible leg enchantments");
            var boots=stack(level,Items.LEATHER_BOOTS,"slow_fall",3);player.setItemSlot(EquipmentSlot.FEET,boots);player.setDeltaMovement(.2,-.6,-.3);player.fallDistance=4;
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));close(player.getDeltaMovement().y,-.6/(1f+3*.33f),"actual highest-priority tick bus applies the original slow-fall divisor");close(player.fallDistance,3,"slow-fall distance reduction");
            player.setDeltaMovement(0,-.6,0);player.fallDistance=3;player.setShiftKeyDown(true);NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));close(player.getDeltaMovement().y,-.6,"sneaking disables slow fall");player.setShiftKeyDown(false);
            player.setDeltaMovement(0,-.0784000015258789,0);player.fallDistance=4;NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));close(player.getDeltaMovement().y,-.0784000015258789,"strict original downward-speed threshold");
            player.setDeltaMovement(0,-.6,0);player.fallDistance=2.8f;NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));close(player.getDeltaMovement().y,-.6,"fall distance below 2.9 does not trigger");
            player.setPos(pos.getCenter().add(0,15,0));player.setOnGround(false);player.setDeltaMovement(0,-.6,0);player.fallDistance=4;player.doTick();close(player.getDeltaMovement().y,(-.6/(1f+3*.33f)-.08)*.98,"real ServerPlayer tick invokes slow fall before native gravity and drag");
            bare(player);player.setItemSlot(EquipmentSlot.LEGS,stack(level,Items.LEATHER_LEGGINGS,"ascent_boost",4));player.setDeltaMovement(0,0,0);player.jumpFromGround();close(player.getDeltaMovement().y,(double).42f*3,"actual native jump event multiplies the source velocity");
            player.setShiftKeyDown(true);player.setDeltaMovement(0,0,0);player.jumpFromGround();close(player.getDeltaMovement().y,.42f,"sneaking preserves ordinary native jump");bare(player);
            player.setItemInHand(InteractionHand.MAIN_HAND,stack(level,Items.BOW,"quick_draw",2));player.startUsingItem(InteractionHand.MAIN_HAND);for(int i=0;i<10;i++)player.doTick();check(player.getTicksUsingItem()==14,"real bow-use ticks preserve original extra-decrement timing at level two");player.stopUsingItem();
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,stack(level,Items.BOW,"quick_draw",2));player.startUsingItem(InteractionHand.OFF_HAND);for(int i=0;i<10;i++)player.doTick();check(player.getTicksUsingItem()==10,"modern off-hand use does not invent an original main-hand quick-draw path");player.stopUsingItem();bare(player);player.setPos(Vec3.atCenterOf(pos));
            var sword=stack(level,Items.IRON_SWORD,"focused_strike",3);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
            close(damage(victim,player,12),6,"first focused strike halves damage");close(damage(victim,player,12),15,"repeated focused strike uses source one-bit flag");close(damage(victim,player,12),15,"third focused strike does not use the accumulated count as a multiplier");check(ItemData.integer(sword,"TTEnchantSuccessiveStrike")==2&&ItemData.integer(sword,"TTEnchantLastTarget")==victim.getId(),"actual legacy successive/target keys live in item components");
            var saved=sword.save(level.registryAccess());var restored=ItemStack.parseOptional(level.registryAccess(),(CompoundTag)saved);check(ItemData.integer(restored,"TTEnchantSuccessiveStrike")==2&&TinkererEnchantments.level(player,restored,"focused_strike")==3,"native item codec persists both enchantment and its legacy target state");
            sword=stack(level,Items.IRON_SWORD,"dispersed_strike",5);player.setItemInHand(InteractionHand.MAIN_HAND,sword);close(damage(victim,player,12),12,"first dispersed strike has ordinary damage");close(damage(victim,player,12),4,"repeated dispersed strike retains integer division var16/5");close(damage(victim,player,12),4,"accumulated dispersed counter does not turn into a larger damage multiplier");
            player.setItemInHand(InteractionHand.MAIN_HAND,stack(level,Items.IRON_SWORD,"valiance",5));player.setHealth(player.getMaxHealth()/2);close(damage(victim,player,10),10,"exact half health does not trigger valiance");player.setHealth(player.getMaxHealth()/2-.1f);close(damage(victim,player,10),15,"strictly below half health applies the source valiance multiplier");
            player.setItemInHand(InteractionHand.MAIN_HAND,stack(level,Items.IRON_SWORD,"final_strike",19));close(damage(victim,player,12),36,"native event exercises guaranteed source roll nextInt(20-19), without altering the normal max level five");
            player.setItemInHand(InteractionHand.MAIN_HAND,stack(level,Items.IRON_SWORD,"vampirism",2));player.setHealth(2);close(damage(victim,player,12),12,"vampirism leaves attack amount unchanged");close(player.getHealth(),4,"actual hurt event heals exactly the enchantment level");
            bare(player);player.setItemSlot(EquipmentSlot.LEGS,stack(level,Items.LEATHER_LEGGINGS,"pounce",4));player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);close(damage(victim,player,12),24,"pounce tests exact air block beneath the original feet position");level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);close(damage(victim,player,12),12,"solid block below prevents pounce");player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);close(damage(victim,player,12),12,"source empty-held-item early return also skips pounce");bare(player);
            player.setItemSlot(EquipmentSlot.FEET,stack(level,Items.LEATHER_BOOTS,"shockwave",5));var corner=cow(level,pos.getCenter().add(6.5,6.5,6.5));var outside=cow(level,pos.getCenter().add(0,11,0));entities.add(corner);entities.add(outside);victim.setHealth(200);victim.invulnerableTime=0;player.setHealth(player.getMaxHealth());player.invulnerableTime=0;player.causeFallDamage(6,1,player.damageSources().fall());close(200-victim.getHealth(),3,"actual native fall dispatches original shockwave damage");close(200-corner.getHealth(),3,"shockwave retains the ten-block axis-aligned cube instead of a sphere");close(outside.getHealth(),200,"outside shockwave cube is unchanged");victim.setHealth(200);player.causeFallDamage(3,1,player.damageSources().fall());close(victim.getHealth(),200,"distance exactly three does not trigger shockwave");bare(player);
            check(Blocks.NOTE_BLOCK.defaultBlockState().is(BlockMaterials.WOOD)&&Blocks.MUSHROOM_STEM.defaultBlockState().is(BlockMaterials.WOOD)&&!Blocks.LADDER.defaultBlockState().is(BlockMaterials.WOOD)&&!Blocks.OAK_BUTTON.defaultBlockState().is(BlockMaterials.WOOD)&&!Blocks.PUMPKIN.defaultBlockState().is(BlockMaterials.WOOD),"runtime tag follows actual original materials rather than wooden appearance or axe mining");
            check(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:arcane_bore")).defaultBlockState().is(BlockMaterials.WOOD)&&thaumic.tinkerer.block.PlatformContent.BLOCK.get().defaultBlockState().is(BlockMaterials.WOOD),"native core and addon contribute to the same real material tag");
            check(Blocks.GLASS.defaultBlockState().is(BlockMaterials.WOOD)&&!Blocks.CHERRY_PLANKS.defaultBlockState().is(BlockMaterials.WOOD),"isolated verification datapack extends the shared tag while default pinned mappings do not broaden newer families");
            player.setOnGround(true);var pick=stack(level,Items.DIAMOND_PICKAXE,"auto_smelt",1);player.setItemInHand(InteractionHand.MAIN_HAND,pick);int wear=pick.getDamageValue();check(player.getDigSpeed(Blocks.OAK_PLANKS.defaultBlockState(),pos)==Float.MAX_VALUE&&pick.getDamageValue()==wear+1,"native break-speed query applies Auto Smelt and one actual durability use");
            wear=pick.getDamageValue();check(player.getDigSpeed(Blocks.PUMPKIN.defaultBlockState(),pos)==-1&&pick.getDamageValue()==wear,"nonwood axe-minable block is canceled without wear");check(player.getDigSpeed(Blocks.GLASS.defaultBlockState(),pos)==Float.MAX_VALUE,"actual datapack wood contribution controls Auto Smelt");
            pick=stack(level,Items.DIAMOND_PICKAXE,"desintegrate",1);player.setItemInHand(InteractionHand.MAIN_HAND,pick);wear=pick.getDamageValue();check(player.getDigSpeed(Blocks.STONE.defaultBlockState(),pos)==Float.MAX_VALUE&&pick.getDamageValue()==wear+1,"native Desintegrate query includes the exact hardness 1.5 boundary");wear=pick.getDamageValue();check(player.getDigSpeed(Blocks.OBSIDIAN.defaultBlockState(),pos)==-1&&pick.getDamageValue()==wear,"Desintegrate rejects hardness above 1.5 without wear");check(player.getDigSpeed(Blocks.BEDROCK.defaultBlockState(),pos)==Float.MAX_VALUE,"source negative-hardness comparison is retained without claiming bedrock becomes breakable");
            pick=stack(level,Items.DIAMOND_PICKAXE,"shatter",3);player.setItemInHand(InteractionHand.MAIN_HAND,pick);close(player.getDigSpeed(Blocks.OBSIDIAN.defaultBlockState(),pos),8*9,"shatter triples speed per level above hardness twenty");close(player.getDigSpeed(Blocks.STONE.defaultBlockState(),pos),8*.8,"shatter multiplies softer blocks by source double 0.8");
            pick=stack(level,Items.DIAMOND_PICKAXE,"tunnel",5);player.setItemInHand(InteractionHand.MAIN_HAND,pick);player.setYRot(350);level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);player.gameMode.destroyBlock(pos);check(ItemData.read(pick).getFloat("TTEnchantTunnelDir")==350,"actual server block break records the original raw yaw");player.setYRot(390);close(player.getDigSpeed(Blocks.STONE.defaultBlockState(),pos),16,"tunnel boosts raw yaw difference below fifty");player.setYRot(400);close(player.getDigSpeed(Blocks.STONE.defaultBlockState(),pos),2.4,"exact fifty degrees uses the source penalty");player.setYRot(-10);close(player.getDigSpeed(Blocks.STONE.defaultBlockState(),pos),2.4,"source yaw difference is intentionally not normalized around 360");
            level.setBlock(anvilPos,Blocks.ANVIL.defaultBlockState(),2);var anvil=new AnvilMenu(9,player.getInventory(),ContainerLevelAccess.create(level,anvilPos));var book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(enchant(level,"ascent_boost"),2);anvil.getSlot(0).set(new ItemStack(Items.IRON_LEGGINGS));anvil.getSlot(1).set(book);anvil.createResult();check(!anvil.getSlot(2).getItem().isEmpty()&&TinkererEnchantments.level(player,anvil.getSlot(2).getItem(),"ascent_boost")==2,"actual native anvil accepts the registered custom enchantment and its supported-item holder set");
            book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(enchant(level,"quick_draw"),1);var bow=new ItemStack(Items.BOW);bow.enchant(enchant(level,"minecraft:punch"),1);anvil.getSlot(0).set(bow);anvil.getSlot(1).set(book);anvil.createResult();check(anvil.getSlot(2).getItem().isEmpty(),"actual native anvil enforces the original Quick Draw/Punch conflict");
            System.out.println("TT_ENCHANTMENTS_PASS checks="+checks);helper.succeed();
        }finally{
            player.stopUsingItem();bare(player);player.getInventory().clearContent();for(var entity:entities)entity.discard();for(var p:List.of(pos,pos.below(),anvilPos))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4)).forEach(Entity::discard);
        }
    }
}
