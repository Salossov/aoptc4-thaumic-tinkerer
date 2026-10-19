package thaumic.tinkerer.kami.warpgate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumic.tinkerer.util.ItemData;

/**
 * Modern 1.21.1 port of ItemSkyPearl from Thaumic Tinkerer 2.5-1.7.10-164.
 * Attunes to a Celestial Gateway (Warp Gate). Shift-right click in air resets attunement.
 */
public class SkyPearlItem extends Item {
    public static final String TAG_ATTUNED = "attuned";
    public static final String TAG_X = "x";
    public static final String TAG_Y = "y";
    public static final String TAG_Z = "z";
    public static final String TAG_DIM = "dim";

    public SkyPearlItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean isAttuned(ItemStack stack) {
        if(stack.isEmpty())return false;
        var tag=ItemData.read(stack);
        boolean bound=tag.contains(TAG_ATTUNED,Tag.TAG_BYTE)?tag.getBoolean(TAG_ATTUNED):tag.contains(TAG_Y,Tag.TAG_INT)&&tag.getInt(TAG_Y)!=-1;
        return bound&&tag.contains(TAG_X,Tag.TAG_INT)&&tag.contains(TAG_Y,Tag.TAG_INT)&&tag.contains(TAG_Z,Tag.TAG_INT)&&dimension(tag)!=null;
    }
    private static ResourceKey<Level> dimension(CompoundTag tag){
        if(tag.contains(TAG_DIM,Tag.TAG_STRING)){
            var id=ResourceLocation.tryParse(tag.getString(TAG_DIM));return id==null?null:ResourceKey.create(Registries.DIMENSION,id);
        }
        if(tag.contains(TAG_DIM,Tag.TAG_INT))return switch(tag.getInt(TAG_DIM)){case 0->Level.OVERWORLD;case -1->Level.NETHER;case 1->Level.END;default->null;};
        return null;
    }
    public static BlockPos getTargetPos(ItemStack stack){
        if(!isAttuned(stack))return null;var tag=ItemData.read(stack);
        return new BlockPos(tag.getInt(TAG_X),tag.getInt(TAG_Y),tag.getInt(TAG_Z));
    }
    public static ResourceKey<Level> getTargetDim(ItemStack stack){return isAttuned(stack)?dimension(ItemData.read(stack)):null;}

    public static void setValues(ItemStack stack, BlockPos pos, ResourceKey<Level> dim) {
        ItemData.update(stack, tag -> {
            tag.putBoolean(TAG_ATTUNED, true);
            tag.putInt(TAG_X, pos.getX());
            tag.putInt(TAG_Y, pos.getY());
            tag.putInt(TAG_Z, pos.getZ());
            tag.putString(TAG_DIM, dim.location().toString());
        });
    }

    public static void clearValues(ItemStack stack) {
        ItemData.update(stack, tag -> {
            tag.putBoolean(TAG_ATTUNED, false);
            tag.remove(TAG_X);
            tag.remove(TAG_Y);
            tag.remove(TAG_Z);
            tag.remove(TAG_DIM);
        });
    }

    public static int getX(ItemStack stack) {
        BlockPos pos = getTargetPos(stack);
        return pos != null ? pos.getX() : 0;
    }

    public static int getY(ItemStack stack) {
        BlockPos pos = getTargetPos(stack);
        return pos != null ? pos.getY() : 0;
    }

    public static int getZ(ItemStack stack) {
        BlockPos pos = getTargetPos(stack);
        return pos != null ? pos.getZ() : 0;
    }

    public static void addInfo(ItemStack stack, ResourceKey<Level> currentDim, Vec3 currentPos, List<Component> list, boolean simpleMode) {
        if (isAttuned(stack)) {
            BlockPos targetPos = getTargetPos(stack);
            ResourceKey<Level> targetDim = getTargetDim(stack);
            if (targetPos == null) return;

            int x = targetPos.getX();
            int y = targetPos.getY();
            int z = targetPos.getZ();

            list.add(Component.literal("X: " + x));
            if (!simpleMode) {
                list.add(Component.literal("Y: " + y));
            }
            list.add(Component.literal("Z: " + z));

            if (targetDim != null && !targetDim.equals(currentDim)) {
                if (!simpleMode) {
                    list.add(Component.translatable("ttmisc.differentDim").withStyle(ChatFormatting.RED));
                }
            } else {
                double dx = x - currentPos.x;
                double dy = simpleMode ? 0.0 : (y - currentPos.y);
                double dz = z - currentPos.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                String distStr = new BigDecimal(dist).setScale(2, RoundingMode.UP).toString();
                list.add(Component.translatable("ttmisc.distance").append(": " + distStr + "m").withStyle(ChatFormatting.BLUE));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(pos).is(WarpGateContent.WARP_GATE.get()) && !isAttuned(stack)) {
            setValues(stack,pos,level.dimension());
            level.playSound(player, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.1F);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isAttuned(stack)) {
            clearValues(stack);
            level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, 0.1F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAttuned(stack);
    }
}
