package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.ProjectileHooks;
import thaumic.tinkerer.item.FocusContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BotaniaRulesTest {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="summon_space",batch="botania",timeoutTicks=100)
    public static void nativeBotaniaDeflection(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));
        if(!net.neoforged.fml.ModList.get().isLoaded("botania")){
            var arrow=new Arrow(EntityType.ARROW,level);check(!ProjectileHooks.blacklisted(arrow),"absent Botania does not initialize its optional API or blacklist ordinary arrows");arrow.discard();
            System.out.println("TT_BOTANIA_ABSENT_PASS");helper.succeed();return;
        }
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse("botania:mana_burst")).orElseThrow();var spawned=new ArrayList<Entity>();
        var fixture=new NativePlayerFixture(helper,"TTBotaniaFocus",pos);var player=fixture.player;
        var focus=new ItemStack(FocusContent.DEFLECT.get());var wand=FocusRulesTest.wand(player,focus);
        var lenses=List.of("mana_lens","damaging_lens","entropic_lens");int[] stage={0},ticks={0};boolean[] done={false};
        Runnable cleanup=()->{FocusRulesTest.stop(player,wand);spawned.forEach(Entity::discard);fixture.close();};
        helper.onEachTick(()->{
            if(done[0]||ticks[0]++%10!=0)return;
            try{
                if(stage[0]<lenses.size()){
                    String lens=lenses.get(stage[0]++);
                    var burst=(ThrowableProjectile)Objects.requireNonNull(type.create(level));spawned.add(burst);burst.setPos(player.position().add(-2,0,0));burst.setDeltaMovement(1,0,0);
                    var stack=new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("botania:"+lens)).orElseThrow());burst.getClass().getMethod("setSourceLens",ItemStack.class).invoke(burst,stack);level.addFreshEntity(burst);
                    boolean dangerous=!lens.equals("mana_lens");check(ProjectileHooks.isProjectile(burst)&&ProjectileHooks.blacklisted(burst)!=dangerous,"native mana burst uses original primary-lens classification: "+lens);
                    var old=burst.position();var motion=burst.getDeltaMovement();var cost=WandApi.quote(wand,player,FocusContent.DEFLECT.get().getVisCost(focus));int vis=WandApi.stored(wand,Aspects.ORDER);
                    player.setItemInHand(InteractionHand.MAIN_HAND,wand);wand.getItem().use(level,player,InteractionHand.MAIN_HAND);wand.getItem().onUseTick(level,player,wand,72000);
                    check(dangerous?burst.position().distanceTo(old.add(4,0,0))<1e-8:burst.position().equals(old),"native focus preserves displacement for dangerous lens and ignores normal mana: "+lens+" old="+old+" actual="+burst.position()+" oldVis="+vis+" vis="+WandApi.stored(wand,Aspects.ORDER));
                    check(burst.getDeltaMovement().equals(motion)&&WandApi.stored(wand,Aspects.ORDER)==vis-cost.get(Aspects.ORDER),"trajectory and one-per-tick vis payment remain unchanged");
                    if(dangerous){
                        ProjectileHooks.DEFLECT_BLACKLIST.add(net.minecraft.world.level.entity.EntityAccess.class);
                        try{check(!ProjectileHooks.blacklisted(burst),"source Botania branch returns before an interface blacklist can block a dangerous burst");}finally{ProjectileHooks.DEFLECT_BLACKLIST.remove(net.minecraft.world.level.entity.EntityAccess.class);}
                        ProjectileHooks.DEFLECT_BLACKLIST.add(burst.getClass());
                        try{check(ProjectileHooks.blacklisted(burst),"an exact blacklist class still takes precedence over Botania's early return");}finally{ProjectileHooks.DEFLECT_BLACKLIST.remove(burst.getClass());}
                    }
                    FocusRulesTest.stop(player,wand);burst.discard();return;
                }
                var arrow=new Arrow(EntityType.ARROW,level);arrow.setPos(player.position().add(-2,0,0));arrow.setDeltaMovement(1,0,0);spawned.add(arrow);level.addFreshEntity(arrow);var old=arrow.position();
                check(ProjectileHooks.blacklisted(arrow),"original harmless classification also applies to projectiles which are not mana bursts");FocusContent.DEFLECT.get().cast(player,wand,focus);check(arrow.position().equals(old),"Botania's source quirk leaves a hostile vanilla arrow untouched");
                done[0]=true;cleanup.run();System.out.println("TT_BOTANIA_NATIVE_DEFLECTION_PASS lenses=3 source_early_return=true native_cooldown=true");helper.succeed();
            }catch(Throwable error){done[0]=true;cleanup.run();if(error instanceof RuntimeException failure)throw failure;throw new GameTestAssertException(error.toString());}
        });
    }
    private BotaniaRulesTest(){}
}
