package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import thaumic.tinkerer.menu.AspectAnalyzerMenu;
import thaumic.tinkerer.ThaumicTinkerer;

public final class AspectAnalyzerContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ThaumicTinkerer.ID);
    public static final DeferredHolder<MenuType<?>,MenuType<AspectAnalyzerMenu>> MENU=MENUS.register("aspect_analyzer",()->new MenuType<>(AspectAnalyzerMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredBlock<AspectAnalyzerBlock> BLOCK=BLOCKS.register("aspect_analyzer",AspectAnalyzerBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AspectAnalyzerBlockEntity>> TILE=TILES.register("aspect_analyzer",()->BlockEntityType.Builder.of(AspectAnalyzerBlockEntity::new,BLOCK.get()).build(null));
    public static void register(IEventBus bus){ThaumicTinkerer.ITEMS.register("aspect_analyzer",()->new BlockItem(BLOCK.get(),new Item.Properties()));BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);bus.addListener(AspectAnalyzerContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,TILE.get(),(tile,side)->new InvWrapper(tile));
    }
    private AspectAnalyzerContent(){}
}
