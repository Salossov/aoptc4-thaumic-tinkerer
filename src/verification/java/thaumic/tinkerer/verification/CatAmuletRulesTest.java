package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.entities.MobBehaviorApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiUtilityContent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class CatAmuletRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static Object field(Object object,Class<?> owner,String name) throws Exception {var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static GoalSelector selector(Mob mob,String name) throws Exception {return (GoalSelector)field(mob,Mob.class,name);}
    @GameTest(template="summon_space",batch="cat_amulet",timeoutTicks=100)
    public static void persistentNativeGoalMutation(GameTestHelper helper) throws Exception {
        VerificationReports.install();
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id("cat_amulet")),"KAMI-off leaves amulet unregistered");helper.succeed();return;}
        var created=new ArrayList<net.minecraft.world.entity.Entity>();
        try(var fixture=new NativePlayerFixture(helper,"TTAnimalCharm",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var level=helper.getLevel();var item=KamiUtilityContent.CAT_AMULET.get();var stack=item.getDefaultInstance();
            check(stack.getMaxStackSize()==1&&stack.getRarity()==Rarity.EPIC&&!stack.isDamageableItem(),"source item properties");
            var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();var necklace=inventory.getCurios().get("necklace").getStacks();
            check(necklace.isItemValid(0,stack),"actual Curios necklace handler accepts registered amulet");
            var creeper=EntityType.CREEPER.create(level);creeper.setPos(player.getX()+2,player.getY(),player.getZ());level.addFreshEntity(creeper);created.add(creeper);
            var zombie=EntityType.ZOMBIE.create(level);zombie.setPos(player.getX()+1,player.getY(),player.getZ());level.addFreshEntity(zombie);created.add(zombie);
            var outside=EntityType.ZOMBIE.create(level);outside.setPos(player.getX()+12,player.getY(),player.getZ());level.addFreshEntity(outside);created.add(outside);
            var crystal=EntityType.END_CRYSTAL.create(level);crystal.setPos(player.getX()+4,player.getY(),player.getZ());level.addFreshEntity(crystal);created.add(crystal);
            var catGoal=new AvoidEntityGoal<>(zombie,Cat.class,6,1,1.2);var ocelotGoal=new AvoidEntityGoal<>(zombie,Ocelot.class,6,1,1.2);
            var targetGoal=new NearestAttackableTargetGoal<>(zombie,Player.class,0,false,false,null);
            selector(zombie,"goalSelector").addGoal(3,catGoal);selector(zombie,"targetSelector").addGoal(4,ocelotGoal);selector(zombie,"targetSelector").addGoal(5,targetGoal);
            var goals=new ArrayList<>(selector(zombie,"goalSelector").getAvailableGoals());goals.addAll(selector(zombie,"targetSelector").getAvailableGoals());
            List<Integer> priorities=goals.stream().map(WrappedGoal::getPriority).toList();
            creeper.setTarget(player);zombie.setTarget(player);MobBehaviorApi.setSwell(creeper,20);
            // InventoryTick has no effect, even when the amulet is selected.
            item.inventoryTick(stack,level,player,0,true);check(field(catGoal,AvoidEntityGoal.class,"avoidClass")==Cat.class,"inventory amulet does not mutate AI");
            necklace.setStackInSlot(0,stack);item.curioTick(new SlotContext("necklace",player,0,false,true),stack);
            check(field(catGoal,AvoidEntityGoal.class,"avoidClass")==Player.class&&field(ocelotGoal,AvoidEntityGoal.class,"avoidClass")==Player.class,"both modern cat classes redirect in either selector");
            check(field(targetGoal,NearestAttackableTargetGoal.class,"targetType")==EndCrystal.class,"source player target type is redirected in place");
            check(!targetGoal.canUse(),"non-living crystal is rejected by the actual modern goal search without a cast crash");
            check(zombie.getTarget()==player,"non-creeper current target is not cleared by class mutation");
            check(creeper.getTarget()==null&&(int)field(creeper,Creeper.class,"swell")==2,"creeper current target and fuse progress follow source");
            var after=new ArrayList<>(selector(zombie,"goalSelector").getAvailableGoals());after.addAll(selector(zombie,"targetSelector").getAvailableGoals());
            check(after.equals(goals)&&after.stream().map(WrappedGoal::getPriority).toList().equals(priorities),"same wrapped goals and priorities are retained");
            for(var entry:selector(outside,"targetSelector").getAvailableGoals())if(entry.getGoal() instanceof NearestAttackableTargetGoal<?> goal)
                check(field(goal,NearestAttackableTargetGoal.class,"targetType")!=EndCrystal.class,"outside radius stays unchanged");
            necklace.setStackInSlot(0,ItemStack.EMPTY);check(field(targetGoal,NearestAttackableTargetGoal.class,"targetType")==EndCrystal.class,"unequip does not undo original mutation");
            var saved=zombie.saveWithoutId(new CompoundTag());var restored=EntityType.ZOMBIE.create(level);restored.load(saved);
            check(selector(restored,"targetSelector").getAvailableGoals().stream().anyMatch(entry->{
                try{return entry.getGoal() instanceof NearestAttackableTargetGoal<?> goal&&field(goal,NearestAttackableTargetGoal.class,"targetType")==Player.class;}catch(Exception error){throw new IllegalStateException(error);}
            }),"native save/load reconstructs original default player goals");restored.discard();
        } finally {created.forEach(net.minecraft.world.entity.Entity::discard);}
        helper.succeed();
    }
}
