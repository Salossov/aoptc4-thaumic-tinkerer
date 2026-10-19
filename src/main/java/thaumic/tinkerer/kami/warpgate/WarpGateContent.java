package thaumic.tinkerer.kami.warpgate;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.api.addon.AddonRegistrationEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.api.wands.FocusBehaviors;
import thaumic.tinkerer.ThaumicTinkerer;

public final class WarpGateContent {
    public static final String ID_WARP_GATE = "warp_gate";
    public static final String ID_SKY_PEARL = "sky_pearl";
    public static final String ID_FOCUS_RECALL = "focus_recall";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    public static final DeferredBlock<WarpGateBlock> WARP_GATE = BLOCKS.register(ID_WARP_GATE, () -> new WarpGateBlock());
    public static final DeferredItem<WarpGateBlockItem> WARP_GATE_ITEM = ThaumicTinkerer.ITEMS.register(ID_WARP_GATE, () -> new WarpGateBlockItem(WARP_GATE.get()));

    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicTinkerer.ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarpGateBlockEntity>> WARP_GATE_TILE =
            TILES.register(ID_WARP_GATE, () -> BlockEntityType.Builder.of(WarpGateBlockEntity::new, WARP_GATE.get()).build(null));

    public static final DeferredItem<SkyPearlItem> SKY_PEARL = ThaumicTinkerer.ITEMS.register(ID_SKY_PEARL, SkyPearlItem::new);
    public static final DeferredItem<FocusRecallItem> FOCUS_RECALL = ThaumicTinkerer.ITEMS.register(ID_FOCUS_RECALL, FocusRecallItem::new);

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumicTinkerer.ID);
    public static final DeferredHolder<MenuType<?>, MenuType<WarpGateMenu>> MENU =
            MENUS.register(ID_WARP_GATE, () -> IMenuTypeExtension.create(WarpGateMenu::new));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);

        bus.addListener((RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1");
            registrar.playToServer(WarpGateButtonPayload.TYPE, WarpGateButtonPayload.CODEC, WarpGateButtonPayload::handle);
            registrar.playToServer(WarpGateTeleportPayload.TYPE, WarpGateTeleportPayload.CODEC, WarpGateTeleportPayload::handle);
        });

        AddonRegistrationEvent.BUS.addListener((AddonRegistrationEvent event) ->
                FocusBehaviors.register(FOCUS_RECALL.get(), FOCUS_RECALL.get(), ThaumicTinkerer.id("wand/focus_recall"), null));
    }

    public static void display(CreativeModeTab.Output output) {
        output.accept(WARP_GATE_ITEM.get());
        output.accept(SKY_PEARL.get());
        output.accept(FOCUS_RECALL.get());
    }

    private WarpGateContent() {}
}
