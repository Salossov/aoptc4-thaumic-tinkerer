package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.addon.AddonRegistry;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.DarkQuartzContent;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DarkQuartzRulesTest {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static CraftingInput input(int width, int height, Object... values) {
        var stacks = new ArrayList<ItemStack>();
        for (Object value : values) {
            if (value == null) stacks.add(ItemStack.EMPTY);
            else if (value instanceof ItemStack stack) stacks.add(stack);
            else if (value instanceof net.minecraft.world.level.ItemLike item) stacks.add(new ItemStack(item));
            else throw new IllegalArgumentException(value.toString());
        }
        return CraftingInput.of(width, height, stacks);
    }
    private static CraftingRecipe recipe(GameTestHelper helper, String id) {
        return (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(ThaumicTinkerer.id(id)).orElseThrow().value();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void darkQuartz(GameTestHelper helper) {
        VerificationReports.install();
        checks = 0;
        var level = helper.getLevel();
        var lookup = level.registryAccess();
        var coal = recipe(helper, "dark_quartz0_0");
        var charcoal = recipe(helper, "dark_quartz0_1");
        var cornerCoal = input(3, 3, Items.QUARTZ, null, Items.QUARTZ, null, Items.COAL, null, Items.QUARTZ, null, Items.QUARTZ);
        var cornerCharcoal = input(3, 3, Items.QUARTZ, null, Items.QUARTZ, null, Items.CHARCOAL, null, Items.QUARTZ, null, Items.QUARTZ);
        check(coal.matches(cornerCoal, level) && !coal.matches(cornerCharcoal, level), "coal recipe keeps legacy metadata 0");
        check(charcoal.matches(cornerCharcoal, level) && !charcoal.matches(cornerCoal, level), "charcoal alternative keeps metadata 1");
        var raw = coal.assemble(cornerCoal, lookup);
        check(raw.is(DarkQuartzContent.QUARTZ.get()) && raw.getCount() == 4, "four quartz corners and coal produce four Smokey Quartz");
        var blockRecipe = recipe(helper, "dark_quartz1");
        var blockInput = input(2, 2, DarkQuartzContent.QUARTZ.get(), DarkQuartzContent.QUARTZ.get(), DarkQuartzContent.QUARTZ.get(), DarkQuartzContent.QUARTZ.get());
        check(blockRecipe.matches(blockInput, level) && blockRecipe.assemble(blockInput, lookup).is(DarkQuartzContent.BLOCK.get().asItem()), "four raw quartz make one block");
        var slabInput = input(3, 1, DarkQuartzContent.BLOCK.get(), DarkQuartzContent.BLOCK.get(), DarkQuartzContent.BLOCK.get());
        var slabRecipe = recipe(helper, "dark_quartz2");
        check(slabRecipe.matches(slabInput, level) && slabRecipe.assemble(slabInput, lookup).getCount() == 6, "three blocks make six slabs");
        var pillarInput = input(1, 2, DarkQuartzContent.BLOCK.get(), DarkQuartzContent.BLOCK.get());
        var pillarRecipe = recipe(helper, "dark_quartz3");
        check(pillarRecipe.matches(pillarInput, level) && pillarRecipe.assemble(pillarInput, lookup).is(DarkQuartzContent.PILLAR.get().asItem())
                && pillarRecipe.assemble(pillarInput, lookup).getCount() == 2, "two vertical blocks make two pillars");
        var chiseledInput = input(1, 2, DarkQuartzContent.SLAB.get(), DarkQuartzContent.SLAB.get());
        check(recipe(helper, "dark_quartz4").matches(chiseledInput, level)
                && recipe(helper, "dark_quartz4").assemble(chiseledInput, lookup).is(DarkQuartzContent.CHISELED.get().asItem()), "two vertical slabs make chiseled block");
        var b = DarkQuartzContent.BLOCK.get();
        var stairInput = input(3, 3, null, null, b, null, b, b, b, b, b);
        var stairMirror = input(3, 3, b, null, null, b, b, null, b, b, b);
        for (String id : List.of("dark_quartz5", "dark_quartz_stairs_mirrored")) {
            var stairsRecipe = recipe(helper, id);
            check(stairsRecipe.matches(stairInput, level) && stairsRecipe.matches(stairMirror, level), "legacy stair layouts remain mirrorable: " + id);
            check(stairsRecipe.assemble(stairInput, lookup).getCount() == 4, "six blocks produce four stairs: " + id);
        }
        check(!slabRecipe.matches(input(3, 1, DarkQuartzContent.CHISELED.get(), b, b), level), "metadata variants do not replace the plain block ingredient");

        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (var block : List.of(b, DarkQuartzContent.CHISELED.get(), DarkQuartzContent.PILLAR.get(), DarkQuartzContent.STAIRS.get(), DarkQuartzContent.SLAB.get(), DarkQuartzContent.FULL_SLAB.get())) {
            check(block.defaultBlockState().getDestroySpeed(level, pos) == .8f, "legacy hardness: " + block);
            check(block.getExplosionResistance() == 6f, "legacy effective blast resistance: " + block);
            check(block.defaultBlockState().getMapColor(level, pos) == MapColor.STONE, "original Material.rock map color: " + block);
            check(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(block.defaultBlockState()), "wooden pick is sufficient: " + block);
        }
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var pillar = DarkQuartzContent.PILLAR.get();
        for (var face : List.of(Direction.UP, Direction.EAST, Direction.NORTH)) {
            var held = new ItemStack(pillar);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held,
                    new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
            check(pillar.getStateForPlacement(context).getValue(RotatedPillarBlock.AXIS) == face.getAxis(), "pillar placed on clicked face axis " + face);
            var state = pillar.defaultBlockState().setValue(RotatedPillarBlock.AXIS, face.getAxis());
            level.setBlockAndUpdate(pos, state);
            var drops = Block.getDrops(state, level, pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE));
            check(drops.size() == 1 && drops.getFirst().is(pillar.asItem()) && drops.getFirst().getCount() == 1, "oriented pillar drops neutral pillar item " + face);
        }
        var slab = DarkQuartzContent.SLAB.get();
        level.setBlockAndUpdate(pos, slab.defaultBlockState());
        var held = new ItemStack(slab, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var combineContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, held,
                new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
        check(((BlockItem) slab.asItem()).place(combineContext).consumesAction(), "second slab placement succeeds");
        var doubled = level.getBlockState(pos);
        check(doubled.is(slab) && doubled.getValue(SlabBlock.TYPE) == SlabType.DOUBLE && held.getCount() == 1, "actual block placement combines halves and consumes one");
        var drops = Block.getDrops(doubled, level, pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE));
        check(drops.size() == 1 && drops.getFirst().is(slab.asItem()) && drops.getFirst().getCount() == 2, "double slab drops two half slabs");
        var full = DarkQuartzContent.FULL_SLAB.get();
        level.setBlockAndUpdate(pos, full.defaultBlockState());
        check(full.defaultBlockState().getValue(SlabBlock.TYPE) == SlabType.DOUBLE, "hidden legacy full-slab registration is retained");
        check(full.getCloneItemStack(level, pos, full.defaultBlockState()).is(slab.asItem()), "legacy full-slab pick returns a half-slab item");
        check(Block.getDrops(full.defaultBlockState(), level, pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE)).getFirst().getCount() == 2,
                "hidden legacy double slab drops two");
        check(!slab.placeLiquid(level, pos, slab.defaultBlockState(), Fluids.WATER.defaultFluidState()), "slabs do not gain modern waterlogging");
        check(!DarkQuartzContent.STAIRS.get().placeLiquid(level, pos, DarkQuartzContent.STAIRS.get().defaultBlockState(), Fluids.WATER.defaultFluidState()), "stairs do not gain modern waterlogging");
        for (var item : List.of(slab.asItem(), full.asItem())) {
            held = new ItemStack(item, 3); player.setItemInHand(InteractionHand.MAIN_HAND, held);
            level.setBlockAndUpdate(pos, slab.defaultBlockState());
            var use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
            check(item.useOn(use).consumesAction() && level.getBlockState(pos).is(full) && held.getCount() == 2,
                    "both legacy item registrations combine a clicked bottom half: " + item);
            level.setBlockAndUpdate(pos, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, -.5, 0), Direction.DOWN, pos, false));
            check(item.useOn(use).consumesAction() && level.getBlockState(pos).is(full) && held.getCount() == 1,
                    "both legacy item registrations combine a clicked top half: " + item);
            level.setBlockAndUpdate(pos.west(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos.west()).add(.5, -.25, 0), Direction.EAST, pos.west(), false));
            check(item.useOn(use).consumesAction() && level.getBlockState(pos).is(full) && held.isEmpty(),
                    "adjacent half merges regardless of hit height or half orientation: " + item);
        }
        level.removeBlock(pos.west(), false);
        level.setBlockAndUpdate(pos, slab.defaultBlockState());
        var obstacle = net.minecraft.world.entity.EntityType.PIG.create(level);
        check(obstacle != null, "collision fixture can be created");
        obstacle.setNoAi(true); obstacle.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5); level.addFreshEntity(obstacle);
        try {
            held = new ItemStack(full.asItem()); player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
            check(full.asItem().useOn(use).consumesAction() && level.getBlockState(pos).is(slab)
                    && level.getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.BOTTOM && held.getCount() == 1,
                    "blocked legacy merge is handled without consuming the item or replacing the half");
        } finally { obstacle.discard(); }
        player.getAbilities().instabuild = true;
        try {
            var use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos).add(0, .5, 0), Direction.UP, pos, false));
            check(full.asItem().useOn(use).consumesAction() && level.getBlockState(pos).is(full) && held.getCount() == 1,
                    "creative legacy merge keeps the full-slab item");
        } finally { player.getAbilities().instabuild = false; }
        level.removeBlock(pos, false); level.setBlockAndUpdate(pos.west(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        held = new ItemStack(full.asItem(), 2); player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var placeFull = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.west()).add(.5, 0, 0), Direction.EAST, pos.west(), false));
        check(full.asItem().useOn(placeFull).consumesAction() && level.getBlockState(pos).is(full)
                && level.getBlockState(pos).getValue(SlabBlock.TYPE) == SlabType.DOUBLE && held.getCount() == 1,
                "hidden full-slab item can also place its own full block normally");
        level.removeBlock(pos.west(), false);
        var research = AddonRegistry.research().get("thaumictinkerer:dark_quartz");
        check(research != null && research.autoUnlock() && research.round() && research.stub(), "original auto/round/stub research flags");
        check(research.complexity() == 0 && research.aspects().isEmpty() && research.column() == -2 && research.row() == 2, "original free research coordinates");
        check(research.recipePages().size() == 6 && research.recipePages().getFirst().equals("thaumictinkerer:dark_quartz0_1"), "book preserves last-write charcoal recipe choice");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        level.removeBlock(pos, false);
        System.out.println("TT_DARK_QUARTZ_PASS checks=" + checks);
        helper.succeed();
    }
}
