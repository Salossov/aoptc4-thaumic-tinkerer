package thaumic.tinkerer.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.*;
import thaumcraft.api.wands.*;
import thaumic.tinkerer.item.dislocation.*;
import thaumic.tinkerer.util.ItemData;

/** Release-164 block capture. Full native block states replace the old metadata integer. */
public final class DislocationFocusItem extends ItemFocusBasic implements FocusBehavior {
    public DislocationFocusItem(){super(new Properties().durability(1).rarity(Rarity.RARE));}
    @Override public boolean isRepairable(ItemStack stack){return false;}
    @Override public int getEnchantmentValue(){return 5;}
    @Override public int getFocusColor(ItemStack stack){return 16757248;}
    @Override public Map<Aspect,Integer> getVisCost(ItemStack stack){return Map.of(Aspects.ENTROPY,500,Aspects.ORDER,500,Aspects.EARTH,100);}
    @Override public List<FocusUpgradeType> getPossibleUpgradesByRank(ItemStack stack,int rank){return List.of(FocusUpgradeType.treasure);}
    @Override public boolean managesVis(){return true;}
    @Override public String getSortingHelper(ItemStack focus){
        // Original getPickedBlockStack creates a fresh, tagless stack: hash is always 0.
        return DislocationCapture.read(focus).map(value->"DISLOCATION"+value.root().state().getBlock().getDescriptionId()+"-0").orElse("DISLOCATION");
    }
    public Optional<BlockState> captured(ItemStack focus,net.minecraft.core.HolderLookup.Provider registries){
        return DislocationCapture.read(focus).map(value->value.root().state());
    }
    @Override public boolean cast(ServerPlayer player,ItemStack wand,ItemStack focus){
        if(!player.server.isSameThread())return false;
        // An invalid stored payload remains stored; never overwrite it with a
        // fresh capture merely because decoding failed.
        if(ItemData.flag(focus,"available")&&DislocationCapture.read(focus).isEmpty())return false;
        var ray=DislocationRay.trace(player);
        if(ray.complete()&&(ray.hit()==null||ray.hit().getType()!=HitResult.Type.BLOCK))return false;
        if(DislocationActions.defer(player,wand,focus,ray,DislocationTransfer::apply))return false;
        return DislocationTransfer.apply(player,wand,focus,ray);
    }
}
