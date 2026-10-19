package thaumic.tinkerer.kami.blocktalisman;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import thaumic.tinkerer.util.ItemData;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class BlockTalismanItem extends Item implements ICurioItem {

    public BlockTalismanItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static Block getBlock(ItemStack stack) {
        var tag = ItemData.read(stack);
        String id = tag.getString("block");
        if (id.isEmpty()) {
            id = tag.getString("blockName");
        }
        if (id.isEmpty()) {
            return Blocks.AIR;
        }
        var key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            return Blocks.AIR;
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    public static int getBlockCount(ItemStack stack) {
        var tag = ItemData.read(stack);
        if (tag.contains("count")) {
            return Math.max(0, tag.getInt("count"));
        }
        if (tag.contains("blockCount")) {
            return Math.max(0, tag.getInt("blockCount"));
        }
        return 0;
    }

    public static void setCount(ItemStack stack, int count) {
        ItemData.update(stack, tag -> tag.putInt("count", Math.max(0, count)));
    }

    public static int add(ItemStack stack, int count) {
        if (count <= 0) return 0;
        int current = getBlockCount(stack);
        long sum = (long) current + count;
        int capped = (int) Math.min(sum, (long) Integer.MAX_VALUE);
        setCount(stack, capped);
        return capped - current;
    }

    public static int remove(ItemStack stack, int count) {
        if (count <= 0) return 0;
        int current = getBlockCount(stack);
        int toRemove = Math.min(current, count);
        setCount(stack, current - toRemove);
        return toRemove;
    }

    public static boolean setBlock(ItemStack stack, Block block) {
        if (block == null || block == Blocks.AIR) return false;
        Block current = getBlock(stack);
        int currentCount = getBlockCount(stack);
        if (current != Blocks.AIR && currentCount > 0) {
            return false;
        }
        var key = BuiltInRegistries.BLOCK.getKey(block);
        ItemData.update(stack, tag -> tag.putString("block", key.toString()));
        return true;
    }

    public static boolean isEnabled(ItemStack stack) {
        var tag = ItemData.read(stack);
        if (tag.contains("active")) {
            return tag.getBoolean("active");
        }
        if (tag.contains("enabled")) {
            return tag.getBoolean("enabled");
        }
        return false;
    }

    public static void setEnabled(ItemStack stack, boolean active) {
        ItemData.update(stack, tag -> tag.putBoolean("active", active));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            Block bound = getBlock(stack);
            if (bound != Blocks.AIR) {
                if (!level.isClientSide()) {
                    boolean nextState = !isEnabled(stack);
                    setEnabled(stack, nextState);
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.1F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult preparation = TalismanActions.prepare(context);
        if (preparation != null) return preparation;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block clickedBlock = level.getBlockState(pos).getBlock();

        // 1. Try to set/bind block if empty or unassigned
        if (getBlock(stack) == Blocks.AIR || getBlockCount(stack) == 0) {
            if (setBlock(stack, clickedBlock)) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        Block boundBlock = getBlock(stack);
        if (boundBlock == Blocks.AIR) {
            return InteractionResult.PASS;
        }

        // 2. Check for container deposit
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, context.getClickedFace());
        if (itemHandler != null) {
            return depositIntoHandler(level, stack, boundBlock, itemHandler);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container container) {
            return depositIntoContainer(level, stack, boundBlock, container, context.getClickedFace());
        }

        // 3. Otherwise: place block in world
        return placeBlockInWorld(context, stack, boundBlock);
    }

    private InteractionResult depositIntoHandler(Level level, ItemStack talisman, Block boundBlock, IItemHandler handler) {
        int available = getBlockCount(talisman);
        if (available <= 0) return InteractionResult.PASS;

        Item blockItem = boundBlock.asItem();
        if (blockItem == Items.AIR) return InteractionResult.PASS;

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int totalInserted = 0;
        int remainingToInsert = available;

        for (int slot = 0; slot < handler.getSlots() && remainingToInsert > 0; slot++) {
            ItemStack existing = handler.getStackInSlot(slot);
            if (!existing.isEmpty() && !existing.is(blockItem)) continue;
            int maxInsertForSlot = (existing.isEmpty() ? blockItem.getDefaultMaxStackSize() : existing.getMaxStackSize()) - existing.getCount();
            if (maxInsertForSlot <= 0) continue;

            int batch = Math.min(remainingToInsert, maxInsertForSlot);
            // The source matches block/meta, retaining an occupied slot's other
            // data. Offer matching components to modern component-aware handlers.
            ItemStack insertStack = existing.isEmpty() ? new ItemStack(blockItem, batch) : existing.copyWithCount(batch);

            ItemStack remainder = handler.insertItem(slot, insertStack, true);
            int accepted = batch - remainder.getCount();
            if (accepted > 0) {
                ItemStack actualRemainder = handler.insertItem(slot, insertStack.copyWithCount(accepted), false);
                int actuallyInserted = accepted - actualRemainder.getCount();
                if (actuallyInserted > 0) {
                    totalInserted += actuallyInserted;
                    remainingToInsert -= actuallyInserted;
                }
            }
        }

        if (totalInserted > 0) {
            remove(talisman, totalInserted);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult depositIntoContainer(Level level, ItemStack talisman, Block boundBlock, Container container, Direction side) {
        int available = getBlockCount(talisman);
        if (available <= 0) return InteractionResult.PASS;

        Item blockItem = boundBlock.asItem();
        if (blockItem == Items.AIR) return InteractionResult.PASS;

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int totalInserted = 0;
        int remainingToInsert = available;

        WorldlyContainer sided = container instanceof WorldlyContainer wc ? wc : null;
        int[] slots = sided != null ? sided.getSlotsForFace(side) : null;
        int slotCount = slots != null ? slots.length : container.getContainerSize();

        for (int idx = 0; idx < slotCount && remainingToInsert > 0; idx++) {
            int slot = slots != null ? slots[idx] : idx;
            ItemStack existing = container.getItem(slot);

            if (!existing.isEmpty() && !existing.is(blockItem)) continue;
            int before = existing.getCount();
            ItemStack candidate = existing.isEmpty() ? new ItemStack(blockItem) : existing.copy();
            int space = Math.min(container.getMaxStackSize(), candidate.getMaxStackSize()) - before;
            int batch = Math.min(remainingToInsert, space);
            if (batch <= 0) continue;
            candidate.setCount(before + batch);
            // The source validates the offered stack for an empty slot and the
            // existing stack for an occupied one, including side restrictions.
            ItemStack validation = existing.isEmpty() ? candidate : existing;
            if (!container.canPlaceItem(slot, validation) || sided != null && !sided.canPlaceItemThroughFace(slot, validation, side)) continue;
            container.setItem(slot, candidate);
            ItemStack stored = container.getItem(slot);
            int inserted = stored.is(blockItem) ? Math.max(0, Math.min(batch, stored.getCount() - before)) : 0;
            totalInserted += inserted;
            remainingToInsert -= inserted;
            if (inserted > 0) container.setChanged();
        }

        if (totalInserted > 0) {
            remove(talisman, totalInserted);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult placeBlockInWorld(UseOnContext context, ItemStack talisman, Block boundBlock) {
        int available = getBlockCount(talisman);
        if (available <= 0) {
            return InteractionResult.PASS;
        }

        Item item = boundBlock.asItem();
        if (!(item instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        Player player = context.getPlayer();

        ItemStack tempStack = new ItemStack(blockItem, 1);
        BlockPlaceContext placeContext = new BlockPlaceContext(
            level,
            player,
            context.getHand(),
            tempStack,
            new net.minecraft.world.phys.BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
            )
        );

        if (!placeContext.canPlace()) {
            return InteractionResult.FAIL;
        }

        InteractionResult result = blockItem.useOn(placeContext);
        if (result.consumesAction()) {
            if (!level.isClientSide()) {
                remove(talisman, 1);
            }
            return result;
        }

        return result;
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity entity = context.entity();
        if (entity.level().isClientSide()) return;
        if (!isEnabled(stack)) return;
        Block block = getBlock(stack);
        if (block == Blocks.AIR) return;
        if (!(entity instanceof Player player)) return;

        Item blockItem = block.asItem();
        if (blockItem == Items.AIR) return;

        var inv = player.getInventory();
        int mainSize = inv.items.size();

        // 1. Identify protected building slot
        int protectedSlot = -1;
        if (player.getMainHandItem().is(blockItem)) {
            protectedSlot = inv.selected;
        } else if (player.getOffhandItem().is(blockItem)) {
            protectedSlot = 40;
        } else {
            int bestHotbar = -1;
            int maxHotbarCount = -1;
            for (int i = 0; i < 9 && i < mainSize; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(blockItem) && s.getCount() > maxHotbarCount) {
                    bestHotbar = i;
                    maxHotbarCount = s.getCount();
                }
            }
            if (bestHotbar != -1) {
                protectedSlot = bestHotbar;
            } else {
                int bestMain = -1;
                int maxMainCount = -1;
                for (int i = 9; i < mainSize; i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.is(blockItem) && s.getCount() > maxMainCount) {
                        bestMain = i;
                        maxMainCount = s.getCount();
                    }
                }
                if (bestMain != -1) {
                    protectedSlot = bestMain;
                }
            }
        }

        // 2. If NO matching stack exists in the inventory at all:
        if (protectedSlot == -1) {
            ItemStack carried = player.containerMenu != null ? player.containerMenu.getCarried() : ItemStack.EMPTY;
            boolean cursorOk = carried.isEmpty() || carried.is(blockItem);
            if (cursorOk && inv.getFreeSlot() != -1) {
                int toDispense = Math.min(blockItem.getDefaultMaxStackSize(), getBlockCount(stack));
                if (toDispense > 0) {
                    int removed = remove(stack, toDispense);
                    if (removed > 0) {
                        ItemStack dispensed = new ItemStack(blockItem, removed);
                        if (!inv.add(dispensed)) {
                            add(stack, dispensed.getCount());
                        }
                    }
                }
            }
            return;
        }

        // 3. Vacuum excess matching blocks from other inventory slots into talisman
        for (int i = 0; i < mainSize; i++) {
            if (i == protectedSlot) continue;
            ItemStack s = inv.getItem(i);
            if (s.is(blockItem)) {
                int count = s.getCount();
                int added = add(stack, count);
                if (added == count) {
                    inv.setItem(i, ItemStack.EMPTY);
                } else {
                    s.shrink(added);
                }
            }
        }

        if (protectedSlot != 40) {
            ItemStack off = player.getOffhandItem();
            if (off.is(blockItem)) {
                int count = off.getCount();
                int added = add(stack, count);
                if (added == count) {
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                } else {
                    off.shrink(added);
                }
            }
        }

        // 4. Replenish the protected slot up to maxStackSize
        ItemStack protectedStack = protectedSlot == 40 ? player.getOffhandItem() : inv.getItem(protectedSlot);
        if (!protectedStack.isEmpty() && protectedStack.is(blockItem)) {
            int max = protectedStack.getMaxStackSize();
            int missing = max - protectedStack.getCount();
            if (missing > 0 && getBlockCount(stack) > 0) {
                int toReplenish = Math.min(missing, getBlockCount(stack));
                int removed = remove(stack, toReplenish);
                protectedStack.grow(removed);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Block block = getBlock(stack);
        if (block != Blocks.AIR) {
            int count = getBlockCount(stack);
            lines.add(Component.translatable(block.getDescriptionId()).append(" (x" + count + ")"));
        }
        lines.add(Component.translatable(isEnabled(stack) ? "ttmisc.active" : "ttmisc.inactive"));
    }
}
