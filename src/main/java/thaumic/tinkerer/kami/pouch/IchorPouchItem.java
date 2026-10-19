package thaumic.tinkerer.kami.pouch;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.menu.IchorPouchMenu;
import thaumcraft.api.wands.IFocusContainer;
import thaumcraft.api.wands.ItemFocusBasic;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 117-slot KAMI Ichor Pouch (Bottomless Pouch).
 * Implements Curios belt equivalent and stores contents in standard DataComponents.CONTAINER.
 */
public final class IchorPouchItem extends Item implements ICurioItem,IFocusContainer {
    public static final int SLOTS = 117;

    public IchorPouchItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static List<ItemStack> getInventory(ItemStack stack) {
        var contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        var items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            items.set(i, i < contents.getSlots() ? contents.getStackInSlot(i).copy() : ItemStack.EMPTY);
        }
        return items;
    }

    public static void setInventory(ItemStack pouch, List<ItemStack> contents) {
        if (contents.size() != SLOTS) {
            throw new IllegalArgumentException("Invalid ichor pouch contents size: " + contents.size());
        }
        // Enforce insertion through slots, not while saving. Existing contents
        // injected by commands/data must remain retrievable without a crash.
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    public static boolean isPouch(ItemStack stack) {
        return !stack.isEmpty()&&stack.getItem() instanceof IFocusContainer;
    }

    @Override public List<ItemStack> getFocusInventory(ItemStack container){return getInventory(container);}
    @Override public boolean setFocus(ItemStack container,int slot,ItemStack replacement) {
        if(container.getItem()!=this||slot<0||slot>=SLOTS||!replacement.isEmpty()&&(!(replacement.getItem() instanceof ItemFocusBasic)||replacement.getCount()!=1))return false;
        var contents=getInventory(container);contents.set(slot,replacement.copy());setInventory(container,contents);return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new IchorPouchMenu(containerId, playerInventory, slot),
                stack.getHoverName()
            ), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
