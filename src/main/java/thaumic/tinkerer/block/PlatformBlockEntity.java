package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class PlatformBlockEntity extends CamoBlockEntity {
    public PlatformBlockEntity(BlockPos pos,BlockState state){super(PlatformContent.TILE.get(),pos,state);}
}
