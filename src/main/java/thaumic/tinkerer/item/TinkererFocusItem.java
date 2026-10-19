package thaumic.tinkerer.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.entities.PlayerMotionApi;
import thaumcraft.api.wands.FocusBehavior;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.util.LegacyMotion;

public final class TinkererFocusItem extends ItemFocusBasic implements FocusBehavior {
    public enum Kind { SMELT, FLIGHT, TELEKINESIS, HEAL, DEFLECT, ENDER_CHEST }
    private final Kind kind;
    // Release 164 keys this by name and compares only coordinates. Releasing the wand,
    // missing a target or temporarily lacking vis does not erase stored progress.
    private static final Map<String, SmeltData> SMELTING = new HashMap<>();
    private static final Map<String, Integer> HEALING = new HashMap<>();
    public static void playerLogin(String name) {HEALING.put(name,0);}
    public static void playerLogout(String name) {SMELTING.remove(name);HEALING.remove(name);}
    private record SmeltData(BlockPos pos, int remaining) { }
    public TinkererFocusItem(Kind kind) { super(new Properties().durability(1).rarity(Rarity.RARE)); this.kind = kind; }
    public Kind kind() { return kind; }
    @Override public String getSortingHelper(ItemStack stack){return switch(kind){case SMELT->"SMELT";case FLIGHT->"FLIGHT";case TELEKINESIS->"TELEKINESIS";case HEAL->"HEAL";case DEFLECT->"DEFLECT";case ENDER_CHEST->{
        if(!net.neoforged.fml.ModList.get().isLoaded("enderstorage"))yield "ENDERCHEST";
        var data=thaumic.tinkerer.util.ItemData.read(stack);
        yield data.getBoolean("ender")?"ENDERCHEST"+data.getInt("freq")+data.getString("owner"):"ENDERCHEST-VANILLA";
    }};}
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if(kind==Kind.ENDER_CHEST&&net.neoforged.fml.ModList.get().isLoaded("enderstorage")&&stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)){
            String owner=thaumic.tinkerer.util.ItemData.read(stack).getString("owner");
            if(!owner.equals("global"))lines.add(net.minecraft.network.chat.Component.literal(owner));
        }
    }
    @Override public boolean isRepairable(ItemStack stack) { return false; }
    @Override public int getEnchantmentValue() { return 5; }
    @Override public Map<Aspect, Integer> getVisCost(ItemStack stack) {
        return switch (kind) {
            case SMELT -> Map.of(Aspects.FIRE, 45, Aspects.ENTROPY, 12);
            case FLIGHT -> Map.of(Aspects.AIR, 15);
            case TELEKINESIS -> Map.of(Aspects.AIR, 5, Aspects.ENTROPY, 5);
            case HEAL -> Map.of(Aspects.EARTH, 45, Aspects.WATER, 45);
            case DEFLECT -> Map.of(Aspects.ORDER, 8, Aspects.AIR, 4);
            case ENDER_CHEST -> Map.of(Aspects.ENTROPY, 100, Aspects.ORDER, 100);
        };
    }
    @Override public int getFocusColor(ItemStack stack) { return switch (kind) { case SMELT -> 16711680; case FLIGHT -> 10416895; case TELEKINESIS -> 10223806; case HEAL -> 16580822; case DEFLECT -> 16777215; case ENDER_CHEST -> 1253923; }; }
    @Override public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack, int rank) {
        return switch (kind) { case TELEKINESIS -> List.of(); case DEFLECT, ENDER_CHEST -> List.of(FocusUpgradeType.treasure); default -> List.of(FocusUpgradeType.treasure, FocusUpgradeType.potency); };
    }
    @Override public boolean isVisCostPerTick(ItemStack stack) { return kind == Kind.TELEKINESIS || kind == Kind.DEFLECT; }
    @Override public boolean continuous(ItemStack focus) { return kind != Kind.FLIGHT && kind != Kind.ENDER_CHEST; }
    @Override public boolean managesVis() { return true; }
    @Override public boolean begin(ServerPlayer player, ItemStack wand, ItemStack focus, InteractionHand hand) {
        return kind != Kind.FLIGHT || TinkererConfig.FLIGHT.get();
    }
    @Override public boolean cast(ServerPlayer player, ItemStack wand, ItemStack focus) {
        return switch (kind) { case SMELT -> smelt(player, wand, focus); case FLIGHT -> fly(player, wand, focus); case TELEKINESIS -> pull(player, wand, focus); case HEAL -> heal(player, wand, focus); case DEFLECT -> deflect(player, wand, focus); case ENDER_CHEST -> chest(player, wand, focus); };
    }
    @Override public void end(ServerPlayer player, ItemStack wand, ItemStack focus) { if (kind == Kind.HEAL) HEALING.put(player.getGameProfile().getName(), 0); }
    private boolean heal(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if (!WandApi.consume(wand, player, getVisCost(focus), false) || player.getHealth() <= 0 || player.getHealth() >= player.getMaxHealth()) return false;
        String name = player.getGameProfile().getName();
        int progress = HEALING.getOrDefault(name, 0) + 1;
        HEALING.put(name, progress);
        ParticleApi.sparkle(player.level(), (float) player.getX() + player.level().random.nextFloat() - .5f,
                (float) player.getY() + player.level().random.nextFloat(), (float) player.getZ() + player.level().random.nextFloat() - .5f, 0);
        if (progress < 30 - getUpgradeLevel(wand, FocusUpgradeType.potency) * 10 / 3) return false;
        HEALING.put(name, 0);
        WandApi.consume(wand, player, getVisCost(focus), true); player.heal(1);
        player.level().playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:wand")), SoundSource.PLAYERS, .5f, 1);
        return true;
    }
    private boolean deflect(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if (!WandApi.consume(wand, player, getVisCost(focus), true)) return false;
        protectFromProjectiles(player);
        return true;
    }
    /** Shared by the original deflection focus and both chestplate tick callbacks. */
    public static void protectFromProjectiles(Player player) {
        var bounds = new AABB(player.getX() - 4, player.getY() - 4, player.getZ() - 4, player.getX() + 3, player.getY() + 3, player.getZ() + 3);
        for (var entity : player.level().getEntities(player, bounds, thaumic.tinkerer.api.ProjectileHooks::isProjectile)) {
            if (thaumic.tinkerer.api.ProjectileHooks.blacklisted(entity) || thaumic.tinkerer.api.ProjectileHooks.owner(entity) == player) continue;
            var motion = entity.getDeltaMovement(); double length = motion.length();
            if (length != 0) motion = motion.scale(1 / length);
            motion = motion.scale(entity.position().distanceTo(player.position()) * 2);
            for (int i = 0; i < 6; i++) ParticleApi.sparkle(player.level(), (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 6);
            // The original changes position only, leaving trajectory, owner and speed intact.
            entity.setPosRaw(entity.getX() + motion.x, entity.getY() + motion.y, entity.getZ() + motion.z);
        }
    }
    private boolean chest(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if(net.neoforged.fml.ModList.get().isLoaded("enderstorage")){
            if(thaumic.tinkerer.integration.EnderStorageCompatibility.bind(player,wand,focus))return true;
            if(thaumic.tinkerer.util.ItemData.flag(focus,"ender"))return thaumic.tinkerer.integration.EnderStorageCompatibility.open(player,wand,focus,getVisCost(focus));
        }
        if (!WandApi.consume(wand, player, getVisCost(focus), true)) return false;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, owner) ->
                net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory, owner.getEnderChestInventory()),
                net.minecraft.network.chat.Component.translatable("container.enderchest")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
        return true;
    }
    private boolean fly(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if (!TinkererConfig.FLIGHT.get() || !WandApi.consume(wand, player, getVisCost(focus), true)) return false;
        // Original passes the WAND to getUpgradeLevel, not its attached focus. Keep that quirk.
        double force = 2d / 3 * (1 + getUpgradeLevel(wand, FocusUpgradeType.potency) * .2);
        PlayerMotionApi.launch(player, player.getLookAngle().scale(force));
        for(int count=0;count<5;count++)ParticleApi.broadcastSmokeSpiral(player.serverLevel(),new ParticleApi.SmokeSpiral(
                player.getX(),player.getY()-player.getDeltaMovement().y,player.getZ(),2f,(int)(Math.random()*360),(int)player.getY(),647935));
        player.level().playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:wind")), SoundSource.PLAYERS, .4f, 1);
        return true;
    }
    private boolean pull(ServerPlayer player, ItemStack wand, ItemStack focus) {
        Vec3 target = player.position().add(0, player.getBbHeight() / 2d, 0);
        if (!player.isShiftKeyDown()) target = target.add(player.getLookAngle().scale(5));
        target = target.add(0, .5, 0);
        var entities = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(target.subtract(6, 6, 6), target.add(6, 6, 6)));
        if (entities.isEmpty() || !WandApi.consume(wand, player, getVisCost(focus), true)) return false;
        for (var entity : entities) {
            LegacyMotion.towards(entity, target, .3333f);
            ParticleApi.sparkle(player.level(), (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 0);
        }
        return true;
    }
    private boolean smelt(ServerPlayer player, ItemStack wand, ItemStack focus) {
        if (!WandApi.consume(wand, player, getVisCost(focus), false)) return false;
        // Original BlockUtils ray starts at Y+1.62 and traces ten blocks, ignoring fluids.
        Vec3 from = player.position().add(0, 1.62, 0);
        var hit = player.level().clip(new ClipContext(from, from.add(player.getLookAngle().scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        var pos = hit.getBlockPos(); var state = player.level().getBlockState(pos);
        var source = new ItemStack(state.getBlock());
        var recipe = player.level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(source), player.level());
        if (recipe.isEmpty()) return false;
        var output = recipe.get().value().assemble(new SingleRecipeInput(source), player.registryAccess());
        if (!(output.getItem() instanceof BlockItem blockItem)) return false;
        String name = player.getGameProfile().getName();
        var previous = SMELTING.get(name);
        boolean decremented = previous != null && previous.pos().equals(pos);
        if (decremented) {
            int remaining = previous.remaining() - 1;
            if (remaining <= 0) {
                player.level().setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
                WandApi.consume(wand, player, getVisCost(focus), true);
                SMELTING.remove(name); decremented = false;
                player.level().playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, .6f, 1);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1, 1);
                wisps(player, pos, 25);
            } else {
                SMELTING.put(name, new SmeltData(pos.immutable(), remaining));
                if (player.getUseItemRemainingTicks() % 3 == 0) player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, .2f, 1);
                wisps(player, pos, 2);
            }
        }
        if (!decremented) SMELTING.put(name, new SmeltData(pos.immutable(), 20 - Math.min(3, getUpgradeLevel(wand, FocusUpgradeType.potency)) * 5));
        // Original red beam runs on the client every use tick; client callback remains to be ported.
        return !decremented && previous != null && previous.pos().equals(pos);
    }
    private static void wisps(ServerPlayer player, BlockPos pos, int count) {
        for (int i = 0; i < count; i++) ParticleApi.wisp(player.level(), pos.getX() + Math.random(), pos.getY() + Math.random(),
                pos.getZ() + Math.random(), (float) Math.random() / 2, 4, true, true, (float) -Math.random() / 10);
    }
}
