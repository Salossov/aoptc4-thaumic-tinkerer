package thaumic.tinkerer.kami.bedrock;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Release portal has a full selection cube, no collision and no item form. */
public final class BedrockPortalBlock extends Block {
    public BedrockPortalBlock(){this(Properties.of().mapColor(MapColor.NONE).strength(-1,3600000).noCollission().noOcclusion().noLootTable().sound(SoundType.STONE));}
    private BedrockPortalBlock(Properties properties){super(properties);}
    @Override protected MapCodec<BedrockPortalBlock> codec(){return simpleCodec(BedrockPortalBlock::new);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide())level.setBlockAndUpdate(pos,Blocks.BEDROCK.defaultBlockState());
        return InteractionResult.PASS;
    }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
        if(entity instanceof net.minecraft.server.level.ServerPlayer player)BedrockPortals.touch(player,pos);
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity){return false;}
    @Override public boolean canHarvestBlock(BlockState state,BlockGetter level,BlockPos pos,Player player){return false;}
}
