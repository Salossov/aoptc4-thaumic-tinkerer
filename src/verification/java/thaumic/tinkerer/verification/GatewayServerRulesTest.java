package thaumic.tinkerer.verification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.wands.FocusBehaviors;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.wands.WandParts;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumcraft.api.world.ContentRules;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiWandContent;
import thaumic.tinkerer.kami.warpgate.FocusRecallItem;
import thaumic.tinkerer.kami.warpgate.SkyPearlItem;
import thaumic.tinkerer.kami.warpgate.WarpGateBlockEntity;
import thaumic.tinkerer.kami.warpgate.WarpGateContent;
import thaumic.tinkerer.kami.warpgate.WarpGateMenu;

/** Registered native objects and owner-bound chunk jobs; compiled until game acceptance. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class GatewayServerRulesTest {
    private static void check(boolean condition,String label){if(!condition)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    private static boolean enabled(GameTestHelper helper){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;helper.succeed();return false;}

    private static final class Peer implements AutoCloseable {
        final NativePlayerFixture fixture;
        final ServerPlayer player;
        final Map<UUID,ServerPlayer> index;
        @SuppressWarnings("unchecked")
        Peer(GameTestHelper helper,String name,BlockPos pos)throws ReflectiveOperationException{
            fixture=new NativePlayerFixture(helper,name,pos);player=fixture.player;
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            index=(Map<UUID,ServerPlayer>)field.get(helper.getLevel().getServer().getPlayerList());
            check(index.putIfAbsent(player.getUUID(),player)==null,"unique native owner");
        }
        @Override public void close(){
            WarpGateContent.FOCUS_RECALL.get().end(player,player.getMainHandItem(),ItemStack.EMPTY);
            index.remove(player.getUUID(),player);fixture.close();
        }
    }

    private static WarpGateBlockEntity gate(GameTestHelper helper,BlockPos relative){
        helper.setBlock(relative,WarpGateContent.WARP_GATE.get());
        return (WarpGateBlockEntity)helper.getBlockEntity(relative);
    }
    private static ItemStack pearl(BlockPos target,Level level){
        var stack=WarpGateContent.SKY_PEARL.toStack();SkyPearlItem.setValues(stack,target,level.dimension());return stack;
    }
    private static ItemStack wand(ServerPlayer player){
        var stack=FocusRulesTest.wand(player,WarpGateContent.FOCUS_RECALL.toStack());
        WandApi.parts(stack,new WandParts(KamiWandContent.ROD,KamiWandContent.CAP));
        for(var aspect:Aspects.PRIMALS)WandApi.set(stack,aspect,WandApi.capacity(stack));
        return stack;
    }
    private static void elapsed(ServerPlayer player,int ticks)throws ReflectiveOperationException{
        // Drive the actual native use clock without waiting for 61 unrelated world ticks.
        var field=LivingEntity.class.getDeclaredField("useItemRemaining");field.setAccessible(true);
        field.setInt(player,player.getUseItem().getUseDuration(player)-ticks);
        check(player.getTicksUsingItem()==ticks,"native use clock fixture");
    }
    private static ChunkPreparationApi.Request pending(WarpGateBlockEntity gate)throws ReflectiveOperationException{
        var field=WarpGateBlockEntity.class.getDeclaredField("chunkRequest");field.setAccessible(true);
        return (ChunkPreparationApi.Request)field.get(gate);
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void pearlFormatAndNativeRoundTrips(GameTestHelper helper){
        if(!enabled(helper))return;
        var stack=WarpGateContent.SKY_PEARL.toStack();var target=new BlockPos(-17,-1,214);
        SkyPearlItem.setValues(stack,target,Level.OVERWORLD);
        check(SkyPearlItem.isAttuned(stack)&&target.equals(SkyPearlItem.getTargetPos(stack)),"explicit binding permits Y=-1");
        var tag=stack.get(DataComponents.CUSTOM_DATA).copyTag();
        check(tag.getBoolean("attuned")&&tag.getString("dim").equals("minecraft:overworld"),"accepted persistent format");
        var saved=ItemStack.parse(helper.getLevel().registryAccess(),stack.save(helper.getLevel().registryAccess())).orElseThrow();
        check(ItemStack.matches(stack,saved),"native disk codec");
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),helper.getLevel().registryAccess());
        try{ItemStack.STREAM_CODEC.encode(buffer,stack);check(ItemStack.matches(stack,ItemStack.STREAM_CODEC.decode(buffer)),"native wire codec");}finally{buffer.release();}
        tag.remove("attuned");tag.putInt("y",64);tag.putInt("dim",231);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        check(!SkyPearlItem.isAttuned(stack)&&SkyPearlItem.getTargetDim(stack)==null,"unknown legacy dimension never falls back to Overworld");
        tag.putInt("dim",-1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        check(Level.NETHER.equals(SkyPearlItem.getTargetDim(stack)),"known legacy Nether");
        SkyPearlItem.clearValues(stack);check(!SkyPearlItem.isAttuned(stack)&&!stack.get(DataComponents.CUSTOM_DATA).copyTag().contains("x"),"clear binding");
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void actualRegisteredDataAndUnclampedContainer(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        try(var peer=new Peer(helper,"TTGateData",helper.absolutePos(new BlockPos(8,4,8)))){
            var level=helper.getLevel();var gate=gate(helper,new BlockPos(2,2,2));
            var stack=pearl(gate.getBlockPos(),level);stack.setCount(12);gate.setItem(0,stack);
            check(gate.getItem(0).getCount()==12&&gate.getMaxStackSize()==1,"assignment preserves existing data; insertion still limits one");
            check(gate.canPlaceItem(0,WarpGateContent.SKY_PEARL.toStack())&&!gate.canPlaceItem(0,new ItemStack(Items.DIAMOND)),"exact registered pearl filter");
            gate.setLocked(true);var tag=gate.saveWithFullMetadata(level.registryAccess());gate.clearContent();gate.setLocked(false);gate.loadWithComponents(tag,level.registryAccess());
            check(gate.isLocked()&&gate.getItem(0).getCount()==12,"native container and lock persistence");
            var focus=WarpGateContent.FOCUS_RECALL.toStack();var item=WarpGateContent.FOCUS_RECALL.get();
            check(focus.getRarity()==Rarity.RARE&&focus.getMaxDamage()==1&&item.getEnchantmentValue()==5&&!item.isRepairable(focus),"source Recall properties");
            check(item.getFocusColor(focus)==10287359&&item.continuous(focus)&&item.managesVis()&&!item.isVisCostPerTick(focus),"source Recall use and color");
            check(FocusBehaviors.get(focus).behavior()==item,"public registered behavior");
            var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("focus_recall")).orElseThrow().value();
            check(recipe.instability()==10&&recipe.aspects().equals(Map.of("iter",100,"alienis",64,"praecantatio",50)),"source Recall infusion");
            var recipes=level.getRecipeManager();check(recipes.byKey(ThaumicTinkerer.id("sky_pearl")).isPresent(),"source pearl recipe exists independently of mirror configuration");
            var nodes=ResearchApi.definitions(peer.player);
            check(nodes.containsKey("thaumictinkerer:warp_gate")==ContentRules.mirrorsEnabled(),"source conditional gate research");
            if(ContentRules.mirrorsEnabled())check(nodes.get("thaumictinkerer:focus_recall").hiddenParents().contains("thaumictinkerer:rod_ichorcloth"),"source hidden rod prerequisite");
            check(WarpGateContent.WARP_GATE.get().defaultBlockState().requiresCorrectToolForDrops(),"source rock harvest semantics");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="infusion_output",timeoutTicks=100)
    public static void originalTwoPearlsKeepLimitAndSurvivePedestalCollection(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var level=helper.getLevel();var registries=level.registryAccess();
        var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("sky_pearl")).orElseThrow().value();
        var output=recipe.getRecipeOutput();
        check(output.is(WarpGateContent.SKY_PEARL.get())&&output.getCount()==2&&output.getMaxStackSize()==1,"source two-item result and ordinary limit one");
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,registries);
        var encoded=net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(ops,recipe).getOrThrow();
        var decoded=(InfusionRecipe)net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops,encoded).getOrThrow();
        check(decoded.getRecipeOutput().getCount()==2,"real registered recipe serializer retains the approved output");
        var invalid=encoded.deepCopy().getAsJsonObject();invalid.getAsJsonObject("result").addProperty("count",3);
        check(net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops,invalid).isError(),"opt-in permits only two pearls");
        invalid=encoded.deepCopy().getAsJsonObject();invalid.getAsJsonObject("result").addProperty("id","minecraft:diamond_sword");
        check(net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops,invalid).isError(),"ordinary items retain their strict inventory limit");
        try(var fixture=new NativePlayerFixture(helper,"TTPearlOutput",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var pos=helper.absolutePos(new BlockPos(8,4,8));
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("thaumcraft:arcane_pedestal"));
            level.setBlock(pos,block.defaultBlockState(),18);var pedestal=level.getBlockEntity(pos);
            check(pedestal instanceof thaumcraft.api.crafting.IInfusionPedestal,"real native infusion output inventory");
            var inventory=(thaumcraft.api.crafting.IInfusionPedestal)pedestal;
            // Exercise the actual matrix completion sink without importing core internals.
            var write=pedestal.getClass().getMethod("setInfusionResult",ItemStack.class);write.invoke(pedestal,output.copy());
            var saved=pedestal.saveWithFullMetadata(registries);inventory.clearContent();pedestal.loadWithComponents(saved,registries);
            check(inventory.getItem(0).getCount()==2&&inventory.getMaxStackSize()==1,"pedestal NBT round trip does not clamp the crafted result");
            var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
            player.getInventory().clearContent();level.getBlockState(pos).useWithoutItem(level,player,hit);
            var pearls=player.getInventory().items.stream().filter(s->s.is(WarpGateContent.SKY_PEARL.get())).toList();
            check(inventory.isEmpty()&&pearls.size()==2&&pearls.stream().allMatch(s->s.getCount()==1),"native collection splits both pearls into ordinary single-item slots");
            for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.STONE,64));
            write.invoke(pedestal,output.copy());level.getBlockState(pos).useWithoutItem(level,player,hit);
            int dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(3)).stream().filter(e->e.getItem().is(WarpGateContent.SKY_PEARL.get())).mapToInt(e->e.getItem().getCount()).sum();
            check(inventory.isEmpty()&&dropped==2,"full inventory drops both pearls without loss");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void quickMoveUsesLastInventorySlotAndHotbar(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var gate=gate(helper,new BlockPos(2,2,2));
        try(var peer=new Peer(helper,"TTGateSlots",gate.getBlockPos().above())){
            var inventory=peer.player.getInventory();var menu=new WarpGateMenu(1,inventory,gate);
            for(int empty:new int[]{35,8}){
                for(int slot=0;slot<36;slot++)inventory.setItem(slot,new ItemStack(Items.DIAMOND,64));
                inventory.setItem(empty,ItemStack.EMPTY);gate.setItem(0,WarpGateContent.SKY_PEARL.toStack());
                check(!menu.quickMoveStack(peer.player,0).isEmpty(),"accepted Shift-click transfer");
                check(gate.getItem(0).isEmpty()&&inventory.getItem(empty).is(WarpGateContent.SKY_PEARL.get()),"accepted full 36-slot range");
            }
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void nativeTeleportLockAndOneSuccessPerTick(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var source=gate(helper,new BlockPos(1,2,1));var destination=gate(helper,new BlockPos(4,2,4));
        try(var peer=new Peer(helper,"TTGateMove",source.getBlockPos().above())){
            var player=peer.player;player.setYRot(71);player.setXRot(-19);
            source.setItem(0,pearl(destination.getBlockPos(),helper.getLevel()));
            destination.setLocked(true);var before=player.position();source.teleportPlayer(player,0);
            check(player.position().equals(before),"locked target refuses movement");
            destination.setLocked(false);source.teleportPlayer(player,0);
            var pos=destination.getBlockPos();check(player.getX()==pos.getX()+.5&&player.getY()==pos.getY()+1.6&&player.getZ()==pos.getZ()+.5,"source exact arrival coordinates");
            check(player.getYRot()==71&&player.getXRot()==-19,"source view preservation");
            source.setItem(1,pearl(source.getBlockPos(),helper.getLevel()));source.teleportPlayer(player,1);
            check(player.getX()==pos.getX()+.5,"one successful transfer per source tick");
            WarpGateBlockEntity.serverTick(helper.getLevel(),source.getBlockPos(),source.getBlockState(),source);source.teleportPlayer(player,1);
            check(player.getX()==source.getBlockPos().getX()+.5,"source counter resets for next tile tick");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void serverDimensionGuardAndColdJobCancellation(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var source=gate(helper,new BlockPos(2,2,2));
        try(var peer=new Peer(helper,"TTGateCold",source.getBlockPos().above())){
            var foreign=WarpGateContent.SKY_PEARL.toStack();SkyPearlItem.setValues(foreign,new BlockPos(1,64,1),Level.NETHER);source.setItem(0,foreign);
            source.teleportPlayer(peer.player,0);check(pending(source)==null,"forged cross-dimension pearl cannot submit work");
            var cold=new BlockPos(29_000_000,64,29_000_000);
            check(helper.getLevel().getChunkSource().getChunkNow(cold.getX()>>4,cold.getZ()>>4)==null,"cold fixture column");
            source.setItem(0,pearl(cold,helper.getLevel()));source.teleportPlayer(peer.player,0);
            var request=pending(source);check(request!=null&&!request.closed(),"request survives its first asynchronous poll");
            source.teleportPlayer(peer.player,0);check(pending(source)==request,"duplicate packet retains existing owner request");
            source.setItem(0,pearl(cold.offset(16,0,0),helper.getLevel()));
            WarpGateBlockEntity.serverTick(helper.getLevel(),source.getBlockPos(),source.getBlockState(),source);
            check(request.closed()&&pending(source)==null,"pearl replacement cancels and frees tickets");
            source.setItem(0,pearl(cold,helper.getLevel()));source.teleportPlayer(peer.player,0);request=pending(source);check(request!=null,"second request");
            source.onChunkUnloaded();check(request.closed()&&pending(source)==null,"source unload frees tickets");
        }finally{source.cancelChunkWork();}
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    public static void recallNativeClockPaymentAndFailure(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        var destination=gate(helper,new BlockPos(4,2,4));
        try(var peer=new Peer(helper,"TTRecall",helper.absolutePos(new BlockPos(1,3,1)))){
            var player=peer.player;var level=helper.getLevel();var stack=wand(player);var focus=WarpGateContent.FOCUS_RECALL.toStack();var item=WarpGateContent.FOCUS_RECALL.get();
            player.getInventory().setItem(1,pearl(destination.getBlockPos(),level));
            stack.getItem().use(level,player,InteractionHand.MAIN_HAND);check(player.isUsingItem(),"real registered wand starts Recall");
            var origin=player.position();var costs=WandApi.quote(stack,player,item.getVisCost(focus));int initial=WandApi.stored(stack,Aspects.AIR);
            elapsed(player,60);stack.getItem().onUseTick(level,player,stack,player.getUseItemRemainingTicks());
            check(player.position().equals(origin)&&player.isUsingItem()&&WandApi.stored(stack,Aspects.AIR)==initial,"exactly 60 elapsed ticks keep charging without debit");
            elapsed(player,61);stack.getItem().onUseTick(level,player,stack,player.getUseItemRemainingTicks());
            check(player.getY()==destination.getBlockPos().getY()+1.6&&!player.isUsingItem(),"strictly after 60 native use ticks transfers and ends use");
            check(WandApi.stored(stack,Aspects.AIR)==initial-costs.get(Aspects.AIR),"source one-time debit after movement");
            destination.setLocked(true);item.begin(player,stack,focus,InteractionHand.MAIN_HAND);player.startUsingItem(InteractionHand.MAIN_HAND);elapsed(player,61);
            initial=WandApi.stored(stack,Aspects.AIR);check(!item.cast(player,stack,focus)&&!player.isUsingItem()&&WandApi.stored(stack,Aspects.AIR)==initial,"locked target ends use and spends no vis");
        }
        helper.succeed();
    }

    @GameTest(template="summon_space",batch="gateway_server",timeoutTicks=100)
    @SuppressWarnings("unchecked")
    public static void recallColdOwnerCancelsWhenPearlChanges(GameTestHelper helper)throws Exception{
        if(!enabled(helper))return;
        try(var peer=new Peer(helper,"TTRecallCold",helper.absolutePos(new BlockPos(2,4,2)))){
            var player=peer.player;var level=helper.getLevel();var stack=wand(player);var focus=WarpGateContent.FOCUS_RECALL.toStack();var item=WarpGateContent.FOCUS_RECALL.get();
            var cold=new BlockPos(-29_000_000,64,-29_000_000);player.getInventory().setItem(1,pearl(cold,level));
            stack.getItem().use(level,player,InteractionHand.MAIN_HAND);elapsed(player,61);
            int initial=WandApi.stored(stack,Aspects.AIR);check(!item.cast(player,stack,focus)&&player.isUsingItem(),"cold destination retains actual wand use");
            var field=FocusRecallItem.class.getDeclaredField("CASTING");field.setAccessible(true);
            var sessions=(Map<ServerPlayer,?>)field.get(null);var session=sessions.get(player);check(session!=null,"actual casting owner retained");
            var requestField=session.getClass().getDeclaredField("request");requestField.setAccessible(true);
            var request=(ChunkPreparationApi.Request)requestField.get(session);check(request!=null&&!request.closed(),"actual player-owned preparation persists");
            player.getInventory().setItem(1,pearl(cold,level));check(!item.cast(player,stack,focus),"replacement does not replay stale action");
            check(request.closed()&&!sessions.containsKey(player)&&!player.isUsingItem(),"pearl identity change closes owner and use");
            check(WandApi.stored(stack,Aspects.AIR)==initial,"cancellation spends no vis");
        }
        helper.succeed();
    }
    private GatewayServerRulesTest(){}
}
