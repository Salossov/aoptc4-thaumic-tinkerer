package thaumic.tinkerer.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.*;

public final class PlatformBlock extends CamoBlock {
    public PlatformBlock(){super(Properties.of().mapColor(MapColor.WOOD).strength(2,3).sound(SoundType.WOOD).ignitedByLava().noOcclusion());}
    @Override protected MapCodec<? extends PlatformBlock> codec(){return simpleCodec(properties->new PlatformBlock());}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PlatformBlockEntity(pos,state);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(!(context instanceof EntityCollisionContext entityContext)||entityContext.getEntity()==null)return Shapes.empty();
        var entity=entityContext.getEntity();boolean player=entity instanceof Player;
        // User-approved conversion of the old client posY (feet + yOffset 1.62).
        if(entity.getY()+(player?1.62:0)>pos.getY()+(player?2:0)&&(!player||!entity.isShiftKeyDown()))return Shapes.block();
        return Shapes.empty();
    }
    @Override protected boolean isPathfindable(BlockState state,PathComputationType type){return false;}
}
