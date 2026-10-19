package thaumic.tinkerer.kami;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;

public final class KamiArmorContent {
    private static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,ThaumicTinkerer.ID);
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> MATERIAL=MATERIALS.register("ichor",()->{
        var defense=new EnumMap<ArmorItem.Type,Integer>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET,3);defense.put(ArmorItem.Type.CHESTPLATE,8);defense.put(ArmorItem.Type.LEGGINGS,6);defense.put(ArmorItem.Type.BOOTS,3);defense.put(ArmorItem.Type.BODY,0);
        return new ArmorMaterial(defense,20,SoundEvents.ARMOR_EQUIP_LEATHER,()->Ingredient.EMPTY,List.of(new ArmorMaterial.Layer(ThaumicTinkerer.id("ichor"))),0,0);
    });
    private static DeferredItem<IchorclothArmorItem> basic(String name,ArmorItem.Type type) {return ThaumicTinkerer.ITEMS.register("ichorcloth_"+name,()->new IchorclothArmorItem(type));}
    private static DeferredItem<AwakenedIchorclothArmorItem> awakened(String name,ArmorItem.Type type) {return ThaumicTinkerer.ITEMS.register("ichorcloth_"+name+"_gem",()->new AwakenedIchorclothArmorItem(type));}
    public static final DeferredItem<IchorclothArmorItem> HELM=basic("helm",ArmorItem.Type.HELMET);
    public static final DeferredItem<IchorclothArmorItem> CHEST=basic("chest",ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<IchorclothArmorItem> LEGS=basic("legs",ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<IchorclothArmorItem> BOOTS=basic("boots",ArmorItem.Type.BOOTS);
    public static final DeferredItem<AwakenedIchorclothHelmItem> GEM_HELM=ThaumicTinkerer.ITEMS.register("ichorcloth_helm_gem",AwakenedIchorclothHelmItem::new);
    public static final DeferredItem<AwakenedIchorclothArmorItem> GEM_CHEST=awakened("chest",ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<AwakenedIchorclothArmorItem> GEM_LEGS=awakened("legs",ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<AwakenedIchorclothArmorItem> GEM_BOOTS=awakened("boots",ArmorItem.Type.BOOTS);
    public static void register(IEventBus bus) {MATERIALS.register(bus);}
    public static void display(CreativeModeTab.Output output) {for(var item:List.of(HELM,CHEST,LEGS,BOOTS,GEM_HELM,GEM_CHEST,GEM_LEGS,GEM_BOOTS))output.accept(item.get());}
    private KamiArmorContent() {}
}
