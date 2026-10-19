package thaumic.tinkerer.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.Aspects;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.crop.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class CropModelDataRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    @GameTest(template="summon_space",batch="crop_model_data",timeoutTicks=100)
    public static void nativeCropSnapshotAndSerializedAppearance(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(8,4,8));
        level.setBlockAndUpdate(pos.below(),InfusedContent.SOIL.get().defaultBlockState());
        var state=InfusedContent.CROP.get().defaultBlockState().setValue(InfusedCropBlock.AGE,3);
        level.setBlockAndUpdate(pos,state);var tile=(InfusedCropBlockEntity)level.getBlockEntity(pos);
        check(tile!=null&&state.getRenderShape()==RenderShape.MODEL,"registered crop uses the chunk model route");
        tile.aspect=Aspects.FIRE;var original=tile.getModelData();
        check(original.get(CropRenderData.PROPERTY).equals(new CropRenderData(1,1,0xffffff)),"primal pixels stay untinted at source texture age");
        tile.aspect=Aspects.FLIGHT;state=state.setValue(InfusedCropBlock.AGE,7);level.setBlockAndUpdate(pos,state);
        var compound=tile.getModelData().get(CropRenderData.PROPERTY);
        check(compound.equals(new CropRenderData(6,3,Aspects.FLIGHT.getColor()&0xffffff)),"compound tint and mature texture are snapshotted");
        check(original.get(CropRenderData.PROPERTY).equals(new CropRenderData(1,1,0xffffff)),"published ModelData has no live mutable tile reference");
        var copy=InfusedContent.CROP_TILE.get().create(pos,state);check(copy!=null,"actual registered block entity factory");
        copy.loadWithComponents(tile.getUpdateTag(level.registryAccess()),level.registryAccess());
        check(compound.equals(copy.getModelData().get(CropRenderData.PROPERTY)),"real update tag retains the complete visual appearance");
        tile.aspect=null;check(tile.getModelData().get(CropRenderData.PROPERTY).equals(new CropRenderData(6,3,0xffffff)),"missing aspect is generic and untinted");
        helper.succeed();
    }
    private CropModelDataRulesTest(){}
}
