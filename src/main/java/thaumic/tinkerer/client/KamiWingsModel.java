package thaumic.tinkerer.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;

/** Release wings and the visible legacy biped poses. Modern swimming, elytra and
 * offhand poses did not exist in this model. Geometry uses the original 64x32 UVs.
 */
public final class KamiWingsModel extends HumanoidModel<LivingEntity> {
    private final ModelPart root,leftWing,rightWing;
    public KamiWingsModel(ModelPart root) {
        super(root);this.root=root;leftWing=body.getChild("wing_1");rightWing=body.getChild("wing_2");
    }
    public static LayerDefinition layer() {
        var mesh=HumanoidModel.createMesh(new CubeDeformation(1),0);
        var body=mesh.getRoot().getChild("body");
        body.addOrReplaceChild("wing_1",CubeListBuilder.create().texOffs(16,-12).addBox(0,0,0,0,7,12),PartPose.offsetAndRotation(-2,1,2,0,-.6108652F,0));
        body.addOrReplaceChild("wing_2",CubeListBuilder.create().texOffs(16,-12).addBox(.1F,0,0,0,7,12),PartPose.offsetAndRotation(2,1,2,0,.4468043F,0));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(LivingEntity wearer,float limbSwing,float amount,float age,float yaw,float pitch) {
        // The original creates a new model for each draw. Reset the cached model
        // so flight, bow use and body pivots cannot leak into the next wearer/frame.
        root.getAllParts().forEach(ModelPart::resetPose);
        head.visible=false;hat.visible=false;leftLeg.visible=false;rightLeg.visible=false;
        body.visible=true;leftArm.visible=true;rightArm.visible=true;
        float headYaw=yaw/57.295776F,headPitch=pitch/57.295776F;
        rightArm.xRot=Mth.cos(limbSwing*.6662F+(float)Math.PI)*2F*amount*.5F;
        leftArm.xRot=Mth.cos(limbSwing*.6662F)*2F*amount*.5F;
        if(riding) {rightArm.xRot-=.62831855F;leftArm.xRot-=.62831855F;}
        int heldRight=0;boolean bow=false;
        if(wearer instanceof Player player) {
            var held=player.getMainHandItem();heldRight=held.isEmpty()?0:1;
            if(!held.isEmpty()&&player.getUseItemRemainingTicks()>0) {
                if(held.getUseAnimation()==UseAnim.BLOCK)heldRight=3;
                else if(held.getUseAnimation()==UseAnim.BOW)bow=true;
            }
            if(player.getAbilities().flying) {
                leftWing.yRot=(float)((Math.sin(wearer.tickCount)+1D)*(Math.PI/180D)*15D-.6108652F);
                rightWing.yRot=-leftWing.yRot;
            }
        }
        if(heldRight!=0)rightArm.xRot=rightArm.xRot*.5F-.31415927F*heldRight;
        if(attackTime> -9990F) {
            body.yRot=Mth.sin(Mth.sqrt(attackTime)*(float)Math.PI*2F)*.2F;
            rightArm.z=Mth.sin(body.yRot)*5F;rightArm.x=-Mth.cos(body.yRot)*5F;
            leftArm.z=-Mth.sin(body.yRot)*5F;leftArm.x=Mth.cos(body.yRot)*5F;
            rightArm.yRot=body.yRot;leftArm.yRot=body.yRot;leftArm.xRot+=body.yRot;
            float eased=1F-attackTime;eased*=eased;eased*=eased;eased=1F-eased;
            float swing=Mth.sin(eased*(float)Math.PI);
            float headInfluence=Mth.sin(attackTime*(float)Math.PI)*-(headPitch-.7F)*.75F;
            rightArm.xRot=(float)((double)rightArm.xRot-((double)swing*1.2D+(double)headInfluence));
            rightArm.yRot+=body.yRot*2F;rightArm.zRot=Mth.sin(attackTime*(float)Math.PI)*-.4F;
        }
        if(wearer.isShiftKeyDown()) {body.xRot=.5F;rightArm.xRot+=.4F;leftArm.xRot+=.4F;}
        idleArms(age);
        if(bow) {
            rightArm.zRot=0;leftArm.zRot=0;
            rightArm.yRot=-.1F+headYaw;leftArm.yRot=.1F+headYaw+.4F;
            rightArm.xRot=-1.5707964F+headPitch;leftArm.xRot=-1.5707964F+headPitch;
            idleArms(age);
        }
    }
    private void idleArms(float age) {
        rightArm.zRot+=Mth.cos(age*.09F)*.05F+.05F;leftArm.zRot-=Mth.cos(age*.09F)*.05F+.05F;
        rightArm.xRot+=Mth.sin(age*.067F)*.05F;leftArm.xRot-=Mth.sin(age*.067F)*.05F;
    }
}
