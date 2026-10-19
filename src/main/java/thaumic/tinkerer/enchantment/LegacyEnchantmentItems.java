package thaumic.tinkerer.enchantment;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.holdersets.*;

/** Original EnumEnchantmentType class tests, including third-party subclasses.
 * Modern mining tags include hoes; ItemHoe was not an ItemTool in 1.7.10. */
public final class LegacyEnchantmentItems<T> extends CompositeHolderSet<T> {
    private static final Set<String> TYPES=Set.of("armor_legs","armor_feet","weapon","digger","bow");
    private static final Codec<String> TYPE=Codec.STRING.validate(value->TYPES.contains(value)?DataResult.success(value):DataResult.error(()->"Unknown legacy enchantment type: "+value));
    private final HolderLookup.RegistryLookup<T> lookup;
    private final String legacyType;
    public LegacyEnchantmentItems(HolderLookup.RegistryLookup<T> lookup,String legacyType){super(List.of());this.lookup=lookup;this.legacyType=legacyType;}
    @Override public HolderSetType type(){return TinkererEnchantments.ITEM_TYPES.get();}
    @Override protected Set<Holder<T>> createSet(){
        var result=new LinkedHashSet<Holder<T>>();lookup.listElements().filter(holder->accepts(holder.value())).forEach(result::add);return Collections.unmodifiableSet(result);
    }
    private boolean accepts(Object value){return accepts(value,legacyType);}
    public static boolean accepts(Object value,String legacyType){return switch(legacyType){
        case "all"->true;
        case "armor"->value instanceof ArmorItem;
        case "armor_head"->value instanceof ArmorItem armor&&armor.getType()==ArmorItem.Type.HELMET;
        case "armor_torso"->value instanceof ArmorItem armor&&armor.getType()==ArmorItem.Type.CHESTPLATE;
        case "armor_legs"->value instanceof ArmorItem armor&&armor.getType()==ArmorItem.Type.LEGGINGS;
        case "armor_feet"->value instanceof ArmorItem armor&&armor.getType()==ArmorItem.Type.BOOTS;
        case "weapon"->value instanceof SwordItem;
        case "digger"->value instanceof DiggerItem&&!(value instanceof HoeItem);
        case "bow"->value instanceof BowItem;
        case "fishing_rod"->value instanceof FishingRodItem;
        case "breakable"->value instanceof Item item&&item.components().getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE,0)>0;
        default->false;
    };}
    public static final class Type implements HolderSetType {
        @Override public <T> MapCodec<? extends ICustomHolderSet<T>> makeCodec(ResourceKey<? extends Registry<T>> key,Codec<Holder<T>> holderCodec,boolean forceList){
            return RecordCodecBuilder.<LegacyEnchantmentItems<T>>mapCodec(builder->builder.group(RegistryOps.retrieveRegistryLookup(key).forGetter(value->value.lookup),TYPE.fieldOf("legacy_type").forGetter(value->value.legacyType)).apply(builder,LegacyEnchantmentItems::new));
        }
        @Override public <T> StreamCodec<RegistryFriendlyByteBuf,? extends ICustomHolderSet<T>> makeStreamCodec(ResourceKey<? extends Registry<T>> key){
            return new StreamCodec<RegistryFriendlyByteBuf,LegacyEnchantmentItems<T>>(){
                @Override public LegacyEnchantmentItems<T> decode(RegistryFriendlyByteBuf buffer){String type=buffer.readUtf(32);if(!TYPES.contains(type))throw new IllegalArgumentException("Unknown legacy enchantment type");return new LegacyEnchantmentItems<>(buffer.registryAccess().lookupOrThrow(key),type);}
                @Override public void encode(RegistryFriendlyByteBuf buffer,LegacyEnchantmentItems<T> value){buffer.writeUtf(value.legacyType,32);}
            };
        }
    }
}
