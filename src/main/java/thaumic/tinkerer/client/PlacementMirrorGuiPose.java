package thaumic.tinkerer.client;

import org.joml.Matrix4f;

/** Forge 1.7.10 inventory helper and source mirror pose, in native GUI coordinates. */
public final class PlacementMirrorGuiPose {
    public static Matrix4f transform(){
        float degree=(float)(Math.PI/180);
        return new Matrix4f().scale(1/16F,-1/16F,1/16F)
                .translate(-10,-5,-3).scale(10).translate(1,.5F,1).scale(1,1,-1)
                .rotateX(210*degree).rotateY(45*degree).rotateY(-90*degree)
                .rotateY(45*degree).scale(1.5F,1.8F,1.8F).rotateZ(-45*degree)
                .translate(-.46F,-.58F,0)
                // ItemRenderer translates the native model by -.5 afterwards.
                // Generated z=7.5..8.5 maps to the source extrusion -1/16..0.
                .translate(.5F,.5F,-1/32F);
    }
    private PlacementMirrorGuiPose(){}
}
