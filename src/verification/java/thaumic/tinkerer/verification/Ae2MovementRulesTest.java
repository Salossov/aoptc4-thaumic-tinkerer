package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.integration.Ae2Movement;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD)
public final class Ae2MovementRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){if(Ae2Movement.present())event.enqueueWork(Backend::register);}
    @GameTest(template="empty",batch="ae2_movement",timeoutTicks=70)
    public static void ae2NativeStrategies(GameTestHelper helper){
        VerificationReports.install();checks=0;
        if(!Ae2Movement.present()){check(true,"optional adapter remains absent without AE2");System.out.println("TT_AE2_ABSENT_PASS checks="+checks);helper.succeed();return;}
        Backend.verify(helper);
    }
    private static final class Backend {
        static void register(){
            // Real public AE2 registration, not a replacement registry/loader.
            appeng.api.movable.BlockEntityMoveStrategies.add(new appeng.api.movable.DefaultBlockEntityMoveStrategy(){
                @Override public boolean canHandle(BlockEntityType<?> type){return type==BlockEntityType.CHEST;}
                @Override public net.minecraft.nbt.CompoundTag beginMove(BlockEntity entity,HolderLookup.Provider registries){return entity.getPersistentData().getBoolean("TTAe2Veto")?null:super.beginMove(entity,registries);}
                @Override public boolean completeMove(BlockEntity entity,net.minecraft.world.level.block.state.BlockState state,net.minecraft.nbt.CompoundTag data,net.minecraft.world.level.Level level,BlockPos pos){return !entity.getPersistentData().getBoolean("TTAe2FailComplete")&&super.completeMove(entity,state,data,level,pos);}
            });
        }
        static void verify(GameTestHelper helper){
            var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
            var pos=new BlockPos((anchor.getX()>>4)*16+5,Math.min(anchor.getY()+345,level.getMaxBuildHeight()-8),(anchor.getZ()>>4)*16+5);
            var target=pos.east(2);var tabletPos=pos.south(3);var tabletTarget=tabletPos.east(2);var drivePos=pos.south(6);var driveTarget=drivePos.east(2);
            var blacklistPos=pos.north(2);var blacklistTarget=blacklistPos.east(2);
            var positions=List.of(pos,target,tabletPos,tabletTarget,tabletPos.north(),drivePos,driveTarget,blacklistPos,blacklistTarget);
            Runnable cleanup=()->{for(var p:positions){if(level.getBlockEntity(p) instanceof Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(9)).forEach(ItemEntity::discard);};
            try{
                cleanup.run();level.setBlock(pos,Blocks.CHEST.defaultBlockState(),2);var chest=(ChestBlockEntity)level.getBlockEntity(pos);var diamonds=new ItemStack(Items.DIAMOND,9);chest.setItem(0,diamonds);chest.getPersistentData().putBoolean("TTAe2Veto",true);
                check(!Ae2Movement.move(level,chest,chest.getBlockState(),target)&&level.getBlockEntity(pos)==chest&&chest.getItem(0)==diamonds&&level.getBlockState(target).isAir(),"real custom AE2 beginMove null veto leaves source and inventory untouched");
                chest.getPersistentData().remove("TTAe2Veto");chest.getPersistentData().putBoolean("TTAe2FailComplete",true);
                check(!Ae2Movement.move(level,chest,chest.getBlockState(),target),"real custom AE2 completeMove false triggers native recovery");
                var recovered=(ChestBlockEntity)level.getBlockEntity(pos);check(recovered!=chest&&chest.isRemoved()&&recovered.getItem(0).getCount()==9&&recovered.getItem(0).is(Items.DIAMOND)&&level.getBlockState(target).isAir(),"failed move reconstructs native source snapshot without item loss or invalid object reuse");
                check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).isEmpty(),"recovery does not spill either original or placeholder contents");
                recovered.getPersistentData().remove("TTAe2FailComplete");check(Ae2Movement.move(level,recovered,recovered.getBlockState(),target),"registered public strategy completes a normal move");
                var moved=(ChestBlockEntity)level.getBlockEntity(target);check(moved!=recovered&&moved.getItem(0).getCount()==9&&level.getBlockState(pos).isAir(),"successful AE2 path reconstructs the passenger and retains inventory");
                level.setBlock(blacklistPos,EnchanterContent.BLOCK.get().defaultBlockState(),2);var blacklisted=level.getBlockEntity(blacklistPos);check(blacklisted.getBlockState().is(appeng.api.ids.AETags.SPATIAL_BLACKLIST),"test-only c relocation tag contributes through real AE2 blacklist");
                check(!Ae2Movement.move(level,blacklisted,blacklisted.getBlockState(),blacklistTarget)&&level.getBlockEntity(blacklistPos)==blacklisted&&level.getBlockState(blacklistTarget).isAir(),"real blacklist rejects before callbacks, detach or target placement");
                level.setBlock(tabletPos,AnimationTabletContent.BLOCK.get().defaultBlockState(),2);var tablet=(AnimationTabletBlockEntity)level.getBlockEntity(tabletPos);tablet.setItem(0,new ItemStack(Items.IRON_PICKAXE));level.setBlock(tabletPos.north(),Blocks.OBSIDIAN.defaultBlockState(),2);tablet.swingHit();
                check(tablet.isBreaking(),"real tablet action begins mining its source target");
                check(appeng.api.movable.BlockEntityMoveStrategies.get(tablet)!=appeng.api.movable.BlockEntityMoveStrategies.getDefault(),"Tinkerer registers source movable callbacks into real AE2 registry");
                check(Ae2Movement.move(level,tablet,tablet.getBlockState(),tabletTarget)&&!tablet.isBreaking(),"AE2 begin invokes the source tablet stopBreaking callback before invalidation");
                check(level.getBlockEntity(tabletTarget) instanceof AnimationTabletBlockEntity replacement&&replacement!=tablet&&replacement.getItem(0).is(Items.IRON_PICKAXE),"AE2 creates valid tablet replacement with saved tool");
                var driveBlock=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive"));check(driveBlock!=Blocks.AIR,"actual AE2 drive exists in runtime fixture");level.setBlock(drivePos,driveBlock.defaultBlockState(),2);var drive=level.getBlockEntity(drivePos);drive.getPersistentData().putString("TTDriver","native-grid");
                helper.runAfterDelay(5,()->{try{
                    var oldHost=level.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,drivePos,null);check(oldHost!=null&&oldHost.getGridNode(Direction.UP)!=null,"native AE2 grid node initializes before move");var oldNode=oldHost.getGridNode(Direction.UP);
                    check(Ae2Movement.move(level,drive,drive.getBlockState(),driveTarget)&&level.getBlockEntity(driveTarget)!=drive&&drive.isRemoved(),"actual AE2 network device uses its supported reconstruction lifecycle");
                    helper.runAfterDelay(5,()->{try{
                        var newHost=level.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,driveTarget,null);check(newHost!=null&&newHost.getGridNode(Direction.UP)!=null&&newHost.getGridNode(Direction.UP)!=oldNode&&oldHost.getGridNode(Direction.UP)==null,"replacement owns a fresh native grid node; removed source node is retired");
                        check(level.getBlockEntity(driveTarget).getPersistentData().getString("TTDriver").equals("native-grid")&&level.getCapability(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,drivePos,null)==null,"AE2 passenger persistent data and capability relocation survive");
                        System.out.println("TT_AE2_MOVEMENT_PASS checks="+checks);helper.succeed();
                    }finally{cleanup.run();}});
                }catch(Throwable failure){cleanup.run();throw failure;}});
            }catch(Throwable failure){cleanup.run();throw failure;}
        }
    }
}
