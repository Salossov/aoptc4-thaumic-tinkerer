package thaumic.tinkerer.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Retain legacy keys while using component replacement for inventory/Curios synchronization. */
public final class ItemData {
    public static CompoundTag read(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static void update(ItemStack stack, java.util.function.Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, change);
    }
    public static int integer(ItemStack stack, String key) { return read(stack).getInt(key); }
    public static boolean flag(ItemStack stack, String key) { return read(stack).getBoolean(key); }
    private ItemData() {}
}
