package thaumic.tinkerer.kami.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import thaumic.tinkerer.kami.ToolModeNotification;
import thaumic.tinkerer.util.ItemData;

public final class AdvancedToolMode {
    public static int get(ItemStack stack){return ItemData.integer(stack,"variant");}
    public static void set(ItemStack stack,int mode){ItemData.update(stack,data->data.putInt("variant",mode));}
    public static int next(int mode){return mode==2?0:mode+1;}
    public static String key(String tool,int mode){return "ttmisc.mode."+tool+"."+mode;}
    public static void change(Player player,ItemStack stack,String tool) {
        int changed=next(get(stack));
        if(player.level().isClientSide())NeoForge.EVENT_BUS.post(new ToolModeNotification(key(tool,changed)));
        else set(stack,changed);
    }
    private AdvancedToolMode(){}
}
