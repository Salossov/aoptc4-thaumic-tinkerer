package thaumic.tinkerer.kami.construction;

import java.util.ArrayList;
import java.util.List;

/** Release mirror plane/column choice and x-y-z visitation order, before world filtering. */
public final class PlacementMirrorShape {
    public record Cell(int x,int y,int z){}
    public static List<Cell> candidates(int originX,int originY,int originZ,int faceX,int faceY,int faceZ,int size,int rotation,float pitch){
        int range=(size^1)/2;boolean topOrBottom=faceY!=0;
        int xOff=faceX!=0?0:(!topOrBottom?range:(!(pitch>75F)&&(rotation&1)!=0?0:range));
        int yOff=topOrBottom?(pitch>75F?0:range):range;
        int zOff=faceZ!=0?0:(topOrBottom?(!(pitch>75F)&&(rotation&1)!=1?0:range):range);
        var result=new ArrayList<Cell>();
        for(int x=-xOff;x<xOff+1;x++)for(int y=0;y<yOff*2+1;y++)for(int z=-zOff;z<zOff+1;z++)
            result.add(new Cell(originX+x+faceX,originY+y+faceY,originZ+z+faceZ));
        return List.copyOf(result);
    }
    private PlacementMirrorShape(){}
}
