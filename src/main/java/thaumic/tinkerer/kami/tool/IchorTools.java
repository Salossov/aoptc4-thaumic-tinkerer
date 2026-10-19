package thaumic.tinkerer.kami.tool;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumic.tinkerer.ThaumicTinkerer;

/** Release tool values, with the agreed modern shield, cooldown and sweep. */
public final class IchorTools {
    public static final TagKey<Block> LEGACY_BLOCKS=tag("legacy_blocks");
    private static TagKey<Block> tag(String key) {return TagKey.create(Registries.BLOCK,ThaumicTinkerer.id("ichor_tools/"+key));}
    public static final Tier TIER=new Tier() {
        public int getUses(){return -1;}
        public float getSpeed(){return 10;}
        public float getAttackDamageBonus(){return 5;}
        public int getEnchantmentValue(){return 25;}
        public TagKey<Block> getIncorrectBlocksForDrops(){return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;}
        public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}
        @Override public Tool createToolProperties(TagKey<Block> blocks) {
            String kind=blocks.equals(BlockTags.MINEABLE_WITH_PICKAXE)?"pickaxe":blocks.equals(BlockTags.MINEABLE_WITH_AXE)?"axe":"shovel";
            return new Tool(List.of(Tool.Rule.deniesDrops(getIncorrectBlocksForDrops()),
                    Tool.Rule.overrideSpeed(tag(kind+"_efficient"),10),Tool.Rule.overrideSpeed(LEGACY_BLOCKS,1),
                    Tool.Rule.minesAndDrops(blocks,10)),1,1);
        }
    };
    private static Item.Properties properties(float damage,float speed) {
        return new Item.Properties().rarity(Rarity.EPIC).attributes(DiggerItem.createAttributes(TIER,damage,speed));
    }
    public static class Pick extends PickaxeItem {
        public Pick(){super(TIER,properties(2,-2.8f));}
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    }
    public static class Axe extends AxeItem {
        public Axe(){super(TIER,properties(3,-3));}
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
        @Override public InteractionResult useOn(UseOnContext context){return InteractionResult.PASS;}
        @Override public boolean canPerformAction(ItemStack stack,ItemAbility ability){return ability==ItemAbilities.AXE_DIG;}
    }
    public static class Shovel extends ShovelItem {
        public Shovel(){super(TIER,properties(1,-3));}
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
        @Override public InteractionResult useOn(UseOnContext context){return InteractionResult.PASS;}
        @Override public boolean canPerformAction(ItemStack stack,ItemAbility ability){return ability==ItemAbilities.SHOVEL_DIG;}
    }
    public static class Sword extends SwordItem {
        public Sword(){super(TIER,properties(4,-2.4f),new Tool(List.of(
                Tool.Rule.minesAndDrops(List.of(net.minecraft.world.level.block.Blocks.COBWEB),15),
                Tool.Rule.overrideSpeed(tag("sword_efficient"),1.5f),Tool.Rule.overrideSpeed(LEGACY_BLOCKS,1),
                Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT,1.5f)),1,2));}
        @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
        @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BLOCK;}
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 72000;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        @Override public boolean canPerformAction(ItemStack stack,ItemAbility ability) {
            return ability==ItemAbilities.SHIELD_BLOCK||super.canPerformAction(stack,ability);
        }
    }
    public static final DeferredItem<Pick> PICK=ThaumicTinkerer.ITEMS.register("ichor_pick",Pick::new);
    public static final DeferredItem<Shovel> SHOVEL=ThaumicTinkerer.ITEMS.register("ichor_shovel",Shovel::new);
    public static final DeferredItem<Axe> AXE=ThaumicTinkerer.ITEMS.register("ichor_axe",Axe::new);
    public static final DeferredItem<Sword> SWORD=ThaumicTinkerer.ITEMS.register("ichor_sword",Sword::new);
    public static List<DeferredItem<? extends TieredItem>> sourceOrder(){return List.of(PICK,SHOVEL,AXE,SWORD);}
    public static void register(IEventBus bus){bus.addListener(IchorTools::components);}
    private static void components(ModifyDefaultComponentsEvent event) {
        // TieredItem adds durability even for uses=-1. Remove both defaults before
        // stacks/codecs are exposed, retaining the original non-damageable item.
        for(var item:sourceOrder())event.modify(item.get(),patch->patch.remove(DataComponents.MAX_DAMAGE).remove(DataComponents.DAMAGE));
    }
    public static void display(CreativeModeTab.Output output){for(var item:sourceOrder())output.accept(item.get());}
    private IchorTools(){}
}
