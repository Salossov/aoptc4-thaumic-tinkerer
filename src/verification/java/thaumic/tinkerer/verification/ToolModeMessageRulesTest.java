package thaumic.tinkerer.verification;

import java.awt.Color;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.kami.ToolModeMessage;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ToolModeMessageRulesTest {
    private static void check(boolean result,String message) {if(!result)throw new IllegalStateException(message);}
    public static void verify() {
        var hud=new ToolModeMessage();check(!hud.visible(),"no message before a mode change");
        hud.set("disabled");check(hud.visible()&&hud.remainingTicks()==400,"a changed message begins at 400 client ticks");
        check(hud.x(321,80)==120&&hud.y(240)==168,"source integer centering and height minus 72");
        int initial=hud.color();
        for(int frame=0;frame<1000;frame++) {check(hud.color()==initial&&hud.visible(),"render reads never advance time");}
        check((initial>>>24)==159,"source addition preserves the wrapped alpha of 159 rather than OR alpha 160");
        for(int tick=1;tick<=400;tick++) {
            hud.clientTick();int left=400-tick;
            int expected=Color.HSBtoRGB((float)Math.cos(tick/250D),.6F,1F)+(Math.min(160,(int)(left*256F/10F))<<24);
            check(hud.remainingTicks()==left&&hud.visible()==(left>0),"only client ticks advance the fixed duration");
            check(hud.color()==expected,"source hue and fade arithmetic through every tick including the last six");
            if(tick==100) {hud.set("disabled");check(hud.remainingTicks()==300,"same visible text does not reset duration");}
        }
        hud.set("disabled");check(!hud.visible()&&hud.remainingTicks()==0,"same expired text stays expired as in source");
        hud.clientTick();check(hud.remainingTicks()==0,"expired timer does not become negative");
        hud.set("enabled");check(hud.visible()&&hud.remainingTicks()==400,"opposite mode restarts the full timer");
        hud.set("");check(!hud.visible(),"empty source messages do not draw");
    }
    @GameTest(template="empty",batch="kami_state")
    public static void messageTiming(GameTestHelper helper) {VerificationReports.install();verify();helper.succeed();}
    public static void main(String[] args) {verify();System.out.println("TT_TOOL_MODE_MESSAGE_RULES_PASS duration=400 render_independent=true source_fade=true");}
    private ToolModeMessageRulesTest() {}
}
