package thaumic.tinkerer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.crafting.SpellClothRecipe;
import thaumic.tinkerer.item.SpellClothItem;
import thaumic.tinkerer.research.TinkererResearch;

/** Unofficial port of Thaumic Tinkerer 2.5-1.7.10-164, originally by Vazkii and contributors. */
@Mod(ThaumicTinkerer.ID)
public final class ThaumicTinkerer {
    public static final String ID = "thaumictinkerer";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<SpellClothItem> SPELL_CLOTH = ITEMS.register("spell_cloth", SpellClothItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.ShareTomeItem> SHARE_TOME=ITEMS.register("share_tome",thaumic.tinkerer.item.ShareTomeItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.InfusedInkwellItem> INFUSED_INKWELL = ITEMS.register("infused_inkwell", thaumic.tinkerer.item.InfusedInkwellItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.SoulMouldItem> SOUL_MOULD = ITEMS.register("soul_mould", thaumic.tinkerer.item.SoulMouldItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.RevealingHelmItem> REVEALING_HELM = ITEMS.register("revealing_helm", thaumic.tinkerer.item.RevealingHelmItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.XpTalismanItem> XP_TALISMAN = ITEMS.register("xp_talisman", thaumic.tinkerer.item.XpTalismanItem::new);
    public static final DeferredItem<thaumic.tinkerer.item.CleansingTalismanItem> CLEANSING_TALISMAN = ITEMS.register("cleansing_talisman", thaumic.tinkerer.item.CleansingTalismanItem::new);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SpellClothRecipe>> CLOTH_RECIPE =
            RECIPES.register("spell_cloth", () -> new SimpleCraftingRecipeSerializer<>(SpellClothRecipe::new));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumictinkerer"))
            .icon(TinkererCreativeTab::icon)
            .displayItems((parameters, destination) -> TinkererCreativeTab.display(destination, output -> {
                output.accept(SPELL_CLOTH.get());
                thaumic.tinkerer.crop.InfusedContent.display(output);
                thaumic.tinkerer.summon.SummonContent.display(output);
                thaumic.tinkerer.fire.ImbuedFireContent.display(output);
                output.accept(SHARE_TOME.get());
                output.accept(thaumic.tinkerer.block.PlatformContent.BLOCK.get());
                output.accept(thaumic.tinkerer.block.FunnelContent.BLOCK.get());
                output.accept(thaumic.tinkerer.block.RepairerContent.BLOCK.get());
                output.accept(thaumic.tinkerer.block.EnchanterContent.BLOCK.get());
                output.accept(thaumic.tinkerer.block.AnimationTabletContent.BLOCK.get());
                output.accept(thaumic.tinkerer.block.MobilizerContent.LOCOMOTIVE.get());
                output.accept(thaumic.tinkerer.block.MobilizerContent.RELAY.get());
                output.accept(XP_TALISMAN.get());
                output.accept(CLEANSING_TALISMAN.get());
                output.accept(SOUL_MOULD.get());
                output.accept(REVEALING_HELM.get());
                output.accept(thaumic.tinkerer.block.MagnetContent.MAGNET.get());
                output.accept(thaumic.tinkerer.block.MagnetContent.MOB_MAGNET.get());
                output.accept(thaumic.tinkerer.block.TransvectorContent.INTERFACE.get());
                output.accept(thaumic.tinkerer.block.TransvectorContent.DISLOCATOR.get());
                output.accept(thaumic.tinkerer.block.TransvectorContent.CONNECTOR.get());
                if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) { output.accept(thaumic.tinkerer.block.GolemConnectorContent.BLOCK.get()); output.accept(thaumic.tinkerer.block.AspectAnalyzerContent.BLOCK.get()); }
                thaumic.tinkerer.block.DarkQuartzContent.display(output);
                thaumic.tinkerer.block.GasContent.display(output);
                thaumic.tinkerer.item.FocusContent.display(output);
                if(TinkererConfig.KAMI_FORCE_ENABLED.get()) {thaumic.tinkerer.kami.KamiArmorContent.display(output);thaumic.tinkerer.kami.KamiUtilityContent.display(output);}
            }))
            .build());

    public ThaumicTinkerer(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.STARTUP, TinkererConfig.SPEC);
        thaumcraft.api.research.ResearchApi.registerEasyMode(id("easy_research"),TinkererConfig.EASY_RESEARCH::get);
        if(TinkererConfig.KAMI_FORCE_ENABLED.get()) {thaumic.tinkerer.kami.KamiArmorContent.register(bus);thaumic.tinkerer.kami.KamiUtilityContent.register(bus);}
        thaumic.tinkerer.enchantment.TinkererEnchantments.register(bus);
        thaumic.tinkerer.crop.InfusedContent.register(bus);
        thaumic.tinkerer.summon.SummonContent.register(bus);
        thaumic.tinkerer.fire.ImbuedFireContent.register(bus);
        thaumic.tinkerer.item.FocusContent.initialize();
        thaumic.tinkerer.block.DarkQuartzContent.register(bus);
        thaumic.tinkerer.block.GasContent.register(bus);
        thaumic.tinkerer.block.MagnetContent.register(bus);
        thaumic.tinkerer.block.ForcefieldContent.register(bus);
        thaumic.tinkerer.block.TransvectorContent.register(bus);
        thaumic.tinkerer.block.PlatformContent.register(bus);
        thaumic.tinkerer.block.FunnelContent.register(bus);
        thaumic.tinkerer.block.RepairerContent.register(bus);
        thaumic.tinkerer.block.EnchanterContent.register(bus);
        thaumic.tinkerer.block.AnimationTabletContent.register(bus);
        thaumic.tinkerer.block.MobilizerContent.register(bus);
        thaumic.tinkerer.integration.Ae2Movement.register(bus);
        thaumic.tinkerer.integration.MultipartCompatibility.register(bus);
        thaumic.tinkerer.kami.TinkererPlayerLifecycle.register(bus);
        if (thaumic.tinkerer.block.GolemConnectorContent.enabled()) { thaumic.tinkerer.block.GolemConnectorContent.register(bus); thaumic.tinkerer.block.AspectAnalyzerContent.register(bus); }
        ITEMS.register(bus);
        RECIPES.register(bus);
        thaumic.tinkerer.crafting.ShareTomeCondition.register(bus);
        thaumic.tinkerer.kami.KamiEnabledCondition.register(bus);
        thaumic.tinkerer.kami.bedrock.BedrockContent.register(bus);
        thaumic.tinkerer.crafting.LegacyDisplayCountIngredient.register(bus);
        thaumic.tinkerer.crafting.LegacyPotionIngredient.register(bus);
        TABS.register(bus);
        thaumic.tinkerer.compat.TinkererResearchImc.register(bus);
        thaumcraft.api.addon.AddonRegistrationEvent.BUS.addListener(TinkererResearch::register);
        thaumcraft.api.addon.AddonRegistrationEvent.BUS.addListener(thaumic.tinkerer.item.FocusContent::registerAddon);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SpellClothRecipe::crafted);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(thaumic.tinkerer.command.TinkererCommands::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(thaumic.tinkerer.crop.InfusedBonemeal::onBonemeal);
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
