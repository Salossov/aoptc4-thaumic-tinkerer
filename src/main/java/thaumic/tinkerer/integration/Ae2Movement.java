package thaumic.tinkerer.integration;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.api.world.BlockEntityMovementApi;
import thaumcraft.api.world.IMovableBlockEntity;
import thaumic.tinkerer.ThaumicTinkerer;

/** Optional public AE2 API adapter. The nested backend is never loaded without AE2. */
public final class Ae2Movement {
    public static boolean present(){return ModList.get().isLoaded("ae2");}
    public static void register(IEventBus bus){bus.addListener((FMLCommonSetupEvent event)->{if(present())event.enqueueWork(Backend::register);});}
    public static boolean move(ServerLevel level,BlockEntity passenger,BlockState state,BlockPos destination){return present()&&Backend.move(level,passenger,state,destination);}
    private static final class Backend {
        private static final Set<String> MARKED_TYPES=Set.of("magnet","mob_magnet","enchanter","aspect_analyzer","animation_tablet","interface","dislocator","golem_connector","platform","funnel","repairer","levitational_locomotive_relay");
        static void register(){
            appeng.api.movable.BlockEntityMoveStrategies.add(new appeng.api.movable.DefaultBlockEntityMoveStrategy(){
                @Override public boolean canHandle(BlockEntityType<?> type){var id=BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);return id!=null&&id.getNamespace().equals(ThaumicTinkerer.ID)&&MARKED_TYPES.contains(id.getPath());}
                @Override public net.minecraft.nbt.CompoundTag beginMove(BlockEntity entity,net.minecraft.core.HolderLookup.Provider registries){
                    if(entity instanceof IMovableBlockEntity movable&&!movable.prepareToMove())return null;
                    return super.beginMove(entity,registries);
                }
                @Override public boolean completeMove(BlockEntity entity,BlockState state,net.minecraft.nbt.CompoundTag data,net.minecraft.world.level.Level level,BlockPos destination){
                    boolean moved=super.completeMove(entity,state,data,level,destination);
                    if(moved&&level.getBlockEntity(destination) instanceof IMovableBlockEntity movable)movable.doneMoving();
                    return moved;
                }
            });
        }
        static boolean move(ServerLevel level,BlockEntity entity,BlockState state,BlockPos destination){
            if(state.is(appeng.api.ids.AETags.SPATIAL_BLACKLIST))return false;
            var origin=entity.getBlockPos().immutable();var previous=level.getBlockState(destination);
            var recovery=entity.saveWithFullMetadata(level.registryAccess());
            var strategy=appeng.api.movable.BlockEntityMoveStrategies.get(entity);
            var data=strategy.beginMove(entity,level.registryAccess());if(data==null)return false;
            if(!level.setBlock(destination,state,3))return false;
            if(!BlockEntityMovementApi.detach(level,entity)){level.setBlock(destination,previous,3);return false;}
            level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            level.removeBlockEntity(destination); // Retire native placement's empty placeholder/listeners.
            if(strategy.completeMove(entity,state,data,level,destination)&&level.getBlockEntity(destination)!=null){
                level.getBlockEntity(destination).setChanged();level.sendBlockUpdated(destination,state,level.getBlockState(destination),3);return true;
            }
            // AE2 strategies may use a nonstandard data payload. Recover using an
            // independent native snapshot, never reuse the invalidated old object.
            level.removeBlockEntity(destination);level.setBlock(destination,previous,3);level.setBlock(origin,state,3);level.removeBlockEntity(origin);
            var restored=BlockEntity.loadStatic(origin,state,recovery,level.registryAccess());
            if(restored==null)throw new IllegalStateException("Could not recover AE2 passenger at "+origin);
            level.setBlockEntity(restored);restored.setChanged();level.sendBlockUpdated(origin,state,state,3);return false;
        }
    }
    private Ae2Movement(){}
}
