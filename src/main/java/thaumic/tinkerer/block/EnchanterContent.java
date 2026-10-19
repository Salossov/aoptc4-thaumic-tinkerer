package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.menu.EnchanterMenu;

public final class EnchanterContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ThaumicTinkerer.ID);
    public static final DeferredBlock<EnchanterBlock> BLOCK=BLOCKS.register("enchanter",EnchanterBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<EnchanterBlockEntity>> TILE=TILES.register("enchanter",()->BlockEntityType.Builder.of(EnchanterBlockEntity::new,BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<EnchanterMenu>> MENU=MENUS.register("enchanter",()->IMenuTypeExtension.create(EnchanterMenu::new));
    public static void register(IEventBus bus){ThaumicTinkerer.ITEMS.register("enchanter",()->new BlockItem(BLOCK.get(),new Item.Properties()));BLOCKS.register(bus);TILES.register(bus);MENUS.register(bus);bus.addListener(EnchanterContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,TILE.get(),(tile,side)->new SidedInvWrapper(tile,side==null?net.minecraft.core.Direction.DOWN:side));}
    private EnchanterContent(){}
}
