package thaumic.tinkerer.summon;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.*;
import thaumcraft.api.entities.LivingAttackAttemptEvent;
import thaumic.tinkerer.ThaumicTinkerer;

public final class SummonContent {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,ThaumicTinkerer.ID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENTS=DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES,ThaumicTinkerer.ID);
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThaumicTinkerer.ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThaumicTinkerer.ID);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> VARIANT=COMPONENTS.register("mob_aspect_variant",()->DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<IngredientType<?>,IngredientType<MobAspectIngredient>> INGREDIENT=INGREDIENTS.register("mob_aspect",()->new IngredientType<>(MobAspectIngredient.CODEC));
    public static final DeferredItem<MobAspectItem> MOB_ASPECT=ThaumicTinkerer.ITEMS.register("mob_aspect",MobAspectItem::new);
    public static final DeferredItem<BloodSwordItem> BLOOD_SWORD=ThaumicTinkerer.ITEMS.register("blood_sword",BloodSwordItem::new);
    public static final DeferredBlock<SummonBlock> BLOCK=BLOCKS.register("spawner",SummonBlock::new);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SummonBlockEntity>> TILE=TILES.register("spawner",()->BlockEntityType.Builder.of(SummonBlockEntity::new,BLOCK.get()).build(null));
    static {ThaumicTinkerer.ITEMS.register("spawner",()->new BlockItem(BLOCK.get(),new Item.Properties()));}
    public static void register(IEventBus bus) {COMPONENTS.register(bus);INGREDIENTS.register(bus);BLOCKS.register(bus);TILES.register(bus);LivingAttackAttemptEvent.BUS.addListener(BloodSwordItem::attack);}
    public static void display(CreativeModeTab.Output output) {output.accept(BLOOD_SWORD.get());output.accept(BLOCK.get());for(int i=0;i<MobAspects.ASPECTS.size();i++){output.accept(MobAspectItem.of(i));output.accept(MobAspectItem.of(42+i));}}
    public static void research() {
        var category=thaumic.tinkerer.research.TinkererResearch.CATEGORY;
        thaumcraft.api.addon.AddonRegistry.research(new thaumcraft.api.research.ResearchDefinition("thaumictinkerer:blood_sword",category,java.util.List.of("thaumictinkerer:cleansing_talisman"),java.util.Map.of("fames",2,"telum",1,"corpus",1,"spiritus",1),3,false,false,-4,6,
                java.util.List.of("page.thaumictinkerer.blood_sword.0","page.thaumictinkerer.blood_sword.1"),java.util.List.of("thaumictinkerer:blood_sword"),"thaumictinkerer:blood_sword",false,false,false,true,java.util.List.of(),java.util.List.of("page.thaumictinkerer.blood_sword.0","thaumictinkerer:blood_sword","page.thaumictinkerer.blood_sword.1"),false,1,java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of("INFUSION")));
        thaumcraft.api.addon.AddonRegistry.research(new thaumcraft.api.research.ResearchDefinition("thaumictinkerer:summon",category,java.util.List.of("thaumictinkerer:blood_sword"),java.util.Map.of("telum",1,"bestia",3,"praecantatio",3),3,false,false,-5,8,
                java.util.List.of("page.thaumictinkerer.summon.0","page.thaumictinkerer.summon.1"),java.util.List.of("thaumictinkerer:spawner","thaumictinkerer:mob_aspect_condensed_messis","thaumictinkerer:mob_aspect_infused_messis"),"thaumictinkerer:spawner",false,false,false,false,java.util.List.of(),java.util.List.of("page.thaumictinkerer.summon.0","thaumictinkerer:spawner","thaumictinkerer:mob_aspect_condensed_messis","thaumictinkerer:mob_aspect_infused_messis","page.thaumictinkerer.summon.1"),false,3,java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of("INFUSION")));
    }
    private SummonContent() { }
}
