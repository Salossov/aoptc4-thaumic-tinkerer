package thaumic.tinkerer.util;

import net.minecraft.world.entity.player.Player;
import thaumic.tinkerer.block.TabletPlayer;

/** The tablet's old EntityPlayer kept raw posY above its feet by a float yOffset. */
public final class LegacyPlayerCoordinates {
    public static final float TABLET_Y_OFFSET=1.62f;
    public static double tabletRawY(int blockY){return blockY+1.6;}
    public static double tabletFeetY(int blockY){return tabletRawY(blockY)-(double)TABLET_Y_OFFSET;}
    static double tabletRayY(double rawY){return rawY+1.62-(double)TABLET_Y_OFFSET;}
    /** Only explicit source posY effects use this; native eyes/centres use modern feet. */
    public static double rawY(Player player){return player instanceof TabletPlayer tablet?tablet.legacyRawY():player.getY();}
    /** Original fixed-eye tool ray, with the removed tablet yOffset and operation order. */
    public static double rayY(Player player){return player instanceof TabletPlayer tablet?tabletRayY(tablet.legacyRawY()):player.getY()+1.62;}
    private LegacyPlayerCoordinates(){}
}
