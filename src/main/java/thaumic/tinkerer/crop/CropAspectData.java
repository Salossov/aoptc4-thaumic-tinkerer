package thaumic.tinkerer.crop;

import java.util.Random;
import net.minecraft.nbt.*;
import thaumcraft.api.aspects.*;

/** Release compound/list layout, insertion order, discovered zero and saturation. */
public final class CropAspectData {
    public static CompoundTag write(AspectList values){
        var result=new CompoundTag();var entries=new ListTag();
        for(var aspect:values.getAspects())if(aspect!=null){var entry=new CompoundTag();entry.putString("key",aspect.getTag());entry.putInt("amount",values.getAmount(aspect));entries.add(entry);}
        result.put("Aspects",entries);return result;
    }
    public static AspectList read(CompoundTag compound){
        var result=new AspectList();var entries=compound.getList("Aspects",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++){var entry=entries.getCompound(i);if(entry.contains("key"))result.add(Aspect.getAspect(entry.getString("key")),entry.getInt("amount"));}
        return result;
    }
    public static Aspect first(AspectList values){return values.size()==0?null:values.getAspects()[0];}
    public static void saturate(AspectList values,int maximum){
        // Approved bug adaptation: selecting a discovered zero must not spend a
        // removal iteration while leaving the actual amount above the limit.
        while(values.visSize()>maximum){var types=values.getAspects();var selected=types[new Random().nextInt(types.length)];values.remove(selected,1);}
    }
    private CropAspectData(){}
}
