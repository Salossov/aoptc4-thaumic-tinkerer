package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.BlockReplacementApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;

/** Actual native API, container onRemove and XP merge paths; compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ReplacementApiRulesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static BlockPos target(GameTestHelper helper){return helper.absolutePos(new BlockPos(3,4,3));}
    private static void prepared(GameTestHelper helper,String name,Consumer<DislocationServerRulesTest.Peer> scenario)throws Exception{
        VerificationReports.install();var peer=new DislocationServerRulesTest.Peer(helper,name);var pos=target(helper);var columns=new ArrayList<ChunkPos>();
        for(int x=(pos.getX()-3)>>4;x<=(pos.getX()+3)>>4;x++)for(int z=(pos.getZ()-3)>>4;z<=(pos.getZ()+3)>>4;z++)columns.add(new ChunkPos(x,z));
        prepared(helper,peer,columns,scenario);
    }
    static void prepared(GameTestHelper helper,String name,List<ChunkPos> columns,Consumer<DislocationServerRulesTest.Peer> scenario)throws Exception{
        VerificationReports.install();prepared(helper,new DislocationServerRulesTest.Peer(helper,name),columns,scenario);
    }
    private static void prepared(GameTestHelper helper,DislocationServerRulesTest.Peer peer,List<ChunkPos> columns,Consumer<DislocationServerRulesTest.Peer> scenario){
        var request=ChunkPreparationApi.prepare(peer.player,columns);
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;request.close();peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(new Runnable(){boolean done;public void run(){if(done||!request.advance(2))return;done=true;try{scenario.accept(peer);helper.succeed();}finally{cleanup.run();}}});
    }
    private static List<ItemEntity> drops(GameTestHelper helper,BlockPos pos){return helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));}
    private static ChestBlockEntity chest(GameTestHelper helper,BlockPos pos){helper.getLevel().setBlock(pos,Blocks.CHEST.defaultBlockState(),18);var entity=(ChestBlockEntity)helper.getLevel().getBlockEntity(pos);entity.setItem(0,new ItemStack(Items.DIAMOND,17));return entity;}

    @GameTest(template="empty",batch="replacement_api",timeoutTicks=1200)
    public static void canceledProtectionKeepsInventoryAndPublishesNoDrops(GameTestHelper helper)throws Exception{
        prepared(helper,"TTReplaceProtect",peer->{
            var level=helper.getLevel();var pos=target(helper);var original=chest(helper,pos);var before=original.saveWithFullMetadata(level.registryAccess());var wand=FocusRulesTest.wand(peer.player,thaumic.tinkerer.item.FocusContent.SMELT.toStack());int[] payments={0};
            Consumer<BlockEvent.BreakEvent> denyBreak=event->{if(event.getPlayer()==peer.player&&event.getPos().equals(pos))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(denyBreak);
            try{check(!BlockReplacementApi.replace(peer.player,wand,pos,Direction.NORTH,level.getBlockState(pos),Blocks.STONE.defaultBlockState(),()->{payments[0]++;return true;}),"break protection rejects before mutation");}finally{NeoForge.EVENT_BUS.unregister(denyBreak);}
            check(level.getBlockEntity(pos)==original&&payments[0]==0,"break rejection retains live original and no debit");
            Consumer<BlockEvent.EntityPlaceEvent> denyPlace=event->{if(event.getEntity()==peer.player&&event.getPos().equals(pos)){check(drops(helper,pos).isEmpty(),"provisional container drops have not entered world");event.setCanceled(true);}};NeoForge.EVENT_BUS.addListener(denyPlace);
            try{check(!BlockReplacementApi.replace(peer.player,wand,pos,Direction.NORTH,level.getBlockState(pos),Blocks.STONE.defaultBlockState(),()->{payments[0]++;return true;}),"place protection rejects provisional mutation");}finally{NeoForge.EVENT_BUS.unregister(denyPlace);}
            check(level.getBlockState(pos).is(Blocks.CHEST)&&level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()).equals(before),"old state and full inventory NBT restored");
            check(payments[0]==0&&drops(helper,pos).isEmpty(),"cancellation emits no duplicate inventory or debit");((ChestBlockEntity)level.getBlockEntity(pos)).clearContent();
        });
    }

    @GameTest(template="empty",batch="replacement_api",timeoutTicks=1200)
    public static void failedPaymentAndUnsupportedOutputRestoreOuterCapture(GameTestHelper helper)throws Exception{
        prepared(helper,"TTReplacePayment",peer->{
            var level=helper.getLevel();var pos=target(helper);var original=chest(helper,pos);var before=original.saveWithFullMetadata(level.registryAccess());var wand=FocusRulesTest.wand(peer.player,thaumic.tinkerer.item.FocusContent.SMELT.toStack());int[] payments={0};
            boolean capture=level.captureBlockSnapshots;var outer=List.copyOf(level.capturedBlockSnapshots);var marker=BlockSnapshot.create(level.dimension(),level,pos,18);level.capturedBlockSnapshots.add(marker);level.captureBlockSnapshots=true;
            try{
                check(!BlockReplacementApi.replace(peer.player,wand,pos,Direction.UP,level.getBlockState(pos),Blocks.STONE.defaultBlockState(),()->{payments[0]++;return false;}),"failed atomic payment rolls replacement back");
                check(payments[0]==1&&level.captureBlockSnapshots&&level.capturedBlockSnapshots.contains(marker),"one payment attempt and preserved enclosing snapshot scope");
                check(level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()).equals(before)&&drops(helper,pos).isEmpty(),"failed payment retains inventory without provisional drops");
                level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),18);payments[0]=0;
                check(!BlockReplacementApi.replace(peer.player,wand,pos,Direction.UP,level.getBlockState(pos),Blocks.TORCH.defaultBlockState(),()->{payments[0]++;return true;}),"unsupported output rejected");
                check(payments[0]==0&&level.getBlockState(pos).is(Blocks.CHEST)&&drops(helper,pos).isEmpty(),"support failure precedes debit and restores container");
                ((ChestBlockEntity)level.getBlockEntity(pos)).clearContent();
            }finally{level.captureBlockSnapshots=capture;level.capturedBlockSnapshots.clear();level.capturedBlockSnapshots.addAll(outer);}
        });
    }

    private static int count(ExperienceOrb orb){return orb.saveWithoutId(new CompoundTag()).getInt("Count");}
    @GameTest(template="empty",batch="replacement_api",timeoutTicks=1200)
    public static void furnaceExperienceMergeAndInventoryPublishOnlyAfterCommit(GameTestHelper helper)throws Exception{
        prepared(helper,"TTReplaceFurnace",peer->{
            var level=helper.getLevel();var pos=target(helper);level.setBlock(pos,Blocks.FURNACE.defaultBlockState(),18);var furnace=(FurnaceBlockEntity)level.getBlockEntity(pos);furnace.setItem(0,new ItemStack(Items.RAW_IRON,11));
            var recipe=level.getRecipeManager().byKey(ResourceLocation.parse("minecraft:iron_ingot_from_smelting_raw_iron")).orElseThrow();for(int i=0;i<1000;i++)furnace.setRecipeUsed(recipe);
            var before=furnace.saveWithFullMetadata(level.registryAccess());var wand=FocusRulesTest.wand(peer.player,thaumic.tinkerer.item.FocusContent.SMELT.toStack());var orbs=new ArrayList<ExperienceOrb>();
            // One orb for every entity-id residue makes the first 617-XP grant
            // merge regardless of the native random selector's 0..39 outcome.
            for(int i=0;i<40;i++){var orb=new ExperienceOrb(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,617);level.addFreshEntity(orb);orbs.add(orb);}
            int total=orbs.stream().mapToInt(ReplacementApiRulesTest::count).sum();int[] payments={0};
            Consumer<BlockEvent.EntityPlaceEvent> deny=event->{if(event.getEntity()==peer.player&&event.getPos().equals(pos)){check(orbs.stream().mapToInt(ReplacementApiRulesTest::count).sum()==total,"existing orb counts unchanged before protection decision");event.setCanceled(true);}};NeoForge.EVENT_BUS.addListener(deny);
            try{check(!BlockReplacementApi.replace(peer.player,wand,pos,Direction.UP,level.getBlockState(pos),Blocks.STONE.defaultBlockState(),()->{payments[0]++;return true;}),"furnace replacement canceled");}finally{NeoForge.EVENT_BUS.unregister(deny);}
            check(orbs.stream().mapToInt(ReplacementApiRulesTest::count).sum()==total&&level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()).equals(before),"XP merge and recipes-used NBT restored on cancellation");
            check(drops(helper,pos).isEmpty()&&payments[0]==0,"canceled furnace produces no inventory/debit");
            check(BlockReplacementApi.replace(peer.player,wand,pos,Direction.UP,level.getBlockState(pos),Blocks.STONE.defaultBlockState(),()->{payments[0]++;return true;}),"allowed native furnace transformation succeeds");
            check(payments[0]==1&&level.getBlockState(pos).is(Blocks.STONE),"successful replacement pays once");
            check(orbs.stream().mapToInt(ReplacementApiRulesTest::count).sum()==total+1,"existing 617-XP orb receives precisely one committed merge");
            check(drops(helper,pos).stream().mapToInt(e->e.getItem().is(Items.RAW_IRON)?e.getItem().getCount():0).sum()==11,"native inventory emitted exactly once after commit");
            for(var orb:level.getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(2)))orb.discard();for(var drop:drops(helper,pos))drop.discard();
        });
    }
    private ReplacementApiRulesTest(){}
}
