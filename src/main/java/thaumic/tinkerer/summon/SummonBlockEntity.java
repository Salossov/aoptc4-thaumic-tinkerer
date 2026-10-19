package thaumic.tinkerer.summon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.crafting.IInfusionPedestal;
import thaumcraft.api.world.ChunkPreparationApi;

/** Ordered pedestal triples and the source's set-based (not multiset) aspect comparison. */
public final class SummonBlockEntity extends BlockEntity {
    private ChunkPreparationApi.Request chunks;
    private boolean pending;
    private long cycleTime;
    private record Pedestal(BlockPos pos,IInfusionPedestal inventory) { }
    public SummonBlockEntity(BlockPos pos,BlockState state) {super(SummonContent.TILE.get(),pos,state);}
    public boolean preparing() {return pending;}
    public void tick() {
        if(!(level instanceof ServerLevel server))return;
        if(server.hasNeighborSignal(worldPosition)){cancelWork();return;}
        if(!pending){if(server.getGameTime()%300!=0)return;pending=true;cycleTime=server.getGameTime();}
        if(chunks!=null&&chunks.closed()){cancelWork();return;}
        var columns=new LinkedHashSet<ChunkPos>();
        for(int x=-5;x<5;x++)for(int z=-5;z<5;z++)columns.add(new ChunkPos(worldPosition.offset(x,0,z)));
        boolean loaded=true;for(var column:columns)if(server.getChunkSource().getChunkNow(column.x,column.z)==null){loaded=false;break;}
        if(!loaded){if(chunks==null)chunks=ChunkPreparationApi.prepare(this,columns);if(!chunks.advance(2))return;}
        try {summon(server);}finally {cancelWork();}
    }
    private void summon(ServerLevel level) {
        for(int radius=1;radius<6;radius++){
            var pedestals=new ArrayList<Pedestal>();
            for(int x=-radius;x<radius;x++)for(int z=-radius;z<radius;z++){
                var pos=worldPosition.offset(x,0,z);
                if(level.getBlockEntity(pos) instanceof IInfusionPedestal pedestal&&pedestal.getItem(0).getItem() instanceof MobAspectItem)pedestals.add(new Pedestal(pos,pedestal));
            }
            for(var first:pedestals)for(var second:pedestals)for(var third:pedestals){
                if(first.inventory()==second.inventory()||second.inventory()==third.inventory()||first.inventory()==third.inventory())continue;
                var a=first.inventory().getItem(0);var b=second.inventory().getItem(0);var c=third.inventory().getItem(0);
                var aspects=List.of(MobAspectItem.aspect(a),MobAspectItem.aspect(b),MobAspectItem.aspect(c));var recipe=MobAspects.matching(aspects);if(recipe==null)continue;
                boolean infused=MobAspectItem.infused(a)&&MobAspectItem.infused(b)&&MobAspectItem.infused(c);
                if(infused&&cycleTime%1200!=0)return;
                if(!infused){first.inventory().setItem(0,ItemStack.EMPTY);second.inventory().setItem(0,ItemStack.EMPTY);third.inventory().setItem(0,ItemStack.EMPTY);}
                if(!infused||MobAspectItem.lastUsedMatches(a,worldPosition)&&MobAspectItem.lastUsedMatches(b,worldPosition)&&MobAspectItem.lastUsedMatches(c,worldPosition)){
                    var type=BuiltInRegistries.ENTITY_TYPE.getOptional(recipe.entity()).orElseThrow();
                    var prepared=SummonSpawns.prepare(level,(Mob)Objects.requireNonNull(type.create(level)));var entity=prepared.entity();entity.moveTo(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,0,0);
                    level.addFreshEntity(entity);
                    prepared.initialize(level);
                    trails(level,List.of(first,second,third),aspects);
                }
                if(infused)for(var pedestal:List.of(first,second,third)){MobAspectItem.markLastUsed(pedestal.inventory().getItem(0),worldPosition);pedestal.inventory().setChanged();}
                return;
            }
        }
    }
    private void trails(ServerLevel level,List<Pedestal> pedestals,List<thaumcraft.api.aspects.Aspect> aspects){
        for(int i=0;i<3;i++){
            var pos=pedestals.get(i).pos();int encoded=((pos.getX()-worldPosition.getX()+5)<<4)|(pos.getZ()-worldPosition.getZ()+5);
            level.blockEvent(worldPosition,getBlockState().getBlock(),encoded,MobAspects.ASPECTS.indexOf(aspects.get(i)));
        }
    }
    @Override public boolean triggerEvent(int id,int data){
        if(level==null||!level.isClientSide())return true;
        int x=(id>>4)-5,z=(id&15)-5;if(x< -5||x>4||z< -5||z>4||data<0||data>=MobAspects.ASPECTS.size())return false;
        ParticleApi.essentiaTrail(level,worldPosition.offset(x,0,z),worldPosition,20,MobAspects.ASPECTS.get(data).getColor(),20);return true;
    }
    public void cancelWork(){pending=false;if(chunks!=null){chunks.close();chunks=null;}}
    @Override public void setRemoved(){cancelWork();super.setRemoved();}
    @Override public void onChunkUnloaded(){cancelWork();super.onChunkUnloaded();}
}
