package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class PlatformRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @GameTest(template="empty",timeoutTicks=100)
    public static void platform(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+130,(anchor.getZ()>>4)*16+8);
        var player=FocusRulesTest.player(helper,"[TTPlatform]");var sheep=EntityType.SHEEP.create(level);var changed=new ArrayList<BlockPos>();
        try{
            for(var p:BlockPos.betweenClosed(pos.offset(-2,-3,-2),pos.offset(2,5,2))){changed.add(p.immutable());level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}
            level.setBlock(pos,PlatformContent.BLOCK.get().defaultBlockState(),3);var state=level.getBlockState(pos);var tile=(PlatformBlockEntity)level.getBlockEntity(pos);
            check(tile!=null&&tile.camouflage().is(Blocks.AIR),"registered platform creates its actual empty camouflage entity");
            check(state.getDestroySpeed(level,pos)==2&&state.getBlock().getExplosionResistance()==3&&!state.canOcclude()&&state.ignitedByLava(),"original hardness, effective resistance, nonopaque wood and lava ignition");
            check(!state.getShape(level,pos).isEmpty()&&state.getCollisionShape(level,pos,CollisionContext.empty()).isEmpty(),"selection cube remains while a null entity has no collision boxes");
            check(!state.isPathfindable(PathComputationType.LAND)&&!state.isPathfindable(PathComputationType.WATER)&&!state.isPathfindable(PathComputationType.AIR),"original non-passable pathfinding is independent of directional physical collision");
            player.setShiftKeyDown(false);player.setPos(pos.getX()+.5,pos.getY()+.37,pos.getZ()+.5);
            check(state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"player below the approved old-client threshold can pass through");
            player.setPos(player.getX(),pos.getY()+.39,player.getZ());
            check(!state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"player above the old-client threshold gets support on the server");
            player.setShiftKeyDown(true);check(state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"sneaking always removes player collision");
            player.setShiftKeyDown(false);player.setPos(pos.getX()+.5,pos.getY()+3,pos.getZ()+.5);player.setOnGround(false);player.move(MoverType.SELF,new Vec3(0,-4,0));
            check(Math.abs(player.getY()-pos.getY()-1)<1e-7,"actual downward player movement lands on the top face");
            player.setShiftKeyDown(true);player.setOnGround(false);player.move(MoverType.SELF,new Vec3(0,-2,0));
            check(Math.abs(player.getY()-pos.getY()+1)<1e-7,"actual sneaking player movement passes downward");
            player.setShiftKeyDown(false);player.setPos(pos.getX()+.5,pos.getY()-2,pos.getZ()+.5);player.setOnGround(false);player.move(MoverType.SELF,new Vec3(0,4,0));
            check(Math.abs(player.getY()-pos.getY()-2)<1e-7,"actual upward player movement passes from below");
            sheep.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);check(state.getCollisionShape(level,pos,CollisionContext.of(sheep)).isEmpty(),"nonplayer at block origin has no support");
            sheep.setPos(sheep.getX(),pos.getY()+.01,sheep.getZ());check(!state.getCollisionShape(level,pos,CollisionContext.of(sheep)).isEmpty(),"nonplayer retains the original unshifted strict height condition");
            var disguise=new ItemStack(Blocks.STONE);player.setItemInHand(InteractionHand.MAIN_HAND,disguise);
            var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
            state.useItemOn(disguise,level,player,InteractionHand.MAIN_HAND,hit);
            check(tile.camouflage().is(Blocks.STONE)&&disguise.getCount()==1,"actual camouflage interaction changes the displayed block without consuming it");
            var saved=tile.saveWithFullMetadata(level.registryAccess());var restored=new PlatformBlockEntity(pos,state);restored.loadWithComponents(saved,level.registryAccess());
            check(restored.camouflage().is(Blocks.STONE)&&tile.getUpdateTag(level.registryAccess()).contains("camoState",10),"camouflage persists and is included in the real synchronization tag");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);state.useWithoutItem(level,player,hit);
            check(tile.camouflage().is(Blocks.AIR),"empty-hand interaction clears the camouflage");
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("platform")).orElseThrow().value();
            check(recipe.research().equals("thaumictinkerer:platform")&&recipe.vis().equals(Map.of("aer",2,"perditio",4))&&recipe.getResultItem(level.registryAccess()).getCount()==2,"actual arcane recipe has exact original gate, vis and output count");
            check(recipe.getIngredients().stream().filter(i->i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:greatwood_planks"))))).count()==2&&recipe.getIngredients().stream().filter(i->i.test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:silverwood_planks"))))).count()==1,"original wooden-device metadata 6/7 resolve to the two correct native planks");
            var research=AddonRegistry.research().get("thaumictinkerer:platform");
            check(research.concealed()&&research.secondary()&&research.column()==-2&&research.row()==6&&research.complexity()==3&&research.parents().equals(List.of("thaumictinkerer:cleansing_talisman")),"original research parent, grid, flags and complexity");
            System.out.println("TT_PLATFORM_PASS checks="+checks);helper.succeed();
        }finally{player.setShiftKeyDown(false);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);sheep.discard();for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
    }
}
