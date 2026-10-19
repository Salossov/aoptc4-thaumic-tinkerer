package thaumic.tinkerer.kami.construction;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.IAdvancedTool;
import thaumic.tinkerer.kami.tool.AwakenedMining;
import thaumic.tinkerer.kami.tool.AwakenedMiningShape.Kind;

/** Source material priority, first-match inventory swap and fluid-aware targeting. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class ProtoclayItem extends Item {
    private static final Map<ServerPlayer,Job> PENDING=new IdentityHashMap<>();
    public ProtoclayItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    private static boolean present(Player player,ItemStack stack){
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i)==stack)return true;
        return false;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!(entity instanceof Player player)||!present(player,stack))return;
        if(player instanceof ServerPlayer server){
            var pending=PENDING.get(server);
            if(pending!=null){if(pending.valid())return;pending.close();}
        }
        swap(player,stack,true);
    }
    private static void swap(Player player,ItemStack protoclay,boolean prepare){
        var held=player.getMainHandItem();
        if(held.isEmpty()||!(held.getItem() instanceof IAdvancedTool tool)||"sword".equals(tool.getToolType())||!player.swinging)return;
        var target=AwakenedMining.target(player,true,4.5);
        if(!target.missing().isEmpty()){
            if(prepare&&player instanceof ServerPlayer server&&server.isAlive()&&!server.isRemoved()&&server.getServer().getPlayerList().getPlayer(server.getUUID())==server){
                var job=new Job(server,protoclay,target.visited());PENDING.put(server,job);job.request.advance(2);
            }
            return;
        }
        if(target.hit().getType()!=HitResult.Type.BLOCK)return;
        var state=player.level().getBlockState(target.hit().getBlockPos());
        String wanted="";
        for(var kind:Kind.values())if(AwakenedMining.rightMaterial(state,kind)){wanted=kind.name().toLowerCase(Locale.ROOT);break;}
        if(wanted.isEmpty()||wanted.equals(tool.getToolType()))return;
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++){
            var candidate=inventory.getItem(i);
            if(candidate!=held&&!candidate.isEmpty()&&candidate.getItem() instanceof IAdvancedTool other&&wanted.equals(other.getToolType())){
                inventory.setItem(inventory.selected,candidate);inventory.setItem(i,held);inventory.setChanged();
                if(player instanceof ServerPlayer server){server.inventoryMenu.broadcastChanges();if(server.containerMenu!=server.inventoryMenu)server.containerMenu.broadcastChanges();}
                return;
            }
        }
    }
    private static final class Job implements AutoCloseable {
        final ServerPlayer player;final ServerLevel level;final ItemStack protoclay,tool,snapshot;final int slot;
        final double x,y,z;final float yaw,pitch;final ChunkPreparationApi.Request request;
        Job(ServerPlayer player,ItemStack protoclay,Collection<net.minecraft.world.level.ChunkPos> chunks){
            this.player=player;level=player.serverLevel();this.protoclay=protoclay;tool=player.getMainHandItem();snapshot=tool.copy();slot=player.getInventory().selected;
            x=player.getX();y=player.getY();z=player.getZ();yaw=player.getYRot();pitch=player.getXRot();request=ChunkPreparationApi.prepare(player,chunks);
        }
        boolean valid(){return !request.closed()&&player.level()==level&&player.isAlive()&&!player.isRemoved()&&player.swinging
                &&level.getServer().getPlayerList().getPlayer(player.getUUID())==player&&present(player,protoclay)
                &&player.getInventory().selected==slot&&player.getMainHandItem()==tool&&ItemStack.matches(tool,snapshot)
                &&player.getX()==x&&player.getY()==y&&player.getZ()==z&&player.getYRot()==yaw&&player.getXRot()==pitch;}
        @Override public void close(){request.close();if(PENDING.get(player)==this)PENDING.remove(player);}
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:List.copyOf(PENDING.values()))if(job.level==level){
            if(!job.valid()){job.close();continue;}
            if(job.request.advance(2))try{if(job.valid())swap(job.player,job.protoclay,false);}finally{job.close();}
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event){for(var job:List.copyOf(PENDING.values()))if(job.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:List.copyOf(PENDING.values()))if(job.level.getServer()==event.getServer())job.close();}
}
