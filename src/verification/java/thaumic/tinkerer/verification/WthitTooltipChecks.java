package thaumic.tinkerer.verification;

import java.util.List;
import mcp.mobius.waila.api.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.integration.WthitCompatibility;
import static thaumic.tinkerer.verification.DeviceTooltipRulesTest.*;

final class WthitTooltipChecks {
    static void verify(BlockEntity tile,CompoundTag expected) {
        try {
            // Reflection is verification-only: production imports public API classes exclusively.
            var type=Class.forName("mcp.mobius.waila.registry.Registrar");var registry=type.getMethod("get").invoke(null);var dataRegistry=type.getField("blockData").get(registry);
            var providers=(List<?>)dataRegistry.getClass().getMethod("get",Object.class).invoke(dataRegistry,tile);
            check(providers.stream().map(entry->((IInstanceRegistry.Entry<?>)entry).instance()).anyMatch(p->p==WthitCompatibility.PROVIDER),"native WTHIT JSON discovery registered this target's actual provider");
            var data=new CompoundTag();data.putInt("other_mod",31);var writer=proxy(IDataWriter.class,key->key.equals("raw")?data:null);
            IServerAccessor<BlockEntity> accessor=proxy(IServerAccessor.class,key->key.equals("getTarget")?tile:key.equals("getWorld")?tile.getLevel():null);
            WthitCompatibility.PROVIDER.appendData(writer,accessor,null);
            check(data.equals(expected),"native WTHIT server provider returns exact shared payload");
        }catch(ReflectiveOperationException error) {throw new net.minecraft.gametest.framework.GameTestAssertException("WTHIT discovery check: "+error);}
    }
    private WthitTooltipChecks() {}
}
