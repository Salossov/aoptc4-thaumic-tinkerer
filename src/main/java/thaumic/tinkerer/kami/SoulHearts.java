package thaumic.tinkerer.kami;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import thaumcraft.api.entities.LivingPreArmorDamageEvent;

/** Original separate soul health, including integer truncation before armor. */
public final class SoulHearts {
    private static int clientHealth;
    private static CompoundTag data(Player player) {
        var root=player.getPersistentData();
        if(!root.contains("ThaumicTinkerer"))root.put("ThaumicTinkerer",new CompoundTag());
        return root.getCompound("ThaumicTinkerer");
    }
    public static int get(Player player){return data(player).getInt("soulHearts");}
    public static void set(Player player,int hp){data(player).putInt("soulHearts",hp);}
    public static boolean add(Player player,int hp) {
        int current=get(player);if(current>=20)return false;
        set(player,Math.min(20,current+hp));return true;
    }
    public static int remove(Player player,int hp) {
        int remaining=get(player)-hp;set(player,Math.max(0,remaining));return Math.max(0,-remaining);
    }
    public static void addHeart(Player player){add(player,1);sync(player);}
    public static int clientHealth(){return clientHealth;}
    public static void receiveClient(int hp){clientHealth=hp;}
    public static void sync(Player player) {
        if(player instanceof ServerPlayer server&&server.connection!=null)PacketDistributor.sendToPlayer(server,new SoulHeartsPayload(get(player)));
    }
    public static void damage(LivingPreArmorDamageEvent event) {
        if(event.entity() instanceof Player player&&!player.level().isClientSide()&&event.damage()>0) {
            // Even with zero soul HP, the source truncates positive fractional damage.
            event.damage(remove(player,(int)event.damage()));sync(player);
        }
    }
    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent event)->event.registrar("1").playToClient(SoulHeartsPayload.TYPE,SoulHeartsPayload.CODEC,SoulHeartsPayload::handle));
        LivingPreArmorDamageEvent.BUS.addListener(SoulHearts::damage);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event)->sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event)->sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event)->sync(event.getEntity()));
    }
    private SoulHearts(){}
}
