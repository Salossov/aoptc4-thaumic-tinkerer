package thaumic.tinkerer.kami;

import net.neoforged.bus.api.Event;

/** Local item-use notification, so common items never link to Minecraft client classes. */
public final class ToolModeNotification extends Event {
    private final String translationKey;
    public ToolModeNotification(String translationKey) {this.translationKey=translationKey;}
    public String translationKey() {return translationKey;}
}
