package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.menu.MobMagnetMenu;

public final class MagnetContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumicTinkerer.ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ThaumicTinkerer.ID);
    private static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENTS = DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES, ThaumicTinkerer.ID);
    public static final DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>, net.neoforged.neoforge.common.crafting.IngredientType<thaumic.tinkerer.crafting.OreMetalIngredient>> ORE_METAL = INGREDIENTS.register("ore_metal", () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(thaumic.tinkerer.crafting.OreMetalIngredient.CODEC));
    public static final DeferredBlock<MagnetBlock> MAGNET = BLOCKS.register("magnet", () -> new MagnetBlock(false));
    public static final DeferredBlock<MagnetBlock> MOB_MAGNET = BLOCKS.register("mob_magnet", () -> new MagnetBlock(true));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagnetBlockEntity>> MAGNET_TILE = TILES.register("magnet", () -> BlockEntityType.Builder.of(MagnetBlockEntity::new, MAGNET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobMagnetBlockEntity>> MOB_TILE = TILES.register("mob_magnet", () -> BlockEntityType.Builder.of(MobMagnetBlockEntity::new, MOB_MAGNET.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MobMagnetMenu>> MENU = MENUS.register("mob_magnet", () -> new MenuType<>(MobMagnetMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static {
        ThaumicTinkerer.ITEMS.register("magnet", () -> new BlockItem(MAGNET.get(), new Item.Properties()));
        ThaumicTinkerer.ITEMS.register("mob_magnet", () -> new BlockItem(MOB_MAGNET.get(), new Item.Properties()));
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); TILES.register(bus); MENUS.register(bus); INGREDIENTS.register(bus); bus.addListener(MagnetContent::capabilities); }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MOB_TILE.get(), (tile, side) -> tile.handler);
    }
    private MagnetContent() { }
}
