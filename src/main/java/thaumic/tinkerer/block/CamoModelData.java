package thaumic.tinkerer.block;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Model-data types are common NeoForge contracts; client rendering lives separately. */
public final class CamoModelData {
    public static final ModelProperty<BlockState> CAMOUFLAGE = new ModelProperty<>();
    public static final ModelProperty<Integer> ORIENTATION = new ModelProperty<>();
    private CamoModelData() { }
}
