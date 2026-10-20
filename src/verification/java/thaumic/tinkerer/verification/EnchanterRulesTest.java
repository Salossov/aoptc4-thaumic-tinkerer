package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchSharingApi;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.enchantment.EnchanterCatalog;
import thaumic.tinkerer.menu.EnchanterMenu;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class EnchanterRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static ResourceLocation id(String name){return ResourceLocation.parse(name.contains(":")?name:"thaumictinkerer:"+name);}
    private static ItemStack wand(String kind,String cap){var stack=new ItemStack(BuiltInRegistries.ITEM.get(id("thaumcraft:casting_"+kind)));WandApi.parts(stack,new WandParts(kind.equals("staff")?"silverwood_staff":"silverwood",cap));return stack;}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(ServerLevel level,BlockPos pos,int count){var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);BlockEntityTicker ticker=((EntityBlock)state.getBlock()).getTicker(level,state,tile.getType());for(int i=0;i<count;i++)ticker.tick(level,pos,state,tile);}
    private static void pillar(ServerLevel level,BlockPos pos,int height){for(int y=0;y<15;y++)level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),2);for(int y=0;y<height;y++)level.setBlock(pos.above(y),BuiltInRegistries.BLOCK.get(id("thaumcraft:obsidian_totem")).defaultBlockState(),2);level.setBlock(pos.above(height),BuiltInRegistries.BLOCK.get(id("thaumcraft:nitor")).defaultBlockState(),2);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void enchanter(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+240,(anchor.getZ()>>4)*16+8);var fake=FocusRulesTest.player(helper,"[TTEnchanter]");
        var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"[TTEnchanter]"),ClientInformation.createDefault()){@Override public BlockPos adjustSpawnLocation(ServerLevel world,BlockPos spawn){return spawn;}};player.connection=fake.connection;player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var changed=new LinkedHashSet<BlockPos>();for(var p:BlockPos.betweenClosed(pos.offset(-4,0,-4),pos.offset(4,15,4)))changed.add(p.immutable());
        Runnable cleanup=()->{player.closeContainer();player.getInventory().clearContent();for(var p:changed){if(level.getBlockEntity(p)instanceof Container container)container.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(10)).forEach(Entity::discard);};
        try{
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            var state=EnchanterContent.BLOCK.get().defaultBlockState();level.setBlock(pos,state,3);var tile=(EnchanterBlockEntity)level.getBlockEntity(pos);
            check(tile!=null&&tile.getContainerSize()==2&&tile.getMaxStackSize()==1&&tile.isEmpty(),"native singleton tool/wand inventory");
            check(state.getDestroySpeed(level,pos)==5&&state.getBlock().getExplosionResistance()==1200&&!state.canOcclude()&&state.getCollisionShape(level,pos).bounds().maxY==.75,"source hardness, effective resistance, nonopaque 3/4 collision");
            for(var side:Direction.values()){var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side);check(handler!=null&&handler.getSlots()==0&&tile.getSlotsForFace(side).length==0,"no sided automation "+side);}
            check(!tile.canPlaceItem(0,new ItemStack(Items.IRON_SWORD))&&!tile.canPlaceItemThroughFace(1,wand("wand","gold"),Direction.UP)&&!tile.canTakeItemThroughFace(0,new ItemStack(Items.IRON_SWORD),Direction.DOWN),"original direct and sided predicates deny automation");
            check(EnchanterCatalog.ENTRIES.size()==40,"forty effective costs after duplicate dispersed overwrite");
            check(EnchanterCatalog.find(id("thaumcraft:haste")).legacyId()==150&&EnchanterCatalog.find(id("thaumcraft:repair")).legacyId()==151&&EnchanterCatalog.find(id("focused_strike")).legacyId()==169,"source default numeric identities used only to preserve manager ordering");
            check(EnchanterCatalog.find(id("thaumcraft:repair")).canApply(new ItemStack(ThaumicTinkerer.REVEALING_HELM.get()),level.registryAccess(),List.of())&&!EnchanterCatalog.find(id("thaumcraft:repair")).canApply(new ItemStack(Items.IRON_HELMET),level.registryAccess(),List.of()),"original repair marker accepts actual addon helmet independently of native static item tags");
            check(EnchanterCatalog.find(id("minecraft:thorns")).canApply(new ItemStack(Items.IRON_CHESTPLATE),level.registryAccess(),List.of())&&!EnchanterCatalog.find(id("minecraft:thorns")).canApply(new ItemStack(Items.IRON_BOOTS),level.registryAccess(),List.of()),"original thorns torso type narrows broad canApply override");
            check(EnchanterCatalog.find(id("focused_strike")).research().equals("thaumictinkerer:ttench_vampirism")&&EnchanterCatalog.find(id("pounce")).research().equals("thaumictinkerer:ttench_vampirism"),"original advanced enchant costs share vampirism gate despite their distinct research pages");
            check(EnchanterCatalog.find(id("minecraft:protection")).cost(1).getAmount(Aspects.EARTH)==12&&EnchanterCatalog.find(id("minecraft:protection")).cost(1).getAmount(Aspects.ENTROPY)==8,"source per-aspect truncation at level one");
            check(EnchanterCatalog.find(id("ascent_boost")).cost(4).getAmount(Aspects.AIR)==72&&EnchanterCatalog.find(id("ascent_boost")).cost(4).getAmount(Aspects.ENTROPY)==57,"source exponential level-four truncation");
            check(EnchanterCatalog.tool(new ItemStack(Items.IRON_HOE))&&EnchanterCatalog.tool(new ItemStack(Items.IRON_HELMET))&&EnchanterCatalog.tool(new ItemStack(Items.FISHING_ROD))&&!EnchanterCatalog.tool(new ItemStack(Items.BOOK))&&!EnchanterCatalog.tool(new ItemStack(Items.STONE)),"original tool slot includes durable armor/hoes/rods but excludes books and blocks");
            var gold=wand("wand","gold");var thaumium=wand("wand","thaumium");var voidWand=wand("wand","void");var sceptre=wand("sceptre","gold");var iron=wand("wand","iron");var copper=wand("wand","copper");var staff=wand("staff","gold");
            check(EnchanterBlockEntity.acceptsWand(gold)&&EnchanterBlockEntity.acceptsWand(thaumium)&&EnchanterBlockEntity.acceptsWand(voidWand)&&EnchanterBlockEntity.acceptsWand(sceptre),"source accepts efficient caps and sceptres");
            check(!EnchanterBlockEntity.acceptsWand(iron)&&!EnchanterBlockEntity.acceptsWand(copper)&&!EnchanterBlockEntity.acceptsWand(staff)&&!EnchanterBlockEntity.acceptsWand(ItemStack.EMPTY),"source rejects base modifier above one even special discounted aspects, and all staffs");
            check(WandApi.quoteCrafting(thaumium,null,Map.of(Aspects.ORDER,1)).get(Aspects.ORDER)==90&&WandApi.quoteCrafting(sceptre,null,Map.of(Aspects.ORDER,1)).get(Aspects.ORDER)==90,"native cap/sceptre crafting quote accepts absent player");
            WandApi.set(thaumium,Aspects.ORDER,100);check(WandApi.consumeCrafting(level,thaumium,null,Map.of(Aspects.ORDER,1),false)&&WandApi.stored(thaumium,Aspects.ORDER)==100,"crafting simulation does not debit");
            check(WandApi.consumeCrafting(level,thaumium,null,Map.of(Aspects.ORDER,1),true)&&WandApi.stored(thaumium,Aspects.ORDER)==10&&!WandApi.consumeCrafting(level,thaumium,null,Map.of(Aspects.ORDER,1),true)&&WandApi.stored(thaumium,Aspects.ORDER)==10,"native crafting debit once, insufficient attempt atomic");
            var menu=new EnchanterMenu(90,player.getInventory(),tile);
            check(menu.slots.size()==38&&menu.getSlot(0).x==6&&menu.getSlot(0).y==6&&menu.getSlot(1).y==31&&menu.getSlot(2).x==8&&menu.getSlot(29).y==142,"original tool, wand and player menu coordinates");
            check(menu.getSlot(0).mayPlace(new ItemStack(Items.IRON_SWORD))&&!menu.getSlot(0).mayPlace(new ItemStack(Items.BOOK))&&menu.getSlot(1).mayPlace(gold)&&!menu.getSlot(1).mayPlace(iron),"real native menu slot restrictions");
            player.getInventory().setItem(9,new ItemStack(Items.IRON_SWORD));check(menu.quickMoveStack(player,2).is(Items.IRON_SWORD)&&tile.getItem(0).is(Items.IRON_SWORD),"native shift-click moves durable tool into slot zero");
            player.getInventory().setItem(10,gold);check(!menu.quickMoveStack(player,3).isEmpty()&&WandApi.kind(tile.getItem(1))==WandApi.Kind.WAND,"native shift-click moves efficient wand into slot one");gold=tile.getItem(1);
            player.getInventory().setItem(11,new ItemStack(Items.BOOK));check(menu.quickMoveStack(player,4).isEmpty()&&player.getInventory().getItem(11).is(Items.BOOK),"native shift-click book rejected");
            check(!tile.select(player,id("ascent_boost"),0)&&!tile.select(player,id("vampirism"),0),"source item class and research gates enforced on server");
            ResearchSharingApi.complete(player,"thaumictinkerer:ttench_vampirism");check(tile.select(player,id("vampirism"),0)&&tile.levels().equals(List.of(1)),"unlocked custom enchant begins at one");
            check(!tile.select(player,id("minecraft:knockback"),0)&&!tile.select(player,id("minecraft:fire_aspect"),0),"both-direction compatibility with vampirism");
            tile.setItem(0,new ItemStack(Items.IRON_SWORD));check(tile.enchantments().isEmpty()&&tile.totalAspects.size()>0,"idle inventory markDirty clears selections but preserves original stale costs");
            check(tile.select(player,id("minecraft:sharpness"),999)&&tile.levels().getFirst()==1,"new selection ignores requested level, starts one");
            check(tile.select(player,id("minecraft:sharpness"),999)&&tile.levels().getFirst()==5&&tile.totalAspects.getAmount(Aspects.ORDER)==100,"existing level clamps to source maximum, recomputes cost");
            check(!tile.select(player,id("minecraft:smite"),0)&&tile.select(player,id("minecraft:sharpness"),-2),"sharpness excludes smite; existing negative level clamps to one");
            check(tile.levels().getFirst()==1&&tile.select(player,id("minecraft:unbreaking"),0)&&tile.enchantments().size()==2,"compatible additional enchantment selection");
            check(tile.select(player,id("minecraft:unbreaking"),-1)&&tile.enchantments().size()==1&&!tile.select(player,id("minecraft:unbreaking"),-1),"remove selected enchant and reject absent removal");
            check(tile.start()&&tile.working&&!menu.getSlot(0).mayPickup(player)&&!menu.getSlot(0).mayPlace(new ItemStack(Items.IRON_SWORD))&&menu.getSlot(1).mayPickup(player),"working locks tool while wand remains removable");
            check(!tile.select(player,id("minecraft:unbreaking"),0)&&!tile.start(),"working rejects selection changes and duplicate starts");tick(level,pos,1);
            check(!tile.working&&tile.currentAspects.size()==0&&tile.enchantments().size()==1,"missing pillars stop work and reset progress while preserving selection");
            var bases=List.of(pos.offset(-4,0,-4),pos.offset(-4,0,-2),pos.offset(-4,0,0),pos.offset(-4,0,2),pos.offset(-4,0,4),pos.offset(-2,0,-4));
            for(var base:bases)pillar(level,base,2);
            check(tile.checkPillars()&&tile.pillars().size()==6&&tile.pillars().getFirst().equals(bases.getFirst().above(2)),"six native totem/nitor pillars in original x-outer z-inner search order");
            pillar(level,bases.getFirst(),1);check(tile.findPillar(bases.getFirst().getX(),pos.getY(),bases.getFirst().getZ())==-1,"one totem rejected");
            pillar(level,bases.getFirst(),12);check(tile.findPillar(bases.getFirst().getX(),pos.getY(),bases.getFirst().getZ())==pos.getY()+12&&tile.checkPillars()&&tile.pillars().getFirst().getY()==pos.getY()+12,"twelve totems accepted and cached cap height refreshed");
            pillar(level,bases.getFirst(),13);check(!tile.checkPillars()&&tile.pillars().isEmpty(),"thirteen totems rejected, fewer than six columns clears cache");pillar(level,bases.getFirst(),2);
            var high=pos.offset(3,254-pos.getY(),3);for(int y=0;y<3;y++)changed.add(high.above(y));pillar(level,high,2);check(tile.findPillar(high.getX(),254,high.getZ())==-1,"literal original y<256 scan does not include cap at 256");
            tile.setItem(1,voidWand);WandApi.set(voidWand,Aspects.ORDER,99);check(tile.enchantments().isEmpty()&&tile.select(player,id("minecraft:sharpness"),0),"idle wand inventory change clears selection just like tool change");check(tile.start(),"restart after reselecting enchant");tick(level,pos,1);
            check(tile.currentAspects.getAmount(Aspects.ORDER)==0&&WandApi.stored(voidWand,Aspects.ORDER)==99,"eligible aspect requires 100 raw centivis despite discount to 80");
            WandApi.set(voidWand,Aspects.ORDER,100);tick(level,pos,1);check(tile.currentAspects.getAmount(Aspects.ORDER)==1&&WandApi.stored(voidWand,Aspects.ORDER)==20,"one whole progress vis per tick for discounted 80-centivis debit");
            var saved=tile.saveWithFullMetadata(level.registryAccess());var loaded=(EnchanterBlockEntity)BlockEntity.loadStatic(pos,state,saved,level.registryAccess());
            check(loaded.working&&loaded.enchantments().equals(tile.enchantments())&&loaded.levels().equals(tile.levels())&&loaded.currentAspects.getAmount(Aspects.ORDER)==1&&loaded.totalAspects.getAmount(Aspects.ORDER)==12&&WandApi.stored(loaded.getItem(1),Aspects.ORDER)==20&&loaded.pillars().isEmpty(),"actual native NBT roundtrip preserves work, namespaced selections, vis and progress, rebuilding transient pillars");
            tile.working=false;tile.setItem(0,new ItemStack(Items.IRON_SWORD));tile.setItem(1,iron);tile.select(player,id("minecraft:sharpness"),0);WandApi.set(iron,Aspects.ORDER,100);tile.currentAspects=new AspectList();tile.start();tick(level,pos,1);
            check(tile.currentAspects.getAmount(Aspects.ORDER)==1&&WandApi.stored(iron,Aspects.ORDER)==100,"source ignores failed debit result for externally injected inefficient wand");
            tile.working=false;tile.setItem(0,new ItemStack(Items.FISHING_ROD));tile.select(player,id("minecraft:lure"),0);tile.currentAspects=new AspectList().add(Aspects.WATER,24);check(tile.totalAspects.getAmount(Aspects.BEAST)==24,"raw lure cost retains compound bestia");tile.start();tick(level,pos,1);
            check(!tile.working&&tile.getItem(0).getEnchantmentLevel(EnchanterCatalog.find(id("minecraft:lure")).holder(level.registryAccess()))==1&&tile.currentAspects.size()==0&&tile.totalAspects.size()==0,"completion checks only primals and does not consume compound bestia");
            var recipe=(InfusionRecipe)level.getRecipeManager().byKey(id("enchanter")).orElseThrow().value();check(recipe.instability()==15&&recipe.aspects().equals(Map.of("praecantatio",50,"potentia",20,"alienis",20,"vacuos",20,"cognitio",10))&&recipe.input().test(new ItemStack(Blocks.ENCHANTING_TABLE))&&recipe.components().size()==8,"original enchanter infusion costs and center");
            var tileItem=new ItemStack(BuiltInRegistries.ITEM.get(id("thaumcraft:obsidian_tile")));var totemItem=new ItemStack(BuiltInRegistries.ITEM.get(id("thaumcraft:obsidian_totem")));
            check(recipe.components().subList(0,5).stream().allMatch(i->i.test(tileItem)&&!i.test(totemItem))&&recipe.components().get(7).test(new ItemStack(ThaumicTinkerer.SPELL_CLOTH.get())),"five cosmetic-solid metadata-one tiles, not pillar totems, and spell cloth");
            var research=AddonRegistry.research().get("thaumictinkerer:enchanter");check(research.complexity()==5&&research.column()==5&&research.row()==4&&!research.secondary()&&!research.concealed()&&research.parents().equals(List.of("thaumictinkerer:spell_cloth"))&&research.hiddenParents().equals(List.of("INFUSION"))&&research.pageOrder().size()==4,"original enchanter research flags, grid, hidden infusion and four pages");
            var advanced=AddonRegistry.research().get("thaumictinkerer:ttench_final");check(advanced.secondary()&&advanced.parents().equals(List.of("thaumictinkerer:ttench_focused","thaumictinkerer:ttench_dispersed"))&&advanced.column()==0&&advanced.row()==8,"original advanced research tree retained separately from source cost gate quirk");
            check(AddonRegistry.research().keySet().stream().filter(s->s.startsWith("thaumictinkerer:ttench_")).count()==14,"all fourteen enchantment research entries registered");
            tile.setItem(0,new ItemStack(Items.IRON_SWORD));tile.setItem(1,gold);tile.currentAspects=new AspectList();WandApi.set(gold,Aspects.ORDER,2000);tile.select(player,id("minecraft:sharpness"),0);tile.getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal("Osmotic tool"));
            var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);check(state.useWithoutItem(level,player,hit).consumesAction()&&player.containerMenu instanceof EnchanterMenu,"actual ServerPlayer opens native enchanter menu");var actual=(EnchanterMenu)player.containerMenu;
            check(!actual.clickMenuButton(player,-1)&&!actual.clickMenuButton(player,100000),"native menu rejects invalid action encoding");
            check(actual.clickMenuButton(player,0)&&tile.working,"actual menu action starts real world ticker");
            helper.runAfterDelay(16,()->{try{
                check(!tile.working&&tile.getItem(0).getEnchantmentLevel(EnchanterCatalog.find(id("minecraft:sharpness")).holder(level.registryAccess()))==1,"registered native world ticker completes sharpness application");
                check(WandApi.stored(tile.getItem(1),Aspects.ORDER)==800&&tile.enchantments().isEmpty()&&tile.levels().isEmpty()&&tile.currentAspects.size()==0&&tile.totalAspects.size()==0,"actual ticking spends twelve gold-cap vis and clears completed task");
                level.destroyBlock(pos,true,player);var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));check(drops.stream().anyMatch(e->e.getItem().is(EnchanterContent.BLOCK.get().asItem()))&&drops.stream().anyMatch(e->e.getItem().is(Items.IRON_SWORD)&&e.getItem().getHoverName().getString().equals("Osmotic tool")&&e.getItem().isEnchanted())&&drops.stream().anyMatch(e->WandApi.kind(e.getItem())==WandApi.Kind.WAND&&WandApi.stored(e.getItem(),Aspects.ORDER)==800),"breaking drops device, exact enchanted named tool and remaining wand vis");
                System.out.println("TT_ENCHANTER_PASS checks="+checks);helper.succeed();
            }finally{cleanup.run();}});
        }catch(Throwable failure){cleanup.run();throw failure;}
    }
}
