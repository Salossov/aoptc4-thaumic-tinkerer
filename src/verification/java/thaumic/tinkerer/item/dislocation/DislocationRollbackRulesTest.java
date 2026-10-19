package thaumic.tinkerer.item.dislocation;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;

/** Native rollback regressions at the package-private snapshot boundary. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DislocationRollbackRulesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void detachedInventoryRestoresWhenStateWriteNeverHappens(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,Blocks.CHEST.defaultBlockState(),18);var state=level.getBlockState(pos);
        var chest=(ChestBlockEntity)level.getBlockEntity(pos);chest.setItem(0,new ItemStack(Items.EMERALD,23));
        boolean outerCapture=level.captureBlockSnapshots;var outer=List.copyOf(level.capturedBlockSnapshots);
        var marker=BlockSnapshot.create(level.dimension(),level,pos,18);level.capturedBlockSnapshots.add(marker);level.captureBlockSnapshots=true;
        try{
            try(var scope=new DislocationSnapshots(level,List.of(pos))){scope.detach(pos);check(level.getChunkAt(pos).getBlockEntities().get(pos)==null,"entity really detached before attempted write");}
            check(level.getBlockState(pos)==state,"unchanged old state kept");
            var restored=(ChestBlockEntity)level.getBlockEntity(pos);check(restored!=null&&restored.getItem(0).is(Items.EMERALD)&&restored.getItem(0).getCount()==23,"failed-write rollback recreates and loads the original entity");
            check(level.captureBlockSnapshots&&level.capturedBlockSnapshots.contains(marker),"outer native capture scope survives");
            check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"no duplicate inventory drops");restored.clearContent();
        }finally{level.captureBlockSnapshots=outerCapture;level.capturedBlockSnapshots.clear();level.capturedBlockSnapshots.addAll(outer);}
        helper.succeed();
    }
    @GameTest(template="empty",batch="dislocation_server",timeoutTicks=100)
    public static void partialAssemblyRollbackRestoresBothPartsBeforeNotifications(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,2,2));
        var lower=Blocks.OAK_DOOR.defaultBlockState();var upper=lower.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER);
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),18);level.setBlock(pos,lower,18);level.setBlock(pos.above(),upper,18);
        try(var scope=new DislocationSnapshots(level,List.of(pos,pos.above()))){check(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3),"first part actually removed");}
        check(level.getBlockState(pos)==lower&&level.getBlockState(pos.above())==upper,"aborted partial mutation restores intact assembly");
        check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).expandTowards(0,1,0).inflate(1)).isEmpty(),"rollback never drops a door partner");
        helper.succeed();
    }
    private DislocationRollbackRulesTest(){}
}
