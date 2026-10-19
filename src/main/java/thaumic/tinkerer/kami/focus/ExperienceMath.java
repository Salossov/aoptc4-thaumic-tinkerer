package thaumic.tinkerer.kami.focus;

/** Modern integer-point progression. Keep truncation separate from level totals. */
public final class ExperienceMath {
    public static int pointsForLevel(int level){return (int)Math.min(Integer.MAX_VALUE,curve(level));}
    private static double curve(int level){
        if(level<=0)return 0;
        if(level<=16)return (double)level*level+6D*level;
        if(level<=31)return 2.5D*level*level-40.5D*level+360;
        return 4.5D*level*level-162.5D*level+2220;
    }
    public static int total(int level,float progress,int nextCost){
        long total=(long)pointsForLevel(level)+(int)(progress*nextCost);
        return (int)Math.max(0,Math.min(Integer.MAX_VALUE,total));
    }
    public static int levelForPoints(int points){
        if(points<=0)return 0;
        // 32768 already exceeds the int point counter. Compare the uncapped
        // curve so Integer.MAX_VALUE cannot cause an unbounded increment loop.
        int low=0,high=32768;
        while(low+1<high){int mid=(low+high)>>>1;if(curve(mid)<=points)low=mid;else high=mid;}
        return low;
    }
    public static float progressForPoints(int remainder,int nextCost){
        if(remainder<=0||nextCost<=0)return 0;
        float result=(float)remainder/nextCost;
        // A rounded-down float quotient must not lose an additional XP point
        // when the next drain truncates its product again.
        if((int)(result*nextCost)<remainder)result=Math.nextUp(result);
        return result;
    }
    private ExperienceMath(){}
}
