package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.world.BlockEntityMovementApi;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class BlockEntityMovementRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static BlockEntity registered(net.minecraft.server.level.ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4).getBlockEntities().get(pos);}
    @GameTest(template="empty",timeoutTicks=80)
    public static void movementIdentity(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var source=new BlockPos((anchor.getX()>>4)*16+3,Math.min(anchor.getY()+300,level.getMaxBuildHeight()-8),(anchor.getZ()>>4)*16+3);var destination=source.east(2);var crossChunk=source.west(5);var invalid=source.north(2);var positions=List.of(source,destination,crossChunk,invalid);
        var player=FocusRulesTest.player(helper,"[TTMovement]");player.setPos(source.getCenter());
        try{
            for(var pos:positions)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            check(BlockEntityMovementApi.loaded(level,source)&&BlockEntityMovementApi.loaded(level,destination),"API uses already loaded native fixture chunks");
            check(!BlockEntityMovementApi.loaded(level,new BlockPos(source.getX(),level.getMaxBuildHeight(),source.getZ())),"out-of-height destination rejected without loading a chunk");
            level.setBlock(source,Blocks.CHEST.defaultBlockState(),2);var chest=(ChestBlockEntity)level.getBlockEntity(source);var stack=new ItemStack(Items.DIAMOND,7);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("retained-stack"));chest.setItem(0,stack);chest.getPersistentData().putString("TTMoveIdentity","same-object");
            var menu=chest.createMenu(91,player.getInventory(),player);var oldCapability=level.getCapability(Capabilities.ItemHandler.BLOCK,source,Direction.UP);
            check(menu!=null&&menu.stillValid(player)&&menu.slots.getFirst().container==chest,"actual native chest menu initially references this object");
            check(!BlockEntityMovementApi.relocate(level,chest,invalid)&&level.getBlockEntity(source)==chest&&!chest.isRemoved(),"incompatible destination rejects before touching source identity or registration");
            check(BlockEntityMovementApi.detach(level,chest)&&chest.isRemoved()&&registered(level,source)==null,"detach removes the registered object and old ticker/listener binding");
            check(!BlockEntityMovementApi.detach(level,chest),"a second detach does not invent another registered copy");
            level.setBlock(source,Blocks.AIR.defaultBlockState(),2);level.setBlock(destination,Blocks.CHEST.defaultBlockState(),2);var placeholder=level.getBlockEntity(destination);
            check(placeholder!=chest,"native destination placement creates its ordinary fresh placeholder");
            check(BlockEntityMovementApi.relocate(level,chest,destination),"public backend reattaches the detached object");
            check(level.getBlockEntity(destination)==chest&&chest.getBlockPos().equals(destination)&&!chest.isRemoved()&&placeholder.isRemoved(),"same object moves and destination placeholder is retired");
            check(chest.getItem(0)==stack&&chest.getItem(0).getCount()==7&&chest.getPersistentData().getString("TTMoveIdentity").equals("same-object"),"inventory stack identity, components and transient persistent-data object survive without NBT reconstruction");
            check(menu.slots.getFirst().container==chest&&menu.stillValid(player)&&menu.slots.getFirst().getItem()==stack,"existing menu retains live slots and validity after move");
            check(oldCapability!=null&&oldCapability.getStackInSlot(0)==stack&&level.getCapability(Capabilities.ItemHandler.BLOCK,source,Direction.UP)==null,"old live capability still references the object; source lookup invalidates");
            var newCapability=level.getCapability(Capabilities.ItemHandler.BLOCK,destination,Direction.UP);check(newCapability!=null&&newCapability.getStackInSlot(0)==stack,"destination capability exposes the same inventory");
            var tag=chest.saveWithFullMetadata(level.registryAccess());check(tag.getInt("x")==destination.getX()&&tag.getInt("y")==destination.getY()&&tag.getInt("z")==destination.getZ(),"native NBT writes the new position");
            check(level.getEntitiesOfClass(ItemEntity.class,new AABB(source).inflate(3)).isEmpty(),"moving a populated chest does not drop or duplicate its inventory");
            check(BlockEntityMovementApi.loaded(level,crossChunk),"cross-chunk fixture is already loaded; movement never calls a loading getter");
            level.setBlock(crossChunk,Blocks.CHEST.defaultBlockState(),2);check(BlockEntityMovementApi.relocate(level,chest,crossChunk)&&registered(level,destination)==null&&level.getBlockEntity(crossChunk)==chest,"automatic detach handles a second move across a chunk boundary");
            check(chest.getBlockPos().equals(crossChunk)&&chest.getItem(0)==stack,"cross-chunk move retains position and inventory identity");
            // Moving the object does not decide which source block states to erase.
            check(level.getBlockState(destination).is(Blocks.CHEST),"API leaves block-state ownership with the mover");
            var reloaded=(ChestBlockEntity)BlockEntity.loadStatic(crossChunk,chest.getBlockState(),chest.saveWithFullMetadata(level.registryAccess()),level.registryAccess());check(reloaded.getItem(0).getCount()==7&&ItemStack.isSameItemSameComponents(reloaded.getItem(0),stack),"saved relocated inventory round-trips through the real native loader");
            System.out.println("TT_BLOCK_ENTITY_MOVEMENT_PASS checks="+checks);helper.succeed();
        }finally{
            for(var pos:positions){if(level.getBlockEntity(pos) instanceof Container container)container.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
            level.getEntitiesOfClass(ItemEntity.class,new AABB(source).inflate(6)).forEach(ItemEntity::discard);
        }
    }
}
