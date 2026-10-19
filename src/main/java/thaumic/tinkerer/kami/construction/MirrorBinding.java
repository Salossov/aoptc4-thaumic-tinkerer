package thaumic.tinkerer.kami.construction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import thaumic.tinkerer.util.ItemData;

/** Approved block ID/properties/grid format; numeric legacy IDs are never registry IDs. */
final class MirrorBinding {
    static BlockState state(ItemStack stack){
        var data=ItemData.read(stack);var key=ResourceLocation.tryParse(data.getString("block"));
        if(key==null||!BuiltInRegistries.BLOCK.containsKey(key))return Blocks.AIR.defaultBlockState();
        var state=BuiltInRegistries.BLOCK.get(key).defaultBlockState();var properties=data.getCompound("properties");
        for(var property:state.getProperties())if(properties.contains(property.getName()))state=apply(state,property,properties.getString(property.getName()));
        return state;
    }
    private static <T extends Comparable<T>> BlockState apply(BlockState state,Property<T> property,String value){
        var parsed=property.getValue(value);return parsed.isPresent()?state.setValue(property,parsed.get()):state;
    }
    private static <T extends Comparable<T>> String value(BlockState state,Property<T> property){return property.getName(state.getValue(property));}
    static void state(ItemStack stack,BlockState state){
        ItemData.update(stack,data->{
            data.remove("blockName");data.remove("blockID");data.remove("blockMeta");
            if(state==null||state.isAir()){data.remove("block");data.remove("properties");return;}
            data.putString("block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());var properties=new CompoundTag();
            for(var property:state.getProperties())properties.putString(property.getName(),value(state,property));
            data.put("properties",properties);
        });
    }
    static int size(ItemStack stack){var data=ItemData.read(stack);return sanitize(data.contains("size")?data.getInt("size"):3);}
    static void size(ItemStack stack,int value){ItemData.update(stack,data->data.putInt("size",sanitize(value)));}
    private static int sanitize(int value){return Math.max(3,Math.min(11,value|1));}
    private MirrorBinding(){}
}
