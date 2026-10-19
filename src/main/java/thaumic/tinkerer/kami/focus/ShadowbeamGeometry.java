package thaumic.tinkerer.kami.focus;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Arithmetic from the release Beam and the 1.7.10 throwable/entity AABB ray. */
final class ShadowbeamGeometry {
    static Vec3 origin(double x,double eyeY,double z,float yaw) {
        float angle=yaw/180f*(float)Math.PI;
        return new Vec3(x-(double)(Mth.cos(angle)*.16f),eyeY-(double).1f,
                z-(double)(Mth.sin(angle)*.16f));
    }

    static Vec3 motion(float yaw,float pitch) {
        float y=yaw/180f*(float)Math.PI,p=pitch/180f*(float)Math.PI;
        double x=-Mth.sin(y)*Mth.cos(p)*.4f;
        double z=Mth.cos(y)*Mth.cos(p)*.4f;
        double vertical=-Mth.sin(p)*.4f;
        float length=(float)Math.sqrt(x*x+vertical*vertical+z*z);
        x/=length;vertical/=length;z/=length;
        x+=.0075f*1f;vertical+=.0075f*1f;z+=.0075f*1f;
        x*=1.5f;vertical*=1.5f;z*=1.5f;
        return new Vec3(x/10,vertical/10,z/10);
    }

    static Vec3 reflect(Vec3 motion,Direction face) {
        var normal=new Vec3(face.getStepX(),face.getStepY(),face.getStepZ());
        return normal.scale(-2*motion.dot(normal)).add(motion);
    }

    static boolean valid(Vec3 position,Vec3 motion) {
        return Double.isFinite(position.x)&&Double.isFinite(position.y)&&Double.isFinite(position.z)
                &&Double.isFinite(motion.x)&&Double.isFinite(motion.y)&&Double.isFinite(motion.z);
    }

    // Modern AABB.clip ignores an exit intersection when the start lies inside.
    // The old throwable instead checks all six planes, including that exit.
    static Vec3 intercept(AABB box,Vec3 from,Vec3 to) {
        Vec3 nearest=null;
        for(int face=0;face<6;face++) {
            int axis=face/2;
            double low=axis==0?box.minX:axis==1?box.minY:box.minZ;
            double high=axis==0?box.maxX:axis==1?box.maxY:box.maxZ;
            double start=axis==0?from.x:axis==1?from.y:from.z;
            double delta=axis==0?to.x-from.x:axis==1?to.y-from.y:to.z-from.z;
            if(delta*delta<(double)1.0e-7f)continue;
            double t=((face%2==0?low:high)-start)/delta;
            if(t<0||t>1)continue;
            var point=new Vec3(from.x+(to.x-from.x)*t,from.y+(to.y-from.y)*t,from.z+(to.z-from.z)*t);
            boolean inside=axis==0?point.y>=box.minY&&point.y<=box.maxY&&point.z>=box.minZ&&point.z<=box.maxZ:
                    axis==1?point.x>=box.minX&&point.x<=box.maxX&&point.z>=box.minZ&&point.z<=box.maxZ:
                    point.x>=box.minX&&point.x<=box.maxX&&point.y>=box.minY&&point.y<=box.maxY;
            if(inside&&(nearest==null||from.distanceToSqr(point)<from.distanceToSqr(nearest)))nearest=point;
        }
        return nearest;
    }

    /** Vec3.distanceTo in the original widens MathHelper's float sqrt to double. */
    static double legacyDistance(Vec3 from,Vec3 to) {
        return (float)Math.sqrt(from.distanceToSqr(to));
    }

    // Reflections preserve speed, so every possible path lies within this disc.
    // The margin covers the .25-block ray box, entity search, and native neighbour shapes.
    static java.util.List<net.minecraft.world.level.ChunkPos> requiredChunks(Vec3 start,Vec3 motion) {
        double radius=motion.length()*300+3;
        int minX=net.minecraft.util.Mth.floor(start.x-radius)>>4,maxX=net.minecraft.util.Mth.floor(start.x+radius)>>4;
        int minZ=net.minecraft.util.Mth.floor(start.z-radius)>>4,maxZ=net.minecraft.util.Mth.floor(start.z+radius)>>4;
        var result=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++) {
            double dx=Math.max(Math.max((double)x*16-start.x,start.x-((double)x*16+16)),0);
            double dz=Math.max(Math.max((double)z*16-start.z,start.z-((double)z*16+16)),0);
            if(dx*dx+dz*dz<=radius*radius)result.add(new net.minecraft.world.level.ChunkPos(x,z));
        }
        return java.util.List.copyOf(result);
    }

    private ShadowbeamGeometry(){}
}
