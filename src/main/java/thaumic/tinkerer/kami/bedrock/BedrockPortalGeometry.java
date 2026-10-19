package thaumic.tinkerer.kami.bedrock;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

public final class BedrockPortalGeometry {
    /** Release starts at 120, walks down through air, then stands three higher.
     * Bound the walk before transfer; an empty column leaves its player home.
     */
    public static OptionalInt returnHeight(int minY,int maxY,IntPredicate air){
        int y=Math.min(120,maxY-1);
        while(y>=minY&&air.test(y))y--;
        return y<minY||y+3>=maxY?OptionalInt.empty():OptionalInt.of(y+3);
    }
    private BedrockPortalGeometry(){}
}
