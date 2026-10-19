package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import thaumic.tinkerer.item.SoulMouldItem;
import thaumic.tinkerer.menu.MobMagnetMenu;

public final class MobMagnetBlockEntity extends MagnetBlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    public final InvWrapper handler = new InvWrapper(this);
    private boolean adult = true;
    public MobMagnetBlockEntity(BlockPos pos, BlockState state) { super(MagnetContent.MOB_TILE.get(), pos, state); }
    public boolean searchesAdults() { return adult; }
    public void setSearchAdults(boolean value) {
        adult = value; setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public boolean accepts(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) return false;
        if (entity instanceof AgeableMob ageable && adult == ageable.isBaby()) return false;
        return items.getFirst().isEmpty() || BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(SoulMouldItem.getPatternName(items.getFirst()));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); adult = tag.getBoolean("adultCheck"); items.set(0, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items, registries);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putBoolean("adultCheck", adult); ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { var stack = ContainerHelper.removeItem(items, slot, count); if (!stack.isEmpty()) setChanged(); return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return true; }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }
    @Override public void clearContent() { items.set(0, ItemStack.EMPTY); setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumictinkerer.mob_magnet"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MobMagnetMenu(id, inventory, this); }
}
