package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.DimensionalShardDrops;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.summon.BloodSwordItem;
import thaumic.tinkerer.summon.SummonContent;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DimensionalShardRulesTest {
    private static void check(boolean value,String label) {if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="kami_shards",timeoutTicks=100)
    public static void configuredDropsAndBloodSwordComposition(GameTestHelper helper) {
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()) {helper.succeed();return;}
        String oldEnd=TinkererConfig.KAMI_END_DIMENSION.get(),oldNether=TinkererConfig.KAMI_NETHER_DIMENSION.get();
        try(var fixture=new NativePlayerFixture(helper,"TTShardDrops",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var level=helper.getLevel();var source=level.damageSources().playerAttack(player);
            var enderman=EntityType.ENDERMAN.create(level);var pigman=EntityType.ZOMBIFIED_PIGLIN.create(level);var piglin=EntityType.PIGLIN.create(level);
            TinkererConfig.KAMI_END_DIMENSION.set(level.dimension().location().toString());TinkererConfig.KAMI_NETHER_DIMENSION.set(level.dimension().location().toString());
            var calls=new AtomicInteger();
            var shard=DimensionalShardDrops.rollDrop(enderman,source,()->{calls.incrementAndGet();return .03125D;});
            check(shard.is(KamiMaterials.ENDER_SHARD.get())&&shard.getCount()==1&&calls.get()==1,"inclusive enderman probability and one independent RNG draw");
            check(DimensionalShardDrops.rollDrop(enderman,source,()->Math.nextUp(.03125D)).isEmpty(),"first value above 1/32 does not drop");
            check(DimensionalShardDrops.rollDrop(pigman,source,()->.0625D).is(KamiMaterials.NETHER_SHARD.get()),"inclusive pig-zombie probability 1/16");
            check(DimensionalShardDrops.rollDrop(pigman,source,()->Math.nextUp(.0625D)).isEmpty(),"first value above 1/16 does not drop");
            check(DimensionalShardDrops.rollDrop(piglin,source,()->{throw new AssertionError("normal piglin must not consume RNG");}).isEmpty(),"ordinary piglin is not the source pig-zombie class");
            check(DimensionalShardDrops.rollDrop(enderman,level.damageSources().magic(),()->{throw new AssertionError("non-player cause must not consume RNG");}).isEmpty(),"player damage-source owner required");
            TinkererConfig.KAMI_END_DIMENSION.set("thaumictinkerer:verification_other_dimension");
            check(DimensionalShardDrops.rollDrop(enderman,source,()->{throw new AssertionError("wrong dimension must not consume RNG");}).isEmpty(),"configured names compare against actual victim dimension");
            TinkererConfig.KAMI_END_DIMENSION.set(level.dimension().location().toString());
            var sword=SummonContent.BLOOD_SWORD.get().getDefaultInstance();ItemData.update(sword,data->data.putInt("Activated",1));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
            var drops=new ArrayList<ItemEntity>();drops.add(new ItemEntity(level,0,0,0,new ItemStack(Items.ENDER_PEARL)));
            var event=new LivingDropsEvent(enderman,source,drops,false);BloodSwordItem.drops(event);var soulDrops=new ArrayList<>(drops);
            check(soulDrops.stream().anyMatch(drop->drop.getItem().is(SummonContent.MOB_ASPECT.get())),"real blood-sword callback produces mob-aspect drops");
            DimensionalShardDrops.append(event,()->0D);
            check(drops.containsAll(soulDrops)&&drops.stream().anyMatch(drop->drop.getItem().is(KamiMaterials.ENDER_SHARD.get())),"source callback composition retains blood-sword drops and then adds shard");
            check(drops.stream().noneMatch(drop->drop.getItem().is(Items.ENDER_PEARL)),"blood-sword replacement still removes the original vanilla drop");
            enderman.discard();pigman.discard();piglin.discard();drops.forEach(ItemEntity::discard);
        } finally {TinkererConfig.KAMI_END_DIMENSION.set(oldEnd);TinkererConfig.KAMI_NETHER_DIMENSION.set(oldNether);}
        helper.succeed();
    }
}
