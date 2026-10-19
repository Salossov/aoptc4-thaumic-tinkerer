package thaumic.tinkerer.crop;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.entities.LivingAttackAttemptEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** The original fruit/potion metadata order differs from primal seed order. */
public final class InfusedContent {
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,ThaumicTinkerer.ID);
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredBlock<InfusedFarmlandBlock> SOIL=BLOCKS.register("infused_farmland",InfusedFarmlandBlock::new);
    public static final DeferredBlock<InfusedCropBlock> CROP=BLOCKS.register("infused_grain_block",InfusedCropBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<InfusedFarmlandBlockEntity>> SOIL_TILE=TILES.register("infused_farmland",()->BlockEntityType.Builder.of(InfusedFarmlandBlockEntity::new,SOIL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<InfusedCropBlockEntity>> CROP_TILE=TILES.register("infused_grain_block",()->BlockEntityType.Builder.of(InfusedCropBlockEntity::new,CROP.get()).build(null));
    public static final DeferredItem<InfusedSeedsItem> SEEDS=ThaumicTinkerer.ITEMS.register("infused_seeds",InfusedSeedsItem::new);
    public static final DeferredItem<LegacySpawnEggItem> LEGACY_EGG=ThaumicTinkerer.ITEMS.register("legacy_spawn_egg",LegacySpawnEggItem::new);
    static{ThaumicTinkerer.ITEMS.register("infused_farmland",()->new BlockItem(SOIL.get(),new Item.Properties()));ThaumicTinkerer.ITEMS.register("infused_grain_block",()->new BlockItem(CROP.get(),new Item.Properties()));}
    public static final List<Aspect> FRUIT_ASPECTS=List.of(Aspects.AIR,Aspects.FIRE,Aspects.EARTH,Aspects.WATER);
    public static final List<DeferredHolder<MobEffect,MobEffect>> IMBUED=List.of(effect("air_imbued"),effect("fire_imbued"),effect("earth_imbued"),effect("water_imbued"));
    public static final List<DeferredItem<InfusedGrainItem>> FRUITS=java.util.stream.IntStream.range(0,4).mapToObj(i->ThaumicTinkerer.ITEMS.register("infused_grain_"+FRUIT_ASPECTS.get(i).getTag(),()->new InfusedGrainItem(FRUIT_ASPECTS.get(i)))).toList();
    public static final List<DeferredItem<InfusedPotionItem>> POTIONS=java.util.stream.IntStream.range(0,4).mapToObj(i->ThaumicTinkerer.ITEMS.register("infused_potion_"+FRUIT_ASPECTS.get(i).getTag(),()->new InfusedPotionItem(i))).toList();
    private static DeferredHolder<MobEffect,MobEffect> effect(String name){return EFFECTS.register(name,()->new MobEffect(MobEffectCategory.HARMFUL,0){});}
    public static void register(IEventBus bus){BLOCKS.register(bus);TILES.register(bus);EFFECTS.register(bus);bus.addListener(InfusedContent::capabilities);LivingAttackAttemptEvent.BUS.addListener(InfusedPotionEffects::attack);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(EssentiaCapabilities.CONTAINER,SOIL_TILE.get(),(tile,unused)->tile);event.registerBlockEntity(EssentiaCapabilities.CONTAINER,CROP_TILE.get(),(tile,unused)->tile);}
    public static void display(CreativeModeTab.Output output){for(var aspect:Aspects.PRIMALS){var stack=InfusedSeedsItem.of(aspect);InfusedSeedsItem.tendencies(stack,new AspectList());output.accept(stack);}FRUITS.forEach(i->output.accept(i.get()));POTIONS.forEach(i->output.accept(i.get()));}
    private InfusedContent(){}
}
