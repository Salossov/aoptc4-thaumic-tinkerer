package thaumic.tinkerer.verification;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import thaumic.tinkerer.integration.JadeCompatibility;
import static thaumic.tinkerer.verification.DeviceTooltipRulesTest.*;

final class JadeTooltipChecks {
    static void verify(BlockEntity tile,CompoundTag expected) {
        try {
            // Inspect the host's real post-discovery registry; never register the plugin from the test.
            var type=Class.forName("snownee.jade.impl.WailaCommonRegistration");var registry=type.getMethod("instance").invoke(null);
            var providers=(List<?>)type.getMethod("getBlockNBTProviders",net.minecraft.world.level.block.Block.class,BlockEntity.class).invoke(registry,tile.getBlockState().getBlock(),tile);
            check(providers.contains(JadeCompatibility.PROVIDER),"native Jade annotation discovery registered this target's actual provider");
            var accessor=proxy(BlockAccessor.class,key->switch(key) {case "getBlockEntity","getTarget"->tile;case "getLevel"->tile.getLevel();case "getPosition"->tile.getBlockPos();case "getBlockState"->tile.getBlockState();case "getBlock"->tile.getBlockState().getBlock();default->null;});
            var data=new CompoundTag();data.putInt("other_mod",31);JadeCompatibility.PROVIDER.appendServerData(data,accessor);
            check(data.equals(expected),"native Jade server provider returns exact shared payload");
        } catch(ReflectiveOperationException error) {throw new net.minecraft.gametest.framework.GameTestAssertException("Jade discovery check: "+error);}
    }
    private JadeTooltipChecks() {}
}
