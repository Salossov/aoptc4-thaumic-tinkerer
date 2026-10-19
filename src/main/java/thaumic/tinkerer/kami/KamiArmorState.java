package thaumic.tinkerer.kami;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Source KamiArmorHandler storage, with per-player authority on either server type. */
public final class KamiArmorState {
    private static boolean clientEnabled=true;
    private static CompoundTag data(Player player) {
        var root=player.getPersistentData();
        if(!root.contains("ThaumicTinkerer"))root.put("ThaumicTinkerer",new CompoundTag());
        return root.getCompound("ThaumicTinkerer");
    }
    public static boolean savedEnabled(Player player) {
        var data=data(player);return !data.contains("GemArmor")||data.getBoolean("GemArmor");
    }
    public static void save(Player player,boolean enabled) {data(player).putBoolean("GemArmor",enabled);}
    /** Integrated servers must not read the physical client's shared optimistic flag. */
    public static boolean enabled(Player player) {return player.level().isClientSide()?clientEnabled:savedEnabled(player);}
    public static boolean clientEnabled() {return clientEnabled;}
    /** Local key handling changes this immediately; the packet carries the requested value. */
    public static void setLocal(boolean enabled) {clientEnabled=enabled;}
    public static void receiveClient(Player player,boolean enabled) {save(player,enabled);clientEnabled=enabled;}
    private KamiArmorState() {}
}
