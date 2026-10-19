package thaumic.tinkerer.fire;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import thaumic.tinkerer.ThaumicTinkerer;

/** Global within one running server, matching the original fire block's shared cache. */
@EventBusSubscriber(modid=ThaumicTinkerer.ID)
public final class OrderFireRules {
    private static final Map<MinecraftServer,Map<FireLegacyStates.Key,Optional<BlockState>>> CACHES=new WeakHashMap<>();
    private static Map<FireLegacyStates.Key,Optional<BlockState>> cache(ServerLevel level){return CACHES.computeIfAbsent(level.getServer(),server->{
        var map=new HashMap<FireLegacyStates.Key,Optional<BlockState>>();for(var block:List.of(Blocks.STONE,Blocks.GLASS,Blocks.SAND,Blocks.GRAVEL))map.put(FireLegacyStates.key(block.defaultBlockState()).zero(),Optional.empty());return map;
    });}
    private static List<ItemStack> drops(ServerLevel level,BlockPos pos){return Block.getDrops(level.getBlockState(pos),level,pos,level.getBlockEntity(pos));}
    /** Forge 1.7.10's default getDrops adds one ItemStack per unit, not a merged stack. */
    public static int decayChance(ServerLevel level,BlockPos pos){int count=drops(level,pos).stream().mapToInt(ItemStack::getCount).sum();return count==2?2:count>=3?1:3;}
    public static ItemStack craftNine(Level level,ItemStack stack){
        var input=CraftingInput.of(3,3,Collections.nCopies(9,stack));
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).map(recipe->recipe.value().assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
    }
    private static boolean sameLegacyItem(ItemStack a,ItemStack b){return ItemStack.isSameItem(a,b)&&Objects.equals(a.get(DataComponents.DAMAGE),b.get(DataComponents.DAMAGE));}
    private static BlockState compute(ServerLevel level,BlockPos pos){
        var drops=drops(level,pos);if(drops.isEmpty()||drops.stream().anyMatch(stack->!sameLegacyItem(stack,drops.getFirst())))return null;
        var stack=drops.getFirst();var result=craftNine(level,stack);
        if(result.isEmpty()){
            var input=new SingleRecipeInput(stack);var smelted=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).map(recipe->recipe.value().assemble(input,level.registryAccess())).orElse(ItemStack.EMPTY);
            result=craftNine(level,smelted);
        }
        return result.getItem() instanceof BlockItem block?block.getBlock().defaultBlockState():null;
    }
    private static Map<FireLegacyStates.Key,Optional<BlockState>> populate(ServerLevel level,BlockPos pos){
        var map=cache(level);var actual=FireLegacyStates.key(level.getBlockState(pos));
        if(!map.containsKey(actual))map.put(actual,Optional.ofNullable(compute(level,pos)));
        return map;
    }
    public static BlockState transformation(Level level,BlockPos pos,BlockState query){
        if(!(level instanceof ServerLevel server))return null;
        return populate(server,pos).getOrDefault(FireLegacyStates.key(query).zero(),Optional.empty()).orElse(null);
    }
    public static boolean isResult(Level level,BlockPos pos,BlockState query){
        if(!(level instanceof ServerLevel server))return false;
        return populate(server,pos).values().stream().flatMap(Optional::stream).anyMatch(result->FireLegacyStates.matchesResult(result,query));
    }
    @SubscribeEvent public static void reload(TagsUpdatedEvent event){if(event.getUpdateCause()==TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)CACHES.clear();}
    @SubscribeEvent public static void stop(ServerStoppedEvent event){CACHES.remove(event.getServer());}
    private OrderFireRules(){}
}
