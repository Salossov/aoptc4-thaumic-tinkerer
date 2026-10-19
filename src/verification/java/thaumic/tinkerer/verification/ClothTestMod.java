package thaumic.tinkerer.verification;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumic.tinkerer.api.INoRemoveEnchant;
import thaumic.tinkerer.ThaumicTinkerer;

/** Test-only content, not included in the main/source release JARs. */
@EventBusSubscriber(modid = ThaumicTinkerer.ID)
public final class ClothTestMod {
    public static final String ID = ThaumicTinkerer.ID;
    public static Item PROTECTED;
    public static Item ENCHANTMENT_PICKAXE, ENCHANTMENT_SWORD, ENCHANTMENT_BOW;
    public static Item TABLET_PROBE,TABLET_BREAK_PROBE;
    public static final class TabletBreakProbe extends Item implements thaumcraft.api.world.IBlockStartBreakTool {
        public static final class State {public int calls;public boolean defer,emptyHand;}
        public static final java.util.Map<net.minecraft.world.item.ItemStack,State> states=new java.util.IdentityHashMap<>();
        public TabletBreakProbe(){super(new Properties().durability(32));}
        @Override public boolean onBlockStartBreak(net.minecraft.world.item.ItemStack stack,net.minecraft.core.BlockPos pos,net.minecraft.server.level.ServerPlayer player){var state=states.computeIfAbsent(stack,key->new State());state.calls++;state.emptyHand=player.getMainHandItem().isEmpty();return state.defer;}
        @Override public boolean mineBlock(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.level.block.state.BlockState block,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.LivingEntity player){stack.hurtAndBreak(1,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);return true;}
    }
    public static final class TabletProbe extends Item {
        public static final java.util.List<String> calls=new java.util.ArrayList<>();
        public static boolean replaceHand;
        public TabletProbe(){super(new Properties());}
        @Override public net.minecraft.world.InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.context.UseOnContext context){calls.add("first");return net.minecraft.world.InteractionResult.SUCCESS;}
        @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){calls.add("useOn");return net.minecraft.world.InteractionResult.PASS;}
        @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){calls.add("use");if(replaceHand)player.setItemInHand(hand,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD));return net.minecraft.world.InteractionResultHolder.success(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));}
    }
    public static final class ProtectedItem extends Item implements INoRemoveEnchant, thaumcraft.api.IGoggles, thaumcraft.api.nodes.IRevealer {
        public ProtectedItem() { super(new Properties()); }
        public boolean showNodes(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity wearer) { return thaumic.tinkerer.util.ItemData.flag(stack, "nodes"); }
        public boolean showIngamePopups(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity wearer) { return thaumic.tinkerer.util.ItemData.flag(stack, "popups"); }
    }
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            thaumcraft.api.addon.AddonRegistrationEvent.BUS.addListener(ShareTomeRulesTest::fixture);
            PROTECTED = new ProtectedItem();
            helper.register(ThaumicTinkerer.id("test_protected"), PROTECTED);
            ENCHANTMENT_PICKAXE=new net.minecraft.world.item.PickaxeItem(net.minecraft.world.item.Tiers.IRON,new Item.Properties());
            ENCHANTMENT_SWORD=new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON,new Item.Properties());
            ENCHANTMENT_BOW=new net.minecraft.world.item.BowItem(new Item.Properties().durability(384));
            helper.register(ThaumicTinkerer.id("test_enchantment_pickaxe"),ENCHANTMENT_PICKAXE);
            helper.register(ThaumicTinkerer.id("test_enchantment_sword"),ENCHANTMENT_SWORD);
            helper.register(ThaumicTinkerer.id("test_enchantment_bow"),ENCHANTMENT_BOW);
            TABLET_PROBE=new TabletProbe();helper.register(ThaumicTinkerer.id("test_tablet_probe"),TABLET_PROBE);
            TABLET_BREAK_PROBE=new TabletBreakProbe();helper.register(ThaumicTinkerer.id("test_tablet_break_probe"),TABLET_BREAK_PROBE);
        });
    }
}
