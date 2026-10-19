package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.world.ContentRules;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.item.ConnectorItem;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class TransvectorDislocatorRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new GameTestAssertException(label); }
    private static TransvectorDislocatorBlockEntity place(ServerLevel level, BlockPos pos, BlockPos end, Direction direction) {
        level.setBlock(pos, TransvectorContent.DISLOCATOR.get().defaultBlockState(), 2);
        var tile = (TransvectorDislocatorBlockEntity)level.getBlockEntity(pos); tile.bind(end); tile.orientation(direction.get3DDataValue()); return tile;
    }
    private static void tick(TransvectorDislocatorBlockEntity tile, int count) {
        for (int i = 0; i < count; i++) TransvectorDislocatorBlockEntity.tick(tile.getLevel(), tile.getBlockPos(), tile.getBlockState(), tile);
    }
    private static void clear(ServerLevel level, BlockPos... positions) {
        for (var pos : positions) if (level.isInWorldBounds(pos)) {
            if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }
    /** Only the wire is captured: the actual native server teleport implementation runs. */
    private static final class CapturedConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets = new ArrayList<>();
        final io.netty.channel.embedded.EmbeddedChannel wire;
        CapturedConnection() { super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND); wire = new io.netty.channel.embedded.EmbeddedChannel(this); }
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener, boolean flush) { packets.add(packet); }
    }
    // GameTestServer has no 50 ms pacing: cold generation needs a wall-clock allowance.
    @GameTest(template="summon_space", batch="dislocator", timeoutTicks=12000)
    public static void dislocatorNativePlayerTeleport(GameTestHelper helper) {
        VerificationReports.install(); checks=0;var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));var target=pos.east();var end=pos.west(3);
        var tile=place(level,pos,end,Direction.EAST);var transport=new CapturedConnection();
        var channels=net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateCommonChannels(transport,net.minecraft.network.ConnectionProtocol.PLAY);
        channels.addAll(net.neoforged.neoforge.network.registration.NetworkRegistry.getCommonPlayChannels(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND));
        channels.addAll(Set.of(ResourceLocation.parse("curios:sync_curios"),ResourceLocation.parse("thaumcraft:biome_columns")));
        var profile=new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("TTDislocatorNativePlayer".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TTDislocatorPlayer");
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),transport,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        player.setPos(target.getX()+.5,target.getY()+.125,target.getZ()+.5);player.setYRot(71);player.setXRot(-23);player.setDeltaMovement(.1,.2,.3);
        Runnable cleanup=()->{level.removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);transport.wire.finishAndReleaseAll();clear(level,pos,end,target);};
        try {
            level.addNewPlayer(player);level.setBlock(target,Blocks.AIR.defaultBlockState(),2);level.setBlock(end,Blocks.AIR.defaultBlockState(),2);transport.packets.clear();tile.receiveRedstonePulse();
            check(player.getX()==end.getX()+.5&&player.getY()==end.getY()+.125&&player.getYRot()==71&&player.getXRot()==-23,"actual ServerGamePacketListenerImpl teleports player and retains rotation");
            check(player.getDeltaMovement().equals(new Vec3(.1,.2,.3))&&transport.packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket),"native position packet is sent and velocity is retained");
            tick(tile,10);player.setPos(target.getX()+.5,target.getY()+.125,target.getZ()+.5);var cold=target.offset(30000,0,0);tile.bind(cold);
            level.setBlock(target,Blocks.CHEST.defaultBlockState(),2);var chest=(ChestBlockEntity)level.getBlockEntity(target);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
            check(!level.hasChunkAt(cold),"native player teleport cold destination fixture");tile.receiveRedstonePulse();tile.receiveRedstonePulse();
            check(tile.preparing()&&tile.cooldown()==0&&player.getX()==target.getX()+.5&&!level.hasChunkAt(cold),"cold pulse defers without moving player, starting cooldown or synchronously loading destination; repeated pulse coalesces");
            long waitingSince=System.nanoTime();
            helper.onEachTick(()->{
                if(tile.preparing()){
                    if(System.nanoTime()-waitingSince>45_000_000_000L)throw new GameTestAssertException("Cold player preparation exceeded 45 seconds: ownerTicking="+level.shouldTickBlocksAt(tile.getBlockPos())+", ownerRemoved="+tile.isRemoved()+", destinationFull="+(level.getChunkSource().getChunkNow(cold.getX()>>4,cold.getZ()>>4)!=null));
                    return;
                }
                try {
                    check(player.getX()==cold.getX()+.5&&tile.hasBinding()&&tile.cooldown()>0&&level.hasChunkAt(cold),"prepared FULL destination executes actual player teleport before starting cooldown");
                    check(level.getBlockEntity(target)==chest&&chest.getItem(0).getCount()==7,"initially unloaded link keeps its source inventory and skips block exchange even after preparation");
                    check(!tile.pulseStored(),"coalesced preparation pulses do not queue an extra cooldown pulse");
                    System.out.println("TT_DISLOCATOR_NATIVE_PLAYER_PASS checks="+checks+" asynchronous_cold_destination=true");helper.succeed();
                } finally {cleanup.run();}
            });
        } catch(RuntimeException|Error failure) {cleanup.run();throw failure;}
    }
    // GameTestServer has no 50 ms pacing: cold generation needs a wall-clock allowance.
    @GameTest(template="summon_space", batch="dislocator", timeoutTicks=12000)
    public static void dislocatorPreparationLifetime(GameTestHelper helper) {
        VerificationReports.install();checks=0;var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));var target=pos.east();var cold=target.offset(-30000,0,0);
        var tile=place(level,pos,cold,Direction.EAST);tile.receiveRedstonePulse();check(tile.preparing()&&tile.cooldown()==0,"cold pulse owns deferred work");
        tile.orientation(Direction.NORTH.get3DDataValue());tick(tile,1);check(!tile.preparing()&&tile.cooldown()==0,"changed working cell cancels pending operation instead of applying stale orientation");
        tile.orientation(Direction.EAST.get3DDataValue());tile.receiveRedstonePulse();check(tile.preparing(),"new signal may begin preparation after cancellation");
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);check(tile.isRemoved()&&!tile.preparing(),"breaking owner cancels pending pulse immediately");
        tile=place(level,pos,cold,Direction.EAST);tile.receiveRedstonePulse();tile.onChunkUnloaded();check(!tile.preparing(),"owner unload callback cancels operation and releases its request");
        // A different cold column avoids reusing an earlier request's in-flight work.
        var destination=cold.west(32);var active=place(level,pos,destination,Direction.EAST);level.setBlock(target,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        var stand=new ArmorStand(level,target.getX()+.5,target.getY()+.125,target.getZ()+.5);stand.setNoGravity(true);level.addFreshEntity(stand);active.receiveRedstonePulse();
        check(active.preparing()&&!level.hasChunkAt(destination),"non-player entity also waits for FULL destination without native setPos loading it");
        long waitingSince=System.nanoTime();
        helper.onEachTick(()->{
            if(active.preparing()){
                if(System.nanoTime()-waitingSince>45_000_000_000L)throw new GameTestAssertException("Cold non-player preparation exceeded 45 seconds: ownerTicking="+level.shouldTickBlocksAt(active.getBlockPos())+", ownerRemoved="+active.isRemoved()+", destinationFull="+(level.getChunkSource().getChunkNow(destination.getX()>>4,destination.getZ()>>4)!=null));
                return;
            }
            try {
                check(stand.getX()==destination.getX()+.5&&stand.getY()==destination.getY()+.125&&active.cooldown()>0,"prepared cold destination receives native non-player entity");
                check(level.getBlockState(target).is(Blocks.DIAMOND_BLOCK)&&!active.pulseStored(),"initially cold link skips source exchange and executes one pulse");
                System.out.println("TT_DISLOCATOR_PREPARATION_LIFETIME_PASS checks="+checks);helper.succeed();
            } finally {stand.discard();clear(level,pos,target);}
        });
    }
    @GameTest(template="summon_space", batch="dislocator", timeoutTicks=100)
    public static void dislocatorNativeSwap(GameTestHelper helper) {
        VerificationReports.install(); checks = 0; var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 4, 3)); var end = pos.west(3); var target = pos.east();
        var tile = place(level, pos, end, Direction.EAST);
        var first = new ArmorStand(level, end.getX()+.5, end.getY()+.125, end.getZ()+.5);
        var second = new ArmorStand(level, target.getX()+.5, target.getY()+.125, target.getZ()+.5);
        try {
            level.setBlock(end, Blocks.CHEST.defaultBlockState(), 2); var chest = (ChestBlockEntity)level.getBlockEntity(end);
            chest.setItem(0, new ItemStack(Items.DIAMOND, 7)); var named = chest.saveWithFullMetadata(level.registryAccess()); named.putString("CustomName", net.minecraft.network.chat.Component.Serializer.toJson(net.minecraft.network.chat.Component.literal("Dislocator payload"), level.registryAccess())); chest.loadWithComponents(named, level.registryAccess());
            level.setBlock(target, Blocks.BEDROCK.defaultBlockState(), 2);
            ThaumcraftApi.portableHoleBlackList.add(Blocks.CHEST);
            first.setDeltaMovement(.1, .2, .3); level.addFreshEntity(first); level.addFreshEntity(second);
            tile.receiveRedstonePulse();
            var moved = (ChestBlockEntity)level.getBlockEntity(target);
            check(moved != null && moved != chest && chest.isRemoved(), "release dislocator retires source BE and creates a fresh NBT copy even with AE2 absent");
            check(moved.getItem(0).getCount()==7 && moved.getItem(0).is(Items.DIAMOND) && moved.getCustomName().getString().equals("Dislocator payload"), "actual inventory and custom name survive swap");
            check(moved.getBlockPos().equals(target) && moved.saveWithFullMetadata(level.registryAccess()).getInt("x")==target.getX(), "restored native coordinates belong to destination");
            check(level.getBlockState(end).is(Blocks.BEDROCK), "original OR allows bedrock and portable-hole-blacklisted chest");
            check(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6)).isEmpty(), "exchange never spills the original inventory");
            check(first.getX()==target.getX()+.5 && second.getX()==end.getX()+.5 && first.getY()==target.getY()+.125, "both entity lists are captured before relative teleportation");
            check(first.getDeltaMovement().equals(new Vec3(.1,.2,.3)), "entity velocity survives position exchange");
            check(tile.cooldown()==10 && !tile.pulseStored(), "first pulse starts ten-tick cooldown");
            tile.receiveRedstonePulse(); tile.receiveRedstonePulse();
            check(tile.pulseStored() && level.getBlockEntity(target)==moved, "multiple pulses during cooldown coalesce without early swap");
            tick(tile, 9); check(tile.cooldown()==1 && level.getBlockEntity(target)==moved, "stored pulse waits for complete ten-tick interval");
            tick(tile, 1);
            check(level.getBlockEntity(end) instanceof ChestBlockEntity returned && returned != moved && returned.getItem(0).getCount()==7 && level.getBlockState(target).is(Blocks.BEDROCK), "queued pulse swaps exactly once at cooldown zero");
            check(tile.cooldown()==10 && !tile.pulseStored(), "queued pulse starts new cooldown and clears its flag");
            var saved = tile.saveWithFullMetadata(level.registryAccess());
            var restored = new TransvectorDislocatorBlockEntity(pos, tile.getBlockState()); restored.loadWithComponents(saved, level.registryAccess()); restored.setLevel(level);
            check(restored.orientation()==5 && restored.hasBinding() && restored.targetPosition().equals(end) && restored.cooldown()==0 && !restored.pulseStored(), "orientation and binding persist; original transient cooldown/pulse do not");
            saved.putInt("orientation", 37); restored.loadWithComponents(saved, level.registryAccess());
            check(restored.orientation()==37 && restored.workingPosition().equals(pos), "unknown ForgeDirection retains raw NBT and zero offset");
            tile.bind(pos.offset(17,0,0)); tile.resolve(); check(!tile.hasBinding(), "loaded link beyond per-axis sixteen-block range is invalidated");
            tile.bind(pos.offset(30000,0,0)); tile.receiveRedstonePulse();
            check(tile.hasBinding() && !level.hasChunkAt(tile.targetPosition()), "unloaded link stays bound without synchronous chunk loading");
            System.out.println("TT_DISLOCATOR_NATIVE_SWAP_PASS checks="+checks); helper.succeed();
        } finally { ThaumcraftApi.portableHoleBlackList.remove(Blocks.CHEST); first.discard(); second.discard(); clear(level, pos, end, target); }
    }
    @GameTest(template="summon_space", batch="dislocator", timeoutTicks=100)
    public static void dislocatorWorldBounds(GameTestHelper helper) {
        VerificationReports.install(); checks = 0; var level = helper.getLevel(); var anchor = helper.absolutePos(new BlockPos(4, 1, 4));
        for (boolean upper : new boolean[]{true, false}) {
            var pos = new BlockPos(anchor.getX(), upper ? level.getMaxBuildHeight()-1 : level.getMinBuildHeight(), anchor.getZ());
            var end = pos.west(3); var tile = place(level, pos, end, upper ? Direction.UP : Direction.DOWN); var outside = tile.workingPosition();
            var entity = new ArmorStand(level, end.getX()+.5, end.getY()+.125, end.getZ()+.5);
            try {
                level.setBlock(end, Blocks.CHEST.defaultBlockState(), 2); var chest = (ChestBlockEntity)level.getBlockEntity(end); chest.setItem(0, new ItemStack(Items.EMERALD, 11)); level.addFreshEntity(entity);
                check(!level.isInWorldBounds(outside), "fixture working cell is beyond legal build height");
                tile.receiveRedstonePulse();
                check(level.getBlockEntity(end)==chest && chest.getItem(0).getCount()==11 && !chest.isRemoved(), "approved invalid-destination fix preserves source BE and all items");
                check(entity.getX()==outside.getX()+.5 && entity.getY()==outside.getY()+.125 && tile.cooldown()==10, "entity teleport and ten-tick cooldown still happen when block exchange is cancelled");
                check(level.getEntitiesOfClass(ItemEntity.class,new AABB(end).inflate(2)).isEmpty(), "cancelled out-of-height swap has no drops");
            } finally { entity.discard(); clear(level,pos,end); }
        }
        var pos = new BlockPos(anchor.getX(), -1, anchor.getZ()); var end = pos.west(3); var target = pos.east(); var tile = place(level,pos,end,Direction.EAST);
        try {
            level.setBlock(end,Blocks.GOLD_BLOCK.defaultBlockState(),2);level.setBlock(target,Blocks.STONE.defaultBlockState(),2);tile.receiveRedstonePulse();
            check(tile.hasBinding() && level.getBlockState(target).is(Blocks.GOLD_BLOCK) && level.getBlockState(end).is(Blocks.STONE), "explicit binding flag enables actual block exchange at Y minus one");
            System.out.println("TT_DISLOCATOR_WORLD_BOUNDS_PASS checks="+checks);helper.succeed();
        } finally {clear(level,pos,end,target);}
    }
    @GameTest(template="summon_space", batch="dislocator", timeoutTicks=100)
    public static void dislocatorInteractionAndRedstone(GameTestHelper helper) {
        VerificationReports.install(); checks=0;var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(4,4,4));var end=pos.west(3);var target=pos.east();var power=pos.north();
        var player=FocusRulesTest.player(helper,"[TTDislocator]");var tile=place(level,pos,end,Direction.EAST);
        var binder=new ItemStack(TransvectorContent.CONNECTOR.get());player.setItemInHand(InteractionHand.MAIN_HAND,binder);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);binder.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        check(ConnectorItem.selected(binder).orElseThrow().equals(pos),"binder selects newly ported dislocator");
        var interfacePos=pos.south(2);level.setBlock(interfacePos,TransvectorContent.INTERFACE.get().defaultBlockState(),2);
        binder.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(interfacePos),Direction.UP,interfacePos,false)));
        check(tile.targetPosition().equals(interfacePos) && ConnectorItem.selected(binder).isEmpty(),"dislocator may bind to an interface; only interface-to-interface is forbidden");
        var wand=FocusRulesTest.wand(player,ItemStack.EMPTY);
        for(var direction:Direction.values()) {
            level.getBlockState(pos).useItemOn(wand,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),direction,pos,false));
            check(tile.orientation()==direction.get3DDataValue(),"wand selects clicked face "+direction);
        }
        player.setPos(pos.getX(),pos.getY()+.2,pos.getZ());check(TransvectorDislocatorBlock.placementOrientation(pos,player)==Direction.UP,"original fixed +1.82 placement eye offset above block");
        player.setPos(pos.getX(),pos.getY()-2,pos.getZ());check(TransvectorDislocatorBlock.placementOrientation(pos,player)==Direction.DOWN,"original placement checks below block");
        player.setPos(pos.getX()+3,pos.getY(),pos.getZ());player.setYRot(90);check(TransvectorDislocatorBlock.placementOrientation(pos,player)==Direction.EAST,"source horizontal yaw mapping is retained");
        var research=AddonRegistry.research().get("thaumictinkerer:dislocator");
        check(research.concealed()&&research.secondary()&&research.complexity()==3&&research.hiddenParents().equals(List.of("MIRROR"))&&research.aspects().equals(Map.of("iter",2,"machina",1,"alienis",1)),"original research properties and hidden mirror parent");
        check(ResearchApi.exists(player,"thaumictinkerer:dislocator")==ContentRules.mirrorsEnabled(),"actual research visibility follows mirrors configuration");
        var recipe=level.getRecipeManager().byKey(ThaumicTinkerer.id("dislocator"));
        check(recipe.isPresent()==ContentRules.mirrorsEnabled(),"actual recipe condition follows mirrors while block remains registered");
        if(recipe.isPresent()) {
            var arcane=(IArcaneRecipe)recipe.get().value();var mirror=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:mirrored_glass")));
            check(arcane.vis().equals(Map.of("terra",5,"perditio",5))&&arcane.matches(CraftingInput.of(1,3,List.of(mirror,new ItemStack(TransvectorContent.INTERFACE.get()),new ItemStack(Items.COMPARATOR))),level),"original glass/interface/comparator arcane recipe and costs");
        }
        tile.bind(end);tile.orientation(Direction.EAST.get3DDataValue());level.setBlock(end,Blocks.GOLD_BLOCK.defaultBlockState(),2);level.setBlock(target,Blocks.STONE.defaultBlockState(),2);
        level.setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        check(level.getBlockState(pos).getValue(TransvectorDislocatorBlock.POWERED),"native neighbor power sets edge latch and schedules tick");
        level.setBlock(power,Blocks.AIR.defaultBlockState(),3);
        check(!level.getBlockState(pos).getValue(TransvectorDislocatorBlock.POWERED)&&level.getBlockState(target).is(Blocks.STONE),"falling edge resets latch before scheduled pulse");
        helper.runAfterDelay(3,()->{
            try {
                check(level.getBlockState(target).is(Blocks.GOLD_BLOCK)&&level.getBlockState(end).is(Blocks.STONE)&&tile.cooldown()>0,"scheduled pulse still executes after power removal, including mirrors-disabled mode");
                System.out.println("TT_DISLOCATOR_INTERACTION_REDSTONE_PASS checks="+checks);helper.succeed();
            } finally {player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);clear(level,pos,end,target,power,interfacePos);}
        });
    }
    private TransvectorDislocatorRulesTest() { }
}
