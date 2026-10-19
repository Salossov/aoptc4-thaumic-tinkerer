package thaumic.tinkerer.kami.bedrock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Original tile scans all entities, independently of player block collision. */
public final class BedrockPortalBlockEntity extends BlockEntity {
    public BedrockPortalBlockEntity(BlockPos pos,BlockState state){super(BedrockContent.PORTAL_TILE.get(),pos,state);}
    public static void tick(Level level,BlockPos pos,BlockState state,BedrockPortalBlockEntity tile){
        if(level.isClientSide())return;
        for(var entity:level.getEntitiesOfClass(Entity.class,new AABB(pos)))BedrockEntityPortals.touch(tile,entity);
    }
    @Override public void setRemoved(){if(level instanceof net.minecraft.server.level.ServerLevel)BedrockEntityPortals.removed(this);super.setRemoved();}
    @Override public void onChunkUnloaded(){if(level instanceof net.minecraft.server.level.ServerLevel)BedrockEntityPortals.removed(this);super.onChunkUnloaded();}
}
