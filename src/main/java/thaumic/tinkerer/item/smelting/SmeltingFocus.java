package thaumic.tinkerer.item.smelting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.BlockReplacementApi;
import thaumic.tinkerer.item.TinkererFocusItem;

/** The accepted server-only source progress; client visuals never change this map. */
public final class SmeltingFocus {
    private record Progress(BlockPos pos,int remaining){}
    private static final Map<String,Progress> PROGRESS=new HashMap<>();
    public static void logout(String name){PROGRESS.remove(name);SmeltingActions.logout(name);}
    public static boolean cast(TinkererFocusItem item,ServerPlayer player,ItemStack wand,ItemStack focus){
        if(!player.server.isSameThread()||!WandApi.consume(wand,player,item.getVisCost(focus),false))return false;
        var ray=SmeltingRay.trace(player);
        if(ray.complete()&&(ray.hit()==null||ray.hit().getType()!=HitResult.Type.BLOCK)){SmeltingActions.cancel(player);return false;}
        if(SmeltingActions.defer(item,player,wand,focus,ray))return false;
        return ready(item,player,wand,focus,ray);
    }
    static boolean ready(TinkererFocusItem item,ServerPlayer player,ItemStack wand,ItemStack focus,SmeltingRay.Trace ray){
        if(!ray.complete()||ray.hit()==null||ray.hit().getType()!=HitResult.Type.BLOCK||!WandApi.consume(wand,player,item.getVisCost(focus),false))return false;
        var level=player.serverLevel();var pos=ray.hit().getBlockPos();var state=level.getBlockState(pos);
        var input=new SingleRecipeInput(new ItemStack(state.getBlock()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
        if(recipe.isEmpty())return false;
        var output=recipe.get().value().assemble(input,level.registryAccess());if(!(output.getItem() instanceof BlockItem blockItem))return false;
        var name=player.getGameProfile().getName();var previous=PROGRESS.get(name);
        int initial=20-Math.min(3,item.getUpgradeLevel(wand,FocusUpgradeType.potency))*5;
        if(previous==null||!previous.pos().equals(pos)){PROGRESS.put(name,new Progress(pos.immutable(),initial));return false;}
        int remaining=previous.remaining()-1;
        if(remaining>0){
            PROGRESS.put(name,new Progress(pos.immutable(),remaining));
            if(player.getUseItemRemainingTicks()%3==0)level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.FIRE_AMBIENT,SoundSource.PLAYERS,.2f,1);
            wisps(player,pos,2);return false;
        }
        if(!BlockReplacementApi.replace(player,wand,pos,ray.hit().getDirection(),state,blockItem.getBlock().defaultBlockState(),
                ()->WandApi.consume(wand,player,item.getVisCost(focus),true)))return false;
        // Source removes its entry then immediately initializes it again at
        // these coordinates; release/miss/temporary lack of vis preserve it.
        PROGRESS.put(name,new Progress(pos.immutable(),initial));
        level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.PLAYERS,.6f,1);
        level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.FIRE_AMBIENT,SoundSource.PLAYERS,1,1);
        wisps(player,pos,25);return true;
    }
    private static void wisps(ServerPlayer player,BlockPos pos,int count){
        for(int index=0;index<count;index++)ParticleApi.broadcast(player.serverLevel(),new ParticleApi.Effect(ParticleApi.Kind.WISP,
                pos.getX()+Math.random(),pos.getY()+Math.random(),pos.getZ()+Math.random(),(float)Math.random()/2,4,0,0,0,true,true,(float)-Math.random()/10));
    }
    private SmeltingFocus(){}
}
