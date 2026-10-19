package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.InfusionInput;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.IFocusContainer;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.pouch.IchorPouchContent;
import thaumic.tinkerer.kami.pouch.IchorPouchItem;
import thaumic.tinkerer.menu.IchorPouchMenu;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class IchorPouchRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    private static ItemStack stack(String id){var key=ResourceLocation.parse(id);check(BuiltInRegistries.ITEM.containsKey(key),"registered item "+id);return new ItemStack(BuiltInRegistries.ITEM.get(key));}
    private static ArrayList<ItemStack> empty(){return new ArrayList<>(java.util.Collections.nCopies(117,ItemStack.EMPTY));}
    private static boolean enabled(GameTestHelper helper){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id("ichor_pouch")),"disabled KAMI omits pouch");helper.succeed();return false;}

    @GameTest(template="summon_space",batch="ichor_pouch",timeoutTicks=100)
    public static void nativeStorageMenuRecipeAndCatalog(GameTestHelper helper) {
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTIchorPouch",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var pouch=IchorPouchContent.POUCH.get().getDefaultInstance();
            check(pouch.getMaxStackSize()==1&&!pouch.isDamageableItem()&&pouch.getRarity()==net.minecraft.world.item.Rarity.EPIC,"source item properties");
            var contents=empty();contents.set(0,new ItemStack(Items.DIAMOND,64));contents.set(12,new ItemStack(Items.GOLD_INGOT,65));contents.get(12).set(DataComponents.MAX_STACK_SIZE,99);contents.set(116,new ItemStack(Items.NETHER_STAR));
            IchorPouchItem.setInventory(pouch,contents);
            var encoded=pouch.save(helper.getLevel().registryAccess());var loaded=ItemStack.parse(helper.getLevel().registryAccess(),encoded).orElseThrow();
            check(ItemStack.isSameItemSameComponents(pouch,loaded),"native item codec retains all117 positions and components");
            var wire=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
            try {ItemStack.STREAM_CODEC.encode(wire,pouch);check(ItemStack.isSameItemSameComponents(pouch,ItemStack.STREAM_CODEC.decode(wire)),"native117-slot stack wire codec");}finally{wire.release();}
            var snapshot=IchorPouchItem.getInventory(pouch);snapshot.get(0).setCount(1);
            check(IchorPouchItem.getInventory(pouch).get(0).getCount()==64,"detached inventory cannot mutate saved component");
            player.getInventory().selected=0;player.getInventory().setItem(0,pouch);var menu=new IchorPouchMenu(1,player.getInventory(),0);
            check(ItemStack.isSameItemSameComponents(pouch,loaded),"opening populated pouch does not overwrite data during hydration");
            check(menu.slots.size()==153&&menu.slots.get(116).x==228&&menu.slots.get(116).y==152&&menu.slots.get(117).x==48&&menu.slots.get(117).y==177&&menu.slots.get(144).y==235,"source grid and player coordinates");
            check(menu.slots.get(116).getItem().is(Items.NETHER_STAR),"last slot hydrates correctly");
            check(menu.slots.get(12).getItem().getCount()==65&&menu.slots.get(12).getMaxStackSize()==64,"existing oversized stack is preserved; new insertions retain source64 limit");
            check(!menu.slots.get(3).mayPlace(pouch)&&!menu.slots.get(3).mayPlace(stack("thaumcraft:focus_pouch"))&&menu.slots.get(3).mayPlace(new ItemStack(Items.SHULKER_BOX)),"only focus-container identity prevents nesting");
            player.getInventory().setItem(1,new ItemStack(Items.EMERALD,10));
            check(menu.quickMoveStack(player,145).is(Items.EMERALD)&&IchorPouchItem.getInventory(pouch).get(1).getCount()==10,"shift transfer saves immediately");
            check(menu.quickMoveStack(player,1).getCount()==10&&IchorPouchItem.getInventory(pouch).get(1).isEmpty(),"reverse shift transfer persists removal");
            menu.slots.get(2).set(new ItemStack(Items.REDSTONE,7));menu.removed(player);
            check(IchorPouchItem.getInventory(pouch).get(2).getCount()==7&&IchorPouchItem.getInventory(pouch).get(12).getCount()==65,"close retains edits without truncating loaded oversized stack");
            IchorPouchContent.POUCH.get().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
            check(player.containerMenu instanceof IchorPouchMenu actual&&actual.getPouchStack()==pouch&&actual.slots.size()==153,"real item use opens the registered117-slot menu");player.closeContainer();
            var recipe=(InfusionRecipe)helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("ichor_pouch")).orElseThrow().value();
            check(recipe.instability()==9&&recipe.aspects().equals(Map.of("vacuos",64,"humanus",32,"pannus",32,"alienis",32,"aer",64)),"source infusion and vis values");
            var ingredients=recipe.components().stream().map(value->value.getItems()[0].copy()).toList();var center=stack("thaumcraft:focus_pouch");
            check(recipe.matches(new InfusionInput(center,ingredients)),"native infusion accepts source components");
            var result=recipe.assemble(new InfusionInput(center,ingredients),helper.getLevel().registryAccess());
            check(result.is(IchorPouchContent.POUCH.get())&&IchorPouchItem.getInventory(result).stream().allMatch(ItemStack::isEmpty),"source infusion outputs fresh empty pouch");
            var node=ResearchApi.definitions(player).get("thaumictinkerer:ichor_pouch");
            check(node.concealed()&&node.column()==13&&node.row()==6&&node.complexity()==5&&node.parents().equals(List.of("thaumictinkerer:ichor_cloth")),"source research and parent");
            var belt=CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("belt").getStacks();check(belt.isItemValid(0,pouch),"actual Curios belt accepts pouch");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="ichor_pouch",timeoutTicks=100)
    public static void nativeClickGuardsOffhandAndExistingNestedData(GameTestHelper helper) {
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTPouchGuards",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var pouch=IchorPouchContent.POUCH.get().getDefaultInstance();player.getInventory().setItem(0,pouch);
            var menu=new IchorPouchMenu(1,player.getInventory(),0);
            check(!menu.slots.get(144).mayPickup(player)&&!menu.slots.get(144).mayPlace(new ItemStack(Items.DIAMOND)),"bound host slot blocks extraction and insertion");
            for(var type:ClickType.values())menu.clicked(144,0,type,player);
            menu.clicked(0,0,ClickType.SWAP,player);
            check(player.getInventory().getItem(0)==pouch&&menu.getCarried().isEmpty()&&IchorPouchItem.getInventory(pouch).stream().allMatch(ItemStack::isEmpty),"all direct click modes and indirect hotbar swap preserve host");
            check(menu.quickMoveStack(player,144).isEmpty(),"host cannot shift transfer");
            var offhand=IchorPouchContent.POUCH.get().getDefaultInstance();player.getInventory().setItem(40,offhand);var offMenu=new IchorPouchMenu(2,player.getInventory(),40);
            offMenu.slots.get(0).set(new ItemStack(Items.DIAMOND,3));offMenu.clicked(0,40,ClickType.SWAP,player);
            check(player.getInventory().getItem(40)==offhand&&offMenu.slots.get(0).getItem().getCount()==3,"offhand host cannot be swapped through another slot");
            var replacement=IchorPouchContent.POUCH.get().getDefaultInstance();player.getInventory().setItem(40,replacement);offMenu.clicked(0,0,ClickType.PICKUP,player);
            check(!offMenu.stillValid(player)&&offMenu.quickMoveStack(player,0).isEmpty()&&offMenu.getCarried().isEmpty(),"stale menu rejects subsequent mutation");offMenu.removed(player);
            check(IchorPouchItem.getInventory(offhand).get(0).getCount()==3&&IchorPouchItem.getInventory(replacement).stream().allMatch(ItemStack::isEmpty),"closing stale menu cannot overwrite replacement or erase old bag");
            // Commands or old saved data can bypass insertion rules. Keep these
            // contents readable, prohibit new insertion, and permit recovery.
            var contents=empty();contents.set(116,stack("thaumcraft:focus_pouch"));contents.set(0,new ItemStack(Items.GOLD_INGOT,5));
            pouch.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(contents));var recovery=new IchorPouchMenu(3,player.getInventory(),0);
            check(recovery.quickMoveStack(player,116).isEmpty(),"source shift-click rejects stored pouch category");
            recovery.clicked(116,0,ClickType.PICKUP,player);check(IchorPouchItem.isPouch(recovery.getCarried())&&IchorPouchItem.getInventory(pouch).get(116).isEmpty()&&IchorPouchItem.getInventory(pouch).get(0).getCount()==5,"ordinary pickup recovers preexisting forbidden item without crashing or losing other slots");recovery.removed(player);
        }
        helper.succeed();
    }

    private record Choice(int id,ItemStack stack){}
    /** Exercise the actual core selector through reflection without introducing
     * a production dependency or a fake replacement implementation. */
    private static List<Choice> choices(ServerPlayer player) throws ReflectiveOperationException {
        var type=Class.forName("thaumcraft.items.WandFocus");var values=(List<?>)type.getMethod("choices",net.minecraft.world.entity.player.Player.class).invoke(null,player);var result=new ArrayList<Choice>();
        for(var value:values)result.add(new Choice((int)value.getClass().getMethod("slot").invoke(value),(ItemStack)value.getClass().getMethod("stack").invoke(value)));
        return result;
    }
    private static boolean select(ServerPlayer player,int id) throws ReflectiveOperationException {
        return (boolean)Class.forName("thaumcraft.items.WandFocus").getMethod("select",ServerPlayer.class,InteractionHand.class,int.class).invoke(null,player,InteractionHand.MAIN_HAND,id);
    }
    @GameTest(template="summon_space",batch="ichor_pouch",timeoutTicks=100)
    public static void nativeWandFocusSelectionInventoryOffhandAndCurios(GameTestHelper helper) throws Exception {
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTPouchFocus",helper.absolutePos(new BlockPos(8,4,8)))) {
            var player=fixture.player;var wand=stack("thaumcraft:casting_wand");player.setItemInHand(InteractionHand.MAIN_HAND,wand);
            var fire=stack("thaumcraft:focus_fire");var frost=stack("thaumcraft:focus_frost");
            var regular=stack("thaumcraft:focus_pouch");var regularApi=(IFocusContainer)regular.getItem();check(regularApi.setFocus(regular,17,fire),"ordinary pouch implements public focus contract");player.getInventory().setItem(1,regular);
            var inventory=IchorPouchContent.POUCH.get().getDefaultInstance();var invContents=empty();invContents.set(0,new ItemStack(Items.DIAMOND,19));invContents.set(116,fire.copy());IchorPouchItem.setInventory(inventory,invContents);player.getInventory().setItem(8,inventory);
            var offhand=IchorPouchContent.POUCH.get().getDefaultInstance();var offContents=empty();offContents.set(116,frost.copy());IchorPouchItem.setInventory(offhand,offContents);player.getInventory().setItem(40,offhand);
            var shock=stack("thaumcraft:focus_shock");var worn=IchorPouchContent.POUCH.get().getDefaultInstance();var wornContents=empty();wornContents.set(115,shock);IchorPouchItem.setInventory(worn,wornContents);
            var belt=CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("belt").getStacks();belt.setStackInSlot(0,worn);
            try {
                var options=choices(player);check(options.size()==4&&options.stream().map(Choice::id).distinct().count()==4,"ordinary18, addon117, offhand and belt addresses never collide");
                var offChoice=options.stream().filter(choice->choice.stack().is(frost.getItem())).findFirst().orElseThrow();check(select(player,offChoice.id())&&WandApi.focus(wand).is(frost.getItem())&&IchorPouchItem.getInventory(offhand).get(116).isEmpty(),"last offhand slot moves focus to wand");
                var inventoryChoice=choices(player).stream().filter(choice->choice.stack().is(fire.getItem())).skip(1).findFirst().orElseThrow();check(select(player,inventoryChoice.id()),"inventory pouch selection commits");
                check(WandApi.focus(wand).is(fire.getItem())&&IchorPouchItem.getInventory(inventory).get(116).is(frost.getItem())&&IchorPouchItem.getInventory(inventory).get(0).getCount()==19,"exchange preserves previous focus and unrelated contents");
                check(select(player,options.getLast().id())&&WandApi.focus(wand).is(shock.getItem())&&IchorPouchItem.getInventory(worn).get(115).is(fire.getItem()),"actual Curios container swap uses the same public contract");
                var api=(IFocusContainer)inventory.getItem();var before=inventory.copy();check(!api.setFocus(inventory,117,fire)&&!api.setFocus(inventory,116,new ItemStack(Items.DIAMOND))&&ItemStack.isSameItemSameComponents(inventory,before),"invalid focus writes are atomic no-ops");
                check(!select(player,Integer.MAX_VALUE)&&!select(player,-2),"malformed selection addresses are rejected");
            } finally {belt.setStackInSlot(0,ItemStack.EMPTY);}
        }
        helper.succeed();
    }
    private IchorPouchRulesTest(){}
}
