package thaumic.tinkerer.crop;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.FarmBlock;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.util.ItemData;

/** One item with the release mainAspect/aspectTendencies compounds. */
public final class InfusedSeedsItem extends Item {
    public InfusedSeedsItem(){super(new Properties().rarity(Rarity.RARE));}
    private static void ensureTag(ItemStack stack){if(!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA))stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag()));}
    public static Aspect aspect(ItemStack stack){ensureTag(stack);return CropAspectData.first(CropAspectData.read(ItemData.read(stack).getCompound("mainAspect")));}
    public static AspectList tendencies(ItemStack stack){ensureTag(stack);return CropAspectData.read(ItemData.read(stack).getCompound("aspectTendencies"));}
    public static void aspect(ItemStack stack,Aspect aspect){ItemData.update(stack,tag->tag.put("mainAspect",CropAspectData.write(new AspectList().add(aspect,1))));}
    public static void tendencies(ItemStack stack,AspectList values){ItemData.update(stack,tag->tag.put("aspectTendencies",CropAspectData.write(values)));}
    public static ItemStack of(Aspect aspect){var stack=new ItemStack(InfusedContent.SEEDS.get());aspect(stack,aspect);return stack;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        var aspect=aspect(stack);lines.add(aspect==null?Component.translatable("tooltip.thaumictinkerer.missing_crop_aspect"):Component.literal(aspect.getName()));
        var tendencies=tendencies(stack);for(var primal:tendencies.getAspects())if(primal!=null)lines.add(Component.literal(primal.getName()+": "+tendencies.getAmount(primal)));
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var pos=context.getClickedPos();var player=context.getPlayer();var level=context.getLevel();var stack=context.getItemInHand();
        if(aspect(stack)==null)return InteractionResult.PASS;
        if(context.getClickedFace()!=Direction.UP||!(level.getBlockState(pos).getBlock() instanceof FarmBlock)||player==null||!player.mayUseItemAt(pos,Direction.UP,stack)||!player.mayUseItemAt(pos.above(),Direction.UP,stack))return InteractionResult.PASS;
        if(!level.isClientSide()){
            level.setBlock(pos,InfusedContent.SOIL.get().defaultBlockState(),3);
            level.setBlock(pos.above(),InfusedContent.CROP.get().defaultBlockState(),3);
            var tile=(InfusedCropBlockEntity)level.getBlockEntity(pos.above());tile.aspect=aspect(stack);tile.primalTendencies=tendencies(stack);tile.reduceSaturatedAspects();tile.changed();
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
