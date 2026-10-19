package thaumic.tinkerer.kami;

import java.awt.Color;

/** ToolModeHUDHandler's message, fade and color; advanced once per client tick. */
public final class ToolModeMessage {
    private String text;
    private int remainingTicks;
    private int elapsedTicks;

    public void set(String message) {
        // Source quirk: repeating even an expired message does not restart its timer.
        if(!message.equals(text)) {text=message;remainingTicks=400;}
    }
    public void clientTick() {
        if(remainingTicks>0)remainingTicks--;
        elapsedTicks++;
    }
    public String text() {return text;}
    public int remainingTicks() {return remainingTicks;}
    public boolean visible() {return remainingTicks>0&&text!=null&&!text.isEmpty();}
    public int color() {
        int opacity=Math.min(160,(int)(remainingTicks*256F/10F));
        // Keep addition: the original RGB already contains alpha FF, so the sum wraps.
        return Color.HSBtoRGB((float)Math.cos(elapsedTicks/250D),.6F,1F)+(opacity<<24);
    }
    public int x(int screenWidth,int textWidth) {return (screenWidth-textWidth)/2;}
    public int y(int screenHeight) {return screenHeight-72;}
}
