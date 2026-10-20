package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.SoulHearts;
import thaumic.tinkerer.kami.SoulHeartsPayload;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class SoulHeartsRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @GameTest(template="summon_space",batch="soul_hearts",timeoutTicks=100)
    public static void nativeDamageOrderPersistenceAndLifecycleSync(GameTestHelper helper) {
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        try(var target=new NativePlayerFixture(helper,"TTSoulHealth",helper.absolutePos(new BlockPos(8,4,8)));
            var control=new NativePlayerFixture(helper,"TTSoulControl",helper.absolutePos(new BlockPos(12,4,8)))) {
            for(var fixture:java.util.List.of(target,control)) {
                fixture.player.setNoGravity(true);fixture.player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
                for(int i=0;i<3;i++)fixture.player.tick();
            }
            SoulHearts.set(target.player,5);SoulHearts.set(control.player,0);
            target.player.hurt(target.player.damageSources().generic(),8.9f);
            control.player.hurt(control.player.damageSources().generic(),3);
            check(SoulHearts.get(target.player)==0,"five soul HP consumed from truncated incoming8");
            check(Math.abs(target.player.getHealth()-control.player.getHealth())<.0001,"soul HP is subtracted before diamond armor, not after it");
            check(target.player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==control.player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue(),"armor wears for remaining3, not incoming8.9");
            target.player.invulnerableTime=0;target.player.hurtTime=0;float health=target.player.getHealth();int wear=target.player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
            target.player.hurt(target.player.damageSources().generic(),.9f);
            check(target.player.getHealth()==health&&target.player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==wear,"source truncation absorbs fractional damage even with zero soul HP; no armor wear");
            SoulHearts.set(target.player,2);target.player.invulnerableTime=0;target.player.hurtTime=0;
            target.player.hurt(target.player.damageSources().fellOutOfWorld(),3);
            check(SoulHearts.get(target.player)==0&&Math.abs(target.player.getHealth()-(health-1))<.0001,"soul HP also absorbs armor-bypassing damage");
            SoulHearts.set(target.player,19);check(SoulHearts.add(target.player,3)&&SoulHearts.get(target.player)==20&&!SoulHearts.add(target.player,1),"source upper cap20 and full result");
            SoulHearts.set(target.player,7);var saved=target.player.saveWithoutId(new CompoundTag());control.player.load(saved);
            check(SoulHearts.get(control.player)==7,"native player persistence retains ThaumicTinkerer.soulHearts");
            target.packets.clear();NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(target.player));
            check(hasSync(target,7),"login synchronizes this player's exact current value");
            SoulHearts.set(target.player,0);target.packets.clear();NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(target.player,false));
            check(hasSync(target,0),"respawn synchronizes reset value rather than stale client health");
            SoulHearts.set(target.player,4);target.packets.clear();NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(target.player,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
            check(hasSync(target,4),"dimension event synchronizes personal value");
        }
        helper.succeed();
    }
    private static boolean hasSync(NativePlayerFixture fixture,int hp) {
        return fixture.packets.stream().anyMatch(packet->packet instanceof ClientboundCustomPayloadPacket wire&&wire.payload() instanceof SoulHeartsPayload payload&&payload.health()==hp);
    }
    private SoulHeartsRulesTest(){}
}
