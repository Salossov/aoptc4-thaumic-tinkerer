package thaumic.tinkerer.kami.tool;

import java.util.ArrayList;
import java.util.List;

/** Release-164 loop bounds. Upper endpoints are exclusive, including the
 * intentionally asymmetric pick tunnel and the shovel's mixed-block column. */
public final class AwakenedMiningShape {
    public enum Kind { PICK,SHOVEL,AXE }
    public record Offset(int x,int y,int z){}
    public record Bounds(int xs,int ys,int zs,int xe,int ye,int ze) {
        public List<Offset> extraBlocks(){
            var result=new ArrayList<Offset>();
            for(int x=xs;x<xe;x++)for(int y=ys;y<ye;y++)for(int z=zs;z<ze;z++)
                if(x!=0||y!=0||z!=0)result.add(new Offset(x,y,z));
            return List.copyOf(result);
        }
    }
    public static Bounds bounds(Kind kind,int mode,int faceX,int faceY,int faceZ){
        if(Math.abs(faceX)+Math.abs(faceY)+Math.abs(faceZ)!=1)throw new IllegalArgumentException("One block face required");
        if(mode==1)return new Bounds(faceX==0?-2:0,faceY==0?-1:0,faceZ==0?-2:0,
                faceX==0?3:1,faceY==0?4:1,faceZ==0?3:1);
        if(mode==2&&kind==Kind.PICK){
            int x=-faceX,y=-faceY,z=-faceZ;
            return new Bounds(x>=0?0:-10,y>=0?0:-10,z>=0?0:-10,x>0?10:1,y>0?10:1,z>0?10:1);
        }
        if(mode==2&&kind==Kind.SHOVEL)return new Bounds(0,-8,0,1,8,1);
        return null;
    }
    /** Keep floating-point NaN/zero behavior of the original comparison. */
    public static boolean allowsStrength(float reference,float target){return !(reference/target>10.0f);}
    @FunctionalInterface public interface LogAt {boolean test(int x,int y,int z);}
    /** Source first-outward-neighbor recursion expressed iteratively, including
     * the original early return when a candidate crosses the search boundary. */
    public static Offset furthestLog(LogAt log){
        int lx=0,ly=0,lz=0;double distance=0;
        search:while(true){
            for(int x=-2;x<=2;x++)for(int y=2;y>=-2;y--)for(int z=-2;z<=2;z++){
                int dx=lx+x,dy=ly+y,dz=lz+z;
                if(Math.abs(dx)>24||Math.abs(dy)>48||Math.abs(dz)>24)return new Offset(lx,ly,lz);
                double d=(double)dx*dx+(double)dy*dy+(double)dz*dz;
                if(log.test(dx,dy,dz)&&d>distance){lx=dx;ly=dy;lz=dz;distance=d;continue search;}
            }
            return new Offset(lx,ly,lz);
        }
    }
    private AwakenedMiningShape(){}
}
