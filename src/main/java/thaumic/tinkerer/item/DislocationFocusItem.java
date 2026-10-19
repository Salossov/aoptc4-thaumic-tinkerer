package thaumic.tinkerer.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.*;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.util.ItemData;

/** Release-164 block capture. Full native block states replace the old metadata integer. */
public final class DislocationFocusItem extends ItemFocusBasic implements FocusBehavior {
    public DislocationFocusItem(){super(new Properties().durability(1).rarity(Rarity.RARE));}
    @Override public boolean isRepairable(ItemStack stack){return false;}
    @Override public int getEnchantmentValue(){return 5;}
    @Override public int getFocusColor(ItemStack stack){return 16757248;}
    @Override public Map<Aspect,Integer> getVisCost(ItemStack stack){return Map.of(Aspects.ENTROPY,500,Aspects.ORDER,500,Aspects.EARTH,100);}
    @Override public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack,int rank){return List.of(FocusUpgradeType.treasure);}
    @Override public boolean managesVis(){return true;}
    @Override public String getSortingHelper(ItemStack focus){
        var data=ItemData.read(focus);if(!data.getBoolean("available"))return "DISLOCATION";
        var id=net.minecraft.resources.ResourceLocation.tryParse(data.getString("blockName"));
        var block=id==null?Optional.<net.minecraft.world.level.block.Block>empty():net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(id);
        // Original getPickedBlockStack creates a fresh, tagless stack: hash is always 0.
        return block.map(value->"DISLOCATION"+value.getDescriptionId()+"-0").orElse("DISLOCATION");
    }
    public Optional<BlockState> captured(ItemStack focus,net.minecraft.core.HolderLookup.Provider registries){
        var tag=ItemData.read(focus);if(!tag.getBoolean("available"))return Optional.empty();
        if(tag.contains("blockState",10))return Optional.of(NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),tag.getCompound("blockState")));
        var name=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("blockName"));
        return name==null?Optional.empty():net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(name).map(net.minecraft.world.level.block.Block::defaultBlockState);
    }
    @Override public boolean cast(ServerPlayer player,ItemStack wand,ItemStack focus){
        if(!player.server.isSameThread())return false;
        var level=player.serverLevel();var from=player.position().add(0,1.62,0);
        var hit=level.clip(new ClipContext(from,from.add(player.getLookAngle().scale(5)),ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
        return hit.getType()==HitResult.Type.BLOCK&&move(player,wand,focus,hit);
    }
    private boolean move(ServerPlayer player,ItemStack wand,ItemStack focus,BlockHitResult hit){
        var level=player.serverLevel();var origin=hit.getBlockPos();
        if(!level.getServer().isSameThread()||!level.hasChunkAt(origin)||!player.mayUseItemAt(origin,hit.getDirection(),wand))return false;
        var clicked=level.getBlockState(origin);var stored=captured(focus,level.registryAccess());
        if(stored.isPresent()){
            var destination=origin.relative(hit.getDirection());
            if(!level.hasChunkAt(destination)||level.isOutsideBuildHeight(destination))return false;
            // Original asks the CLICKED block's placement predicate, not the captured one.
            if(!level.getBlockState(destination).canBeReplaced()||!clicked.canSurvive(level,destination))return false;
            var state=stored.get();var data=ItemData.read(focus).getCompound("tileCmp").copy();
            if(!level.setBlock(destination,state,3))return false;
            state.getBlock().setPlacedBy(level,destination,state,player,wand);
            if(!data.isEmpty()){
                data.putInt("x",destination.getX());data.putInt("y",destination.getY());data.putInt("z",destination.getZ());
                var restored=BlockEntity.loadStatic(destination,state,data,level.registryAccess());
                if(restored!=null){level.setBlockEntity(restored);restored.setChanged();}
            }
            ItemData.update(focus,tag->tag.putBoolean("available",false));WandApi.focus(wand,focus);
            level.sendBlockUpdated(destination,state,state,3);
            bursts(player,destination,true);
            level.playSound(null,player.blockPosition(),SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:wand")),SoundSource.PLAYERS,.5f,1);
            return true;
        }
        if(clicked.is(Blocks.MOVING_PISTON)||clicked.is(Blocks.PISTON_HEAD)||ThaumcraftApi.portableHoleBlackList.contains(clicked.getBlock())||clicked.getDestroySpeed(level,origin)==-1)return false;
        var tile=level.getBlockEntity(origin);boolean spawner=tile instanceof SpawnerBlockEntity;
        var cost=tile==null?getVisCost(focus):spawner?Map.of(Aspects.ENTROPY,10000,Aspects.ORDER,10000,Aspects.EARTH,5000):Map.of(Aspects.ENTROPY,2500,Aspects.ORDER,2500,Aspects.EARTH,500);
        if(!WandApi.consume(wand,player,cost,true))return false;
        var data=tile==null?new CompoundTag():tile.saveWithFullMetadata(level.registryAccess());
        var sound=clicked.getSoundType(level,origin,player).getBreakSound();
        level.removeBlockEntity(origin);level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
        ItemData.update(focus,tag->{tag.putString("blockName",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(clicked.getBlock()).toString());tag.putInt("blockMeta",0);tag.put("blockState",NbtUtils.writeBlockState(clicked));tag.put("tileCmp",data);tag.putBoolean("available",true);});WandApi.focus(wand,focus);
        bursts(player,origin,false);
        level.playSound(null,player.blockPosition(),sound,SoundSource.PLAYERS,1,1);
        level.playSound(null,player.blockPosition(),SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:wand")),SoundSource.PLAYERS,.5f,1);
        return true;
    }
    private static void bursts(ServerPlayer player,BlockPos pos,boolean placed){
        for(int i=0;i<8;i++){
            float x=(float)(pos.getX()+Math.random()),y=(float)(pos.getY()+Math.random())+(placed?.65f:0),z=(float)(pos.getZ()+Math.random());
            thaumcraft.api.client.ParticleApi.burst(player.level(),x,y,z,.2f);
        }
    }
}
