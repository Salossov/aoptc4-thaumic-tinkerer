package thaumic.tinkerer.integration;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.item.SoulMouldItem;

/** Original Waila body lines, shared by the Jade and WTHIT public API adapters. */
public final class DeviceTooltip {
    public static final String DATA_KEY="thaumictinkerer:device_details";
    public static final List<Class<? extends BlockEntity>> TYPES=List.of(AnimationTabletBlockEntity.class,
            TransvectorInterfaceBlockEntity.class,RepairerBlockEntity.class,MagnetBlockEntity.class);
    public static List<Component> lines(BlockEntity tile) {
        var result=new ArrayList<Component>();
        if(tile instanceof AnimationTabletBlockEntity tablet) {
            var item=tablet.getItem(0);
            result.add(text("currentTool",name(item)));
            if(!item.isEmpty()) {
                result.add(text(tablet.leftClick?"leftClick":"rightClick"));
                result.add(text(tablet.redstone?"redstone":"autonomous"));
            }
        } else if(tile instanceof TransvectorInterfaceBlockEntity transvector) {
            var target=transvector.resolve();
            result.add(text("connected",target==null?text("nothing"):target.getBlockState().getBlock().getName()));
            if(target!=null) {
                var p=target.getBlockPos();result.add(Component.literal("x: "+p.getX()+" y: "+p.getY()+" z: "+p.getZ()));
            }
        } else if(tile instanceof RepairerBlockEntity repairer) {
            var item=repairer.getItem(0);
            if(!item.isEmpty())result.add(text(item.getDamageValue()>0?"repairing":"finishedRepairing",item.getHoverName()));
        } else if(tile instanceof MagnetBlockEntity magnet) {
            boolean pull=magnet.getBlockState().getValue(MagnetBlock.PULLING);
            result.add(text(pull?"pullingMode":"pushingMode"));
            if(magnet instanceof MobMagnetBlockEntity mobs) {
                var item=mobs.getItem(0);String key=(pull?"pulling":"pushing")+(mobs.searchesAdults()?"Adult":"Child");
                if(item.isEmpty())result.add(text(key));
                else {
                    String pattern=SoulMouldItem.getPatternName(item);var id=ResourceLocation.tryParse(pattern);
                    var entity=id==null?null:BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
                    // Flattened entity IDs replace the old entity.<registry-name>.name translation.
                    result.add(text(key+"Type",entity==null?Component.translatable("entity."+pattern+".name"):entity.getDescription()));
                }
            }
        }
        return List.copyOf(result);
    }
    private static Component name(ItemStack item) {return item.isEmpty()?text("nothing"):item.getHoverName();}
    private static Component text(String key,Object... args) {return Component.translatable("ttwaila."+key,args);}
    public static void write(CompoundTag data,BlockEntity tile) {
        var lines=lines(tile);data.remove(DATA_KEY);
        if(tile==null||tile.getLevel()==null||TYPES.stream().noneMatch(type->type.isInstance(tile)))return;
        var encoded=new ListTag();
        for(var line:lines)encoded.add(StringTag.valueOf(Component.Serializer.toJson(line,tile.getLevel().registryAccess())));
        data.put(DATA_KEY,encoded);
    }
    public static List<Component> read(CompoundTag data,HolderLookup.Provider registries) {
        var result=new ArrayList<Component>();
        for(var entry:data.getList(DATA_KEY,Tag.TAG_STRING)) {
            try {var line=Component.Serializer.fromJson(entry.getAsString(),registries);if(line!=null)result.add(line);}
            catch(com.google.gson.JsonParseException ignored) { /* An incomplete tooltip payload must not break HUD rendering. */ }
        }
        return List.copyOf(result);
    }
    public static List<Component> readOrLocal(CompoundTag data,BlockEntity tile,HolderLookup.Provider registries) {
        return data.contains(DATA_KEY)?read(data,registries):lines(tile);
    }
    private DeviceTooltip() {}
}
