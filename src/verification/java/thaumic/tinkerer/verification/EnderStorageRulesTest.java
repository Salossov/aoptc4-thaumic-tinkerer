package thaumic.tinkerer.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class EnderStorageRulesTest {
    static final Path SAVED_FOCUS=Path.of(System.getProperty("thaumictinkerer.testReport","build/test-results/gametest.xml")).resolveSibling("enderstorage-bound-focus.snbt");
    static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    static void use(net.minecraft.server.level.ServerPlayer player,ItemStack wand){
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);wand.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
    }
    static List<String> tooltip(ItemStack focus){
        var lines=new ArrayList<Component>();focus.getItem().appendHoverText(focus,net.minecraft.world.item.Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
        return lines.stream().map(Component::getString).toList();
    }
    static ItemStack roundTrip(net.minecraft.server.level.ServerPlayer player,ItemStack focus){
        return ItemStack.parse(player.registryAccess(),focus.save(player.registryAccess())).orElseThrow();
    }
    @GameTest(template="summon_space",batch="enderstorage",timeoutTicks=130)
    public static void nativeEnderStorageFocus(GameTestHelper helper)throws Exception{
        VerificationReports.install();
        var research=AddonRegistry.research().get("thaumictinkerer:focus_ender_chest");
        boolean loaded=net.neoforged.fml.ModList.get().isLoaded("enderstorage");
        check(research.textPages().equals(List.of("page.thaumictinkerer.focus_ender_chest"+(loaded?"_enderstorage":""))),"original ES research page follows actual optional-mod presence");
        if(!loaded){absent(helper);return;}
        EnderStoragePresentChecks.run(helper);
    }
    @GameTest(template="empty",batch="enderstorage_cold",timeoutTicks=6000)
    public static void coldBindingCoalescesCancelsAndOpensOnMiss(GameTestHelper helper)throws Exception{
        VerificationReports.install();
        if(!net.neoforged.fml.ModList.get().isLoaded("enderstorage")){helper.succeed();return;}
        EnderStoragePresentChecks.cold(helper);
    }
    private static void absent(GameTestHelper helper)throws Exception{
        try(var fixture=new NativePlayerFixture(helper,"TTEnderAbsent",helper.absolutePos(new BlockPos(16,5,16)))){
            var player=fixture.player;var item=FocusContent.ENDER_CHEST.get();var focus=new ItemStack(item);
            boolean fromNativeSave=Files.isRegularFile(SAVED_FOCUS);
            if(fromNativeSave)focus=ItemStack.parse(player.registryAccess(),TagParser.parseTag(Files.readString(SAVED_FOCUS))).orElseThrow();
            else ItemData.update(focus,tag->{tag.putBoolean("ender",true);tag.putInt("freq",3803);tag.putString("owner","TTEnderStorage");var nativeTag=new net.minecraft.nbt.CompoundTag();nativeTag.putString("left","red");nativeTag.putString("middle","green");nativeTag.putString("right","blue");nativeTag.putUUID("owner",player.getUUID());tag.put("enderstorage_frequency",nativeTag);});
            var before=ItemData.read(focus);focus=roundTrip(player,focus);
            check(ItemData.read(focus).equals(before),"bound focus deserializes without the EnderStorage registered component or codec");
            check(item.getSortingHelper(focus).equals("ENDERCHEST")&&!tooltip(focus).contains(before.getString("owner")),"absent integration retains original sorting and hides its owner tooltip");
            var wand=FocusRulesTest.wand(player,focus);player.getEnderChestInventory().setItem(0,new ItemStack(Items.DIAMOND,3));
            var cost=WandApi.quote(wand,player,item.getVisCost(focus));
            use(player,wand);
            try{
                check(player.containerMenu instanceof ChestMenu&&player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND),"bound focus falls back to real vanilla ender inventory when EnderStorage is absent");
                check(ItemData.read(WandApi.focus(wand)).equals(before),"fallback opening preserves all optional binding data");
                check(WandApi.stored(wand,Aspects.ORDER)==1000-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.ENTROPY)==1000-cost.get(Aspects.ENTROPY),"fallback charges one original discounted activation");
            }finally{player.closeContainer();player.getEnderChestInventory().clearContent();}
            System.out.println("TT_ENDERSTORAGE_ABSENT_PASS native_saved_binding="+fromNativeSave);helper.succeed();
        }
    }
    private EnderStorageRulesTest(){}
}
