package thaumic.tinkerer.verification;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.kami.blocktalisman.*;
import thaumic.tinkerer.util.ItemData;
import top.theillusivec4.curios.api.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BlockTalismanRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static boolean enabled(GameTestHelper helper){
        VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;
        check(!BuiltInRegistries.ITEM.containsKey(ThaumicTinkerer.id("block_talisman")),"disabled KAMI omits talisman");helper.succeed();return false;
    }
    private static ItemStack stack(int count){var stack=BlockTalismanContent.TALISMAN.toStack();BlockTalismanItem.setBlock(stack,Blocks.COBBLESTONE);BlockTalismanItem.setCount(stack,count);return stack;}
    private static UseOnContext context(GameTestHelper helper,net.minecraft.server.level.ServerPlayer player,ItemStack stack,BlockPos pos){
        return new UseOnContext(helper.getLevel(),player,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atBottomCenterOf(pos.above()),Direction.UP,pos,false));
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    public static void propertiesStorageAndOverflow(GameTestHelper helper){
        if(!enabled(helper))return;
        var stack=BlockTalismanContent.TALISMAN.toStack();
        check(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ThaumicTinkerer.id("block_talisman")),"actual registered item");
        check(stack.getMaxStackSize()==1&&!stack.isDamageableItem()&&stack.getRarity()==Rarity.EPIC,"source item properties");
        ItemData.update(stack,data->{data.putString("other","kept");data.putInt("blockID",1);});
        check(BlockTalismanItem.getBlock(stack)==Blocks.AIR,"old numeric ID must never resolve through modern registry");
        check(BlockTalismanItem.setBlock(stack,Blocks.COBBLESTONE),"bind empty talisman");
        BlockTalismanItem.setCount(stack,Integer.MAX_VALUE-5);
        check(BlockTalismanItem.add(stack,100)==5&&BlockTalismanItem.getBlockCount(stack)==Integer.MAX_VALUE,"exact accepted amount without overflow");
        check(!BlockTalismanItem.setBlock(stack,Blocks.DIRT),"cannot rebind nonempty reserve");
        check(BlockTalismanItem.remove(stack,Integer.MAX_VALUE)==Integer.MAX_VALUE,"exact removal");
        BlockTalismanItem.setCount(stack,-1);check(BlockTalismanItem.getBlockCount(stack)==0,"nonnegative stored count");
        BlockTalismanItem.setCount(stack,117);BlockTalismanItem.setEnabled(stack,true);
        var copy=stack.copy();BlockTalismanItem.remove(copy,17);check(BlockTalismanItem.getBlockCount(stack)==117,"independent component copy");
        var loaded=ItemStack.parse(helper.getLevel().registryAccess(),stack.save(helper.getLevel().registryAccess())).orElseThrow();
        check(ItemStack.matches(stack,loaded)&&ItemData.read(loaded).getString("other").equals("kept"),"native NBT roundtrip retains unrelated data");
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
        try{ItemStack.STREAM_CODEC.encode(buffer,stack);check(ItemStack.matches(stack,ItemStack.STREAM_CODEC.decode(buffer)),"native wire roundtrip");}finally{buffer.release();}
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    public static void modeToggle(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTTalismanMode",helper.absolutePos(new BlockPos(8,4,6)))){
            var player=fixture.player;var stack=BlockTalismanContent.TALISMAN.toStack();player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.setShiftKeyDown(true);
            check(stack.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND).getResult()==InteractionResult.PASS,"unbound ignores toggle");
            BlockTalismanItem.setBlock(stack,Blocks.COBBLESTONE);
            for(boolean state:new boolean[]{true,false}){check(stack.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"bound toggle handled");check(BlockTalismanItem.isEnabled(stack)==state,"mode toggles");}
        }helper.succeed();
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    public static void containerDepositAndNativePlacementCancellation(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTTalismanPlace",helper.absolutePos(new BlockPos(8,4,6)))){
            var player=fixture.player;var stack=stack(100);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var chestPos=helper.absolutePos(new BlockPos(8,5,8));var level=helper.getLevel();
            level.setBlockAndUpdate(chestPos,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)level.getBlockEntity(chestPos);check(chest!=null,"native chest");chest.clearContent();
            var context=context(helper,player,stack,chestPos);
            check(stack.useOn(context).consumesAction(),"deposit handled through native ItemStack action");
            check(chest.getItem(0).is(Items.COBBLESTONE)&&chest.getItem(0).getCount()==64&&chest.getItem(1).getCount()==36,"source deposits all slots in one click:64+36");
            check(BlockTalismanItem.getBlockCount(stack)==0,"all100 actually deposited");
            check(stack.useOn(context).consumesAction()&&BlockTalismanItem.getBlock(stack)==Blocks.CHEST,"source empty reserve rebinds before container handling");
            check(BlockTalismanItem.setBlock(stack,Blocks.COBBLESTONE),"rebind empty for partial deposit");BlockTalismanItem.setCount(stack,100);
            for(int i=0;i<chest.getContainerSize();i++)chest.setItem(i,new ItemStack(Items.DIRT,64));chest.setItem(0,new ItemStack(Items.COBBLESTONE,60));
            stack.useOn(context);check(chest.getItem(0).getCount()==64&&BlockTalismanItem.getBlockCount(stack)==96,"only actual free space consumed");
            var base=helper.absolutePos(new BlockPos(8,4,8));var placed=base.above();level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(placed,Blocks.AIR.defaultBlockState());
            BlockTalismanItem.setCount(stack,5);var placeContext=context(helper,player,stack,base);boolean[] observed={false};
            Consumer<BlockEvent.EntityPlaceEvent> deny=event->{if(event.getEntity()==player&&event.getPos().equals(placed)){observed[0]=true;check(BlockTalismanItem.getBlockCount(stack)==5,"native protection sees pre-action reserve");event.setCanceled(true);}};
            NeoForge.EVENT_BUS.addListener(deny);
            try{check(stack.useOn(placeContext)==InteractionResult.FAIL,"placement protection cancels native transaction");}finally{NeoForge.EVENT_BUS.unregister(deny);}
            check(observed[0]&&level.getBlockState(placed).isAir()&&BlockTalismanItem.getBlockCount(stack)==5,"world and reserve rolled back together");
            check(stack.useOn(placeContext).consumesAction()&&level.getBlockState(placed).is(Blocks.COBBLESTONE)&&BlockTalismanItem.getBlockCount(stack)==4,"accepted native placement consumes exactly one");
            check(!stack.useOn(placeContext).consumesAction()&&BlockTalismanItem.getBlockCount(stack)==4,"occupied target spends nothing");
        }helper.succeed();
    }
    private static InteractionResult deposit(GameTestHelper helper,ItemStack stack,Object inventory,Direction side){
        try{
            var item=(BlockTalismanItem)stack.getItem();boolean handler=inventory instanceof IItemHandler;
            var method=handler?BlockTalismanItem.class.getDeclaredMethod("depositIntoHandler",Level.class,ItemStack.class,Block.class,IItemHandler.class)
                    :BlockTalismanItem.class.getDeclaredMethod("depositIntoContainer",Level.class,ItemStack.class,Block.class,Container.class,Direction.class);
            method.setAccessible(true);
            return (InteractionResult)(handler?method.invoke(item,helper.getLevel(),stack,Blocks.COBBLESTONE,inventory):method.invoke(item,helper.getLevel(),stack,Blocks.COBBLESTONE,inventory,side));
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Production inventory insertion",error);}
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    public static void rejectingSidedAndPartialInventories(GameTestHelper helper){
        if(!enabled(helper))return;
        var stack=stack(100);
        var denied=new SimpleContainer(2){@Override public boolean canPlaceItem(int slot,ItemStack offered){return false;}};denied.setItem(0,new ItemStack(Items.COBBLESTONE,4));
        check(!deposit(helper,stack,denied,Direction.UP).consumesAction()&&denied.getItem(0).getCount()==4&&BlockTalismanItem.getBlockCount(stack)==100,"both occupied and empty invalid slots reject");
        class Sided extends SimpleContainer implements WorldlyContainer {
            Sided(){super(1);setItem(0,new ItemStack(Items.COBBLESTONE,4));}
            public int[] getSlotsForFace(Direction side){return new int[]{0};}
            public boolean canPlaceItemThroughFace(int slot,ItemStack item,Direction side){return side==Direction.UP;}
            public boolean canTakeItemThroughFace(int slot,ItemStack item,Direction side){return false;}
        }
        var sided=new Sided();deposit(helper,stack,sided,Direction.NORTH);check(BlockTalismanItem.getBlockCount(stack)==100,"wrong side refuses occupied slot");
        deposit(helper,stack,sided,Direction.UP);check(sided.getItem(0).getCount()==64&&BlockTalismanItem.getBlockCount(stack)==40,"correct side inserts60");
        BlockTalismanItem.setCount(stack,100);
        var clamp=new SimpleContainer(1){@Override public void setItem(int slot,ItemStack offered){super.setItem(slot,offered.copyWithCount(Math.min(2,offered.getCount())));}};
        deposit(helper,stack,clamp,Direction.UP);check(clamp.getItem(0).getCount()==2&&BlockTalismanItem.getBlockCount(stack)==98,"clamping container consumes only2 accepted");
        var refusing=new SimpleContainer(1){@Override public void setItem(int slot,ItemStack offered){}};deposit(helper,stack,refusing,Direction.UP);check(BlockTalismanItem.getBlockCount(stack)==98,"refusing setItem spends nothing");
        var batchSensitive=new SimpleContainer(1){@Override public boolean canPlaceItem(int slot,ItemStack offered){return offered.getCount()==64;}};
        deposit(helper,stack,batchSensitive,Direction.UP);check(batchSensitive.getItem(0).getCount()==64&&BlockTalismanItem.getBlockCount(stack)==34,"source validates actual empty-slot batch");
        BlockTalismanItem.setCount(stack,100);
        var handler=new ItemStackHandler(2){@Override public int getSlotLimit(int slot){return 7;}};
        deposit(helper,stack,handler,Direction.UP);check(handler.getStackInSlot(0).getCount()==7&&handler.getStackInSlot(1).getCount()==7&&BlockTalismanItem.getBlockCount(stack)==86,"handler limits honored across all slots");
        var changed=new ItemStackHandler(1){@Override public ItemStack insertItem(int slot,ItemStack offered,boolean simulate){return simulate?ItemStack.EMPTY:offered;}};
        deposit(helper,stack,changed,Direction.UP);check(BlockTalismanItem.getBlockCount(stack)==86,"simulation acceptance cannot consume actual refusal");
        var componentSlot=new ItemStackHandler(1);var existing=new ItemStack(Items.COBBLESTONE,4);ItemData.update(existing,data->data.putString("other","retained"));componentSlot.setStackInSlot(0,existing);BlockTalismanItem.setCount(stack,10);
        deposit(helper,stack,componentSlot,Direction.UP);check(componentSlot.getStackInSlot(0).getCount()==14&&ItemData.read(componentSlot.getStackInSlot(0)).getString("other").equals("retained")&&BlockTalismanItem.getBlockCount(stack)==0,"original matching ignores unrelated data while preserving occupied stack components");helper.succeed();
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    public static void realCuriosRingPriorityOverflowAndCursor(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTTalismanRing",helper.absolutePos(new BlockPos(8,4,6)))){
            var player=fixture.player;var inv=player.getInventory();inv.selected=0;
            var ring=CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("ring").getStacks();
            var initial=stack(0);BlockTalismanItem.setEnabled(initial,true);check(ring.isItemValid(0,initial),"registered Curios ring tag");ring.setStackInSlot(0,initial);var stack=ring.getStackInSlot(0);
            Runnable tick=()->CuriosApi.getCurio(stack).orElseThrow().curioTick(new SlotContext("ring",player,0,false,true));
            try{
                inv.setItem(0,new ItemStack(Items.COBBLESTONE,32));inv.setItem(10,new ItemStack(Items.COBBLESTONE,64));inv.setItem(11,new ItemStack(Items.COBBLESTONE,64));player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,10));tick.run();
                check(inv.getItem(0).getCount()==64&&inv.getItem(10).isEmpty()&&inv.getItem(11).isEmpty()&&player.getOffhandItem().isEmpty()&&BlockTalismanItem.getBlockCount(stack)==106,"mainhand priority and exact conservation");
                inv.clearContent();BlockTalismanItem.setCount(stack,0);inv.setItem(0,new ItemStack(Items.DIAMOND));player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,12));inv.setItem(7,new ItemStack(Items.COBBLESTONE,60));inv.setItem(16,new ItemStack(Items.COBBLESTONE,6));tick.run();
                check(player.getOffhandItem().getCount()==64&&inv.getItem(0).is(Items.DIAMOND)&&BlockTalismanItem.getBlockCount(stack)==14,"offhand beats larger hotbar stack");
                inv.clearContent();BlockTalismanItem.setCount(stack,0);inv.setItem(0,new ItemStack(Items.DIAMOND));inv.setItem(1,new ItemStack(Items.COBBLESTONE,4));inv.setItem(2,new ItemStack(Items.COBBLESTONE,40));inv.setItem(3,new ItemStack(Items.COBBLESTONE,40));inv.setItem(10,new ItemStack(Items.COBBLESTONE,64));tick.run();
                check(inv.getItem(2).getCount()==64&&inv.getItem(3).isEmpty()&&BlockTalismanItem.getBlockCount(stack)==84,"largest hotbar wins with first-slot tie break");
                inv.clearContent();BlockTalismanItem.setCount(stack,0);inv.setItem(10,new ItemStack(Items.COBBLESTONE,4));inv.setItem(11,new ItemStack(Items.COBBLESTONE,40));inv.setItem(12,new ItemStack(Items.COBBLESTONE,40));tick.run();
                check(inv.getItem(11).getCount()==64&&BlockTalismanItem.getBlockCount(stack)==20,"largest main inventory stack wins");
                inv.clearContent();inv.setItem(0,new ItemStack(Items.COBBLESTONE,64));inv.setItem(10,new ItemStack(Items.COBBLESTONE,10));BlockTalismanItem.setCount(stack,Integer.MAX_VALUE-5);tick.run();tick.run();
                check(BlockTalismanItem.getBlockCount(stack)==Integer.MAX_VALUE&&inv.getItem(10).getCount()==5,"overflow remainder remains in source after repeated ticks");
                inv.clearContent();BlockTalismanItem.setCount(stack,64);player.containerMenu.setCarried(new ItemStack(Items.DIAMOND));tick.run();check(inv.items.stream().allMatch(ItemStack::isEmpty)&&BlockTalismanItem.getBlockCount(stack)==64,"different cursor prevents dispensing");
                player.containerMenu.setCarried(new ItemStack(Items.COBBLESTONE));tick.run();check(inv.getItem(0).getCount()==64&&BlockTalismanItem.getBlockCount(stack)==0&&player.containerMenu.getCarried().getCount()==1,"matching cursor permits dispense without mutation");
                inv.clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);BlockTalismanItem.setCount(stack,20);tick.run();check(inv.getItem(0).getCount()==20&&BlockTalismanItem.getBlockCount(stack)==0,"empty cursor partial dispense");
                BlockTalismanItem.setEnabled(stack,false);inv.setItem(10,new ItemStack(Items.COBBLESTONE,10));tick.run();check(inv.getItem(10).getCount()==10,"inactive ring leaves inventory untouched");
            }finally{ring.setStackInSlot(0,ItemStack.EMPTY);player.containerMenu.setCarried(ItemStack.EMPTY);}
        }helper.succeed();
    }
    @GameTest(template="summon_space",batch="block_talisman",timeoutTicks=100)
    @SuppressWarnings("unchecked")
    public static void stagedTabletOwnerSurvivesHandResetAndCancelsOnSlotReplacement(GameTestHelper helper)throws ReflectiveOperationException {
        if(!enabled(helper))return;
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(8,4,8));
        var block=BuiltInRegistries.BLOCK.get(ThaumicTinkerer.id("animation_tablet"));check(block!=Blocks.AIR,"registered tablet block");level.setBlockAndUpdate(pos,block.defaultBlockState());
        var tablet=(AnimationTabletBlockEntity)level.getBlockEntity(pos);check(tablet!=null,"actual tablet owner");tablet.leftClick=false;var stack=stack(5);tablet.setItem(0,stack);var actor=tablet.player();actor.prepare();actor.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var target=pos.above();level.setBlockAndUpdate(target,Blocks.STONE.defaultBlockState());
        var jobClass=Class.forName(TalismanActions.class.getName()+"$Job");var constructor=jobClass.getDeclaredConstructor(UseOnContext.class,Collection.class);constructor.setAccessible(true);
        // Only stage a known hot column. This test never submits cold generation
        // and does not claim to cover natural asynchronous completion/replay.
        var job=constructor.newInstance(context(helper,actor,stack,target),List.of(new ChunkPos(pos)));
        var field=TalismanActions.class.getDeclaredField("PENDING");field.setAccessible(true);var pending=(Map<net.minecraft.server.level.ServerPlayer,Object>)field.get(null);
        var valid=jobClass.getDeclaredMethod("valid");valid.setAccessible(true);var close=jobClass.getDeclaredMethod("close");close.setAccessible(true);pending.put(actor,job);
        try{
            actor.prepare();check(actor.getMainHandItem().isEmpty()&&(boolean)valid.invoke(job),"tablet slot remains owner after transient hand reset");
            int rotation=tablet.rotation();tablet.rotation((rotation+1)%4);check(!(boolean)valid.invoke(job),"rotation change invalidates action");tablet.rotation(rotation);
            tablet.leftClick=true;check(!(boolean)valid.invoke(job),"mode change invalidates action");tablet.leftClick=false;
            tablet.redstone=!tablet.redstone;check(!(boolean)valid.invoke(job),"redstone mode change invalidates action");tablet.redstone=!tablet.redstone;
            tablet.setItem(0,stack.copy());TalismanActions.tick(new LevelTickEvent.Post(()->true,level));check(!TalismanActions.hasPending(actor),"equal copy in different owner slot identity cancels action");
            var request=jobClass.getDeclaredField("request");request.setAccessible(true);check(((thaumcraft.api.world.ChunkPreparationApi.Request)request.get(job)).closed(),"cancellation closes chunk preparation request");
        }finally{close.invoke(job);tablet.setItem(0,ItemStack.EMPTY);}
        helper.succeed();
    }
    private BlockTalismanRulesTest(){}
}
