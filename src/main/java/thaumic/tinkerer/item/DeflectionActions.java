package thaumic.tinkerer.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.api.client.ParticleApi;
import thaumcraft.api.wands.WandApi;
import thaumcraft.api.world.ChunkPreparationApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.ProjectileHooks;
import thaumic.tinkerer.block.AnimationTabletBlockEntity;
import thaumic.tinkerer.block.TabletPlayer;
import thaumic.tinkerer.kami.AwakenedIchorclothArmorItem;
import thaumic.tinkerer.kami.KamiArmorContent;
import thaumic.tinkerer.kami.KamiArmorState;
import thaumic.tinkerer.util.LegacyMotion;
import thaumic.tinkerer.util.LegacyPlayerCoordinates;

/** Source displacement is retried only by a current protection callback, never replayed from a stale projectile list. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class DeflectionActions {
    private enum Kind { FOCUS, ARMOR, DIRECT }
    private static final Map<ServerPlayer,EnumMap<Kind,Job>> PENDING=new IdentityHashMap<>();

    public static void focus(ServerPlayer player,ItemStack wand,ItemStack focus){protect(new Binding(Kind.FOCUS,player,wand,focus));}
    public static void armor(Player player){if(player instanceof ServerPlayer server)protect(new Binding(Kind.ARMOR,server,player.getItemBySlot(EquipmentSlot.CHEST),ItemStack.EMPTY));}
    /** Original public static helper remains usable by callers without requiring an armor item. */
    public static void direct(Player player){if(player instanceof ServerPlayer server)protect(new Binding(Kind.DIRECT,server,ItemStack.EMPTY,ItemStack.EMPTY));}

    private static final class Binding {
        final Kind kind;
        final ServerPlayer player;
        final ServerLevel level;
        final ItemStack source,snapshot,focus;
        final InteractionHand hand;
        final AnimationTabletBlockEntity tablet;
        final BlockPos ownerPos;
        final BlockState ownerState;
        final boolean leftClick,redstone;
        Binding(Kind kind,ServerPlayer player,ItemStack source,ItemStack focus){
            this.kind=kind;this.player=player;level=player.serverLevel();
            tablet=player instanceof TabletPlayer actor?actor.tablet():null;
            this.source=kind==Kind.DIRECT&&tablet!=null?tablet.getItem(0):source;
            snapshot=this.source.copy();this.focus=focus.copy();
            hand=player.getMainHandItem()==source?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            ownerPos=tablet==null?null:tablet.getBlockPos().immutable();ownerState=tablet==null?null:tablet.getBlockState();
            leftClick=tablet!=null&&tablet.leftClick;redstone=tablet!=null&&tablet.redstone;
        }
        boolean valid(){
            if(player.level()!=level||!player.isAlive()||player.isRemoved())return false;
            if(tablet!=null){
                if(tablet.isRemoved()||tablet.getLevel()!=level||!tablet.getBlockPos().equals(ownerPos)||tablet.getBlockState()!=ownerState
                        ||tablet.player()!=player||tablet.leftClick!=leftClick||tablet.redstone!=redstone||tablet.getItem(0)!=source)return false;
                var chunk=level.getChunkSource().getChunkNow(ownerPos.getX()>>4,ownerPos.getZ()>>4);
                if(chunk==null||chunk.getBlockEntities().get(ownerPos)!=tablet)return false;
            }
            return switch(kind){
                // Vis changes each source tick; the bound wand and its focus must stay the same.
                case FOCUS -> !source.isEmpty()&&player.isUsingItem()&&player.getUseItem()==source&&player.getUsedItemHand()==hand
                        &&(tablet!=null||player.getItemInHand(hand)==source)&&ItemStack.matches(focus,WandApi.focus(source));
                case ARMOR -> player.getItemBySlot(EquipmentSlot.CHEST)==source&&source.is(KamiArmorContent.GEM_CHEST.get())
                        &&ItemStack.matches(source,snapshot)&&KamiArmorState.enabled(player)&&AwakenedIchorclothArmorItem.variant(source)!=1;
                case DIRECT -> tablet==null||ItemStack.matches(source,snapshot);
            };
        }
        boolean registered(){
            return tablet!=null||!(player instanceof FakePlayer)&&level.getServer().getPlayerList().getPlayer(player.getUUID())==player&&!player.hasDisconnected();
        }
        boolean matches(Binding next){return next.player==player&&next.kind==kind&&next.source==source&&next.hand==hand&&next.tablet==tablet;}
        ChunkPreparationApi.Request prepare(Set<ChunkPos> columns){return tablet==null?ChunkPreparationApi.prepare(player,columns):ChunkPreparationApi.prepare(tablet,columns);}
    }
    private static final class Job implements AutoCloseable {
        final Binding binding;
        Set<ChunkPos> columns;
        ChunkPreparationApi.Request request;
        long lastSeen,lastAdvance=Long.MIN_VALUE;
        Job(Binding binding,Set<ChunkPos> columns){this.binding=binding;this.columns=Set.copyOf(columns);request=binding.prepare(columns);lastSeen=binding.level.getGameTime();}
        boolean valid(){return !request.closed()&&binding.valid()&&binding.registered();}
        boolean advance(Set<ChunkPos> current){
            if(!columns.equals(current)){
                // Release destinations that no longer belong to the current projectile scan.
                request.close();columns=Set.copyOf(current);request=binding.prepare(current);
            }
            long tick=binding.level.getGameTime();if(lastAdvance==tick)return false;lastAdvance=tick;
            return request.advance(2)&&loaded(binding.level,current);
        }
        @Override public void close(){
            request.close();var jobs=PENDING.get(binding.player);
            if(jobs!=null&&jobs.get(binding.kind)==this){jobs.remove(binding.kind);if(jobs.isEmpty())PENDING.remove(binding.player);}
        }
    }
    private record Move(Entity entity,Vec3 position,Vec3 velocity,Vec3 destination){}
    private static Vec3 origin(Player player){return new Vec3(player.getX(),LegacyPlayerCoordinates.rawY(player),player.getZ());}
    private static boolean finite(Vec3 value){return Double.isFinite(value.x)&&Double.isFinite(value.y)&&Double.isFinite(value.z);}
    private static List<Move> scan(ServerPlayer player,Vec3 origin){
        var moves=new ArrayList<Move>();if(!finite(origin))return moves;
        var bounds=new AABB(origin.x-4,origin.y-4,origin.z-4,origin.x+3,origin.y+3,origin.z+3);
        for(var entity:player.level().getEntities(player,bounds,ProjectileHooks::isProjectile)){
            if(entity.isRemoved()||ProjectileHooks.blacklisted(entity)||ProjectileHooks.owner(entity)==player)continue;
            var position=entity.position();var velocity=entity.getDeltaMovement();var destination=LegacyMotion.deflection(position,velocity,origin);
            if(finite(position)&&finite(velocity)&&finite(destination))moves.add(new Move(entity,position,velocity,destination));
        }
        return moves;
    }
    private static Set<ChunkPos> columns(List<Move> moves){var columns=new LinkedHashSet<ChunkPos>();for(var move:moves)columns.add(new ChunkPos(BlockPos.containing(move.destination)));return columns;}
    private static boolean loaded(ServerLevel level,Set<ChunkPos> columns){for(var pos:columns)if(level.getChunkSource().getChunkNow(pos.x,pos.z)==null)return false;return true;}
    private static Job pending(Binding binding){var jobs=PENDING.get(binding.player);return jobs==null?null:jobs.get(binding.kind);}
    private static void protect(Binding binding){
        if(!binding.level.getServer().isSameThread())throw new IllegalStateException("Server thread required");
        var job=pending(binding);
        try{
            if(job!=null&&(!job.valid()||!job.binding.matches(binding))){job.close();job=null;}
            if(!binding.valid()){if(job!=null)job.close();return;}
            var origin=origin(binding.player);var moves=scan(binding.player,origin);
            if(binding.kind==Kind.FOCUS&&thaumic.tinkerer.TinkererConfig.DEFLECT_DESTROY_PROJECTILES.get()){
                if(job!=null)job.close();
                for(var move:moves){
                    if(!binding.valid())break;
                    if(move.entity.isRemoved()||ProjectileHooks.blacklisted(move.entity)||ProjectileHooks.owner(move.entity)==binding.player)continue;
                    for(int i=0;i<6;i++)ParticleApi.broadcastSparkle(binding.level,(float)move.position.x,(float)move.position.y,(float)move.position.z,6);
                    move.entity.discard();
                }
                return;
            }
            var columns=columns(moves);
            if(moves.isEmpty()){if(job!=null)job.close();return;}
            if(job!=null)job.lastSeen=binding.level.getGameTime();
            if(!loaded(binding.level,columns)){
                if(!binding.registered())return;
                if(job==null){job=new Job(binding,columns);PENDING.computeIfAbsent(binding.player,key->new EnumMap<>(Kind.class)).put(binding.kind,job);}
                if(!job.advance(columns))return;
                // Availability never authorizes a saved projectile position; scan the current entities again.
                moves=scan(binding.player,origin);columns=columns(moves);if(!loaded(binding.level,columns))return;
            }
            try{
                for(var move:moves){
                    var entity=move.entity;
                    if(!binding.valid()||!origin(binding.player).equals(origin))break;
                    if(entity.isRemoved()||entity.level()!=binding.level||!entity.position().equals(move.position)||!entity.getDeltaMovement().equals(move.velocity)
                            ||ProjectileHooks.blacklisted(entity)||ProjectileHooks.owner(entity)==binding.player)continue;
                    var column=new ChunkPos(BlockPos.containing(move.destination));if(binding.level.getChunkSource().getChunkNow(column.x,column.z)==null)continue;
                    for(int i=0;i<6;i++)ParticleApi.broadcastSparkle(binding.level,(float)move.position.x,(float)move.position.y,(float)move.position.z,6);
                    entity.setPosRaw(move.destination.x,move.destination.y,move.destination.z);
                }
            }finally{if(job!=null)job.close();}
        }catch(RuntimeException|Error failure){if(job!=null)job.close();throw failure;}
    }
    private static List<Job> jobs(){var result=new ArrayList<Job>();for(var byKind:PENDING.values())result.addAll(byKind.values());return result;}
    public static void cancelFocus(ServerPlayer player){var byKind=PENDING.get(player);if(byKind!=null){var job=byKind.get(Kind.FOCUS);if(job!=null)job.close();}}
    public static void logout(String name){for(var job:jobs())if(job.binding.player.getGameProfile().getName().equals(name))job.close();}
    public static void cancelArmor(Player player){if(player instanceof ServerPlayer server){var byKind=PENDING.get(server);if(byKind!=null){var job=byKind.get(Kind.ARMOR);if(job!=null)job.close();}}}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        for(var job:jobs())if(job.binding.level==level){try{if(job.lastSeen<level.getGameTime()-1||!job.valid())job.close();}catch(RuntimeException|Error failure){job.close();throw failure;}}
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){for(var job:jobs())if(job.binding.level==event.getLevel())job.close();}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){for(var job:jobs())if(job.binding.level.getServer()==event.getServer())job.close();}
    private DeflectionActions(){}
}
