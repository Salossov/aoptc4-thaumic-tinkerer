package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.item.FocusContent;

/** Native server transport and real wand dispatch. Compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class FlightSmokeRulesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static List<ParticleApi.SmokeSpiral> smoke(NativePlayerFixture fixture)throws ReflectiveOperationException{
        var result=new ArrayList<ParticleApi.SmokeSpiral>();
        for(var packet:fixture.packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload().type().id().equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:smoke_spiral")))
            result.add((ParticleApi.SmokeSpiral)custom.payload().getClass().getMethod("effect").invoke(custom.payload()));
        return result;
    }
    @GameTest(template="empty",batch="flight_smoke",timeoutTicks=100)
    public static void oneSuccessfulLaunchSendsFiveSpiralsToEachNearbyPeer(GameTestHelper helper)throws Exception{
        VerificationReports.install();boolean enabled=TinkererConfig.FLIGHT.get();TinkererConfig.FLIGHT.set(true);
        try(var owner=new DislocationServerRulesTest.Peer(helper,"TTFlightOwner");var observer=new DislocationServerRulesTest.Peer(helper,"TTFlightObserver")){
            var player=owner.player;player.setYRot(0);player.setXRot(-30);var item=FocusContent.FLIGHT.get();var focus=FocusContent.FLIGHT.toStack();var wand=FocusRulesTest.wand(player,focus);
            owner.fixture.packets.clear();observer.fixture.packets.clear();var before=WandApi.stored(wand,Aspects.AIR);var cost=WandApi.quote(wand,player,item.getVisCost(focus));
            wand.getItem().use(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
            check(WandApi.stored(wand,Aspects.AIR)==before-cost.get(Aspects.AIR),"one native launch payment");
            var expectedY=player.getY()-player.getDeltaMovement().y;
            for(var fixture:List.of(owner.fixture,observer.fixture)){
                var effects=smoke(fixture);check(effects.size()==5,"exactly five transported spirals per nearby peer");
                for(var effect:effects)check(effect.x()==player.getX()&&effect.y()==expectedY&&effect.z()==player.getZ()&&effect.radius()==2f&&effect.start()>=0&&effect.start()<360&&effect.minY()==(int)player.getY()&&effect.color()==647935,"all source spiral parameters use post-launch motion");
            }
            owner.fixture.packets.clear();observer.fixture.packets.clear();var motion=player.getDeltaMovement();WandApi.set(wand,Aspects.AIR,0);
            check(!item.cast(player,wand,focus)&&player.getDeltaMovement().equals(motion)&&smoke(owner.fixture).isEmpty()&&smoke(observer.fixture).isEmpty(),"unaffordable launch sends no effects and does not move");
            TinkererConfig.FLIGHT.set(false);WandApi.set(wand,Aspects.AIR,before);
            check(!item.cast(player,wand,focus)&&WandApi.stored(wand,Aspects.AIR)==before&&smoke(owner.fixture).isEmpty(),"disabled launch has no debit or smoke");
        }finally{TinkererConfig.FLIGHT.set(enabled);}
        helper.succeed();
    }
    private FlightSmokeRulesTest(){}
}
