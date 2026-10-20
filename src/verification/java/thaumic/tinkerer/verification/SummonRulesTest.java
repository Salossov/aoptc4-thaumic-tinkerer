package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.summon.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class SummonRulesTest {
    private static void check(boolean value,String label){if(!value)throw new GameTestAssertException(label);}
    private static IInfusionPedestal pedestal(ServerLevel level,BlockPos pos,ItemStack item){
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:arcane_pedestal"));level.setBlock(pos,block.defaultBlockState(),2);
        check(level.getBlockEntity(pos) instanceof IInfusionPedestal,"native pedestal implements the public inventory contract");var tile=(IInfusionPedestal)level.getBlockEntity(pos);tile.setItem(0,item);return tile;
    }
    private static SummonBlockEntity tablet(ServerLevel level,BlockPos pos){level.setBlock(pos,SummonContent.BLOCK.get().defaultBlockState(),2);return (SummonBlockEntity)level.getBlockEntity(pos);}
    private static void clear(ServerLevel level,BlockPos... positions){for(var pos:positions){if(level.getBlockEntity(pos) instanceof Container inventory)inventory.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}}
    private static void drain(ServerLevel level,BlockPos pos){var cow=new Cow(EntityType.COW,level);cow.setPos(pos.getX(),pos.getY(),pos.getZ());for(int i=0;i<3;i++)cow.hurt(level.damageSources().magic(),0);}
    @GameTest(template="summon_space",batch="summon",timeoutTicks=100)
    public static void mobAspectVariantsAndRecipes(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();int recipes=0,variants=0;
        check(MobAspects.ASPECTS.size()==17&&MobAspects.ROWS.size()==29,"pinned numeric/enum row counts");
        for(int i=0;i<17;i++){
            var aspect=MobAspects.ASPECTS.get(i);
            for(int grade=0;grade<3;grade++){
                int index=grade*21+i;var stack=MobAspectItem.of(index);var restored=ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();
                check(MobAspectItem.variant(restored)==index&&MobAspectItem.aspect(restored)==aspect&&restored.getMaxStackSize()==16,"variant component survives native NBT roundtrip: "+index);
                check(restored.hasFoil()==(grade==2)&&MobAspectItem.condensed(restored)==(grade==1)&&MobAspectItem.infused(restored)==(grade==2),"original grade/glint predicates: "+index);
                check(AspectApi.aspects(level,restored).equals(Map.of(aspect.getTag(),grade==0?8:grade==1?54:64)),"native public scanning aspects preserve original explicit/derived amount: "+index);
                ItemData.update(restored,tag->tag.putInt("Unrelated",91));check(new MobAspectIngredient(index).test(restored),"custom ingredient ignores incidental old NBT: "+index);
                check(!new MobAspectIngredient((grade*21+(i+1)%17)).test(restored),"custom ingredient distinguishes neighboring metadata: "+index);
                variants++;
            }
            var raw=MobAspectItem.of(i);ItemData.update(raw,tag->tag.putBoolean("Extra",true));
            var condensed=level.getRecipeManager().byKey(ThaumicTinkerer.id("mob_aspect_condensed_"+aspect.getTag())).orElseThrow().value();
            var input=CraftingInput.of(3,3,Collections.nCopies(9,raw));
            check(condensed instanceof ShapedRecipe recipe&&recipe.matches(input,level)&&MobAspectItem.variant(recipe.assemble(input,level.registryAccess()))==21+i,"native condensed recipe accepts extra NBT and returns original variant");recipes++;
            var infused=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("mob_aspect_infused_"+aspect.getTag())).orElseThrow().value();
            var center=MobAspectItem.of(21+i);ItemData.update(center,tag->tag.putInt("Extra",17));
            check(infused.instability()==4&&infused.aspects().equals(Map.of(aspect.getTag(),10))&&infused.matches(new InfusionInput(center,Collections.nCopies(8,center)))&&MobAspectItem.variant(infused.getRecipeOutput())==42+i,"native infusion preserves nine condensed input stacks, ten essentia and exact variant");recipes++;
        }
        var fresh=MobAspectItem.of(42);var pos=new BlockPos(18,-1,27);
        check(!fresh.has(DataComponents.CUSTOM_DATA)&&MobAspectItem.lastUsedMatches(fresh,pos),"fresh infused component does not masquerade as legacy NBT; first tablet is eligible");
        MobAspectItem.markLastUsed(fresh,pos);check(MobAspectItem.lastUsedMatches(fresh,pos)&&!MobAspectItem.lastUsedMatches(fresh,pos.east())&&MobAspectItem.variant(fresh)==42,"legacy LastX/Y/Z binding remains independent of variant");
        var infusedSaved=ItemStack.parse(level.registryAccess(),fresh.save(level.registryAccess())).orElseThrow();check(MobAspectItem.lastUsedMatches(infusedSaved,pos),"tablet binding survives full native stack serialization");
        var stripped=((IObjectAspectItem)fresh.getItem()).inventoryScanStack(fresh);check(MobAspectItem.variant(stripped)==42&&!stripped.has(DataComponents.CUSTOM_DATA)&&((IObjectAspectItem)fresh.getItem()).scanVariant(stripped).equals("42"),"slot-scanning contract preserves metadata identity while stripping incidental NBT");
        var research=AddonRegistry.research().get("thaumictinkerer:summon");check(research.warp()==3&&!research.secondary()&&research.hiddenParents().equals(List.of("INFUSION"))&&research.pageOrder().equals(List.of("page.thaumictinkerer.summon.0","thaumictinkerer:spawner","thaumictinkerer:mob_aspect_condensed_messis","thaumictinkerer:mob_aspect_infused_messis","page.thaumictinkerer.summon.1")),"book retains source last-write crop recipe pages and warp");
        var spawner=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("spawner")).orElseThrow().value();check(spawner.vis().equals(Map.of("ordo",50,"perditio",50)),"source spawner arcane vis");recipes++;
        var tile=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:obsidian_tile")));
        var totem=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:obsidian_totem")));
        check(spawner.matches(CraftingInput.of(3,2,List.of(tile,tile,tile,new ItemStack(Blocks.STONE),new ItemStack(Blocks.STONE),new ItemStack(Blocks.STONE))),level),"source spawner accepts three metadata-one obsidian tiles");
        check(!spawner.matches(CraftingInput.of(3,2,List.of(totem,totem,totem,new ItemStack(Blocks.STONE),new ItemStack(Blocks.STONE),new ItemStack(Blocks.STONE))),level),"source spawner rejects metadata-zero pillar totems");
        var sword=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("blood_sword")).orElseThrow().value();check(sword.instability()==6&&sword.components().size()==6&&sword.aspects().equals(Map.of("fames",20,"tenebrae",5,"spiritus",10,"humanus",6)),"original blood sword infusion costs");recipes++;
        System.out.println("TT_MOB_ASPECT_VARIANTS_PASS variants="+variants+" recipes="+recipes);helper.succeed();
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=100)
    public static void mobAspectExactClasses(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();
        for(var row:MobAspects.ROWS)for(var id:row.harvestTypes()){
            var type=BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow();var entity=Objects.requireNonNull(type.create(level));
            check(MobAspects.forEntity(entity).equals(row.aspects()),"actual native class maps to pinned mob row: "+id);
        }
        check(MobAspects.forEntity(new Cow(EntityType.COW,level){}).isEmpty(),"subclasses do not inherit the source's exact-class essence recipe");
        check(MobAspects.matching(List.of(Aspects.WATER,Aspects.MAN,Aspects.MAN)).legacyName().equals("SnowMan"),"source containsAll ignores duplicate multiplicities");
        check(MobAspects.matching(List.of(Aspects.BEAST,Aspects.EARTH,Aspects.EARTH)).legacyName().equals("Cow"),"source enum order/set equality is preserved");
        System.out.println("TT_MOB_ASPECT_CLASSES_PASS rows=29");helper.succeed();
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=100)
    public static void bloodSwordNativeCombat(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,4,16));
        try(var fixture=new NativePlayerFixture(helper,"TTBloodSword",pos)){
            var player=fixture.player;var sword=new ItemStack(SummonContent.BLOOD_SWORD.get());player.setItemInHand(InteractionHand.MAIN_HAND,sword);
            check(sword.getMaxDamage()==950&&sword.getEnchantmentValue()==22&&sword.getItem() instanceof thaumcraft.api.IRepairable&&!sword.getItem().isValidRepairItem(sword,new ItemStack(Items.IRON_INGOT)),"original durability, thaumium enchantability and no ordinary repair material");
            var modifiers=sword.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();check(modifiers.size()==2&&modifiers.stream().anyMatch(m->m.attribute().equals(Attributes.ATTACK_DAMAGE)&&m.modifier().amount()==10)&&modifiers.stream().anyMatch(m->m.attribute().equals(Attributes.MOVEMENT_SPEED)&&m.modifier().amount()==.25),"original ten attack modifier and 25 percent movement modifier");
            player.setShiftKeyDown(true);sword.getItem().use(level,player,InteractionHand.MAIN_HAND);check(BloodSwordItem.active(sword)&&player.isUsingItem()&&sword.getUseAnimation()==UseAnim.BLOCK,"sneak activation retains original blocking use");
            sword.getItem().use(level,player,InteractionHand.MAIN_HAND);check(!BloodSwordItem.active(sword),"second sneak use deactivates harvest");player.setShiftKeyDown(false);player.stopUsingItem();
            var zombie=new Zombie(EntityType.ZOMBIE,level);zombie.setPos(pos.getX()+1,pos.getY(),pos.getZ());zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(SummonContent.BLOOD_SWORD.get()));
            drain(level,pos);player.setHealth(20);player.invulnerableTime=0;zombie.setHealth(20);zombie.invulnerableTime=0;
            sword.getItem().use(level,player,InteractionHand.MAIN_HAND);
            check(!player.hurt(level.damageSources().mobAttack(zombie),8)&&player.getHealth()==17&&zombie.getHealth()==18,"native blocking cancels eight damage, replaces it with three magic and charges attacking sword two");
            var unrelated=new Cow(EntityType.COW,level);unrelated.setPos(pos.getX()+3,pos.getY(),pos.getZ());zombie.invulnerableTime=0;
            unrelated.hurt(level.damageSources().mobAttack(zombie),1);check(zombie.getHealth()==18,"source global counter suppresses the next unrelated attacker cost");
            zombie.invulnerableTime=0;unrelated.hurt(level.damageSources().mobAttack(zombie),1);check(zombie.getHealth()==16,"subsequent unrelated attempt pays again once shared counter reaches zero");
            player.stopUsingItem();drain(level,pos);player.setHealth(20);player.invulnerableTime=0;
            var target=new Cow(EntityType.COW,level);target.setPos(pos.getX()+2,pos.getY(),pos.getZ());target.hurt(level.damageSources().magic(),2);target.invulnerableTime=20;
            target.hurt(level.damageSources().playerAttack(player),1);check(player.getHealth()==18,"sword pays two magic on an attempt even when target rejects invulnerable hit");
            ItemData.update(sword,tag->tag.putInt("Activated",1));player.invulnerableTime=0;player.setHealth(20);drain(level,pos);
            target.invulnerableTime=0;target.setHealth(1);level.addFreshEntity(target);target.hurt(level.damageSources().playerAttack(player),2);
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5));
            check(drops.size()==3&&drops.stream().allMatch(drop->drop.getItem().is(SummonContent.MOB_ASPECT.get())&&drop.hasPickUpDelay()),"actual death replaces all normal loot with three source essences");
            var aspects=drops.stream().map(drop->MobAspectItem.aspect(drop.getItem())).toList();check(Collections.frequency(aspects,Aspects.BEAST)==2&&Collections.frequency(aspects,Aspects.EARTH)==1,"cow drop multiplicities remain two beast and one earth");drops.forEach(ItemEntity::discard);target.discard();zombie.discard();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);drain(level,pos);
            System.out.println("TT_BLOOD_SWORD_NATIVE_COMBAT_PASS");helper.succeed();
        }
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=1200)
    public static void summonLegacySpawnVariants(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));
        for(int seed=0;seed<40;seed++){
            var original=Objects.requireNonNull(EntityType.HORSE.create(level));original.getRandom().setSeed(seed);boolean donkey=new java.util.Random(seed).nextInt(10)==0;
            var prepared=SummonSpawns.prepare(level,original);var entity=prepared.entity();entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);level.addFreshEntity(entity);
            try{prepared.initialize(level);check(entity.getType()==(donkey?EntityType.DONKEY:EntityType.HORSE),"original 1-in-10 roll chooses the modern native horse/donkey: seed "+seed);check(!donkey||original.isRemoved()&&level.getEntity(original.getUUID())==null,"retyped horse prototype is never left in the world");}
            finally{entity.discard();}
        }
        var nether=Objects.requireNonNull(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER));var column=new net.minecraft.world.level.ChunkPos(0,0);boolean owned=!nether.getForcedChunks().contains(column.toLong());
        if(owned)nether.setChunkForced(0,0,true);
        helper.onEachTick(()->{
            if(nether.getChunkSource().getChunkNow(0,0)==null)return;
            try{
                for(int seed=0;seed<40;seed++){
                    var original=Objects.requireNonNull(EntityType.SKELETON.create(nether));original.getRandom().setSeed(seed);boolean bow=new java.util.Random(seed).nextInt(5)==0;
                    var prepared=SummonSpawns.prepare(nether,original);var entity=prepared.entity();entity.moveTo(.5,80,.5,0,0);nether.addFreshEntity(entity);
                    try{prepared.initialize(nether);check(entity.getType()==EntityType.WITHER_SKELETON&&entity.getMainHandItem().is(bow?Items.BOW:Items.STONE_SWORD),"actual native infernal spawn retains original 1-in-5 bow branch after initialization: seed "+seed);check(original.isRemoved()&&nether.getEntity(original.getUUID())==null,"retyped skeleton prototype is never world-registered");}
                    finally{entity.discard();}
                }
                System.out.println("TT_SUMMON_LEGACY_SPAWN_VARIANTS_PASS horse_seeds=40 infernal_skeleton_seeds=40");helper.succeed();
            }finally{if(owned)nether.setChunkForced(0,0,false);}
        });
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=1000)
    public static void summonNormalTimingAndMixedConsumption(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,4,16));var tablet=tablet(level,pos);
        var a=pos.offset(-1,0,-1);var b=pos.west();var c=pos.north();var power=pos.east();
        int beast=MobAspects.ASPECTS.indexOf(Aspects.BEAST),earth=MobAspects.ASPECTS.indexOf(Aspects.EARTH);
        var first=pedestal(level,a,MobAspectItem.of(beast));first.getItem(0).setCount(5);var second=pedestal(level,b,MobAspectItem.of(21+earth));var third=pedestal(level,c,MobAspectItem.of(42+earth));MobAspectItem.markLastUsed(third.getItem(0),pos.east(20));
        level.setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);int[] stage={0};long initial=level.getGameTime();long[] observed={initial/300};
        helper.onEachTick(()->{
            long now=level.getGameTime();if(now<=initial||now/300==observed[0]||tablet.preparing())return;observed[0]=now/300;
            if(stage[0]==0){check(!first.isEmpty()&&!second.isEmpty()&&!third.isEmpty()&&level.getEntitiesOfClass(Cow.class,new AABB(pos).inflate(3)).isEmpty(),"native power inhibits the first complete 300-tick cycle");level.setBlock(power,Blocks.AIR.defaultBlockState(),3);stage[0]=1;return;}
            try{
                var cows=level.getEntitiesOfClass(Cow.class,new AABB(pos).inflate(3));check(cows.size()==1&&first.isEmpty()&&second.isEmpty()&&third.isEmpty(),"next native cycle spawns cow from set-matched mixed grades and consumes whole stacks, including infused stack");cows.forEach(Entity::discard);System.out.println("TT_SUMMON_NORMAL_MIXED_PASS");helper.succeed();
            }finally{clear(level,pos,a,b,c,power);}
        });
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=2800)
    public static void summonInfusedLastTablet(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,4,16));var tablet=tablet(level,pos);
        var positions=List.of(pos.offset(-1,0,-1),pos.west(),pos.north());var inventories=new ArrayList<IInfusionPedestal>();int beast=MobAspects.ASPECTS.indexOf(Aspects.BEAST);
        for(var pedestal:positions){var item=MobAspectItem.of(42+beast);MobAspectItem.markLastUsed(item,pos.east(8));inventories.add(pedestal(level,pedestal,item));}
        long initial=level.getGameTime();int[] stage={0};long[] observed={initial/300};
        helper.onEachTick(()->{
            long now=level.getGameTime();if(now<=initial||now/300==observed[0]||tablet.preparing())return;observed[0]=now/300;
            var wolves=level.getEntitiesOfClass(Wolf.class,new AABB(pos).inflate(3));
            if((now/300)%4!=0){check(wolves.isEmpty(),"all-infused recipe skips ordinary 300-tick cycles");return;}
            if(stage[0]==0){check(wolves.isEmpty()&&inventories.stream().allMatch(p->MobAspectItem.lastUsedMatches(p.getItem(0),pos)),"first 1200-tick cycle retargets all infused stacks without spawning; time="+now+" pos="+pos+" wolves="+wolves.size()+" inventories="+inventories.stream().map(p->p.getItem(0).toString()+":"+p.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)).toList());stage[0]=1;return;}
            try{check(wolves.size()==1&&inventories.stream().allMatch(p->p.getItem(0).getCount()==1),"next 1200-tick cycle spawns wolf and preserves every infused essence");wolves.forEach(Entity::discard);System.out.println("TT_SUMMON_INFUSED_BINDING_PASS");helper.succeed();}
            finally{clear(level,pos,positions.get(0),positions.get(1),positions.get(2));}
        });
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=1000)
    public static void summonHalfOpenSearchBounds(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,4,16));var tablet=tablet(level,pos);int beast=MobAspects.ASPECTS.indexOf(Aspects.BEAST);
        var positive=List.of(pos.offset(5,0,-1),pos.offset(5,0,0),pos.offset(5,0,1));var negative=List.of(pos.offset(-5,0,-5),pos.offset(-5,0,-4),pos.offset(-4,0,-5));
        for(var p:positive)pedestal(level,p,MobAspectItem.of(beast));long initial=level.getGameTime();int[] stage={0};long[] observed={initial/300};
        helper.onEachTick(()->{
            long now=level.getGameTime();if(now<=initial||now/300==observed[0]||tablet.preparing())return;observed[0]=now/300;
            var wolves=level.getEntitiesOfClass(Wolf.class,new AABB(pos).inflate(3));
            if(stage[0]==0){check(wolves.isEmpty()&&positive.stream().allMatch(p->!((IInfusionPedestal)level.getBlockEntity(p)).isEmpty()),"positive five-block edge is outside every half-open radius");for(var p:positive)clear(level,p);for(var p:negative)pedestal(level,p,MobAspectItem.of(beast));stage[0]=1;return;}
            try{check(wolves.size()==1&&negative.stream().allMatch(p->((IInfusionPedestal)level.getBlockEntity(p)).isEmpty()),"negative five-block edge and source X/Z scan order are included");wolves.forEach(Entity::discard);System.out.println("TT_SUMMON_HALF_OPEN_BOUNDS_PASS");helper.succeed();}
            finally{clear(level,pos);for(var p:positive)clear(level,p);for(var p:negative)clear(level,p);}
        });
    }
    @GameTest(template="summon_space",batch="summon",timeoutTicks=700)
    public static void tabletCreatesPersistentWildFirebat(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));var tablet=tablet(level,pos);
        var positions=List.of(pos.offset(-1,0,-1),pos.west(),pos.north());var aspects=List.of(Aspects.FLIGHT,Aspects.FIRE,Aspects.MAGIC);
        for(int i=0;i<3;i++)pedestal(level,positions.get(i),MobAspectItem.of(MobAspects.ASPECTS.indexOf(aspects.get(i))));
        long initial=level.getGameTime();
        helper.onEachTick(()->{
            if(level.getGameTime()/300==initial/300||tablet.preparing())return;
            try{
                var bats=level.getEntitiesOfClass(net.minecraft.world.entity.ambient.Bat.class,new AABB(pos).inflate(4));
                check(bats.size()==1&&bats.getFirst().getType()==BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("thaumcraft:firebat")),"actual source essence triple creates the native firebat rather than a vanilla bat");
                var bat=bats.getFirst();var saved=new net.minecraft.nbt.CompoundTag();bat.saveWithoutId(saved);
                check(!saved.getBoolean("Summoned")&&!saved.hasUUID("Owner")&&bat.getAttributeValue(Attributes.ATTACK_DAMAGE)==1,"tablet creates an ownerless wild firebat with source attack, not a targetless focus summon");
                var restored=(net.minecraft.world.entity.ambient.Bat)java.util.Objects.requireNonNull(bat.getType().create(level));restored.load(saved);restored.setResting(false);
                for(int i=0;i<50;i++)restored.tick();
                check(restored.isAlive()&&restored.getHealth()==5,"tablet firebat survives native entity save/load and fifty actual ticks without a target");
                check(positions.stream().allMatch(p->((IInfusionPedestal)level.getBlockEntity(p)).isEmpty()),"wild summon consumes its three original raw essences");
                restored.discard();bat.discard();System.out.println("TT_TABLET_WILD_FIREBAT_PASS");helper.succeed();
            }finally{clear(level,pos,positions.get(0),positions.get(1),positions.get(2));}
        });
    }
    private SummonRulesTest(){ }
}
