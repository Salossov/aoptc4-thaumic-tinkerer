package thaumic.tinkerer.kami.bedrock;

import java.util.Random;
import java.util.function.Function;

/** Release164 column RNG and Forge1.7.10 WorldGenMinable geometry. The caller
 * decides whether a visited voxel still permits replacement by this ore.
 * Standalone to compare the exact float math and RNG stream without a game.
 */
public final class BedrockOreShape {
    @FunctionalInterface public interface Voxel {void accept(int x,int y,int z);}
    @FunctionalInterface public interface OreVoxel<T> {void accept(T ore,int x,int y,int z);}
    private static final float[] SINE=new float[65536];
    static{for(int i=0;i<SINE.length;i++)SINE[i]=(float)Math.sin((double)i*Math.PI*2.0/65536.0);}
    private static float sin(float angle){return SINE[(int)(angle*10430.378f)&65535];}
    private static float cos(float angle){return SINE[(int)(angle*10430.378f+16384f)&65535];}
    private static int floor(double value){int integer=(int)value;return value<integer?integer-1:integer;}

    public static <T> void populate(long seed,int chunkX,int chunkZ,int density,Function<Random,T> choose,OreVoxel<T> output){
        var random=new Random(seed);
        long xSeed=random.nextLong()/2L*2L+1L,zSeed=random.nextLong()/2L*2L+1L;
        random.setSeed(chunkX*xSeed+chunkZ*zSeed^seed);
        // ChunkProviderBedrock consumes these unused feature coordinates first.
        random.nextInt(16);random.nextInt(128);random.nextInt(16);
        for(int column=0;column<density;column++){
            int x=16*chunkX+random.nextInt(16),z=16*chunkZ+random.nextInt(16);
            T ore=choose.apply(random);
            for(int cluster=0;cluster<200;cluster++){
                int y=random.nextInt(245)+6,size=random.nextInt(20);
                vein(random,x,y,z,size,(vx,vy,vz)->output.accept(ore,vx,vy,vz));
            }
        }
    }

    public static void vein(Random random,int x,int y,int z,int size,Voxel output){
        float angle=random.nextFloat()*(float)Math.PI;
        double startX=(float)(x+8)+sin(angle)*(float)size/8f;
        double endX=(float)(x+8)-sin(angle)*(float)size/8f;
        double startZ=(float)(z+8)+cos(angle)*(float)size/8f;
        double endZ=(float)(z+8)-cos(angle)*(float)size/8f;
        double startY=y+random.nextInt(3)-2,endY=y+random.nextInt(3)-2;
        for(int step=0;step<=size;step++){
            double centerX=startX+(endX-startX)*(double)step/(double)size;
            double centerY=startY+(endY-startY)*(double)step/(double)size;
            double centerZ=startZ+(endZ-startZ)*(double)step/(double)size;
            double radius=random.nextDouble()*(double)size/16.0;
            double diameterX=(double)(sin((float)step*(float)Math.PI/(float)size)+1f)*radius+1.0;
            double diameterY=(double)(sin((float)step*(float)Math.PI/(float)size)+1f)*radius+1.0;
            int minX=floor(centerX-diameterX/2.0),minY=floor(centerY-diameterY/2.0),minZ=floor(centerZ-diameterX/2.0);
            int maxX=floor(centerX+diameterX/2.0),maxY=floor(centerY+diameterY/2.0),maxZ=floor(centerZ+diameterX/2.0);
            for(int vx=minX;vx<=maxX;vx++){
                double dx=((double)vx+.5-centerX)/(diameterX/2.0);if(!(dx*dx<1.0))continue;
                for(int vy=minY;vy<=maxY;vy++){
                    double dy=((double)vy+.5-centerY)/(diameterY/2.0);if(!(dx*dx+dy*dy<1.0))continue;
                    for(int vz=minZ;vz<=maxZ;vz++){
                        double dz=((double)vz+.5-centerZ)/(diameterX/2.0);
                        if(dx*dx+dy*dy+dz*dz<1.0)output.accept(vx,vy,vz);
                    }
                }
            }
        }
    }
    private BedrockOreShape(){}
}
