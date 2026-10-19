package thaumic.tinkerer.kami.bedrock;

import java.util.*;

/** The ordinary 1.7.10 teleporter used by TileBedrockPortal, independently of
 * the special player collision route. Searches can yield between columns;
 * their traversal, distance ties and failed-orientation skips retain source order.
 * This class reads supplied terrain only and never loads a Minecraft chunk.
 */
public final class LegacyPortalSearch {
    public interface Terrain {
        int height();
        boolean portal(int x,int y,int z);
        boolean air(int x,int y,int z);
        boolean solid(int x,int y,int z);
    }
    public record Pos(int x,int y,int z){}
    public record Site(int x,int y,int z,int orientation,boolean forced){}
    public enum Fill { AIR, OBSIDIAN, PORTAL }
    public record Cell(Pos pos,Fill fill){}
    private static int floor(double value){int n=(int)value;return value<n?n-1:n;}
    public static final class Existing {
        private final Terrain terrain;private final double x,y,z;private final int minX,maxX,minZ,maxZ;
        private int scanX,scanZ;private double bestDistance=-1;private Pos best;private boolean done;
        public Existing(Terrain terrain,double x,double y,double z){
            this.terrain=terrain;this.x=x;this.y=y;this.z=z;
            minX=floor(x)-128;maxX=floor(x)+128;minZ=floor(z)-128;maxZ=floor(z)+128;scanX=minX;scanZ=minZ;
        }
        public boolean advance(int columns){
            for(int count=0;count<columns&&!done;count++){
                double dx=scanX+.5-x,dz=scanZ+.5-z;
                for(int scanY=terrain.height()-1;scanY>=0;scanY--){
                    if(!terrain.portal(scanX,scanY,scanZ))continue;
                    while(terrain.portal(scanX,scanY-1,scanZ))scanY--;
                    double dy=scanY+.5-y,distance=dx*dx+dy*dy+dz*dz;
                    if(bestDistance<0||distance<bestDistance){bestDistance=distance;best=new Pos(scanX,scanY,scanZ);}
                }
                if(++scanZ>maxZ){scanZ=minZ;if(++scanX>maxX)done=true;}
            }
            return done;
        }
        public Optional<Pos> result(){if(!done)throw new IllegalStateException("Portal search unfinished");return Optional.ofNullable(best);}
    }
    public static final class Creation {
        private final Terrain terrain;private final double x,y,z;private final int originX,originY,originZ,firstOrientation;
        private int scanX,scanZ,pass;private double bestDistance=-1;private Site best;private boolean done;
        public Creation(Terrain terrain,double x,double y,double z,int firstOrientation){
            if(firstOrientation<0||firstOrientation>3)throw new IllegalArgumentException("Orientation must be0..3");
            this.terrain=terrain;this.x=x;this.y=y;this.z=z;this.firstOrientation=firstOrientation;
            originX=floor(x);originY=floor(y);originZ=floor(z);scanX=originX-16;scanZ=originZ-16;
        }
        public boolean advance(int columns){
            for(int count=0;count<columns&&!done;count++){
                scanColumn();
                if(++scanZ>originZ+16){
                    scanZ=originZ-16;
                    if(++scanX>originX+16){
                        if(pass==0&&bestDistance<0){pass=1;scanX=originX-16;}
                        else {
                            if(best==null){int height=originY;if(height<70)height=70;if(height>terrain.height()-10)height=terrain.height()-10;best=new Site(originX,height,originZ,0,true);}
                            done=true;
                        }
                    }
                }
            }
            return done;
        }
        private void scanColumn(){
            double dx=scanX+.5-x,dz=scanZ+.5-z;
            columnY:for(int scanY=terrain.height()-1;scanY>=0;scanY--){
                if(!terrain.air(scanX,scanY,scanZ))continue;
                while(scanY>0&&terrain.air(scanX,scanY-1,scanZ))scanY--;
                for(int orientation=firstOrientation;orientation<firstOrientation+(pass==0?4:2);orientation++){
                    int alongX=orientation%2,alongZ=1-alongX;
                    if(pass==0&&orientation%4>=2){alongX=-alongX;alongZ=-alongZ;}
                    for(int depth=0;depth<(pass==0?3:1);depth++)for(int width=0;width<4;width++)for(int dy=-1;dy<4;dy++){
                        int checkX=scanX+(width-1)*alongX+depth*alongZ;
                        int checkZ=scanZ+(width-1)*alongZ-depth*alongX;
                        // The original skips the entire Y candidate, rather
                        // than trying the next orientation after this failure.
                        if(dy<0&&!terrain.solid(checkX,scanY+dy,checkZ)||dy>=0&&!terrain.air(checkX,scanY+dy,checkZ))continue columnY;
                    }
                    double dy=scanY+.5-y,distance=dx*dx+dy*dy+dz*dz;
                    if(bestDistance<0||distance<bestDistance){bestDistance=distance;best=new Site(scanX,scanY,scanZ,orientation%(pass==0?4:2),false);}
                }
            }
        }
        public Site result(){if(!done)throw new IllegalStateException("Portal creation search unfinished");return best;}
    }
    /** Forced platform is written once; the ordinary frame is written and its
     * neighbors notified FOUR times by the source. The caller retains those passes.
     */
    public static List<Cell> platform(Site site){
        if(!site.forced())return List.of();var cells=new ArrayList<Cell>();
        int alongX=site.orientation()%2,alongZ=1-alongX;
        if(site.orientation()%4>=2){alongX=-alongX;alongZ=-alongZ;}
        for(int depth=-1;depth<=1;depth++)for(int width=1;width<3;width++)for(int dy=-1;dy<3;dy++)
            cells.add(new Cell(new Pos(site.x()+(width-1)*alongX+depth*alongZ,site.y()+dy,site.z()+(width-1)*alongZ-depth*alongX),dy<0?Fill.OBSIDIAN:Fill.AIR));
        return List.copyOf(cells);
    }
    public static List<Cell> frame(Site site){
        var cells=new ArrayList<Cell>();int alongX=site.orientation()%2,alongZ=1-alongX;
        if(site.orientation()%4>=2){alongX=-alongX;alongZ=-alongZ;}
        for(int width=0;width<4;width++)for(int dy=-1;dy<4;dy++)
            cells.add(new Cell(new Pos(site.x()+(width-1)*alongX,site.y()+dy,site.z()+(width-1)*alongZ),
                    width==0||width==3||dy==-1||dy==3?Fill.OBSIDIAN:Fill.PORTAL));
        return List.copyOf(cells);
    }
    /** Read the final planned blocks without making the ordinary portal active
     * during a cooperative second search. Out-of-world writes have no effect.
     * Native neighbor callbacks are checked again when the plan is committed.
     */
    public static Terrain planned(Terrain terrain,Site site){
        var cells=new HashMap<Pos,Fill>();var writes=new ArrayList<>(platform(site));writes.addAll(frame(site));
        for(var cell:writes){var p=cell.pos();
            if(p.y()>=0&&p.y()<terrain.height()&&p.x()>=-30000000&&p.x()<30000000&&p.z()>=-30000000&&p.z()<30000000)cells.put(p,cell.fill());
        }
        return new Terrain(){
            private Fill fill(int x,int y,int z){return cells.get(new Pos(x,y,z));}
            public int height(){return terrain.height();}
            public boolean portal(int x,int y,int z){var value=fill(x,y,z);return value==null?terrain.portal(x,y,z):value==Fill.PORTAL;}
            public boolean air(int x,int y,int z){var value=fill(x,y,z);return value==null?terrain.air(x,y,z):value==Fill.AIR;}
            public boolean solid(int x,int y,int z){var value=fill(x,y,z);return value==null?terrain.solid(x,y,z):value==Fill.OBSIDIAN;}
        };
    }
    private LegacyPortalSearch(){}
}
