package thaumic.tinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;
import thaumcraft.api.client.DimensionLightmap;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.bedrock.BedrockLighting;

@EventBusSubscriber(modid=ThaumicTinkerer.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BedrockDimensionClient {
    static final BedrockLighting.Flicker FLICKER=new BedrockLighting.Flicker();
    @SubscribeEvent public static void register(RegisterDimensionSpecialEffectsEvent event){
        event.register(ThaumicTinkerer.id("bedrock"),new Effects());
        DimensionLightmap.register(ThaumicTinkerer.id("bedrock"),(context,block,sky,vanilla)->BedrockLighting.pixel(block,sky,
                context.skyBrightness(),FLICKER.multiplier(),context.skyFlash(),context.darkenCurrent(),context.darkenInterpolated(),context.nightVision(),context.gamma()));
    }
    private static final class Effects extends DimensionSpecialEffects {
        Effects(){super(0f,false,SkyType.NONE,false,false);}
        // RenderGlobal1.7.10 skips BOTH sky and clouds for !isSurfaceWorld().
        // The source's renderStars/setSunSize/etc are not invoked overrides.
        @Override public boolean renderClouds(ClientLevel level,int ticks,float partialTicks,PoseStack pose,double x,double y,double z,Matrix4f view,Matrix4f projection){return true;}
        @Override public Vec3 getBrightnessDependentFogColor(Vec3 color,float brightness){
            return new Vec3((160/255f)*.15f,(129/255f)*.15f,(160/255f)*.15f);
        }
        @Override public boolean isFoggyAt(int x,int y){return false;}
    }
    private BedrockDimensionClient(){}
}
