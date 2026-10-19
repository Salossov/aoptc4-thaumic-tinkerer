package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import thaumic.tinkerer.ThaumicTinkerer;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PlacementMirrorModels {
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event){
        var key=ModelResourceLocation.inventory(ThaumicTinkerer.id("placement_mirror"));
        var model=event.getModels().get(key);
        if(model!=null)event.getModels().put(key,new Mirror(model));
    }
    private static final class Mirror extends BakedModelWrapper<BakedModel> {
        Mirror(BakedModel original){super(original);}
        @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean leftHand){
            if(context!=ItemDisplayContext.GUI)return originalModel.applyTransform(context,pose,leftHand);
            pose.mulPose(PlacementMirrorGuiPose.transform());return this;
        }
    }
    private PlacementMirrorModels(){}
}
