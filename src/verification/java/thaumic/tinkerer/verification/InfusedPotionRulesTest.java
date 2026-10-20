package thaumic.tinkerer.verification;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.entities.LivingAttackAttemptEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.ForcefieldContent;
import thaumic.tinkerer.crop.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfusedPotionRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void infusedPotions(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+245,(anchor.getZ()>>4)*16+8);
        var player=FocusRulesTest.player(helper,"[TTImbued]");player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.getAbilities().instabuild=false;
        var changed=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(pos.offset(-4,-3,-4),pos.offset(4,5,4)))changed.add(p.immutable());
        var cow=new Cow(EntityType.COW,level);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(pos.getX()+2.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(cow);
        Runnable cleanup=()->{InfusedPotionEffects.AIR_HITS.remove(cow);InfusedPotionEffects.FIRE_HITS.remove(cow);cow.discard();player.removeAllEffects();player.getAbilities().instabuild=false;player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);};
        try{
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            for(int i=0;i<4;i++){
                var fruit=InfusedContent.FRUITS.get(i).get();var potion=InfusedContent.POTIONS.get(i).get();var effect=InfusedContent.IMBUED.get(i);
                check(fruit.aspect()==InfusedContent.FRUIT_ASPECTS.get(i)&&fruit.getAspects(new ItemStack(fruit)).getAmount(Aspects.CROP)==1&&fruit.getAspects(new ItemStack(fruit)).getAmount(fruit.aspect())==1,"original primal fruit metadata mapping and contained essentia "+i);
                check(fruit.getDefaultInstance().getFoodProperties(player)==null&&fruit.getDefaultInstance().getMaxStackSize()==64,"fruit is non-food, stackable essentia item "+i);
                var bonus=AspectApi.aspects(level,new ItemStack(fruit));check(bonus.getOrDefault(fruit.aspect().getTag(),0)==1&&bonus.getOrDefault("messis",0)==1,"native object-aspect pipeline includes the original contained-essentia bonus "+i);
                check(effect.value().getCategory()==MobEffectCategory.HARMFUL&&effect.value().getColor()==0,"original harmful dummy effect and black color "+i);
                check(potion.getUseDuration(new ItemStack(potion),player)==32&&potion.getUseAnimation(new ItemStack(potion))==UseAnim.DRINK&&potion.getDefaultInstance().getMaxStackSize()==1,"source drinking duration, animation, singleton "+i);
                player.removeAllEffects();var stack=new ItemStack(potion);var result=potion.finishUsingItem(stack,level,player);var applied=player.getEffect(effect);
                check(stack.isEmpty()&&result.is(Items.GLASS_BOTTLE)&&applied!=null&&applied.getDuration()==3600&&applied.getAmplifier()==0,"actual drink consumes potion, returns bottle and applies exactly 3600 ticks "+i);
                var saved=new CompoundTag();player.saveWithoutId(saved);var effects=saved.getList("active_effects",10);
                check(effects.size()==1&&effects.getCompound(0).getString("id").equals(effect.getId().toString()),"native entity NBT persists registered effect identity "+i);
                var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("infused_potion_"+fruit.aspect().getTag())).orElseThrow().value();var contents=new EssentiaStore(64,true,false);contents.addToContainer(Aspects.AURA,5);contents.addToContainer(fruit.aspect(),5);
                check(recipe.research().equals("thaumictinkerer:infused_potions")&&recipe.matches(contents,new ItemStack(fruit))&&recipe.getRecipeOutput().is(potion)&&recipe.consume(contents)&&contents.amount()==0,"native crucible recipe consumes source aura/primal cost "+i);
                var wrong=InfusedContent.FRUITS.get((i+1)%4).get();check(!recipe.catalyst().test(new ItemStack(wrong)),"recipe rejects different primal fruit "+i);
            }
            player.removeAllEffects();var air=InfusedContent.POTIONS.getFirst().get();var creative=new ItemStack(air);player.getAbilities().instabuild=true;
            check(air.finishUsingItem(creative,level,player)==creative&&creative.getCount()==1&&player.hasEffect(InfusedContent.IMBUED.getFirst())&&player.getInventory().countItem(Items.GLASS_BOTTLE)==0,"creative drink retains potion and produces no bottle");player.getAbilities().instabuild=false;
            var tampered=new ItemStack(air);tampered.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.POISON));player.removeAllEffects();air.finishUsingItem(tampered,level,player);
            check(player.hasEffect(InfusedContent.IMBUED.getFirst())&&!player.hasEffect(MobEffects.POISON),"original fixed effect overrides foreign vanilla potion contents");
            new ItemStack(Items.MILK_BUCKET).finishUsingItem(level,player);check(!player.hasEffect(InfusedContent.IMBUED.getFirst()),"native milk removes the original curable dummy effect");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(air));check(air.use(level,player,InteractionHand.MAIN_HAND).getResult().consumesAction()&&player.isUsingItem(),"actual right click starts drinking");player.stopUsingItem();
            var dirt=pos.below();level.setBlock(dirt,Blocks.DIRT.defaultBlockState(),2);var hit=new BlockHitResult(dirt.getCenter(),Direction.UP,dirt,false);
            check(air.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit))==InteractionResult.PASS&&level.getBlockState(dirt).is(Blocks.DIRT),"imbued potion does not inherit modern water-to-mud use");
            player.addEffect(new MobEffectInstance(InfusedContent.IMBUED.get(0),3600));player.addEffect(new MobEffectInstance(InfusedContent.IMBUED.get(1),3600));
            cow.setInvulnerable(true);float before=cow.getHealth();boolean hurt=cow.hurt(level.damageSources().playerAttack(player),2);
            check(!hurt&&cow.getHealth()==before&&InfusedPotionEffects.AIR_HITS.containsKey(cow)&&InfusedPotionEffects.FIRE_HITS.containsKey(cow),"real core entry hook tracks source attempt before invulnerability rejection");
            long now=level.getGameTime();check(InfusedPotionEffects.AIR_HITS.get(cow)==now&&InfusedPotionEffects.FIRE_HITS.get(cow)==now,"tracked attempt uses target world's total time");
            InfusedPotionEffects.AIR_HITS.remove(cow);InfusedPotionEffects.FIRE_HITS.remove(cow);var arrow=(Arrow)EntityType.ARROW.create(level);arrow.setOwner(player);cow.hurt(level.damageSources().arrow(arrow,player),2);
            check(!InfusedPotionEffects.AIR_HITS.containsKey(cow)&&!InfusedPotionEffects.FIRE_HITS.containsKey(cow),"indirect player-owned arrow is excluded by original direct-source player check");arrow.discard();
            cow.setInvulnerable(false);player.removeAllEffects();int[] attempts={0};Consumer<LivingAttackAttemptEvent> cancel=e->{if(e.target()==cow){attempts[0]++;e.setCanceled(true);}};LivingAttackAttemptEvent.BUS.addListener(cancel);
            try{check(!cow.hurt(level.damageSources().playerAttack(player),2)&&cow.getHealth()==before&&attempts[0]==1,"native attempt cancellation rejects damage, exactly one base invocation");}finally{LivingAttackAttemptEvent.BUS.unregister(cancel);}
            player.addEffect(new MobEffectInstance(InfusedContent.IMBUED.get(2),3600));cow.hurt(level.damageSources().playerAttack(player),1);
            int centerX=(int)((cow.getX()+player.getX())/2),centerY=(int)(player.getY()+2),centerZ=(int)((cow.getZ()+player.getZ())/2);int fields=0;
            for(int y=-2;y<3;y++)for(int z=-2;z<3;z++)if(level.getBlockState(new BlockPos(centerX,centerY+y,centerZ+z)).is(ForcefieldContent.BLOCK.get()))fields++;
            check(fields==25,"actual native attack creates source 5x5 earth plane on X axis");
            for(var p:changed)if(level.getBlockState(p).is(ForcefieldContent.BLOCK.get()))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            cow.setPos(player.getX(),player.getY(),player.getZ()+2);var obstruction=new BlockPos((int)((cow.getX()+player.getX())/2),(int)player.getY()+1,(int)((cow.getZ()+player.getZ())/2));level.setBlock(obstruction,Blocks.STONE.defaultBlockState(),2);cow.invulnerableTime=0;cow.hurt(level.damageSources().playerAttack(player),1);
            centerX=(int)((cow.getX()+player.getX())/2);centerY=(int)(player.getY()+2);centerZ=(int)((cow.getZ()+player.getZ())/2);fields=0;
            for(int y=-2;y<3;y++)for(int x=-2;x<3;x++)if(level.getBlockState(new BlockPos(centerX+x,centerY+y,centerZ)).is(ForcefieldContent.BLOCK.get()))fields++;
            check(fields==24&&level.getBlockState(obstruction).is(Blocks.STONE),"alternate earth axis preserves pre-existing solid block inside plane");
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);player.removeAllEffects();player.addEffect(new MobEffectInstance(InfusedContent.IMBUED.get(3),3600));
            // The test templates can start at negative coordinates. Java truncation
            // starts one cell later there, unlike floor; use the actual first/last
            // integer of the source interval rather than assuming pos-2.
            var waterInside=new BlockPos((int)(player.getX()-2),(int)(player.getY()-2),(int)(player.getZ()-2));var waterEdge=pos.offset(2,1,2);var waterOutside=pos.offset(3,0,0);var nonLava=pos.west();level.setBlock(waterInside,Blocks.LAVA.defaultBlockState(),2);level.setBlock(waterEdge,Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,3),2);level.setBlock(waterOutside,Blocks.LAVA.defaultBlockState(),2);level.setBlock(nonLava,Blocks.WATER.defaultBlockState(),2);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(player));
            check(level.getBlockState(waterInside).is(Blocks.OBSIDIAN)&&level.getBlockState(waterEdge).is(Blocks.OBSIDIAN)&&level.getBlockState(waterOutside).is(Blocks.LAVA)&&level.getBlockState(nonLava).is(Blocks.WATER),"source truncated loop freezes stationary and flowing lava including fractional upper cell, no extra radius or water conversion: player="+player.position()+", first="+level.getBlockState(waterInside)+", edge="+level.getBlockState(waterEdge)+", outside="+level.getBlockState(waterOutside)+", water="+level.getBlockState(nonLava));
            level.setBlock(waterInside,Blocks.LAVA.defaultBlockState(),2);NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));check(level.getBlockState(waterInside).is(Blocks.OBSIDIAN),"water handler also executes in original END phase");
            player.removeAllEffects();InfusedPotionEffects.AIR_HITS.put(cow,level.getGameTime()-20);InfusedPotionEffects.FIRE_HITS.put(cow,level.getGameTime()-6000);InfusedPotionEffects.tracked();
            check(InfusedPotionEffects.AIR_HITS.containsKey(cow)&&InfusedPotionEffects.FIRE_HITS.containsKey(cow),"strict expiry retains exact 20 and 6000 boundaries even after attacker loses potion");
            InfusedPotionEffects.AIR_HITS.put(cow,level.getGameTime()-21);InfusedPotionEffects.FIRE_HITS.put(cow,level.getGameTime()-6001);InfusedPotionEffects.tracked();check(!InfusedPotionEffects.AIR_HITS.containsKey(cow)&&!InfusedPotionEffects.FIRE_HITS.containsKey(cow),"strict > expiry removes both on next tick");
            InfusedPotionEffects.AIR_HITS.put(cow,level.getGameTime());InfusedPotionEffects.FIRE_HITS.put(cow,level.getGameTime());cow.clearFire();cow.setDeltaMovement(10,10,10);cow.setPos(pos.getCenter());
            long delay=5-level.getGameTime()%5;
            helper.runAfterDelay(delay,()->{try{
                NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(()->true,level.getServer()));var motion=cow.getDeltaMovement();check(motion.x>=-.5&&motion.x<.5&&motion.y>=0&&motion.y<1&&motion.z>=-.5&&motion.z<.5,"native tick applies original independently randomized air velocity ranges");
                check(cow.getRemainingFireTicks()==120,"native tick refreshes exactly six seconds of fire");cow.setDeltaMovement(10,10,10);cow.clearFire();NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(()->true,level.getServer()));
                check(cow.getDeltaMovement().x<.5&&cow.getRemainingFireTicks()==120,"server END phase independently runs source tracking effects");
                System.out.println("TT_INFUSED_POTIONS_PASS checks="+checks);helper.succeed();
            }finally{cleanup.run();}});
        }catch(Throwable failure){failure.printStackTrace();cleanup.run();throw failure;}
    }
}
