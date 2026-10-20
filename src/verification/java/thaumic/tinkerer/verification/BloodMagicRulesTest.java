package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.ProjectileHooks;
import thaumic.tinkerer.item.FocusContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BloodMagicRulesTest {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="summon_space",batch="bloodmagic",timeoutTicks=150)
    public static void nativeBloodMagicCompatibility(GameTestHelper helper){
        VerificationReports.install();
        check(ProjectileHooks.DEFLECT_BLACKLIST.stream().noneMatch(c->c.getName().startsWith("wayoftime.bloodmagic.")),"removed particle beam has no substitute exclusion for unrelated modern Blood Magic projectiles");
        if(!net.neoforged.fml.ModList.get().isLoaded("bloodmagic")){
            check(BuiltInRegistries.ENTITY_TYPE.keySet().stream().noneMatch(id->id.getNamespace().equals("bloodmagic")),"absent Blood Magic introduces no entity registrations or loading dependency");
            System.out.println("TT_BLOOD_MAGIC_ABSENT_PASS");helper.succeed();return;
        }
        var types=BuiltInRegistries.ENTITY_TYPE.entrySet().stream().filter(e->e.getKey().location().getNamespace().equals("bloodmagic")).sorted(Comparator.comparing(e->e.getKey().location().toString())).toList();
        check(types.size()==7,"pinned unofficial Blood Magic registers all seven native entities; actual="+types.stream().map(e->e.getKey().location().toString()).toList());
        check(types.stream().noneMatch(e->e.getKey().location().getPath().contains("beam")),"pinned registry has no replacement particle-beam entity");
        var level=helper.getLevel();var fixture=new NativePlayerFixture(helper,"TTBloodMagic",helper.absolutePos(new BlockPos(16,5,16)));var player=fixture.player;
        var focus=new ItemStack(FocusContent.DEFLECT.get());var wand=FocusRulesTest.wand(player,focus);var spawned=new ArrayList<Entity>();
        int[] stage={0},ticks={0},projectiles={0};boolean[] done={false};boolean botania=net.neoforged.fml.ModList.get().isLoaded("botania");
        Runnable cleanup=()->{FocusRulesTest.stop(player,wand);spawned.forEach(Entity::discard);fixture.close();};
        helper.onEachTick(()->{
            if(done[0]||ticks[0]++%10!=0)return;
            try{
                if(stage[0]>=types.size()){
                    check(projectiles[0]>=5,"verification exercised actual Blood Magic projectile factories");done[0]=true;cleanup.run();
                    System.out.println("TT_BLOOD_MAGIC_NATIVE_PASS entities="+types.size()+" projectiles="+projectiles[0]+" botania="+botania+" replacement_blacklist=false");helper.succeed();return;
                }
                var entry=types.get(stage[0]++);var entity=Objects.requireNonNull(entry.getValue().create(level));spawned.add(entity);
                entity.setPos(player.position().add(-2,0,0));entity.setDeltaMovement(1,.125,0);
                boolean tracked=entity instanceof ThrowableProjectile||entity instanceof AbstractArrow;
                check(ProjectileHooks.isProjectile(entity)==tracked,"native projectile family follows the existing extension point: "+entry.getKey().location());
                check(!entity.getClass().getSimpleName().equals("EntityParticleBeam"),"original excluded class is absent from the native entity catalog");
                check(ProjectileHooks.blacklisted(entity)==botania,"no Blood Magic-specific blanket exclusion; only existing source Botania rule can block: "+entry.getKey().location());
                var old=entity.position();var motion=entity.getDeltaMovement();var expected=old.add(motion.normalize().scale(4));
                level.addFreshEntity(entity);
                Entity friendly=null;
                if(tracked){
                    projectiles[0]++;friendly=Objects.requireNonNull(entry.getValue().create(level));spawned.add(friendly);friendly.setPos(player.position().add(-1,0,0));friendly.setDeltaMovement(1,0,0);((Projectile)friendly).setOwner(player);level.addFreshEntity(friendly);
                    check(ProjectileHooks.owner(friendly)==player,"native Blood Magic owner is resolved through the existing projectile API");
                }
                var friendlyPos=friendly==null?null:friendly.position();var cost=WandApi.quote(wand,player,FocusContent.DEFLECT.get().getVisCost(focus));int vis=WandApi.stored(wand,Aspects.ORDER);
                player.setItemInHand(InteractionHand.MAIN_HAND,wand);wand.getItem().use(level,player,InteractionHand.MAIN_HAND);DeflectionRulesTest.legacyUse(player,wand,72000);
                check(tracked&&!botania?entity.position().distanceTo(expected)<1e-8:entity.position().equals(old),"actual focus preserves ordinary deflection or existing Botania early return: "+entry.getKey().location());
                check(entity.getDeltaMovement().equals(motion)&&WandApi.stored(wand,Aspects.ORDER)==vis-cost.get(Aspects.ORDER),"native trajectory remains unchanged and one source payment covers the group");
                if(friendly!=null){check(friendly.position().equals(friendlyPos),"own native Blood Magic projectile remains untouched");friendly.discard();}
                FocusRulesTest.stop(player,wand);entity.discard();
            }catch(Throwable error){done[0]=true;cleanup.run();if(error instanceof RuntimeException failure)throw failure;throw new GameTestAssertException(error.toString());}
        });
    }
    private BloodMagicRulesTest(){}
}
