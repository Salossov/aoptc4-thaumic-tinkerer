package thaumic.tinkerer.verification;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.crafting.IInfusionPedestal;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Runs the core's actual ritual, rather than emulating its ingredient consumption. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfusionRitualRulesTest {
    private static final int[][] OFFSETS={{-3,0},{-3,2},{-2,3},{0,3},{2,3},{3,2},{3,0},{3,-2},{2,-3},{0,-3},{-2,-3},{-3,-2}};
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    private static net.minecraft.world.level.block.Block block(String key){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:"+key));}
    private static void call(BlockEntity tile,String name)throws Exception{tile.getClass().getMethod(name).invoke(tile);}
    private static int remaining(BlockEntity tile)throws Exception{return (Integer)tile.getClass().getMethod("remainingIngredients").invoke(tile);}

    @GameTest(template="summon_space",batch="infusion_ritual",timeoutTicks=100)
    public static void realRitualRemaindersSingleShardsAndSavedProgress(GameTestHelper helper)throws Exception{
        VerificationReports.install();
        var level=helper.getLevel();var registries=level.registryAccess();
        var pos=helper.absolutePos(new BlockPos(8,7,8));
        var pedestalBlock=block("arcane_pedestal");var pillar=block("infusion_pillar");
        level.setBlock(pos.below(2),pedestalBlock.defaultBlockState(),18);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            level.setBlock(pos.offset(x,-2,z),pillar.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER),18);
            level.setBlock(pos.offset(x,-1,z),pillar.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),18);
        }
        for(var offset:OFFSETS)level.setBlock(pos.offset(offset[0],-2,offset[1]),pedestalBlock.defaultBlockState(),18);
        level.setBlock(pos,block("infusion_matrix").defaultBlockState(),18);
        var names=new ArrayList<>(List.of("enchanter"));
        if(TinkererConfig.KAMI_FORCE_ENABLED.get())names.addAll(List.of("ichorcloth_legs_gem","ichor","sky_pearl"));
        try(var peer=new NativePlayerFixture(helper,"TTRitual",pos.above())){
            for(var name:names){
                var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id(name)).orElseThrow().value();
                ResearchApi.grant(peer.player,recipe.research());
                var centre=(IInfusionPedestal)level.getBlockEntity(pos.below(2));
                var input=recipe.input().getItems()[0].copyWithCount(1);
                centre.setItem(0,input.copy());
                var supplies=new ArrayList<ItemStack>();var pedestals=new ArrayList<IInfusionPedestal>();
                for(int i=0;i<OFFSETS.length;i++){
                    var offset=OFFSETS[i];var inventory=(IInfusionPedestal)level.getBlockEntity(pos.offset(offset[0],-2,offset[1]));
                    inventory.clearContent();pedestals.add(inventory);
                    if(i>=recipe.components().size())continue;
                    var supply=recipe.components().get(i).getItems()[0].copyWithCount(1);
                    if(BuiltInRegistries.ITEM.getKey(supply.getItem()).equals(ThaumicTinkerer.id("spell_cloth")))
                        supply.set(DataComponents.CUSTOM_NAME,Component.literal("Preserve ritual cloth"));
                    supplies.add(supply.copy());inventory.setItem(0,supply);
                }
                var matrix=level.getBlockEntity(pos);var ready=matrix.saveWithFullMetadata(registries);
                ready.putBoolean("Active",true);ready.putBoolean("Crafting",false);matrix.loadWithComponents(ready,registries);
                check((Boolean)matrix.getClass().getMethod("start",net.minecraft.server.level.ServerPlayer.class).invoke(matrix,peer.player),"actual ritual starts from native recipe "+name);
                var started=matrix.saveWithFullMetadata(registries);
                check(remaining(matrix)==recipe.components().size(),"native start keeps one entry per pedestal "+name);
                var costs=started.getCompound("RemainingEssentia");
                for(var cost:recipe.aspects().entrySet())check(costs.getInt(cost.getKey())==cost.getValue(),"native start retains actual essentia cost "+name+":"+cost.getKey());
                // Begin at the legitimate post-essentia phase, with instability suppressed.
                // This verifies consumption/remainders and saved progress, not essence supply or timing.
                for(var key:costs.getAllKeys())costs.putInt(key,0);
                started.putInt("Instability",0);matrix.loadWithComponents(started,registries);call(matrix,"getSurroundings");
                boolean reloaded=false;int cycles=0;
                while(matrix.saveWithFullMetadata(registries).getBoolean("Crafting")&&cycles++<100){
                    call(matrix,"craftCycle");
                    if(!reloaded&&remaining(matrix)==recipe.components().size()-1){
                        var saved=matrix.saveWithFullMetadata(registries);
                        var restored=BlockEntity.loadStatic(pos,level.getBlockState(pos),saved,registries);
                        check(restored!=null,"saved in-progress ritual loads "+name);
                        level.removeBlockEntity(pos);level.setBlockEntity(restored);matrix=restored;call(matrix,"getSurroundings");reloaded=true;
                    }
                }
                check(reloaded&&!matrix.saveWithFullMetadata(registries).getBoolean("Crafting")&&remaining(matrix)==0,"restored native ritual finishes "+name);
                check(ItemStack.matches(centre.getItem(0),recipe.getRecipeOutput(input)),"native completion writes exact result and count "+name);
                for(int i=0;i<supplies.size();i++){
                    var supply=supplies.get(i);var remainder=pedestals.get(i).getItem(0);
                    if(BuiltInRegistries.ITEM.getKey(supply.getItem()).equals(ThaumicTinkerer.id("spell_cloth"))){
                        check(remainder.is(supply.getItem())&&remainder.getCount()==1&&remainder.getDamageValue()==1,"ritual returns cloth with one use consumed");
                        check(Component.literal("Preserve ritual cloth").equals(remainder.get(DataComponents.CUSTOM_NAME)),"ritual cloth retains unrelated component");
                    }else if(supply.is(Items.LAVA_BUCKET))check(remainder.is(Items.BUCKET)&&remainder.getCount()==1,"ritual returns the empty lava bucket once");
                    else check(remainder.isEmpty(),"one native pedestal item is consumed "+name+":"+i);
                }
                if(name.equals("ichor"))check(centre.getItem(0).getCount()==8,"single shard pedestals produce eight ichor despite display hints");
                if(name.equals("sky_pearl"))check(centre.getItem(0).getCount()==2&&centre.getItem(0).getMaxStackSize()==1,"full completion preserves both approved non-stackable pearls");
                System.out.println("TT_NATIVE_INFUSION_CONSUMPTION_PASS recipe="+name+" cycles="+cycles+" saved_progress=true");
            }
        }
        helper.succeed();
    }
    private InfusionRitualRulesTest(){}
}
