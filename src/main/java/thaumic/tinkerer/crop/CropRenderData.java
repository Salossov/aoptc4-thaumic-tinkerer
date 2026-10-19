package thaumic.tinkerer.crop;

import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Immutable chunk-compilation snapshot; no client renderer or live tile reference. */
public record CropRenderData(int texture,int stage,int color) {
    public static final ModelProperty<CropRenderData> PROPERTY=new ModelProperty<>();
    public static final CropRenderData EMPTY=new CropRenderData(6,0,0xffffff);
}
