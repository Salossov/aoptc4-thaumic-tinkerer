package thaumic.tinkerer.kami;

import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandParts;
import thaumcraft.api.wands.WandRod;
import thaumcraft.api.wands.WandVisuals;
import thaumic.tinkerer.ThaumicTinkerer;

public final class KamiWandContent {
    public static final String ROD="thaumictinkerer:ichorcloth",CAP="thaumictinkerer:ichor";
    private static final List<String> CAPS=List.of("iron","gold","thaumium","void","copper","silver");
    private static final List<String> RODS=List.of("wood","greatwood","obsidian","silverwood","ice","quartz","reed","blaze","bone");
    public static void register() {
        new WandRod(ThaumicTinkerer.id("ichorcloth"),"thaumictinkerer:rod_ichorcloth",1000,()->KamiMaterials.ICHORCLOTH_ROD.get().getDefaultInstance(),10,null);
        new WandCap(ThaumicTinkerer.id("ichor"),"thaumictinkerer:cap_ichor",.7f,Set.of(),.7f,()->KamiMaterials.ICHOR_CAP.get().getDefaultInstance(),10);
        for(var kind:List.of("wand","sceptre")) {
            for(var cap:CAPS)rodVisual(kind,cap,cap);
            rodVisual(kind,CAP,"ichor");
            for(var rod:RODS)visual(kind,rod,CAP,rod+"_ichor");
        }
        for(var rod:RODS)if(!rod.equals("wood"))visual("staff",rod+"_staff",CAP,rod+"_ichor");
        visual("staff","primal_staff",CAP,"primal_ichor");
    }
    private static void visual(String kind,String rod,String cap,String model) {
        WandVisuals.register(kind,new WandParts(rod,cap),ThaumicTinkerer.id(kind+"/"+model));
    }
    private static void rodVisual(String kind,String cap,String modelCap) {
        String model="ichorcloth_"+modelCap;var parts=new WandParts(ROD,cap);
        visual(kind,ROD,cap,model);
        WandVisuals.registerLayers(kind,parts,List.of(new WandVisuals.Layer(ThaumicTinkerer.id(kind+"/"+model+"_rod"),KamiWandContent::rodPackedLight),new WandVisuals.Layer(ThaumicTinkerer.id(kind+"/"+model+"_cap"),null)));
    }
    /** Old ModelWand writes raw lightmap coordinates j=200+sin(ticks)*5+5, sky=0. */
    public static int rodPackedLight(int worldLight,int viewerTicks) {return (int)(200f+Mth.sin((float)viewerTicks)*5f+5f);}
    private KamiWandContent() {}
}
