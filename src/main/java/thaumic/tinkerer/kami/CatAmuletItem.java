package thaumic.tinkerer.kami;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.MobBehaviorApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Source cat amulet: worn only, no inventory effect or undo on unequip. */
public final class CatAmuletItem extends Item implements ICurioItem {
    public CatAmuletItem() {super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        var wearer=context.entity();var level=wearer.level();if(level.isClientSide())return;
        var bounds=new AABB(wearer.getX()-10,wearer.getY()-4,wearer.getZ()-10,wearer.getX()+10,wearer.getY()+4,wearer.getZ()+10);
        for(var mob:level.getEntitiesOfClass(Mob.class,bounds)) {
            MobBehaviorApi.redirectAvoidance(mob,Ocelot.class,Player.class);
            MobBehaviorApi.redirectAvoidance(mob,Cat.class,Player.class);
            MobBehaviorApi.redirectNearestTarget(mob,Player.class,EndCrystal.class);
            if(mob instanceof Creeper creeper) {MobBehaviorApi.setSwell(creeper,2);creeper.setTarget(null);}
        }
    }
}
