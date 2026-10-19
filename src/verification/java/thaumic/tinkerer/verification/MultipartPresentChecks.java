package thaumic.tinkerer.verification;

import codechicken.microblock.api.*;
import codechicken.microblock.init.CBMicroblockModContent;
import codechicken.microblock.item.*;
import codechicken.microblock.part.MicroblockPart;
import codechicken.microblock.recipe.MicroRecipe;
import codechicken.multipart.block.TileMultipart;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import thaumic.tinkerer.integration.MultipartCompatibility;
import static thaumic.tinkerer.verification.MultipartRulesTest.check;

/** Kept separate: JVM verification must not resolve optional classes in an absent-mod test. */
final class MultipartPresentChecks {
    static void run(GameTestHelper helper) {
        var level=helper.getLevel();var base=helper.absolutePos(new BlockPos(8,3,8));var target=base.above();
        float[] hardness={1.5f,1.5f,30,30,2.5f,.2f,30,30,30,30,30,30,30,30,.8f,.8f};
        float[] resistance={3,3,6,6,2.5f,.2f,3,6,6,6,6,6,6,6,6,6};
        try(var fixture=new NativePlayerFixture(helper,"TTMultipart",base.above(2))) {
            var player=fixture.player;int index=0;
            for(var spec:MultipartCompatibility.materials()) {
                var state=spec.sourceState();var key=BlockMicroMaterial.makeMaterialKey(state);var material=MicroMaterial.REGISTRY.get(key);
                check(material!=null&&material.getItem().is(state.getBlock().asItem()),"native registry and source block item: "+key);
                check(material.getLightEmission()==0,"original no-argument light value remains zero: "+key);
                check(material.getExplosionResistance(level,target,null)==resistance[index],"original entity-only explosion resistance: "+key);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));
                float expected=player.getDigSpeed(state,new BlockPos(0,-1,0))/hardness[index]/30;
                check(Math.abs(material.getStrength(player)-expected)<1e-7,"source null-world hardness fallback and outer /30 conversion: "+key);
                check(material.isTransparent()==(index==2||index==3||index==6),"original server opacity, including planks and amber: "+key);
                if(index==8)check(((BlockMicroMaterial)material).state.is(MultipartCompatibility.materials().get(7).sourceState().getBlock()),"totem renders static tile texture while retaining totem item identity");
                var input=CraftingInput.of(2,1,List.of(new ItemStack(CBMicroblockModContent.STONE_SAW.get()),material.getItem()));
                var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value();
                check(recipe instanceof MicroRecipe,"actual recipe manager selects native saw recipe: "+key);
                var halves=recipe.assemble(input,level.registryAccess());var component=MicroMaterialComponent.getComponent(halves);
                check(halves.getCount()==2&&component!=null&&component.material()==material&&component.factoryId()==0&&component.size()==4,"native stone saw cuts every selected material under approved modern rules: "+key);
                var saved=halves.save(level.registryAccess());var restored=ItemStack.parse(level.registryAccess(),saved).orElseThrow();
                check(ItemStack.isSameItemSameComponents(halves,restored),"native item component round trip: "+key);
                var glue=CraftingInput.of(2,1,List.of(restored.copyWithCount(1),restored.copyWithCount(1)));
                var glued=recipe.assemble(glue,level.registryAccess());
                check(glued.getCount()==1&&ItemStack.isSameItemSameComponents(glued,material.getItem()),"native glue restores exact source item, including totem: "+key);
                level.setBlockAndUpdate(base,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(target,Blocks.AIR.defaultBlockState());
                player.setPos(base.getX()+.5,base.getY()+2,base.getZ()+.5);player.setXRot(90);player.setYRot(0);player.setItemInHand(InteractionHand.MAIN_HAND,restored);
                var hit=new BlockHitResult(Vec3.atCenterOf(base).add(0,.5,0),Direction.UP,base,false);
                var result=restored.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                check(result.consumesAction()&&restored.getCount()==1,"actual item placement consumes one half: "+key);
                check(level.getBlockEntity(target) instanceof TileMultipart,"actual placement creates native multipart tile: "+key);
                var tile=(TileMultipart)level.getBlockEntity(target);
                check(tile.getPartList().size()==1&&tile.getPartList().getFirst() instanceof MicroblockPart,"native tile contains selected microblock: "+key);
                var part=(MicroblockPart)tile.getPartList().getFirst();
                check(part.getMaterial()==material&&part.getSize()==4,"placed part carries native material and size: "+key);
                var shape=level.getBlockState(target).getCollisionShape(level,target).bounds();
                check(shape.minY==0&&shape.maxY==.5&&shape.getXsize()==1&&shape.getZsize()==1,"placed half has native collision geometry: "+key);
                var nbt=tile.saveWithFullMetadata(level.registryAccess());
                var loaded=BlockEntity.loadStatic(target,level.getBlockState(target),nbt,level.registryAccess());
                check(loaded instanceof TileMultipart&&((TileMultipart)loaded).getPartList().size()==1&&((MicroblockPart)((TileMultipart)loaded).getPartList().getFirst()).getMaterial()==material,"native block entity save/load retains material: "+key);
                level.removeBlock(target,false); index++;
            }
            System.out.println("TT_MULTIPART_NATIVE_PASS materials="+index+" native_saw=true native_glue=true native_placement=true native_collision=true item_and_tile_roundtrip=true");
            helper.succeed();
        } finally {level.removeBlock(target,false);level.removeBlock(base,false);}
    }
    private MultipartPresentChecks() {}
}
