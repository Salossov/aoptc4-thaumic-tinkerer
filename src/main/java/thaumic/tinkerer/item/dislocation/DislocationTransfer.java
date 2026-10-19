package thaumic.tinkerer.item.dislocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.util.ItemData;

/** World/inventory transfer with source prices and accepted complete-assembly rollback. */
public final class DislocationTransfer {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private record Cell(BlockPos pos,BlockState state,BlockEntity entity){}
    private static final Set<ServerLevel> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Aspect,Integer> BASIC=Map.of(Aspects.ENTROPY,500,Aspects.ORDER,500,Aspects.EARTH,100);
    private static final Map<Aspect,Integer> TILE=Map.of(Aspects.ENTROPY,2500,Aspects.ORDER,2500,Aspects.EARTH,500);
    private static final Map<Aspect,Integer> SPAWNER=Map.of(Aspects.ENTROPY,10000,Aspects.ORDER,10000,Aspects.EARTH,5000);

    public static boolean apply(ServerPlayer player,ItemStack wand,ItemStack focus,DislocationRay.Trace ray) {
        var level=player.serverLevel();
        if(!level.getServer().isSameThread()||!sameWand(player,wand,focus)||!ACTIVE.add(level))return false;
        try {
            var hit=ray.hit();
            if(!ray.complete()||hit==null||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK||!allowed(player,wand,hit.getBlockPos(),hit))return false;
            boolean available=ItemData.flag(focus,"available");var capture=DislocationCapture.read(focus);
            if(available)return capture.isPresent()&&place(player,wand,focus,capture.get(),hit);
            return capture(player,wand,focus,hit);
        }finally{ACTIVE.remove(level);}
    }
    private static boolean sameWand(ServerPlayer player,ItemStack wand,ItemStack focus){return (player.getMainHandItem()==wand||player.getOffhandItem()==wand)&&ItemStack.matches(focus,WandApi.focus(wand));}
    private static boolean allowed(ServerPlayer player,ItemStack wand,BlockPos pos,BlockHitResult hit){
        var level=player.serverLevel();
        return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)
                &&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null
                &&player.gameMode.getGameModeForPlayer()!=GameType.SPECTATOR&&level.mayInteract(player,pos)
                &&player.mayUseItemAt(pos,hit.getDirection(),wand);
    }
    private static Cell cell(ServerLevel level,BlockPos pos){
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk==null?null:new Cell(pos.immutable(),chunk.getBlockState(pos),chunk.getBlockEntity(pos));
    }
    private static boolean unchanged(ServerLevel level,List<Cell> cells){
        for(var expected:cells){var current=cell(level,expected.pos());if(current==null||current.state()!=expected.state()||current.entity()!=expected.entity())return false;}return true;
    }
    private static boolean capturable(ServerPlayer player,ItemStack wand,Cell cell,BlockHitResult hit){
        var state=cell.state();
        return allowed(player,wand,cell.pos(),hit)&&!state.isAir()&&!state.is(Blocks.MOVING_PISTON)&&!state.is(Blocks.PISTON_HEAD)
                &&!ThaumcraftApi.portableHoleBlackList.contains(state.getBlock())&&state.getDestroySpeed(player.level(),cell.pos())!=-1
                &&(!(state.getBlock() instanceof GameMasterBlock)||player.canUseGameMasterBlocks());
    }
    private static boolean capture(ServerPlayer player,ItemStack wand,ItemStack focus,BlockHitResult hit){
        var level=player.serverLevel();var origin=hit.getBlockPos();var root=cell(level,origin);if(root==null)return false;
        var cells=new ArrayList<Cell>();cells.add(root);
        var offset=DislocationCapture.partnerOffset(root.state());
        if(offset!=null){var other=cell(level,origin.offset(offset));if(other==null||!DislocationCapture.partners(root.state(),other.state()))return false;cells.add(other);}
        for(var entry:cells)if(!capturable(player,wand,entry,hit))return false;
        for(var entry:cells){
            // This is a magical capture, not a playerDestroy/drop operation. The
            // native protection event still covers every cell before any mutation.
            if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,entry.pos(),entry.state(),player)).isCanceled())return false;
        }
        if(!unchanged(level,cells)||!sameWand(player,wand,focus))return false;
        var price=BASIC;
        for(var entry:cells)if(entry.entity() instanceof SpawnerBlockEntity){price=SPAWNER;break;}
        else if(entry.entity()!=null&&!(entry.state().getBlock() instanceof BedBlock))price=TILE;
        if(!WandApi.consume(wand,player,price,false))return false;
        var parts=new ArrayList<DislocationCapture.Part>();
        for(var entry:cells)parts.add(new DislocationCapture.Part(entry.pos().subtract(origin),entry.state(),entry.entity()==null?new CompoundTag():entry.entity().saveWithFullMetadata(level.registryAccess())));
        var captured=new DislocationCapture(parts);var stored=focus.copy();captured.store(stored);
        var sound=root.state().getSoundType(level,origin,player).getBreakSound();
        try(var snapshots=new DislocationSnapshots(level,cells.stream().map(Cell::pos).toList())){
            for(var entry:cells){
                if(level.getBlockState(entry.pos())!=entry.state())return false;
                snapshots.detach(entry.pos());
                if(!level.setBlock(entry.pos(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return false;
            }
            snapshots.finishMutation();
            for(var entry:cells)if(!level.getBlockState(entry.pos()).is(Blocks.AIR))return false;
            if(!sameWand(player,wand,focus))return false;
            var previousVis=new LinkedHashMap<Aspect,Integer>();
            for(var aspect:price.keySet())previousVis.put(aspect,WandApi.stored(wand,aspect));
            boolean saved=false;
            try{
                if(!WandApi.consume(wand,player,price,true))return false;
                WandApi.focus(wand,stored);
                // The native casting callback persists its mutable focus
                // argument after success. Return the same payload through it.
                focus.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stored.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
                saved=true;
            }finally{
                // If persisting the payload fails, rollback owns the blocks;
                // restore the matching vis rather than charging a failed move.
                if(!saved)previousVis.forEach((aspect,value)->WandApi.set(wand,aspect,value));
            }
            snapshots.commit();
        }
        bursts(level,origin,false);sound(level,player,sound,1);wandSound(level,player);return true;
    }
    private static boolean place(ServerPlayer player,ItemStack wand,ItemStack focus,DislocationCapture capture,BlockHitResult hit){
        var level=player.serverLevel();var destination=hit.getBlockPos().relative(hit.getDirection());
        if(DislocationCapture.partnerOffset(capture.root().state())!=null&&capture.parts().size()!=2)return false;
        var cells=new ArrayList<Cell>();var payloads=new LinkedHashMap<BlockPos,CompoundTag>();
        for(var part:capture.parts()){
            var pos=destination.offset(part.offset());
            if(!allowed(player,wand,pos,hit)||part.state().isAir()||!part.state().getBlock().isEnabled(level.enabledFeatures())
                    ||part.state().getBlock() instanceof GameMasterBlock&&!player.canUseGameMasterBlocks())return false;
            var current=cell(level,pos);
            if(current==null||!current.state().canBeReplaced()||current.entity()!=null)return false;
            cells.add(current);var data=part.tile();
            if(!data.isEmpty()){
                if(!(part.state().getBlock() instanceof EntityBlock block))return false;
                var trial=block.newBlockEntity(pos,part.state());
                if(trial==null||!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(trial.getType()).toString().equals(data.getString("id")))return false;
                data.putInt("x",pos.getX());data.putInt("y",pos.getY());data.putInt("z",pos.getZ());
                // Validate loading before touching the world; never instantiate an
                // unrelated BlockEntity selected solely by a forged NBT id.
                try{trial.loadWithComponents(data,level.registryAccess());}
                catch(RuntimeException invalid){LOGGER.warn("Cannot restore dislocation payload for {}",part.state().getBlock(),invalid);return false;}
                payloads.put(pos,data);
            }
        }
        if(!sameWand(player,wand,focus)||!unchanged(level,cells))return false;
        var cleared=focus.copy();ItemData.update(cleared,data->data.putBoolean("available",false));
        try(var snapshots=new DislocationSnapshots(level,cells.stream().map(Cell::pos).toList())){
            for(var part:capture.parts())if(!level.setBlock(destination.offset(part.offset()),part.state(),Block.UPDATE_ALL))return false;
            // Call native assembly setup at the lower/foot anchor, even when
            // the originally selected cell was the upper/head part.
            var anchor=capture.root();
            for(var part:capture.parts())if(part.state().getBlock() instanceof BedBlock&&part.state().getValue(BedBlock.PART)==BedPart.FOOT
                    ||part.state().getBlock() instanceof DoorBlock&&part.state().getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER
                    ||part.state().getBlock() instanceof DoublePlantBlock&&part.state().getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER){anchor=part;break;}
            anchor.state().getBlock().setPlacedBy(level,destination.offset(anchor.offset()),anchor.state(),player,wand);
            // Native double-plant setup builds a default upper half. Keep the
            // captured state of both cells, including modern variant properties.
            for(var part:capture.parts()){
                var pos=destination.offset(part.offset());
                if(level.getBlockState(pos)!=part.state()&&!level.setBlock(pos,part.state(),Block.UPDATE_ALL))return false;
            }
            for(var entry:payloads.entrySet()){
                var entity=level.getBlockEntity(entry.getKey());
                if(entity==null||!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString().equals(entry.getValue().getString("id")))return false;
                entity.loadWithComponents(entry.getValue(),level.registryAccess());entity.setChanged();
            }
            for(var part:capture.parts()){
                var pos=destination.offset(part.offset());var state=level.getBlockState(pos);
                if(state!=part.state()||!state.canSurvive(level,pos))return false;
            }
            snapshots.finishMutation();var changes=snapshots.snapshots();
            boolean canceled=changes.size()==1?EventHooks.onBlockPlace(player,changes.getFirst(),hit.getDirection()):EventHooks.onMultiBlockPlace(player,changes,hit.getDirection());
            if(canceled||!sameWand(player,wand,focus))return false;
            // Protection callbacks may have changed a cell while inspecting it.
            for(var part:capture.parts())if(level.getBlockState(destination.offset(part.offset()))!=part.state())return false;
            WandApi.focus(wand,cleared);
            focus.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,cleared.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA));
            snapshots.commit();
        }
        bursts(level,destination,true);wandSound(level,player);return true;
    }
    private static void bursts(ServerLevel level,BlockPos pos,boolean placed){
        for(int count=0;count<8;count++){
            float x=(float)(pos.getX()+Math.random()),y=(float)(pos.getY()+Math.random())+(placed?.65f:0),z=(float)(pos.getZ()+Math.random());
            ParticleApi.broadcast(level,new ParticleApi.Effect(ParticleApi.Kind.BURST,x,y,z,.2f,-1,1,1,1,false,false,0));
        }
    }
    private static void sound(ServerLevel level,ServerPlayer player,SoundEvent sound,float volume){level.playSound(null,player.getX(),player.getY(),player.getZ(),sound,SoundSource.PLAYERS,volume,1);}
    private static void wandSound(ServerLevel level,ServerPlayer player){sound(level,player,SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:wand")),.5f);}
    private DislocationTransfer(){}
}
