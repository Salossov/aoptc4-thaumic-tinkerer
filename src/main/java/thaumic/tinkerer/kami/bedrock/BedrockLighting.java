package thaumic.tinkerer.kami.bedrock;

/** Release WorldProviderBedrock table + vanilla1.7.10 EntityRenderer pipeline.
 * Standalone float arithmetic; the public final-pixel hook avoids a second
 * modern night-vision, darkness or gamma pass changing this result.
 */
public final class BedrockLighting {
    /** Old EntityRenderer.updateTorchFlicker uses double accumulation/damping
     * and consumes both channels, even though the lightmap reads only red.
     */
    public static final class Flicker {
        private float red,redVelocity,blue,blueVelocity;
        public void tick(java.util.function.DoubleSupplier random){
            redVelocity=(float)((double)redVelocity+(random.getAsDouble()-random.getAsDouble())*random.getAsDouble()*random.getAsDouble());
            blueVelocity=(float)((double)blueVelocity+(random.getAsDouble()-random.getAsDouble())*random.getAsDouble()*random.getAsDouble());
            redVelocity=(float)((double)redVelocity*.9);blueVelocity=(float)((double)blueVelocity*.9);
            red+=(redVelocity-red)*1f;blue+=(blueVelocity-blue)*1f;
        }
        public float multiplier(){return red*.1f+1.5f;}
    }
    public static float brightness(int light){
        float base=12f,step=12f-light/15f;return (1f-step)/(step*3f+1f)*(1f-base)+base;
    }
    public static int pixel(int blockLight,int skyLight,float sun,float torch,boolean flash,float darkenCurrent,float darkenInterpolated,float vision,float gamma){
        float sky=brightness(skyLight)*(sun*.95f+.05f),block=brightness(blockLight)*torch;
        if(flash)sky=brightness(skyLight);
        float redSky=sky*(sun*.65f+.35f),greenSky=sky*(sun*.65f+.35f);
        float greenTorch=block*((block*.6f+.4f)*.6f+.4f),blueTorch=block*(block*block*.6f+.4f);
        float red=(redSky+block)*.96f+.03f,green=(greenSky+greenTorch)*.96f+.03f,blue=(sky+blueTorch)*.96f+.03f;
        if(darkenCurrent>0f){
            red=red*(1f-darkenInterpolated)+red*.7f*darkenInterpolated;
            green=green*(1f-darkenInterpolated)+green*.6f*darkenInterpolated;
            blue=blue*(1f-darkenInterpolated)+blue*.6f*darkenInterpolated;
        }
        if(vision>0f){
            float scale=1f/red;if(scale>1f/green)scale=1f/green;if(scale>1f/blue)scale=1f/blue;
            red=red*(1f-vision)+red*scale*vision;green=green*(1f-vision)+green*scale*vision;blue=blue*(1f-vision)+blue*scale*vision;
        }
        if(red>1f)red=1f;if(green>1f)green=1f;if(blue>1f)blue=1f;
        float r=1f-red,g=1f-green,b=1f-blue;r=1f-r*r*r*r;g=1f-g*g*g*g;b=1f-b*b*b*b;
        red=red*(1f-gamma)+r*gamma;green=green*(1f-gamma)+g*gamma;blue=blue*(1f-gamma)+b*gamma;
        red=red*.96f+.03f;green=green*.96f+.03f;blue=blue*.96f+.03f;
        if(red>1f)red=1f;if(green>1f)green=1f;if(blue>1f)blue=1f;
        if(red<0f)red=0f;if(green<0f)green=0f;if(blue<0f)blue=0f;
        return 0xff000000|((int)(blue*255f)<<16)|((int)(green*255f)<<8)|(int)(red*255f);
    }
    private BedrockLighting(){}
}
