package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.item.dislocation.*;
import thaumic.tinkerer.util.ItemData;

/** Native world/packet/protection scenarios; compilation is not an execution result. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DislocationServerRulesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static final class Peer implements AutoCloseable {
        final NativePlayerFixture fixture;final ServerPlayer player;
        final Map<UUID,ServerPlayer> index;final List<ServerPlayer> players;
        @SuppressWarnings("unchecked") Peer(GameTestHelper helper,String name)throws ReflectiveOperationException{
            fixture=new NativePlayerFixture(helper,name,base(helper).north(2));player=fixture.player;
            var list=helper.getLevel().getServer().getPlayerList();
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            index=(Map<UUID,ServerPlayer>)field.get(list);check(index.putIfAbsent(player.getUUID(),player)==null,"unique native owner");
            field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);
            players=(List<ServerPlayer>)field.get(list);players.add(player);helper.getLevel().addNewPlayer(player);
        }
        List<ParticleApi.Effect> effects()throws ReflectiveOperationException{
            var result=new ArrayList<ParticleApi.Effect>();
            for(var packet:fixture.packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload().type().id().equals(net.minecraft.resources.ResourceLocation.parse("thaumcraft:addon_particle")))
                result.add((ParticleApi.Effect)custom.payload().getClass().getMethod("effect").invoke(custom.payload()));
            return result;
        }
        @Override public void close(){player.discard();players.remove(player);index.remove(player.getUUID(),player);fixture.close();}
    }
    private static BlockPos base(GameTestHelper helper){var anchor=helper.absolutePos(BlockPos.ZERO);return new BlockPos((anchor.getX()>>4)*16+4,anchor.getY()+70,(anchor.getZ()>>4)*16+8);}
    private static ItemStack wand(ServerPlayer player){
        var wand=FocusRulesTest.wand(player,FocusContent.DISLOCATION.toStack());WandApi.parts(wand,new WandParts("silverwood","thaumium"));
        for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,WandApi.capacity(wand));return wand;
    }
    private static DislocationRay.Trace ray(BlockPos pos,Direction face){var location=Vec3.atCenterOf(pos);return new DislocationRay.Trace(location.relative(face,2),location,new BlockHitResult(location,face,pos,false),true,null);}
    private static boolean use(ServerPlayer player,ItemStack wand,BlockPos pos,Direction face){return DislocationTransfer.apply(player,wand,WandApi.focus(wand),ray(pos,face));}
    private static void assertBursts(Peer peer)throws ReflectiveOperationException{
        var effects=peer.effects();check(effects.size()==8,"exactly eight explicitly networked bursts per peer");
        for(var effect:effects)check(effect.kind()==ParticleApi.Kind.BURST&&effect.size()==.2f,"source burst kind/size");peer.fixture.packets.clear();
    }

    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void strictSavedStateRejectsUnknownAndMismatchedParts(GameTestHelper helper){
        VerificationReports.install();var focus=FocusContent.DISLOCATION.toStack();
        var state=Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.Z);
        new DislocationCapture(List.of(new DislocationCapture.Part(BlockPos.ZERO,state,new CompoundTag()))).store(focus);
        var saved=focus.save(helper.getLevel().registryAccess());focus=ItemStack.parse(helper.getLevel().registryAccess(),saved).orElseThrow();
        check(DislocationCapture.read(focus).orElseThrow().root().state()==state,"real item codec keeps selected state");
        var valid=focus.copy();ItemData.update(focus,tag->tag.getCompound("blockState").putString("Name","missing_mod:lost_block"));
        check(DislocationCapture.read(focus).isEmpty()&&ItemData.flag(focus,"available"),"unknown id stays available without default-air interpretation");
        focus=valid.copy();ItemData.update(focus,tag->tag.getCompound("blockState").getCompound("Properties").putString("axis","not_an_axis"));
        check(DislocationCapture.read(focus).isEmpty(),"unknown property value is rejected");
        focus=valid.copy();ItemData.update(focus,tag->tag.putInt("blockState",7));check(DislocationCapture.read(focus).isEmpty(),"wrong state tag does not fall back to blockName");
        var lower=Blocks.OAK_DOOR.defaultBlockState();var upper=lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER);
        var capture=new DislocationCapture(List.of(new DislocationCapture.Part(BlockPos.ZERO,lower,new CompoundTag()),new DislocationCapture.Part(BlockPos.ZERO.above(),upper,new CompoundTag())));
        focus=valid.copy();capture.store(focus);check(DislocationCapture.read(focus).orElseThrow().parts().size()==2,"accepted two-cell format");
        ItemData.update(focus,tag->tag.getList("parts",10).getCompound(1).putInt("dy",2));check(DislocationCapture.read(focus).isEmpty(),"unrelated cell cannot masquerade as assembly partner");
        helper.succeed();
    }

    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void bothHalvesOfDoorBedAndPlantMoveForOneBasicPayment(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var source=base(helper);var destination=source.east(5);
        boolean bedBlacklisted=ThaumcraftApi.portableHoleBlackList.remove(Blocks.RED_BED);
        try(var peer=new Peer(helper,"TTDislocationParts")){
            for(BlockState first:List.of(Blocks.OAK_DOOR.defaultBlockState(),Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.EAST),Blocks.SUNFLOWER.defaultBlockState())){
                BlockState second=first.getBlock() instanceof BedBlock?first.setValue(BedBlock.PART,BedPart.HEAD):first.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER);
                var offset=DislocationCapture.partnerOffset(first);
                for(boolean selectSecond:List.of(false,true)){
                    for(var root:List.of(source,destination)){
                        level.setBlock(root,Blocks.AIR.defaultBlockState(),18);level.setBlock(root.offset(offset),Blocks.AIR.defaultBlockState(),18);
                        level.setBlock(root.below(),Blocks.DIRT.defaultBlockState(),18);if(offset.getY()==0)level.setBlock(root.offset(offset).below(),Blocks.DIRT.defaultBlockState(),18);
                    }
                    level.setBlock(source,first,18);level.setBlock(source.offset(offset),second,18);
                    var selected=selectSecond?source.offset(offset):source;var target=selectSecond?destination.offset(offset):destination;
                    var wand=wand(peer.player);var before=WandApi.stored(wand,Aspects.ORDER);
                    var cost=WandApi.quote(wand,peer.player,FocusContent.DISLOCATION.get().getVisCost(WandApi.focus(wand)));
                    peer.fixture.packets.clear();check(use(peer.player,wand,selected,Direction.NORTH),"capture either selected half: "+first);assertBursts(peer);
                    check(level.getBlockState(source).isAir()&&level.getBlockState(source.offset(offset)).isAir(),"whole source assembly removed");
                    check(WandApi.stored(wand,Aspects.ORDER)==before-cost.get(Aspects.ORDER),"one ordinary payment, including modern bed BE");
                    var parts=DislocationCapture.read(WandApi.focus(wand)).orElseThrow();check(parts.parts().size()==2,"store both cells");
                    wand=ItemStack.parse(level.registryAccess(),wand.save(level.registryAccess())).orElseThrow();peer.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
                    for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);
                    level.setBlock(target.south(),Blocks.STONE.defaultBlockState(),18);
                    check(use(peer.player,wand,target.south(),Direction.NORTH),"free placement of complete serialized assembly");assertBursts(peer);
                    check(level.getBlockState(destination)==first&&level.getBlockState(destination.offset(offset))==second,"exact modern states and original anchor retained");
                    check(!ItemData.flag(WandApi.focus(wand),"available"),"stored assembly cleared only after placement");
                    level.setBlock(target.south(),Blocks.AIR.defaultBlockState(),18);level.setBlock(destination,Blocks.AIR.defaultBlockState(),18);level.setBlock(destination.offset(offset),Blocks.AIR.defaultBlockState(),18);
                }
            }
        }finally{if(bedBlacklisted)ThaumcraftApi.portableHoleBlackList.add(Blocks.RED_BED);}
        helper.succeed();
    }

    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void protectionCancellationKeepsContainerAndStoredInventory(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var source=base(helper);var destination=source.east(5);
        try(var peer=new Peer(helper,"TTDislocationProtection")){
            var wand=wand(peer.player);level.setBlock(source,Blocks.CHEST.defaultBlockState(),18);
            var chest=(ChestBlockEntity)level.getBlockEntity(source);chest.setItem(0,new ItemStack(Items.DIAMOND,17));
            int before=WandApi.stored(wand,Aspects.ORDER);
            Consumer<BlockEvent.BreakEvent> denyCapture=event->{if(event.getPlayer()==peer.player&&event.getPos().equals(source))event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(denyCapture);
            try{check(!use(peer.player,wand,source,Direction.NORTH),"native protection cancels capture");}finally{NeoForge.EVENT_BUS.unregister(denyCapture);}
            check(level.getBlockEntity(source)==chest&&chest.getItem(0).getCount()==17&&WandApi.stored(wand,Aspects.ORDER)==before,"no capture mutation or debit");
            check(use(peer.player,wand,source,Direction.NORTH),"capture after protection allows");var stored=WandApi.focus(wand).copy();
            level.setBlock(destination.south(),Blocks.STONE.defaultBlockState(),18);
            Consumer<BlockEvent.EntityPlaceEvent> denyPlace=event->{if(event.getEntity()==peer.player&&event.getPos().equals(destination))event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(denyPlace);
            try{check(!use(peer.player,wand,destination.south(),Direction.NORTH),"native protection cancels restoration");}finally{NeoForge.EVENT_BUS.unregister(denyPlace);}
            check(level.getBlockState(destination).isAir()&&ItemStack.matches(stored,WandApi.focus(wand)),"canceled restoration rolls back without clearing stored inventory");
            check(level.getEntitiesOfClass(ItemEntity.class,new AABB(destination).inflate(2)).isEmpty(),"rollback does not drop a duplicate inventory");
            check(use(peer.player,wand,destination.south(),Direction.NORTH),"allowed retry succeeds");
            chest=(ChestBlockEntity)level.getBlockEntity(destination);check(chest.getItem(0).is(Items.DIAMOND)&&chest.getItem(0).getCount()==17,"retry restores exact inventory");chest.clearContent();
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void invalidPayloadAndUnsupportedStateRemainStored(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var destination=base(helper);
        try(var peer=new Peer(helper,"TTDislocationInvalid")){
            var wand=wand(peer.player);var focus=WandApi.focus(wand);var tile=new CompoundTag();tile.putString("id","minecraft:chest");
            new DislocationCapture(List.of(new DislocationCapture.Part(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState(),tile))).store(focus);WandApi.focus(wand,focus);
            level.setBlock(destination.south(),Blocks.STONE.defaultBlockState(),18);
            check(!use(peer.player,wand,destination.south(),Direction.NORTH)&&ItemStack.matches(focus,WandApi.focus(wand)),"forged BE type does not instantiate or clear payload");
            new DislocationCapture(List.of(new DislocationCapture.Part(BlockPos.ZERO,Blocks.TORCH.defaultBlockState(),new CompoundTag()))).store(focus);WandApi.focus(wand,focus);
            level.setBlock(destination.below(),Blocks.AIR.defaultBlockState(),18);
            check(!use(peer.player,wand,destination.south(),Direction.NORTH)&&level.getBlockState(destination).isAir()&&ItemStack.matches(focus,WandApi.focus(wand)),"modern support validation restores target and keeps capture");
            ItemData.update(focus,tag->tag.getCompound("blockState").putString("Name","missing:unknown"));WandApi.focus(wand,focus);
            level.setBlock(destination,Blocks.STONE.defaultBlockState(),18);check(!FocusContent.DISLOCATION.get().cast(peer.player,wand,focus)&&level.getBlockState(destination).is(Blocks.STONE),"invalid available payload never becomes a new capture");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked") private static Map<ServerPlayer,?> waiting()throws ReflectiveOperationException{var field=DislocationActions.class.getDeclaredField("PENDING");field.setAccessible(true);return (Map<ServerPlayer,?>)field.get(null);}
    private static ChunkPreparationApi.Request request(Object job)throws ReflectiveOperationException{var field=job.getClass().getDeclaredField("request");field.setAccessible(true);return (ChunkPreparationApi.Request)field.get(job);}
    @GameTest(template="empty",batch="dislocation_cold",timeoutTicks=1200)
    public static void coldClickCoalescesAndCancelsOnLookChange(GameTestHelper helper)throws Exception{
        VerificationReports.install();var peer=new Peer(helper,"TTDislocationCold");var player=peer.player;var level=helper.getLevel();var origin=player.position();
        var column=new ChunkPos(1_240_000,-1_240_000);check(level.getChunkSource().getChunkNow(column.x,column.z)==null,"fresh remote owner column");
        var ownerPreparation=ChunkPreparationApi.prepare(player,List.of(column));
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;ownerPreparation.close();if(level.getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(origin.x)>>4,net.minecraft.util.Mth.floor(origin.z)>>4)!=null)player.setPos(origin);peer.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(()->{
            if(!ownerPreparation.advance(1))return;
            try{
                player.setPos(column.getMinBlockX()+15.5,300,column.getMinBlockZ()+8.5);player.setYRot(-90);player.setXRot(0);var wand=wand(player);var focus=WandApi.focus(wand);
                int before=WandApi.stored(wand,Aspects.ORDER);check(!FocusContent.DISLOCATION.get().cast(player,wand,focus),"cold click waits");
                var pending=waiting().get(player);check(pending!=null,"owner-bound native request exists");var request=request(pending);
                check(!FocusContent.DISLOCATION.get().cast(player,wand,focus)&&waiting().get(player)==pending,"repeat coalesces with original click");
                player.setYRot(90);DislocationActions.tick(new LevelTickEvent.Post(()->true,level));check(request.closed()&&!waiting().containsKey(player),"changed look releases tickets");
                check(WandApi.stored(wand,Aspects.ORDER)==before,"waiting and canceled action never debit");
                cleanup.run();helper.succeed();
            }catch(Exception failure){cleanup.run();throw new RuntimeException(failure);}
        });
    }
    private DislocationServerRulesTest(){}
}
