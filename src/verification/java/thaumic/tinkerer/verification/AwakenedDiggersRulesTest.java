package thaumic.tinkerer.verification;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.crafting.InfusionInput;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.tool.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class AwakenedDiggersRulesTest {
    private static void check(boolean value,String label){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @GameTest(template="summon_space",batch="awakened_diggers",timeoutTicks=100)
    public static void nativeVariantsRecipesMaterialsAndShears(GameTestHelper helper){
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        try(var fixture=new NativePlayerFixture(helper,"TTDiggerData",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var level=helper.getLevel();
            for(var item:List.of(IchorTools.AWAKENED_PICK,IchorTools.AWAKENED_SHOVEL,IchorTools.AWAKENED_AXE)){
                var stack=item.get().getDefaultInstance();check(!stack.isDamageableItem()&&stack.isEnchantable(),"source non-damageable enchantable tiered item");
                ItemData.update(stack,data->data.putString("other","retained"));player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.setShiftKeyDown(true);
                for(int mode:new int[]{1,2,0}){
                    item.get().use(level,player,InteractionHand.MAIN_HAND);check(AdvancedToolMode.get(stack)==mode,"three source modes");
                    var loaded=ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();
                    check(AdvancedToolMode.get(loaded)==mode&&ItemData.read(loaded).getString("other").equals("retained"),"native component round trip");
                }
                String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.get()).toString();
                var recipe=(InfusionRecipe)level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow().value();
                var inputs=recipe.components().stream().map(ingredient->ingredient.getItems()[0].copy()).toList();
                check(recipe.instability()==15&&inputs.size()==12,"source repeated infusion components");
                check(recipe.matches(new InfusionInput(recipe.input().getItems()[0].copy(),inputs)),"native ingredient matching");
                var node=ResearchApi.definitions(player).get(id);check(node.concealed()&&node.complexity()==5,"source concealed research");
            }
            check(IchorTools.AWAKENED_AXE.get().getDestroySpeed(IchorTools.AWAKENED_AXE.toStack(),Blocks.OAK_LEAVES.defaultBlockState())==15,"source shears leaves speed");
            check(AwakenedMining.rightMaterial(Blocks.GLASS.defaultBlockState(),AwakenedMiningShape.Kind.SHOVEL),"tool-not-required overrides category");
            check(AwakenedMining.rightMaterial(Blocks.PISTON.defaultBlockState(),AwakenedMiningShape.Kind.PICK),"original piston category");
            check(AwakenedMining.sameTreeBlock(Blocks.SPRUCE_LOG.defaultBlockState(),Blocks.OAK_LOG),"old log metadata variants retain shared block identity");
            check(!AwakenedMining.sameTreeBlock(Blocks.ACACIA_LOG.defaultBlockState(),Blocks.OAK_LOG),"old log2 remains a separate block identity");
            nativeSightRay(helper,player);
        }
        helper.succeed();
    }
    private static void nativeSightRay(GameTestHelper helper,ServerPlayer player){
        try{
            var view=Class.forName(AwakenedMining.class.getName()+"$LoadedView");
            var constructor=view.getDeclaredConstructor(net.minecraft.server.level.ServerLevel.class);constructor.setAccessible(true);
            var method=AwakenedMining.class.getDeclaredMethod("raytrace",view,ServerPlayer.class,boolean.class);method.setAccessible(true);
            var water=helper.absolutePos(new BlockPos(8,4,8));var behind=water.south(2);var level=helper.getLevel();
            player.setPos(water.getX()+.5,water.getY()+.95-1.62,water.getZ()-2);player.setYRot(0);player.setXRot(0);
            level.setBlock(water,Blocks.WATER.defaultBlockState(),2);level.setBlock(behind,Blocks.STONE.defaultBlockState(),2);
            var source=(net.minecraft.world.phys.BlockHitResult)method.invoke(null,constructor.newInstance(level),player,true);
            check(source.getBlockPos().equals(water),"source liquid ray uses full old block bounds above modern 8/9 surface");
            level.setBlock(water,Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL,2),2);
            var flowing=(net.minecraft.world.phys.BlockHitResult)method.invoke(null,constructor.newInstance(level),player,true);
            check(flowing.getBlockPos().equals(behind),"old ray ignores non-source liquid metadata");
            level.setBlock(water,Blocks.AIR.defaultBlockState(),2);level.setBlock(behind,Blocks.AIR.defaultBlockState(),2);
            var far=water.south(3);level.setBlock(far,Blocks.STONE.defaultBlockState(),2);
            player.setPos(water.getX()+.5,water.getY()+.95-1.62,far.getZ()-4.75);
            var reach=(net.minecraft.world.phys.BlockHitResult)method.invoke(null,constructor.newInstance(level),player,false);
            check(reach.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&reach.getBlockPos().equals(far),"five-block server ray reaches beyond modern base4.5");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native production sight-ray invocation",error);}
    }
    /** Registers this in-memory native peer in the actual player UUID index.
     * The ordinary NativePlayerFixture intentionally does not perform a login;
     * these owner-lifetime tests need the same identity lookup as a connected peer.
     */
    private static final class IndexedPeer implements AutoCloseable {
        final NativePlayerFixture fixture;final ServerPlayer player;final Map<UUID,ServerPlayer> index;boolean closed;
        final List<Runnable> cleanup=new ArrayList<>();
        @SuppressWarnings("unchecked") IndexedPeer(GameTestHelper helper,String name,BlockPos root){
            fixture=new NativePlayerFixture(helper,name,root);player=fixture.player;
            try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);index=(Map<UUID,ServerPlayer>)field.get(player.getServer().getPlayerList());}
            catch(ReflectiveOperationException error){fixture.close();throw new IllegalStateException(error);}
            check(index.putIfAbsent(player.getUUID(),player)==null,"unique native test peer");
            player.setNoGravity(true);player.setPos(root.getX()+.5,root.getY()-1.12,root.getZ()-2);player.setYRot(0);player.setXRot(0);
            helper.testInfo.addListener(new GameTestListener(){
                public void testStructureLoaded(GameTestInfo info){}
                public void testPassed(GameTestInfo info,GameTestRunner runner){close();}
                public void testFailed(GameTestInfo info,GameTestRunner runner){close();}
                public void testAddedForRerun(GameTestInfo oldTest,GameTestInfo newTest,GameTestRunner runner){close();}
            });
        }
        @Override public void close(){if(closed)return;closed=true;for(var action:cleanup)action.run();index.remove(player.getUUID(),player);fixture.close();}
    }
    @GameTest(template="summon_space",batch="awakened_diggers",timeoutTicks=400)
    public static void nativePermissionHookPlaneAndOwnerExpiry(GameTestHelper helper){
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        var level=helper.getLevel();var root=helper.absolutePos(new BlockPos(8,5,8));
        var peer=new IndexedPeer(helper,"TTDiggerPlane",root);var player=peer.player;
        var stack=IchorTools.AWAKENED_PICK.toStack();AdvancedToolMode.set(stack,1);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++)level.setBlock(root.offset(x,y,0),Blocks.STONE.defaultBlockState(),3);
        var protectedPos=root.offset(2,3,0);var observed=new ArrayList<BlockPos>();
        Consumer<BlockEvent.BreakEvent> listener=event->{
            if(event.getPlayer()!=player)return;
            observed.add(event.getPos().immutable());
            if(!event.getPos().equals(root))check(level.getBlockState(root).is(Blocks.STONE),"extras run before root removal");
            if(event.getPos().equals(protectedPos))event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,listener);peer.cleanup.add(()->NeoForge.EVENT_BUS.unregister(listener));
        player.gameMode.destroyBlock(root);
        helper.startSequence().thenWaitUntil(()->check(!AwakenedMining.hasPending(player),"player operation finished preparation"))
            .thenExecute(()->{
                check(!observed.isEmpty()&&observed.getFirst().equals(root),"native root permission event precedes item hook");
                for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++){
                    var pos=root.offset(x,y,0);check(pos.equals(protectedPos)?level.getBlockState(pos).is(Blocks.STONE):level.getBlockState(pos).isAir(),"25-block plane and canceled extra");
                }
                try(var request=ChunkPreparationApi.prepare(player,List.of(new ChunkPos(root)))){
                    check(request.advance(0)&&request.submitted()==1,"already-full player-owned column needs no cold budget");
                    peer.index.remove(player.getUUID(),player);check(!request.advance(2)&&request.closed(),"owner replacement/logout closes player tickets");
                }
            }).thenSucceed();
    }
    @GameTest(template="summon_space",batch="awakened_diggers",timeoutTicks=400)
    public static void nativeTreeProtectionStopsWithoutRootLoss(GameTestHelper helper){
        VerificationReports.install();if(!TinkererConfig.KAMI_FORCE_ENABLED.get()){helper.succeed();return;}
        var level=helper.getLevel();var root=helper.absolutePos(new BlockPos(8,4,8));var peer=new IndexedPeer(helper,"TTDiggerTree",root);
        var player=peer.player;var stack=IchorTools.AWAKENED_AXE.toStack();AdvancedToolMode.set(stack,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var trunk=List.of(Blocks.OAK_LOG,Blocks.SPRUCE_LOG,Blocks.BIRCH_LOG,Blocks.JUNGLE_LOG,Blocks.OAK_WOOD,Blocks.SPRUCE_WOOD);
        for(int y=0;y<=5;y++)level.setBlock(root.above(y),trunk.get(y).defaultBlockState(),3);
        var selected=AwakenedMining.furthest(level,root,Blocks.OAK_LOG);check(selected.equals(root.above(5)),"source outward search selects top of straight trunk");
        Consumer<BlockEvent.BreakEvent> listener=event->{if(event.getPlayer()==player&&event.getPos().equals(selected))event.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,listener);peer.cleanup.add(()->NeoForge.EVENT_BUS.unregister(listener));
        player.gameMode.destroyBlock(root);
        helper.startSequence().thenWaitUntil(()->check(!AwakenedMining.hasPending(player),"tree preparation completed"))
            .thenExecute(()->{
                for(int y=0;y<=5;y++)check(level.getBlockState(root.above(y)).is(trunk.get(y)),"protected selected log stops mixed-metadata tree and retains root; y="+y+" actual="+level.getBlockState(root.above(y)));
                check(!AwakenedMining.hasPending(player),"no retry job survives failed removal");
            }).thenSucceed();
    }
    private AwakenedDiggersRulesTest(){}
}
