package thaumic.tinkerer.verification;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumic.tinkerer.ThaumicTinkerer;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class InfernalNozzleRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(ServerLevel level,BlockPos pos,int times){
        var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);
        BlockEntityTicker ticker=((EntityBlock)state.getBlock()).getTicker(level,state,tile.getType());
        for(int i=0;i<times;i++)ticker.tick(level,pos,state,tile);
    }
    private static int boost(ServerLevel level,BlockEntity core){return core.saveWithFullMetadata(level.registryAccess()).getInt("SpeedyTime");}
    private static void boost(ServerLevel level,BlockEntity core,int amount,int cooking){
        var tag=core.saveWithFullMetadata(level.registryAccess());tag.putInt("SpeedyTime",amount);tag.putInt("CookTime",cooking);core.loadWithComponents(tag,level.registryAccess());
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void infernalNozzles(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var changed=new ArrayList<BlockPos>();
        var center=helper.absolutePos(new BlockPos(4,210,4));
        // This high fixture can extend beyond the template's forced chunks.
        // A newly added entity there can be hidden from the active entity query.
        var spawned=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> joined=event->{
            if(event.getLevel()==level&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item
                    &&new net.minecraft.world.phys.AABB(center).inflate(3).contains(item.position()))spawned.add(item);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(joined);
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:infernal_furnace"));
        var part=(IntegerProperty)block.defaultBlockState().getProperties().stream().filter(p->p.getName().equals("part")).findFirst().orElseThrow();
        try{
            for(var facing:Direction.Plane.HORIZONTAL){
                int nozzles=0;
                for(int i=0;i<27;i++){
                    var pos=center.offset(i%3-1,i/9-1,i/3%3-1);if(!changed.contains(pos))changed.add(pos);
                    level.setBlock(pos,i==22?Blocks.AIR.defaultBlockState():block.defaultBlockState().setValue(part,i).setValue(BlockStateProperties.HORIZONTAL_FACING,facing),2);
                    if(level.getCapability(EssentiaCapabilities.TRANSPORT,pos,Direction.UP)!=null)nozzles++;
                }
                check(nozzles==12,"exact twelve original metadata nozzle tiles for facing "+facing);
                check(level.getCapability(EssentiaCapabilities.TRANSPORT,center.relative(facing),Direction.UP)==null,"output bars do not gain a nozzle for "+facing);
            }
            var core=level.getBlockEntity(center);var nozzlePos=center.below();var nozzle=(IEssentiaTransport)level.getBlockEntity(nozzlePos);
            check(level.getCapability(EssentiaCapabilities.TRANSPORT,center,Direction.UP)==null,"lava core has no essentia transport capability");
            check(nozzle.isConnectable(null)&&nozzle.canInputFrom(Direction.NORTH)&&nozzle.getSuctionAmount(Direction.UP)==0,"unresolved original UNKNOWN nozzle accepts faces without suction");
            var source=center.below(2);changed.add(source);
            level.setBlock(source,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:warded_jar")).defaultBlockState(),2);
            var supply=(IAspectContainer)level.getBlockEntity(source);supply.addToContainer(Aspects.FIRE,4);
            boost(level,core,0,0);tick(level,nozzlePos,4);
            check(boost(level,core)==0&&supply.getAspects().getAmount(Aspects.FIRE)==4,"first four low-boost updates consume nothing");
            check(nozzle.getSuctionType(null)==Aspects.FIRE&&nozzle.getSuctionAmount(null)==128&&nozzle.canInputFrom(Direction.UP),"resolved nozzle has original 128 ignis suction on every queried face");
            tick(level,nozzlePos,1);
            check(boost(level,core)==600&&supply.getAspects().getAmount(Aspects.FIRE)==3,"fifth update transfers exactly one ignis into 600 actual furnace ticks");
            check(nozzle.getSuctionAmount(null)==0&&!nozzle.canOutputTo(null)&&nozzle.getEssentiaType(null)==null&&nozzle.getEssentiaAmount(null)==0,"boosted nozzle neither suctions nor stores or outputs essentia");
            check(nozzle.addEssentia(Aspects.FIRE,1,null)==0&&nozzle.takeEssentia(Aspects.FIRE,1,null)==0&&nozzle.getMinimumSuction()==0&&!nozzle.renderExtendedTube(),"original zero injection/extraction/minimum suction and tube rendering");
            tick(level,center,1);check(boost(level,core)==600,"idle furnace keeps all nozzle acceleration");
            boost(level,core,600,10);tick(level,center,1);check(boost(level,core)==599,"actual cooking decrements the shared original acceleration counter");
            boost(level,core,60,0);tick(level,nozzlePos,4);boost(level,core,39,0);tick(level,nozzlePos,4);
            check(boost(level,core)==39,"boost of 60 pauses the five-call draw delay");tick(level,nozzlePos,1);
            check(boost(level,core)==639&&supply.getAspects().getAmount(Aspects.FIRE)==2,"boost below 40 can refill by 600 rather than replacing the remainder");
            boost(level,core,40,0);tick(level,nozzlePos,5);
            check(boost(level,core)==40&&nozzle.getSuctionAmount(null)==0&&supply.getAspects().getAmount(Aspects.FIRE)==2,"40-to-59 window attempts a draw but has zero suction");
            level.setBlock(source,BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:essentia_tube")).defaultBlockState(),2);
            var tube=(IEssentiaTransport)level.getBlockEntity(source);var tubeContents=(IAspectContainer)tube;tubeContents.addToContainer(Aspects.FIRE,1);tube.setSuction(Aspects.FIRE,128);
            boost(level,core,0,0);tick(level,nozzlePos,5);
            check(boost(level,core)==0&&tubeContents.getAspects().getAmount(Aspects.FIRE)==1,"equal suction cannot steal essentia from the source");
            tube.setSuction(Aspects.FIRE,127);tick(level,nozzlePos,5);
            check(boost(level,core)==600&&tubeContents.getAspects().getAmount(Aspects.FIRE)==0,"strictly lower source suction permits the original transfer");
            var inventory=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,center,Direction.UP);
            check(inventory!=null&&inventory.insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON),false).isEmpty(),"actual core inventory accepts a smeltable input");
            boost(level,core,600,0);tick(level,center,1);
            check(core.saveWithFullMetadata(level.registryAccess()).getInt("CookTime")==80&&boost(level,core)==600,"one ignis selects the original 80-tick cooking duration");
            tick(level,center,80);
            check(inventory.getStackInSlot(0).isEmpty()&&boost(level,core)==520,"one real smelt consumes 80 boosted cooking ticks");
            var output=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(3));
            check(spawned.stream().anyMatch(item->item.isAddedToLevel()&&!item.isRemoved()&&item.getItem().is(net.minecraft.world.item.Items.IRON_INGOT)),"native furnace adds the actual smelting output to its entity manager");
            System.out.println("TT_INFERNAL_OUTPUT_NATIVE added="+spawned.size()+" active_query="+output.size()+" fixture_chunk="+level.getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(new net.minecraft.world.level.ChunkPos(center).toLong()));
            spawned.forEach(net.minecraft.world.entity.Entity::discard);
            inventory.insertItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON),false);boost(level,core,0,0);tick(level,center,1);
            check(core.saveWithFullMetadata(level.registryAccess()).getInt("CookTime")==140,"without essentia or a VisNet source cooking uses the original 140 ticks");
            var dormant=center.offset(0,-1,-1);var dormantTransport=level.getCapability(EssentiaCapabilities.TRANSPORT,dormant,Direction.UP);
            tick(level,dormant,1);check(dormantTransport!=null&&!dormantTransport.isConnectable(null)&&!dormantTransport.canInputFrom(Direction.DOWN),"nonadjacent shell nozzles become disconnected after their first lookup");
            var saved=core.saveWithFullMetadata(level.registryAccess());saved.remove("SpeedyTime");saved.putInt("Boost",123);core.loadWithComponents(saved,level.registryAccess());
            check(boost(level,core)==123,"existing modern Boost saves migrate without losing acceleration");
            saved=core.saveWithFullMetadata(level.registryAccess());saved.putInt("SpeedyTime",321);saved.putInt("Boost",999);core.loadWithComponents(saved,level.registryAccess());
            check(boost(level,core)==321,"original SpeedyTime has precedence and survives an actual NBT reload");
            System.out.println("TT_INFERNAL_NOZZLES_PASS checks="+checks);helper.succeed();
        }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(joined);spawned.forEach(net.minecraft.world.entity.Entity::discard);for(var pos:changed)level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);}
    }
}
