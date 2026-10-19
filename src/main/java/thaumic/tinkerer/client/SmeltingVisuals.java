package thaumic.tinkerer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.api.client.FocusClientVisuals;
import thaumcraft.api.client.WandBeamApi;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.item.smelting.SmeltingRay;

/** Source local beam only; all progress, payment and wisps belong to the server. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class SmeltingVisuals {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->FocusClientVisuals.register(FocusContent.SMELT.get(),(player,wand,focus,remaining)->{
            if(player!=Minecraft.getInstance().player||!WandApi.consume(wand,player,FocusContent.SMELT.get().getVisCost(focus),false))return;
            var level=player.level();var from=player.getEyePosition();var to=SmeltingRay.endpoint(from,player.getYRot(),player.getXRot());
            if(!Double.isFinite(to.x)||!Double.isFinite(to.y)||!Double.isFinite(to.z))return;
            var hit=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(hit.getType()!=HitResult.Type.BLOCK)return;
            var input=new SingleRecipeInput(new ItemStack(level.getBlockState(hit.getBlockPos()).getBlock()));
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
            if(recipe.isEmpty()||!(recipe.get().value().assemble(input,level.registryAccess()).getItem() instanceof BlockItem))return;
            WandBeamApi.emit(player,new WandBeamApi.Beam(hit.getBlockPos().getCenter(),2,0xFF0000,true,0));
        }));
    }
    private SmeltingVisuals(){}
}
