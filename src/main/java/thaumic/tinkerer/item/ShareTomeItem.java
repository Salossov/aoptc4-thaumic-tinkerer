package thaumic.tinkerer.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.ResearchSharingApi;
import thaumic.tinkerer.util.ItemData;

/** Release-164 sharing tome. UUID ownership/offline lookup is an approved adaptation. */
public final class ShareTomeItem extends Item {
    public ShareTomeItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    public static String authorName(ItemStack stack){var data=ItemData.read(stack);return data.contains("player",8)?data.getString("player"):"[none]";}
    public static UUID authorId(ItemStack stack){var data=ItemData.read(stack);return data.hasUUID("authorUUID")?data.getUUID("authorUUID"):null;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer serverPlayer){
            var name=authorName(stack);
            if(name.endsWith("[none]")){
                var research=ResearchSharingApi.completed(serverPlayer.server,player.getUUID());
                ItemData.update(stack,tag->{
                    tag.putString("player",player.getGameProfile().getName());tag.putUUID("authorUUID",player.getUUID());
                    var list=new ListTag();for(var key:research)list.add(StringTag.valueOf(key));tag.put("research",list);
                });
                player.displayClientMessage(Component.translatable("message.thaumictinkerer.share_tome.write"),false);
            }else{
                var author=authorId(stack);
                // Legacy name-only data can acquire its UUID while that author is online.
                if(author==null){
                    var online=serverPlayer.server.getPlayerList().getPlayerByName(name);
                    if(online!=null){author=online.getUUID();var resolved=author;ItemData.update(stack,tag->tag.putUUID("authorUUID",resolved));}
                }
                if(author!=null)for(var key:ResearchSharingApi.completed(serverPlayer.server,author))ResearchSharingApi.complete(serverPlayer,key);
                player.displayClientMessage(Component.translatable("message.thaumictinkerer.share_tome.sync"),false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        var name=authorName(stack);lines.add(name.equals("[none]")?Component.translatable("tooltip.thaumictinkerer.share_tome.unassigned"):Component.translatable("tooltip.thaumictinkerer.share_tome.author",name));
    }
}
