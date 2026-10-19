package thaumic.tinkerer.item.dislocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import thaumic.tinkerer.util.ItemData;

/** Accepted modern capture format, including optional complete two-cell assemblies. */
public record DislocationCapture(List<DislocationCapture.Part> parts) {
    public record Part(BlockPos offset,BlockState state,CompoundTag tile) {
        public Part { offset=offset.immutable();tile=tile.copy(); }
        @Override public CompoundTag tile(){return tile.copy();}
    }
    public DislocationCapture {
        parts=List.copyOf(parts);
        if(parts.isEmpty()||parts.size()>2||!parts.getFirst().offset().equals(BlockPos.ZERO))
            throw new IllegalArgumentException("Invalid capture assembly");
    }
    public Part root(){return parts.getFirst();}

    public static Optional<DislocationCapture> read(ItemStack focus) {
        var data=ItemData.read(focus);
        if(!data.getBoolean("available"))return Optional.empty();
        if(data.contains("blockState")&&!data.contains("blockState",Tag.TAG_COMPOUND))return Optional.empty();
        BlockState state;
        if(data.contains("blockState",Tag.TAG_COMPOUND))state=readState(data.getCompound("blockState"));
        else {
            // Resource names may survive old data. Numeric blockID/blockMeta do
            // not describe entries/properties in the modern block registry.
            var id=ResourceLocation.tryParse(data.getString("blockName"));
            state=id==null?null:BuiltInRegistries.BLOCK.getOptional(id).map(block->block.defaultBlockState()).orElse(null);
        }
        if(state==null||state.isAir())return Optional.empty();
        if(data.contains("tileCmp")&&!data.contains("tileCmp",Tag.TAG_COMPOUND))return Optional.empty();
        var root=new Part(BlockPos.ZERO,state,data.getCompound("tileCmp"));
        if(!data.contains("parts"))return Optional.of(new DislocationCapture(List.of(root)));
        if(!data.contains("parts",Tag.TAG_LIST))return Optional.empty();
        var saved=data.getList("parts",Tag.TAG_COMPOUND);
        if(saved.size()!=2)return Optional.empty();
        var result=new ArrayList<Part>();
        for(int index=0;index<saved.size();index++) {
            var part=saved.getCompound(index);
            if(!part.contains("dx",Tag.TAG_INT)||!part.contains("dy",Tag.TAG_INT)||!part.contains("dz",Tag.TAG_INT)
                    ||!part.contains("blockState",Tag.TAG_COMPOUND))return Optional.empty();
            var value=readState(part.getCompound("blockState"));if(value==null||value.isAir())return Optional.empty();
            if(part.contains("tileCmp")&&!part.contains("tileCmp",Tag.TAG_COMPOUND))return Optional.empty();
            result.add(new Part(new BlockPos(part.getInt("dx"),part.getInt("dy"),part.getInt("dz")),value,part.getCompound("tileCmp")));
        }
        var first=result.getFirst();
        if(!first.offset().equals(BlockPos.ZERO)||first.state()!=root.state()||!first.tile.equals(root.tile))return Optional.empty();
        var offset=partnerOffset(state);
        if(offset==null||!result.get(1).offset().equals(offset)||!partners(state,result.get(1).state()))return Optional.empty();
        return Optional.of(new DislocationCapture(result));
    }

    /** Strict names/properties: unknown values keep the item intact instead of silently becoming air/defaults. */
    private static BlockState readState(CompoundTag tag) {
        var id=ResourceLocation.tryParse(tag.getString("Name"));
        var block=id==null?Optional.<net.minecraft.world.level.block.Block>empty():BuiltInRegistries.BLOCK.getOptional(id);
        if(block.isEmpty())return null;
        var state=block.get().defaultBlockState();
        if(tag.contains("Properties")&&!tag.contains("Properties",Tag.TAG_COMPOUND))return null;
        var properties=tag.getCompound("Properties");
        for(String key:properties.getAllKeys()) {
            var property=block.get().getStateDefinition().getProperty(key);
            if(property==null||!properties.contains(key,Tag.TAG_STRING))return null;
            state=withProperty(state,property,properties.getString(key));if(state==null)return null;
        }
        return state;
    }
    private static <T extends Comparable<T>> BlockState withProperty(BlockState state,Property<T> property,String value) {
        return property.getValue(value).map(parsed->state.setValue(property,parsed)).orElse(null);
    }

    public static BlockPos partnerOffset(BlockState state) {
        if(state.getBlock() instanceof DoorBlock)return state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER?new BlockPos(0,1,0):new BlockPos(0,-1,0);
        if(state.getBlock() instanceof DoublePlantBlock)return state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER?new BlockPos(0,1,0):new BlockPos(0,-1,0);
        if(state.getBlock() instanceof BedBlock){var direction=state.getValue(BedBlock.FACING);return BlockPos.ZERO.relative(state.getValue(BedBlock.PART)==BedPart.FOOT?direction:direction.getOpposite());}
        return null;
    }
    public static boolean partners(BlockState first,BlockState second) {
        if(first.getBlock()!=second.getBlock())return false;
        if(first.getBlock() instanceof DoorBlock)return first.getValue(DoorBlock.HALF)!=second.getValue(DoorBlock.HALF);
        if(first.getBlock() instanceof DoublePlantBlock)return first.getValue(DoublePlantBlock.HALF)!=second.getValue(DoublePlantBlock.HALF);
        if(first.getBlock() instanceof BedBlock)return first.getValue(BedBlock.PART)!=second.getValue(BedBlock.PART)
                &&first.getValue(BedBlock.FACING)==second.getValue(BedBlock.FACING);
        return false;
    }

    public void store(ItemStack focus) {
        ItemData.update(focus,data->{
            var root=root();data.putBoolean("available",true);
            data.putString("blockName",BuiltInRegistries.BLOCK.getKey(root.state().getBlock()).toString());
            data.putInt("blockMeta",0);data.put("blockState",NbtUtils.writeBlockState(root.state()));data.put("tileCmp",root.tile());
            if(parts.size()==1)data.remove("parts");
            else {
                var list=new ListTag();
                for(var part:parts){var saved=new CompoundTag();saved.putInt("dx",part.offset().getX());saved.putInt("dy",part.offset().getY());saved.putInt("dz",part.offset().getZ());saved.put("blockState",NbtUtils.writeBlockState(part.state()));saved.put("tileCmp",part.tile());list.add(saved);}
                data.put("parts",list);
            }
        });
    }
}
