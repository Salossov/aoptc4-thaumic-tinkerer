package thaumic.tinkerer.verification;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.TinkererFocusItem;
import thaumic.tinkerer.kami.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiArmorStateRulesTest {
    private static void check(boolean value,String message) {if(!value)throw new GameTestAssertException(message);}
    private static IPayloadContext context(ServerPlayer player,PacketFlow flow,List<Runnable> work) {
        return (IPayloadContext)Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),new Class<?>[]{IPayloadContext.class},(object,method,args)->switch(method.getName()) {
            case "player"->player;case "flow"->flow;
            case "enqueueWork"->{work.add((Runnable)args[0]);yield CompletableFuture.completedFuture(null);}
            default->throw new UnsupportedOperationException(method.getName());
        });
    }
    private static void deliver(ServerPlayer player,PacketFlow flow,boolean value,List<Runnable> work) {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try {
            ToggleArmorPayload.CODEC.encode(buffer,new ToggleArmorPayload(value));check(buffer.readableBytes()==1,"source payload is exactly one boolean byte");
            var decoded=ToggleArmorPayload.CODEC.decode(buffer);check(decoded.enabled()==value&&!buffer.isReadable(),"real codec round trip consumes exactly one boolean");
            ToggleArmorPayload.handle(decoded,context(player,flow,work));check(work.size()==1,"network handler queues one main-thread mutation");
        }finally {buffer.release();}
    }
    @SuppressWarnings("unchecked")
    @GameTest(template="summon_space",batch="kami_state",timeoutTicks=100)
    public static void nativeArmorStateAndLogin(GameTestHelper helper)throws Exception {
        VerificationReports.install();var level=helper.getLevel();boolean initialClient=KamiArmorState.clientEnabled();
        check(NativePlayerFixture.registeredChannels(PacketFlow.CLIENTBOUND).contains(ToggleArmorPayload.TYPE.id())&&NativePlayerFixture.registeredChannels(PacketFlow.SERVERBOUND).contains(ToggleArmorPayload.TYPE.id()),"native payload registrar exposes both original packet directions");
        var healField=TinkererFocusItem.class.getDeclaredField("HEALING");healField.setAccessible(true);var healing=(Map<String,Integer>)healField.get(null);
        try(var first=new NativePlayerFixture(helper,"TTArmorOne",helper.absolutePos(new BlockPos(8,4,8)));
            var second=new NativePlayerFixture(helper,"TTArmorTwo",helper.absolutePos(new BlockPos(12,4,8)))) {
            var player=first.player;var other=second.player;var root=player.getPersistentData();root.remove("ThaumicTinkerer");
            check(KamiArmorState.savedEnabled(player)&&root.contains("ThaumicTinkerer",Tag.TAG_COMPOUND)&&!root.getCompound("ThaumicTinkerer").contains("GemArmor"),"source read creates the legacy compound and defaults missing GemArmor to true");
            root.getCompound("ThaumicTinkerer").putInt("unrelated",17);KamiArmorState.save(player,false);
            check(!KamiArmorState.savedEnabled(player)&&root.getCompound("ThaumicTinkerer").getInt("unrelated")==17,"false state preserves other legacy compound data");
            var saved=player.saveWithoutId(new CompoundTag());var restored=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTArmorRestore"),ClientInformation.createDefault());restored.load(saved);
            check(!KamiArmorState.savedEnabled(restored)&&restored.getPersistentData().getCompound("ThaumicTinkerer").getInt("unrelated")==17,"native player NBT save/load retains mode and unrelated data");
            KamiArmorState.save(player,true);KamiArmorState.save(other,true);KamiArmorState.setLocal(true);var work=new ArrayList<Runnable>();
            deliver(player,PacketFlow.SERVERBOUND,false,work);check(KamiArmorState.savedEnabled(player),"receiving on network thread does not mutate state early");work.removeFirst().run();
            check(!KamiArmorState.savedEnabled(player)&&KamiArmorState.savedEnabled(other)&&KamiArmorState.clientEnabled(),"server packet changes only the sender's stored value, without modifying the client flag");
            deliver(player,PacketFlow.SERVERBOUND,false,work);work.removeFirst().run();check(!KamiArmorState.savedEnabled(player),"duplicate false packets remain false instead of blindly toggling");
            deliver(other,PacketFlow.CLIENTBOUND,false,work);work.removeFirst().run();
            check(!KamiArmorState.savedEnabled(other)&&!KamiArmorState.clientEnabled(),"client-bound packet sets both local player NBT and the original global client flag");
            KamiArmorState.setLocal(true);check(!KamiArmorState.savedEnabled(player)&&!KamiArmorState.savedEnabled(other),"local optimistic status change does not rewrite server player data");
            KamiArmorState.save(other,true);
            check(!KamiArmorState.enabled(player)&&KamiArmorState.enabled(other),"logical server resolves each player's own mode even while physical client mode is true");
            KamiArmorState.setLocal(false);
            check(!KamiArmorState.enabled(player)&&KamiArmorState.enabled(other),"changing the physical client flag cannot affect either server player's armor mode");
            String name=player.getGameProfile().getName(),otherName=other.getGameProfile().getName();healing.put(name,9);healing.put(otherName,7);first.packets.clear();
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            var loginPackets=first.packets.stream().filter(packet->packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof ToggleArmorPayload).map(packet->(ToggleArmorPayload)((ClientboundCustomPayloadPacket)packet).payload()).toList();
            check(loginPackets.size()==1&&!loginPackets.getFirst().enabled(),"native login event emits exactly one registered payload with the saved false value");
            check(healing.get(name)==0&&healing.get(otherName)==7,"login resets the source healing progress only for this player");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));check(!healing.containsKey(name)&&healing.get(otherName)==7,"native logout clears only this player's focus cache");
            TinkererFocusItem.playerLogout(otherName);
            System.out.println("TT_KAMI_ARMOR_STATE_NATIVE_PASS player_nbt=true boolean_codec=true queued_sender_mutation=true native_login_payload=true focus_lifecycle=true");helper.succeed();
        }finally {KamiArmorState.setLocal(initialClient);TinkererFocusItem.playerLogout("TTArmorOne");TinkererFocusItem.playerLogout("TTArmorTwo");}
    }
    private KamiArmorStateRulesTest() {}
}
