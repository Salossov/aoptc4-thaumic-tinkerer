package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;

/** Actual native player damage/teleport, with an in-memory wire instead of a remote client. */
final class NativePlayerFixture implements AutoCloseable {
    final ServerPlayer player;
    final List<Packet<?>> packets=new ArrayList<>();
    private final io.netty.channel.embedded.EmbeddedChannel wire;
    NativePlayerFixture(GameTestHelper helper,String name,BlockPos pos){
        var transport=new Connection(PacketFlow.SERVERBOUND){@Override public void send(Packet<?> packet,PacketSendListener listener,boolean flush){packets.add(packet);}};
        wire=new io.netty.channel.embedded.EmbeddedChannel(transport);
        var channels=net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(transport,ConnectionProtocol.PLAY);
        channels.addAll(net.neoforged.neoforge.network.registration.NetworkRegistry.getCommonPlayChannels(PacketFlow.CLIENTBOUND));
        channels.addAll(Set.of(ResourceLocation.parse("curios:sync_curios"),ResourceLocation.parse("thaumcraft:biome_columns"),ResourceLocation.parse("thaumcraft:knowledge_snapshot"),ResourceLocation.parse("thaumcraft:notify")));
        if(net.neoforged.fml.ModList.get().isLoaded("enderstorage"))channels.addAll(Set.of(ResourceLocation.parse("enderstorage:network"),ResourceLocation.parse("codechickenlib:internal")));
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name);
        player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),profile,ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(helper.getLevel().getServer(),transport,player,CommonListenerCookie.createInitial(profile,false));
        player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        // Exercise vanilla's real expiration, rather than bypassing its spawn-invulnerability check.
        for(int i=0;i<61;i++)player.tick();
    }
    @Override public void close(){player.stopUsingItem();player.getInventory().clearContent();wire.finishAndReleaseAll();}
}
