package thaumic.tinkerer.kami.focus;

import java.awt.Color;
import java.util.Objects;
import java.util.function.Supplier;

/** Source rainbow follows the viewing player's ticks, and is white without one. */
public final class XpDrainVisual {
    private static Supplier<Integer> viewerTicks=()->null;
    public static void clock(Supplier<Integer> source){viewerTicks=Objects.requireNonNull(source);}
    public static int color(){Integer ticks=viewerTicks.get();return ticks==null?0xFFFFFF:color(ticks.intValue());}
    public static int color(int ticks){return Color.HSBtoRGB(ticks*2%360/360F,1F,1F);}
    private XpDrainVisual(){}
}
