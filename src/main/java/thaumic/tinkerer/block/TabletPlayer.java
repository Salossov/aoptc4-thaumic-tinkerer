package thaumic.tinkerer.block;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

/** One independent player per tablet, retaining the original shared identity and reset/drop order. */
public final class TabletPlayer extends FakePlayer {
    private final AnimationTabletBlockEntity tablet;
    private final Map<Holder<Attribute>,Set<ResourceLocation>> applied=new HashMap<>();
    public TabletPlayer(ServerLevel level,AnimationTabletBlockEntity tablet){super(level,new GameProfile(UUID.fromString("a8f026a0-135b-11e4-9191-0800200c9a66"),"[ThaumcraftTablet]"));this.tablet=tablet;}
    public void prepare(){
        getAbilities().instabuild=false;var p=tablet.getBlockPos();setPos(p.getX()+.5,p.getY()+1.6,p.getZ()+.5);stopRiding();ejectPassengers();setDeltaMovement(0,0,0);setHealth(20);deathTime=0;fallDistance=0;setOnGround(false);
        float yaw=switch(tablet.rotation()){case 0->180;case 1->0;case 2->90;default->-90;};setYRot(yaw);setYHeadRot(yaw);setXRot(-15);
        var inventory=getInventory();for(int i=0;i<inventory.getContainerSize();i++)if(i!=inventory.selected){var stack=inventory.getItem(i);if(!stack.isEmpty()){spawnAtLocation(stack,1);inventory.setItem(i,ItemStack.EMPTY);}}
        inventory.clearContent();
    }
    public void weaponAttributes(ItemStack stack){
        applied.forEach((attribute,ids)->{var instance=getAttribute(attribute);if(instance!=null)ids.forEach(instance::removeModifier);});applied.clear();
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{var instance=getAttribute(attribute);if(instance!=null){instance.removeModifier(modifier.id());instance.addTransientModifier(modifier);applied.computeIfAbsent(attribute,key->new HashSet<>()).add(modifier.id());}});
        // Legacy player attacks had no attack-strength cooldown. The fake player remains airborne, so no modern sweeping attack is introduced.
        attackStrengthTicker=1000;
    }
}
