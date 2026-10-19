package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.EssentiaCapabilities;
import thaumic.tinkerer.ThaumicTinkerer;

public final class RepairerContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredBlock<RepairerBlock> BLOCK=BLOCKS.register("repairer",RepairerBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<RepairerBlockEntity>> TILE=TILES.register("repairer",()->BlockEntityType.Builder.of(RepairerBlockEntity::new,BLOCK.get()).build(null));
    static{ThaumicTinkerer.ITEMS.register("repairer",()->new BlockItem(BLOCK.get(),new Item.Properties()));}
    public static void register(IEventBus bus){BLOCKS.register(bus);TILES.register(bus);bus.addListener(RepairerContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,TILE.get(),(tile,side)->new SidedInvWrapper(tile,side));
        event.registerBlockEntity(EssentiaCapabilities.CONTAINER,TILE.get(),(tile,unused)->tile);
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT,TILE.get(),(tile,side)->tile);
    }
    private RepairerContent(){}
}
