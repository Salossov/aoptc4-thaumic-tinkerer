package thaumic.tinkerer.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumic.tinkerer.ThaumicTinkerer;

public final class MobilizerContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredBlock<MobilizerBlock> LOCOMOTIVE=BLOCKS.register("levitational_locomotive",()->new MobilizerBlock(false));
    public static final DeferredBlock<MobilizerBlock> RELAY=BLOCKS.register("levitational_locomotive_relay",()->new MobilizerBlock(true));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MobilizerBlockEntity>> LOCOMOTIVE_TILE=TILES.register("levitational_locomotive",()->BlockEntityType.Builder.of(MobilizerBlockEntity::new,LOCOMOTIVE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MobilizerRelayBlockEntity>> RELAY_TILE=TILES.register("levitational_locomotive_relay",()->BlockEntityType.Builder.of(MobilizerRelayBlockEntity::new,RELAY.get()).build(null));
    static {
        ThaumicTinkerer.ITEMS.register("levitational_locomotive",()->new BlockItem(LOCOMOTIVE.get(),new Item.Properties()));
        ThaumicTinkerer.ITEMS.register("levitational_locomotive_relay",()->new BlockItem(RELAY.get(),new Item.Properties()));
    }
    public static void register(IEventBus bus){BLOCKS.register(bus);TILES.register(bus);}
    private MobilizerContent(){}
}
