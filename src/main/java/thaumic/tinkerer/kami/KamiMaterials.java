package thaumic.tinkerer.kami;

import java.util.List;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

/** Eight release variants flattened to their permanent item identities. */
public final class KamiMaterials {
    private static DeferredItem<Item> item(String id,boolean kamiRarity) {
        return ThaumicTinkerer.ITEMS.register(id,()->new Item(new Item.Properties().rarity(kamiRarity?Rarity.EPIC:Rarity.COMMON)));
    }
    public static final DeferredItem<Item> ICHOR=item("ichor",true);
    public static final DeferredItem<Item> ICHOR_CLOTH=item("ichor_cloth",true);
    public static final DeferredItem<Item> ICHORIUM=item("ichorium",true);
    public static final DeferredItem<Item> ICHOR_NUGGET=item("ichor_nugget",true);
    public static final DeferredItem<Item> ICHOR_CAP=item("ichor_cap",true);
    public static final DeferredItem<Item> ICHORCLOTH_ROD=item("ichorcloth_rod",true);
    public static final DeferredItem<Item> NETHER_SHARD=item("nether_shard",false);
    public static final DeferredItem<Item> ENDER_SHARD=item("ender_shard",false);
    public static void register() {}
    public static List<DeferredItem<Item>> sourceOrder() {return List.of(ICHOR,ICHOR_CLOTH,ICHORIUM,ICHOR_NUGGET,ICHOR_CAP,ICHORCLOTH_ROD,NETHER_SHARD,ENDER_SHARD);}
    public static void display(CreativeModeTab.Output output) {for(var item:sourceOrder())output.accept(item.get());}
    private KamiMaterials() {}
}
