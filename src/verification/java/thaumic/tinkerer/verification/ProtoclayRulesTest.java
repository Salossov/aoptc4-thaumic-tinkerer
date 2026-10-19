package thaumic.tinkerer.verification;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.crafting.InfusionInput;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.api.IAdvancedTool;
import thaumic.tinkerer.kami.construction.KamiConstructionContent;
import thaumic.tinkerer.kami.tool.*;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ProtoclayRulesTest {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);}
    private static boolean enabled(GameTestHelper helper){VerificationReports.install();if(TinkererConfig.KAMI_FORCE_ENABLED.get())return true;helper.succeed();return false;}
    @GameTest(template="summon_space",batch="protoclay",timeoutTicks=100)
    public static void registeredRecipeResearchAndToolContract(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTProtoData",helper.absolutePos(new BlockPos(8,4,8)))){
            var level=helper.getLevel();var stack=KamiConstructionContent.PROTOCLAY.toStack();
            check(stack.getMaxStackSize()==1&&!stack.isDamageableItem()&&stack.getRarity()==Rarity.EPIC,"source item properties");
            var copy=ItemStack.parse(level.registryAccess(),stack.save(level.registryAccess())).orElseThrow();check(copy.is(KamiConstructionContent.PROTOCLAY),"registered native item codec");
            var key=ThaumicTinkerer.id("protoclay");var recipe=(InfusionRecipe)level.getRecipeManager().byKey(key).orElseThrow().value();
            check(recipe.instability()==4&&recipe.aspects().equals(java.util.Map.of("perfodio",16,"instrumentum",16)),"source infusion values");
            check(recipe.matches(new InfusionInput(new ItemStack(Items.CLAY_BALL),List.of(new ItemStack(Items.DIRT),new ItemStack(Items.STONE),new ItemStack(Items.OAK_LOG),new ItemStack(thaumic.tinkerer.kami.KamiMaterials.ENDER_SHARD.get())))),"actual source ingredients");
            var node=ResearchApi.definitions(fixture.player).get(key.toString());
            check(node.parents().equals(List.of("thaumictinkerer:ichor_pick_gem"))&&node.hiddenParents().equals(List.of("thaumictinkerer:ichor_shovel_gem")),"visible and hidden parents");
            for(var tool:List.of(IchorTools.AWAKENED_PICK.get(),IchorTools.AWAKENED_SHOVEL.get(),IchorTools.AWAKENED_AXE.get(),IchorTools.AWAKENED_SWORD.get()))check(tool instanceof IAdvancedTool,"all four registered awakened tools expose contract");
            check(!(IchorTools.PICK.get() instanceof IAdvancedTool),"ordinary ichor tools remain outside the source contract");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="protoclay",timeoutTicks=100)
    public static void actualInventoryTickSwapsFirstMatchingStack(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTProtoSwap",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var pos=helper.absolutePos(new BlockPos(8,4,8));
            player.setPos(pos.getX()+.5,pos.getY()+.5-1.62,pos.getZ()-2);player.setYRot(0);player.setXRot(0);player.swinging=true;
            var proto=KamiConstructionContent.PROTOCLAY.toStack();var pick=IchorTools.AWAKENED_PICK.toStack();var first=IchorTools.AWAKENED_SHOVEL.toStack();var second=IchorTools.AWAKENED_SHOVEL.toStack();
            AdvancedToolMode.set(pick,2);AdvancedToolMode.set(first,1);ItemData.update(pick,t->t.putString("retained","source stack"));
            player.getInventory().selected=0;player.getInventory().setItem(0,pick);player.getInventory().setItem(1,proto);player.getInventory().setItem(5,first);player.getInventory().setItem(7,second);
            helper.getLevel().setBlock(pos,Blocks.SNOW_BLOCK.defaultBlockState(),2);
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);
            check(player.getMainHandItem()==first&&player.getInventory().getItem(5)==pick&&player.getInventory().getItem(7)==second,"first-match reference swap");
            check(AdvancedToolMode.get(pick)==2&&AdvancedToolMode.get(first)==1&&ItemData.read(pick).getString("retained").equals("source stack"),"swap preserves modes and components");
            helper.getLevel().setBlock(pos,Blocks.GLASS.defaultBlockState(),2);
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);
            check(player.getMainHandItem()==pick&&player.getInventory().getItem(5)==first,"glass selects pick through original tool-free priority");
            var axe=IchorTools.AWAKENED_AXE.toStack();player.getInventory().setItem(6,pick);player.setItemInHand(InteractionHand.MAIN_HAND,axe);
            helper.getLevel().setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),2);helper.getLevel().setBlock(pos,Blocks.DANDELION.defaultBlockState(),2);
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);
            check(player.getMainHandItem()==pick,"source tool-free plants select pick first despite the axe material group");
        }
        helper.succeed();
    }
    @GameTest(template="summon_space",batch="protoclay",timeoutTicks=100)
    public static void idleSwordAndSourceWaterTargeting(GameTestHelper helper){
        if(!enabled(helper))return;
        try(var fixture=new NativePlayerFixture(helper,"TTProtoGuards",helper.absolutePos(new BlockPos(8,4,8)))){
            var player=fixture.player;var pos=helper.absolutePos(new BlockPos(8,4,8));
            player.setPos(pos.getX()+.5,pos.getY()+.95-1.62,pos.getZ()-2);player.setYRot(0);player.setXRot(0);
            var proto=KamiConstructionContent.PROTOCLAY.toStack();var shovel=IchorTools.AWAKENED_SHOVEL.toStack();var pick=IchorTools.AWAKENED_PICK.toStack();
            player.getInventory().selected=0;player.getInventory().setItem(0,shovel);player.getInventory().setItem(1,proto);player.getInventory().setItem(5,pick);
            helper.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),2);player.swinging=false;
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);check(player.getMainHandItem()==shovel,"no swap without swing");
            var sword=IchorTools.AWAKENED_SWORD.toStack();player.setItemInHand(InteractionHand.MAIN_HAND,sword);player.swinging=true;
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);check(player.getMainHandItem()==sword,"sword excluded");
            player.setItemInHand(InteractionHand.MAIN_HAND,shovel);helper.getLevel().setBlock(pos,Blocks.WATER.defaultBlockState(),2);helper.getLevel().setBlock(pos.south(2),Blocks.STONE.defaultBlockState(),2);
            check(AwakenedMining.target(player,true,4.5).hit().getBlockPos().equals(pos),"source fluid is hit at old full-block height; stone behind ignored");
            proto.getItem().inventoryTick(proto,helper.getLevel(),player,1,false);check(player.getMainHandItem()==pick,"source tool-free fluid still chooses pick");
        }
        helper.succeed();
    }
}
