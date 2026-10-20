package thaumic.tinkerer.verification;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.integration.DeviceTooltip;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DeviceTooltipRulesTest {
    static void check(boolean value,String message) {if(!value)throw new GameTestAssertException(message);}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,Function<String,Object> values) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(object,method,args)->values.apply(method.getName()));
    }
    private static void lines(BlockEntity tile,String... keys) {
        var actual=DeviceTooltip.lines(tile);
        check(actual.stream().map(c->c.getContents()instanceof TranslatableContents t?t.getKey():"literal").toList().equals(
                Arrays.stream(keys).map(k->k.equals("literal")?k:"ttwaila."+k).toList()),"source tooltip order and conditional lines: "+tile.getType()+" / "+Arrays.toString(keys));
        var data=new CompoundTag();data.putInt("other_mod",31);DeviceTooltip.write(data,tile);
        check(data.getInt("other_mod")==31,"scoped tooltip payload preserves other providers");
        check(DeviceTooltip.read(data,tile.getLevel().registryAccess()).equals(actual),"component JSON preserves translations, arguments and styles");
        check(DeviceTooltip.readOrLocal(new CompoundTag(),tile,tile.getLevel().registryAccess()).equals(actual),"client-only installation can use the original local block entity path");
    }
    @GameTest(template="summon_space",batch="device_tooltips",timeoutTicks=100)
    public static void nativeDeviceTooltipBodies(GameTestHelper helper) {
        VerificationReports.install();var level=helper.getLevel();var p=helper.absolutePos(new BlockPos(4,4,4));var chest=p.east(3);
        try {
            level.setBlockAndUpdate(p,AnimationTabletContent.BLOCK.get().defaultBlockState());var tablet=(AnimationTabletBlockEntity)level.getBlockEntity(p);
            lines(tablet,"currentTool");
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Test [tool]").withStyle(ChatFormatting.GOLD));tablet.setItem(0,tool.copy());
            lines(tablet,"currentTool","leftClick","autonomous");tablet.leftClick=false;tablet.redstone=true;lines(tablet,"currentTool","rightClick","redstone");
            var toolName=((TranslatableContents)DeviceTooltip.lines(tablet).getFirst().getContents()).getArgs()[0];
            check(toolName.equals(tool.getHoverName()),"tool name keeps custom formatting as a component argument");
            level.setBlockAndUpdate(p,TransvectorContent.INTERFACE.get().defaultBlockState());var transvector=(TransvectorInterfaceBlockEntity)level.getBlockEntity(p);
            lines(transvector,"connected");level.setBlockAndUpdate(chest,Blocks.CHEST.defaultBlockState());transvector.bind(chest);lines(transvector,"connected","literal");
            check(DeviceTooltip.lines(transvector).get(1).getString().equals("x: "+chest.getX()+" y: "+chest.getY()+" z: "+chest.getZ()),"original absolute coordinate format");
            level.removeBlock(chest,false);lines(transvector,"connected");
            level.setBlockAndUpdate(p,RepairerContent.BLOCK.get().defaultBlockState());var repairer=(RepairerBlockEntity)level.getBlockEntity(p);lines(repairer);
            var emptyRepairer=new CompoundTag();DeviceTooltip.write(emptyRepairer,repairer);
            tool.setDamageValue(3);repairer.setItem(0,tool);lines(repairer,"repairing");tool.setDamageValue(0);lines(repairer,"finishedRepairing");
            check(DeviceTooltip.readOrLocal(emptyRepairer,repairer,level.registryAccess()).isEmpty(),"authoritative empty server snapshot suppresses stale local inventory lines");
            level.setBlockAndUpdate(p,MagnetContent.MAGNET.get().defaultBlockState());lines(level.getBlockEntity(p),"pullingMode");
            level.setBlockAndUpdate(p,MagnetContent.MAGNET.get().defaultBlockState().setValue(MagnetBlock.PULLING,false));lines(level.getBlockEntity(p),"pushingMode");
            level.setBlockAndUpdate(p,MagnetContent.MOB_MAGNET.get().defaultBlockState());var mobs=(MobMagnetBlockEntity)level.getBlockEntity(p);
            for(boolean pull:new boolean[]{true,false})for(boolean adult:new boolean[]{true,false})for(boolean pattern:new boolean[]{false,true}) {
                level.setBlockAndUpdate(p,mobs.getBlockState().setValue(MagnetBlock.PULLING,pull));mobs.setSearchAdults(adult);
                var mould=pattern?new ItemStack(ThaumicTinkerer.SOUL_MOULD.get()):ItemStack.EMPTY;if(pattern)ItemData.update(mould,tag->tag.putString("pattern","minecraft:pig"));mobs.setItem(0,mould);
                String direction=pull?"pulling":"pushing";lines(mobs,direction+"Mode",direction+(adult?"Adult":"Child")+(pattern?"Type":""));
            }
            var pigName=((TranslatableContents)DeviceTooltip.lines(mobs).get(1).getContents()).getArgs()[0];
            check(pigName.equals(net.minecraft.world.entity.EntityType.PIG.getDescription()),"flattened mob pattern uses the modern entity translation");
            var stale=new CompoundTag();stale.putString(DeviceTooltip.DATA_KEY,"old");stale.putInt("other_mod",31);DeviceTooltip.write(stale,null);
            check(!stale.contains(DeviceTooltip.DATA_KEY)&&stale.getInt("other_mod")==31,"missing target removes stale own data and preserves other providers");
            check(DeviceTooltip.lines(null).isEmpty(),"no stale tooltip for absent target");
            System.out.println("TT_DEVICE_TOOLTIP_NATIVE_PASS original_conditions=true json_components=true client_only_fallback=true jade="+net.neoforged.fml.ModList.get().isLoaded("jade")+" wthit="+net.neoforged.fml.ModList.get().isLoaded("wthit"));helper.succeed();
        }finally {level.removeBlock(p,false);level.removeBlock(chest,false);}
    }
    private DeviceTooltipRulesTest() {}
}
