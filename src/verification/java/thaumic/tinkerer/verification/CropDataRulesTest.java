package thaumic.tinkerer.verification;

import java.io.*;
import net.minecraft.nbt.*;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.crop.CropAspectData;

/** Native NBT compatibility and source saturation, no game/server bootstrap. */
public final class CropDataRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var input=TagParser.parseTag("{Aspects:[{key:\"ignis\",amount:3},{key:\"aer\",amount:0}]}");var values=CropAspectData.read(input);
        check(values.size()==2&&values.getAspects()[0]==Aspects.FIRE&&values.getAspects()[1]==Aspects.AIR,"release insertion order and discovered zero survive native parsing");
        check(values.getAmount(Aspects.FIRE)==3&&values.getAmount(Aspects.AIR)==0&&CropAspectData.write(values).equals(input),"exact legacy compound/list/key/amount schema");
        var output=new ByteArrayOutputStream();NbtIo.writeCompressed(CropAspectData.write(values),output);var restored=NbtIo.readCompressed(new ByteArrayInputStream(output.toByteArray()),NbtAccounter.unlimitedHeap());
        check(restored.equals(input)&&CropAspectData.read(restored).size()==2,"real compressed native NBT round trip");
        var empty=CropAspectData.read(new CompoundTag());check(empty.size()==0&&CropAspectData.first(empty)==null&&CropAspectData.write(empty).getList("Aspects",10).isEmpty(),"missing/empty main aspect maps original null sentinel without changing public API arrays");
        var duplicate=CropAspectData.read(TagParser.parseTag("{Aspects:[{key:\"aqua\",amount:2},{key:\"aqua\",amount:3}]}"));check(duplicate.size()==1&&duplicate.getAmount(Aspects.WATER)==5,"duplicate legacy NBT entries use source add arithmetic");
        var unknown=CropAspectData.read(TagParser.parseTag("{Aspects:[{amount:2},{key:\"removed_addon_aspect\",amount:1}]}"));check(unknown.size()==1&&CropAspectData.first(unknown)==null&&CropAspectData.write(unknown).getList("Aspects",10).isEmpty(),"source skips keyless entry and omits unresolved null aspect when writing");
        values=new AspectList().add(Aspects.WATER,21);CropAspectData.saturate(values,20);check(values.size()==1&&values.getAmount(Aspects.WATER)==20,"soil saturation removes one actual unit from a single type");
        values=new AspectList().add(Aspects.WATER,55);CropAspectData.saturate(values,50);check(values.size()==1&&values.getAmount(Aspects.WATER)==50,"crop tendency saturation uses fifty rather than soil twenty");
        values=new AspectList().add(Aspects.EARTH,1);CropAspectData.saturate(values,0);check(values.size()==0,"saturation deletes exhausted type instead of retaining a reduce-zero");
        values=new AspectList().add(Aspects.FIRE,3).add(Aspects.AIR,0);CropAspectData.saturate(values,20);check(values.size()==2&&values.getAmount(Aspects.FIRE)==3,"unsaturated lists preserve zero entries and amounts");
        int zeroRemovalHole=0;boolean conserved=true,validHoles=true,strictCrop=true,strictSoil=true;
        for(int iteration=0;iteration<1000;iteration++){
            values=new AspectList().add(Aspects.WATER,51).add(Aspects.AIR,0);legacySaturate(values,50);
            conserved&=values.visSize()==50||values.visSize()==51;
            if(values.visSize()==51){zeroRemovalHole++;validHoles&=values.size()==1&&values.getAmount(Aspects.WATER)==51;}
            values=new AspectList().add(Aspects.WATER,51).add(Aspects.AIR,0);CropAspectData.saturate(values,50);strictCrop&=values.visSize()==50&&values.getAmount(Aspects.WATER)==50;
            values=new AspectList().add(Aspects.EARTH,21).add(Aspects.AIR,0);CropAspectData.saturate(values,20);strictSoil&=values.visSize()==20&&values.getAmount(Aspects.EARTH)==20;
        }
        check(conserved,"source one-iteration saturation neither duplicates units nor erases extra units");
        check(validHoles,"source zero-entry selection consumes removal iteration without reducing total");
        check(zeroRemovalHole>0,"legacy saturation hole observed with genuine per-removal Random instances");
        check(strictCrop,"approved fix enforces crop fifty even when a zero is chosen first");
        check(strictSoil,"approved fix enforces soil twenty even when a zero is chosen first");
        int growthBound=(2510-51*51)*6;boolean rejects=false;try{new java.util.Random().nextInt(growthBound);}catch(IllegalArgumentException expected){rejects=true;}
        check(rejects&&growthBound==-546,"51 water tendency makes original growth bound invalid; do not silently change it");
        int validBound=(2510-50*50)*6;check(validBound==60&&new java.util.Random().nextInt(validBound)>=0,"strict cap keeps unmodified original growth formula valid");
        System.out.println("TT_CROP_DATA_PASS checks="+checks+" zero_removal_hole_observed="+zeroRemovalHole);
    }
    private static void legacySaturate(AspectList values,int maximum){
        // Pinned source, retained only as a private verification reference.
        for(int remaining=values.visSize()-maximum;remaining>0;remaining--){var types=values.getAspects();values.remove(types[new java.util.Random().nextInt(types.length)],1);}
    }
    private CropDataRulesTest(){}
}
