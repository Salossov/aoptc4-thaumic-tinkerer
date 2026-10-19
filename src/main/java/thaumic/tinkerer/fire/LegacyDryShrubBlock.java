package thaumic.tinkerer.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Tallgrass metadata 0: visually dry, but still grass rather than a stick-dropping dead bush. */
public final class LegacyDryShrubBlock extends TallGrassBlock {
    public LegacyDryShrubBlock(){super(Properties.ofFullCopy(Blocks.SHORT_GRASS));}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.box(.5f-.4f,0,.5f-.4f,.5f+.4f,.8f,.5f+.4f);}
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state){return false;}
}
