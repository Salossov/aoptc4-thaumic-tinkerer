package thaumic.tinkerer.crop;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.entities.LivingAttackAttemptEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.ForcefieldContent;

/** Release-164 tracking, including both tick phases and the strict > expiry. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class InfusedPotionEffects {
    public static final Map<Entity,Long> AIR_HITS=new HashMap<>(),FIRE_HITS=new HashMap<>();
    public static void attack(LivingAttackAttemptEvent event){
        if(!(event.source().getDirectEntity() instanceof Player player)||player.level().isClientSide())return;
        var target=event.target();long time=target.level().getGameTime();
        if(player.hasEffect(InfusedContent.IMBUED.get(0)))AIR_HITS.put(target,time);
        if(player.hasEffect(InfusedContent.IMBUED.get(1)))FIRE_HITS.put(target,time);
        if(player.hasEffect(InfusedContent.IMBUED.get(2))){
            boolean xAxis=Math.abs(target.getZ()-player.getZ())<Math.abs(target.getX()-player.getX());
            int x=(int)((target.getX()+player.getX())/2),y=(int)(player.getY()+2),z=(int)((target.getZ()+player.getZ())/2);
            for(int i=-2;i<3;i++)for(int j=-2;j<3;j++){
                var pos=xAxis?new BlockPos(x,y+i,z+j):new BlockPos(x+j,y+i,z);
                if(player.level().getBlockState(pos).isAir()){player.level().setBlock(pos,ForcefieldContent.BLOCK.get().defaultBlockState(),3);ParticleApi.blockSparkle(player.level(),pos,100,100);}
            }
        }
    }
    @SubscribeEvent public static void playerPre(PlayerTickEvent.Pre event){water(event.getEntity());}
    @SubscribeEvent public static void playerPost(PlayerTickEvent.Post event){water(event.getEntity());}
    public static void water(Player player){
        // World mutation belongs to the authoritative server. The original client duplicated these writes.
        if(player.level().isClientSide()||!player.hasEffect(InfusedContent.IMBUED.get(3)))return;
        for(int x=(int)(player.getX()-2);x<player.getX()+2;x++)for(int y=(int)(player.getY()-2);y<player.getY()+2;y++)for(int z=(int)(player.getZ()-2);z<player.getZ()+2;z++){
            var pos=new BlockPos(x,y,z);
            if(player.level().getBlockState(pos).is(Blocks.LAVA)){player.level().setBlock(pos,Blocks.OBSIDIAN.defaultBlockState(),3);ParticleApi.burst(player.level(),x+.5,y+.5,z+.5,1.2f);}
        }
    }
    @SubscribeEvent public static void serverPre(ServerTickEvent.Pre event){tracked();}
    @SubscribeEvent public static void serverPost(ServerTickEvent.Post event){tracked();}
    public static void tracked(){
        var air=AIR_HITS.entrySet().iterator();while(air.hasNext()){
            var entry=air.next();var target=entry.getKey();long time=target.level().getGameTime();
            if(target.isAlive()&&time%5==0){var random=new Random();target.setDeltaMovement(random.nextFloat()-.5,random.nextFloat(),random.nextFloat()-.5);ParticleApi.burst(target.level(),target.getX(),target.getY(),target.getZ(),.5f);}
            if(time>entry.getValue()+20)air.remove();
        }
        var fire=FIRE_HITS.entrySet().iterator();while(fire.hasNext()){
            var entry=fire.next();var target=entry.getKey();long time=target.level().getGameTime();
            if(target.isAlive()&&time%5==0){var random=new Random();target.igniteForSeconds(6);
                for(int i=0;i<30;i++){double theta=random.nextFloat()*2f*Math.PI,phi=random.nextFloat()*2f*Math.PI,r=2.5;ParticleApi.wisp(target.level(),target.getX()+r*Math.sin(theta)*Math.cos(phi),target.getY()+r*Math.sin(theta)*Math.sin(phi)+1,target.getZ()+r*Math.cos(theta),.1f,4,true,true,1);}
            }
            if(time>entry.getValue()+6000)fire.remove();
        }
    }
    private InfusedPotionEffects(){}
}
