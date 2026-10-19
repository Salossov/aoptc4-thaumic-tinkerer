package thaumic.tinkerer.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.kami.KamiArmorContent;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class KamiWingsRegistration {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ThaumicTinkerer.id("kami_wings"),"main");
    private static KamiWingsModel model;
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        if(TinkererConfig.KAMI_FORCE_ENABLED.get())event.registerLayerDefinition(LAYER,KamiWingsModel::layer);
    }
    @SubscribeEvent public static void reload(EntityRenderersEvent.AddLayers event) {
        if(TinkererConfig.KAMI_FORCE_ENABLED.get())model=new KamiWingsModel(event.getEntityModels().bakeLayer(LAYER));
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        if(!TinkererConfig.KAMI_FORCE_ENABLED.get())return;
        event.registerItem(new IClientItemExtensions() {
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {return model;}
            @Override public void setupModelAnimations(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,Model actual,float swing,float amount,float partialTick,float age,float yaw,float pitch) {
                ((KamiWingsModel)actual).setupAnim(wearer,swing,amount,age,yaw,pitch);
            }
        },KamiArmorContent.GEM_CHEST.get());
    }
    private KamiWingsRegistration() {}
}
