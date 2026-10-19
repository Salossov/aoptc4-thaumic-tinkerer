package thaumic.tinkerer.kami.pouch;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.menu.IchorPouchMenu;

public final class IchorPouchContent {
    public static final String ID = "ichor_pouch";
    public static final DeferredItem<IchorPouchItem> POUCH = ThaumicTinkerer.ITEMS.register(ID, IchorPouchItem::new);

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumicTinkerer.ID);
    public static final DeferredHolder<MenuType<?>, MenuType<IchorPouchMenu>> MENU =
        MENUS.register(ID, () -> IMenuTypeExtension.create(IchorPouchMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    public static void display(CreativeModeTab.Output output) {
        output.accept(POUCH.get());
    }

    private IchorPouchContent() {}
}
