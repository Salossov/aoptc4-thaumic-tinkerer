package thaumic.tinkerer.kami.focus;

import java.util.function.IntPredicate;

/** The source singleton counter advances before every attempt, including failure. */
public final class XpDrainCycle {
    private int lastGiven;
    public int next(IntPredicate hasRoom){
        for(int takes=0;takes<7;takes++){
            lastGiven=lastGiven==5?0:lastGiven+1;
            if(hasRoom.test(lastGiven))return lastGiven;
        }
        return -1;
    }
}
