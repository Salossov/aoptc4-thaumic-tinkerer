package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.crafting.*;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.world.BlockPlacementApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiMaterials;
import thaumic.tinkerer.kami.blocktalisman.*;
import thaumic.tinkerer.kami.construction.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class KamiConstructionRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    private static boolean enabled(GameTestHelper h){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;h.succeed();return false;}
    private static BlockPos site(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(8,4,8));
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            h.getLevel().setBlock(p.offset(x,-1,z),Blocks.DIRT.defaultBlockState(),2);
            for(int y=0;y<=2;y++)h.getLevel().setBlock(p.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }return p;
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void registeredStateCodecs(GameTestHelper h){
        if(!enabled(h))return;var s=KamiConstructionContent.PLACEMENT_MIRROR.toStack();
        check(s.getRarity()==Rarity.EPIC&&s.getMaxStackSize()==1&&!s.isDamageableItem(),"source item properties");
        var state=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
        PlacementMirrorItem.setState(s,state);PlacementMirrorItem.setSize(s,11);
        var decoded=ItemStack.parse(h.getLevel().registryAccess(),s.save(h.getLevel().registryAccess())).orElseThrow();
        check(PlacementMirrorItem.getState(decoded)==state&&PlacementMirrorItem.getSize(decoded)==11,"registered save codec");
        var buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try{ItemStack.STREAM_CODEC.encode(buf,s);check(ItemStack.isSameItemSameComponents(s,ItemStack.STREAM_CODEC.decode(buf)),"registered stream codec");}finally{buf.release();}
        ItemData.update(decoded,t->t.getCompound("properties").putString("axis","bad"));check(PlacementMirrorItem.getState(decoded)==Blocks.OAK_LOG.defaultBlockState(),"invalid property defaults");
        ItemData.update(decoded,t->{t.remove("block");t.putInt("blockID",1);});check(PlacementMirrorItem.getState(decoded).isAir(),"legacy numeric ID ignored");h.succeed();
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void sourceRecipeResearchAndActualSizeUse(GameTestHelper h){
        if(!enabled(h))return;
        try(var f=new NativePlayerFixture(h,"TTMirrorData",h.absolutePos(new BlockPos(8,4,8)))){
            var recipe=(InfusionRecipe)h.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id("placement_mirror")).orElseThrow().value();
            check(recipe.instability()==12&&recipe.aspects().equals(Map.of("fabrico",65,"vitreus",32,"praecantatio",50,"cognitio",32)),"source infusion values");
            check(recipe.matches(new InfusionInput(BlockTalismanContent.TALISMAN.toStack(),List.of(KamiMaterials.ICHOR.toStack(),new ItemStack(Items.DROPPER),new ItemStack(Items.DIAMOND),new ItemStack(Items.GLASS),new ItemStack(Items.BLAZE_POWDER),KamiMaterials.ICHOR.toStack()))),"source ingredients");
            var node=ResearchApi.definitions(f.player).get("thaumictinkerer:placement_mirror");check(node.parents().equals(List.of("thaumictinkerer:block_talisman")),"registered research");
            var s=KamiConstructionContent.PLACEMENT_MIRROR.toStack();var other=s.copy();PlacementMirrorItem.setBlock(other,Blocks.GLASS);
            f.player.setItemInHand(InteractionHand.OFF_HAND,s);f.player.setShiftKeyDown(true);
            for(int size:new int[]{5,7,9,11,3}){s.getItem().use(h.getLevel(),f.player,InteractionHand.OFF_HAND);check(PlacementMirrorItem.getSize(s)==size,"actual size action");check(PlacementMirrorItem.getSize(other)==3&&PlacementMirrorItem.getBlock(other)==Blocks.GLASS,"other stack independent");}
        }h.succeed();
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void fullCostsAndNativeServiceFields(GameTestHelper h){
        if(!enabled(h))return;var p=site(h);
        try(var f=new NativePlayerFixture(h,"TTMirrorPlans",p.north(3))){
            var states=List.of(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,8),Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES,4).setValue(CandleBlock.LIT,true),Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,4),Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS,4).setValue(TurtleEggBlock.HATCH,2),Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT,4));var costs=new int[]{2,8,4,4,4,4};
            for(int i=0;i<states.size();i++){
                var plan=BlockPlacementApi.plan(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,states.get(i));check(plan!=null&&plan.cost()==costs[i],"full quantity cost "+i);check(h.getLevel().getBlockState(p).isAir(),"read-only plan");
                if(i==2)check(!plan.state().getValue(CandleBlock.LIT),"no copied flame");if(i==4)check(plan.state().getValue(TurtleEggBlock.HATCH)==0,"no copied hatch progress");
            }
        }h.succeed();
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void nativeAssembliesAndFailedDebit(GameTestHelper h){
        if(!enabled(h))return;var p=site(h);var level=h.getLevel();
        try(var f=new NativePlayerFixture(h,"TTMirrorParts",p.north(3))){
            var paid=new AtomicInteger();var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER).setValue(DoorBlock.FACING,Direction.EAST).setValue(DoorBlock.OPEN,true);
            var plan=BlockPlacementApi.plan(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,door);check(plan!=null&&plan.cost()==1&&plan.parts().size()==2,"complete door plan");
            check(!BlockPlacementApi.place(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,door,n->false),"unpaid door rejected");check(level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir(),"both cells rolled back");
            check(BlockPlacementApi.place(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,door,n->{paid.addAndGet(n);return true;}),"native door placed");
            check(paid.get()==1&&level.getBlockState(p).getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&level.getBlockState(p.above()).getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER,"both halves paid once");check(level.getBlockState(p).getValue(DoorBlock.FACING)==Direction.EAST&&!level.getBlockState(p).getValue(DoorBlock.OPEN),"orientation kept, power native");
            level.setBlock(p,Blocks.AIR.defaultBlockState(),2);level.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);
            var bed=Blocks.RED_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD).setValue(BedBlock.FACING,Direction.WEST).setValue(BedBlock.OCCUPIED,true);
            check(BlockPlacementApi.place(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,bed,n->{paid.addAndGet(n);return true;}),"native bed placed");check(level.getBlockState(p).getValue(BedBlock.PART)==BedPart.FOOT&&level.getBlockState(p.west()).getValue(BedBlock.PART)==BedPart.HEAD&&paid.get()==2&&!level.getBlockState(p).getValue(BedBlock.OCCUPIED),"whole unoccupied bed and exact debit");
        }h.succeed();
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void nativeProtectionAndOuterTransaction(GameTestHelper h){
        if(!enabled(h))return;var p=site(h);var level=h.getLevel();
        try(var f=new NativePlayerFixture(h,"TTMirrorDeny",p.north(3))){
            var paid=new AtomicInteger();var seen=new AtomicInteger();Consumer<BlockEvent.EntityPlaceEvent> deny=e->{if(e.getEntity()==f.player){seen.incrementAndGet();check(paid.get()==0,"event before debit");e.setCanceled(true);}};
            var prior=List.copyOf(level.capturedBlockSnapshots);boolean capture=level.captureBlockSnapshots;level.captureBlockSnapshots=true;NeoForge.EVENT_BUS.addListener(deny);
            try{check(!BlockPlacementApi.place(f.player,InteractionHand.MAIN_HAND,p,Direction.UP,Blocks.OAK_DOOR.defaultBlockState(),n->{paid.incrementAndGet();return true;}),"protection cancellation");check(paid.get()==0&&seen.get()==1&&level.getBlockState(p).isAir()&&level.getBlockState(p.above()).isAir(),"cancelled complete assembly is free of debit");check(level.captureBlockSnapshots&&level.capturedBlockSnapshots.equals(prior),"outer snapshots preserved");}finally{NeoForge.EVENT_BUS.unregister(deny);level.captureBlockSnapshots=capture;}
        }h.succeed();
    }
    @GameTest(template="summon_space",batch="kami_construction",timeoutTicks=100)
    public static void longStockAndAtomicDebitPriority(GameTestHelper h)throws Exception{
        if(!enabled(h))return;
        try(var f=new NativePlayerFixture(h,"TTMirrorStock",h.absolutePos(new BlockPos(8,4,8)))){
            var player=f.player;player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);var s=KamiConstructionContent.PLACEMENT_MIRROR.toStack();PlacementMirrorItem.setBlock(s,Blocks.STONE);
            var t=BlockTalismanContent.TALISMAN.toStack();BlockTalismanItem.setBlock(t,Blocks.STONE);BlockTalismanItem.setCount(t,Integer.MAX_VALUE);player.getInventory().setItem(1,new ItemStack(Items.STONE));player.getInventory().setItem(2,t);
            check(PlacementMirrorItem.hasBlocks(s,player,2147483648L),"no reserve overflow");var consume=PlacementMirrorItem.class.getDeclaredMethod("consumeBlocks",ItemStack.class,net.minecraft.world.entity.player.Player.class,int.class);consume.setAccessible(true);
            check((Boolean)consume.invoke(null,s,player,2),"multi-unit debit");check(player.getInventory().getItem(1).isEmpty()&&BlockTalismanItem.getBlockCount(t)==Integer.MAX_VALUE-1,"loose stock precedes talisman");
            BlockTalismanItem.setCount(t,1);player.getInventory().setItem(1,new ItemStack(Items.STONE));check(!(Boolean)consume.invoke(null,s,player,3),"insufficient debit rejected");check(player.getInventory().getItem(1).getCount()==1&&BlockTalismanItem.getBlockCount(t)==1,"all sources preserved on failure");
        }h.succeed();
    }
    private KamiConstructionRulesTest(){}
}
