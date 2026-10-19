package thaumic.tinkerer.kami.construction;

import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.world.BlockPlacementApi;

/** Placement lifecycle, protection and snapshot rollback belong to the core API. */
final class MirrorPlacement {
    static boolean place(Player player,InteractionHand hand,BlockPos pos,Direction face,BlockState selected,IntPredicate consume){
        return BlockPlacementApi.place(player,hand,pos,face,selected,consume);
    }
    private MirrorPlacement(){}
}
