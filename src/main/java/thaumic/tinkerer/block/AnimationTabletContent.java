package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.menu.AnimationTabletMenu;

public final class AnimationTabletContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ThaumicTinkerer.ID);
    public static final DeferredBlock<AnimationTabletBlock> BLOCK=BLOCKS.register("animation_tablet",AnimationTabletBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AnimationTabletBlockEntity>> TILE=TILES.register("animation_tablet",()->BlockEntityType.Builder.of(AnimationTabletBlockEntity::new,BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<AnimationTabletMenu>> MENU=MENUS.register("animation_tablet",()->new MenuType<>(AnimationTabletMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static void register(IEventBus bus){ThaumicTinkerer.ITEMS.register("animation_tablet",()->new BlockItem(BLOCK.get(),new Item.Properties()));BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);bus.addListener(AnimationTabletContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,TILE.get(),(tile,side)->new InvWrapper(tile));}
    private AnimationTabletContent(){}
}
