package thaumic.tinkerer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared camouflage persistence; modern state properties replace the original metadata. */
public class CamoBlockEntity extends BlockEntity implements thaumcraft.api.world.IMovableBlockEntity {
    private BlockState camouflage = Blocks.AIR.defaultBlockState();
    protected CamoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    public BlockState camouflage() { return camouflage; }
    @Override public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return net.neoforged.neoforge.client.model.data.ModelData.builder().with(CamoModelData.CAMOUFLAGE, camouflage).build();
    }
    public void camouflage(BlockState state) {
        camouflage = state; setChanged(); requestModelDataUpdate();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.put("camoState", NbtUtils.writeBlockState(camouflage));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        camouflage = tag.contains("camoState", 10) ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("camoState")) : Blocks.AIR.defaultBlockState();
        requestModelDataUpdate();
        if (level != null && level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
