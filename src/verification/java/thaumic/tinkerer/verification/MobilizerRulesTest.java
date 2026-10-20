package thaumic.tinkerer.verification;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.*;
import thaumcraft.api.world.IMovableBlockEntity;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class MobilizerRulesTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static MobilizerBlockEntity carrier(ServerLevel level,BlockPos pos){
        var first=pos.west(5);var second=pos.east(5);
        level.setBlock(first,MobilizerContent.RELAY.get().defaultBlockState(),2);level.setBlock(second,MobilizerContent.RELAY.get().defaultBlockState(),2);
        var a=(MobilizerRelayBlockEntity)level.getBlockEntity(first);var b=(MobilizerRelayBlockEntity)level.getBlockEntity(second);
        a.hasPartner=b.hasPartner=true;a.partnerX=second.getX();a.partnerZ=second.getZ();b.partnerX=first.getX();b.partnerZ=first.getZ();
        level.setBlock(pos,MobilizerContent.LOCOMOTIVE.get().defaultBlockState(),2);var tile=(MobilizerBlockEntity)level.getBlockEntity(pos);
        tile.linked=true;tile.firstRelayX=first.getX();tile.firstRelayZ=first.getZ();tile.secondRelayX=second.getX();tile.secondRelayZ=second.getZ();tile.movementDirection=Direction.EAST;return tile;
    }
    /** Test-only passenger proves that the legacy fallback ignores false, while keeping callback order. */
    private static final class MarkedChest extends ChestBlockEntity implements IMovableBlockEntity {
        int prepared,completed;BlockPos before,after;
        MarkedChest(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){super(pos,state);}
        @Override public boolean prepareToMove(){prepared++;before=getBlockPos();return false;}
        @Override public void doneMoving(){completed++;after=getBlockPos();}
    }
    @GameTest(template="empty",batch="mobilizer",timeoutTicks=400)
    public static void relaySearchOrder(GameTestHelper helper){
        VerificationReports.install();var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var origin=new BlockPos((anchor.getX()>>4)*16+8,Math.min(anchor.getY()+325,level.getMaxBuildHeight()-12),(anchor.getZ()>>4)*16+8);
        level.setBlock(origin,MobilizerContent.RELAY.get().defaultBlockState(),2);var owner=(MobilizerRelayBlockEntity)level.getBlockEntity(origin);
        owner.hasPartner=true;owner.partnerX=origin.getX();owner.partnerZ=origin.getZ();
        var columns=new LinkedHashSet<net.minecraft.world.level.ChunkPos>();for(int offset=-32;offset<32;offset++)if(offset!=0){columns.add(new net.minecraft.world.level.ChunkPos(origin.offset(0,0,offset)));columns.add(new net.minecraft.world.level.ChunkPos(origin.offset(offset,0,0)));}
        var preparation=thaumcraft.api.world.ChunkPreparationApi.prepare(owner,columns);
        var candidates=List.of(origin.north(32),origin.west(32),origin.south(31),origin.east(31),origin.east(32));boolean[] done={false};
        helper.onEachTick(()->{if(done[0])return;try{
            helper.assertTrue(!preparation.closed(),"registered ticking relay retains pending operation");if(!preparation.advance(2))return;done[0]=true;
            for(var pos:candidates){level.setBlock(pos,MobilizerContent.RELAY.get().defaultBlockState(),2);var relay=(MobilizerRelayBlockEntity)level.getBlockEntity(pos);relay.hasPartner=true;relay.partnerX=pos.getX();relay.partnerZ=pos.getZ();}
            owner.hasPartner=false;helper.assertTrue(owner.checkForPartner(),"fully prepared source search completes immediately");
            helper.assertTrue(owner.hasPartner&&owner.partnerX==origin.getX()+31&&owner.partnerZ==origin.getZ(),"source scans -32..31, Z then X, retaining last match");
            for(int i=0;i<4;i++){var candidate=(MobilizerRelayBlockEntity)level.getBlockEntity(candidates.get(i));helper.assertTrue(candidate.hasPartner&&candidate.partnerX==origin.getX()&&candidate.partnerZ==origin.getZ(),"source continues scanning and overwrites each encountered relay, including earlier matches "+i);}
            var excluded=(MobilizerRelayBlockEntity)level.getBlockEntity(candidates.get(4));helper.assertTrue(excluded.partnerX==excluded.getBlockPos().getX()&&excluded.partnerZ==excluded.getBlockPos().getZ(),"+32 endpoint is excluded even when a relay is already there");
            owner.verifyPartner();helper.assertTrue(owner.hasPartner,"last pair validates reciprocity");
            level.setBlock(origin.east(31),Blocks.AIR.defaultBlockState(),2);helper.assertTrue(owner.verifyPartner()&&!owner.hasPartner,"breaking partner clears the link after actual native lookup");
            System.out.println("TT_RELAY_SEARCH_PASS offsets=126 negative_endpoint=true positive_endpoint_excluded=true last_match=true");helper.succeed();
        }catch(Throwable failure){done[0]=true;throw failure;}finally{
            if(done[0]){preparation.close();for(var pos:candidates)if(MobilizerLoaded(level,pos))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(origin,Blocks.AIR.defaultBlockState(),2);}
        }});
    }
    private static boolean MobilizerLoaded(ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    @GameTest(template="empty",batch="mobilizer",timeoutTicks=230)
    public static void locomotiveNativePhases(GameTestHelper helper){
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);
        var center=new BlockPos((anchor.getX()>>4)*16+8,Math.min(anchor.getY()+310,level.getMaxBuildHeight()-20),(anchor.getZ()>>4)*16+8);
        var positions=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(center.offset(-6,-1,-5),center.offset(6,6,5)))positions.add(pos.immutable());
        Runnable cleanup=()->{for(var pos:positions){if(level.getBlockEntity(pos) instanceof Container c)c.clearContent();level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10)).forEach(ItemEntity::discard);};
        try{
            var infusion=(InfusionRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("levitational_locomotive")).orElseThrow().value();
            check(infusion.instability()==4&&infusion.aspects().equals(Map.of("motus",15,"ordo",20,"praecantatio",15))&&infusion.input().test(item("thaumcraft:levitator")),"native infusion keeps source instability, aspects and lifter center");
            var components=List.of("minecraft:iron_ingot","minecraft:feather","minecraft:iron_ingot","thaumcraft:obsidian_totem");check(infusion.components().size()==4,"four original infusion components");for(int i=0;i<4;i++)check(infusion.components().get(i).test(item(components.get(i))),"component order including cosmeticSolid metadata 1 totem "+i);
            var arcane=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("levitational_locomotive_relay")).orElseThrow().value();
            var input=CraftingInput.of(3,3,List.of(item("thaumcraft:greatwood_log"),new ItemStack(Items.GLASS),item("thaumcraft:greatwood_log"),item("thaumcraft:air_shard"),new ItemStack(Items.IRON_INGOT),item("thaumcraft:earth_shard"),item("thaumcraft:greatwood_log"),new ItemStack(Items.GLASS),item("thaumcraft:greatwood_log")));
            check(arcane.matches(input,level)&&arcane.vis(input).equals(Map.of("aer",20,"ordo",5,"terra",15))&&arcane.assemble(input,level.registryAccess()).is(MobilizerContent.RELAY.get().asItem()),"actual relay arcane pattern and vis");
            var research=AddonRegistry.research().get("thaumictinkerer:levitator");check(research!=null&&research.complexity()==3&&research.secondary()&&research.column()==-7&&research.row()==5&&research.parents().equals(List.of("thaumictinkerer:magnets"))&&research.hiddenParents().equals(List.of("INFUSION")),"native book registers original location, parent, secondary flag and infusion requirement");
            var fresh=new MobilizerBlockEntity(center,MobilizerContent.LOCOMOTIVE.get().defaultBlockState());var tag=fresh.saveWithoutMetadata(level.registryAccess());check(tag.getInt("Direction")==0,"original unspecialized direction saves zero");fresh.loadWithComponents(tag,level.registryAccess());check(fresh.movementDirection==Direction.DOWN&&!fresh.linked,"original saved null direction loads DOWN");
            var relay=new MobilizerRelayBlockEntity(center,MobilizerContent.RELAY.get().defaultBlockState());var visited=new ArrayList<BlockPos>();relay.partnerX=center.getX()+3;relay.partnerZ=center.getZ();relay.visitRoute(visited::add);check(visited.equals(List.of(center,center.east(),center.east(2))),"positive route excludes endpoint");visited.clear();relay.partnerX=center.getX()-3;relay.visitRoute(visited::add);check(visited.equals(List.of(center)),"source negative do/while visits only starting cell");
            var relayTag=new CompoundTag();relayTag.putBoolean("HasPartner",true);relayTag.putInt("PartnerX",13);relayTag.putInt("PartnerZ",-7);relay.loadWithComponents(relayTag,level.registryAccess());var saved=relay.saveWithoutMetadata(level.registryAccess());check(saved.getBoolean("HasPartner")&&saved.getInt("PartnerX")==13&&saved.getInt("PartnerZ")==-7,"source relay NBT keys round trip");
            var far=new BlockPos(16777216,center.getY(),16777216);var farRelay=new MobilizerRelayBlockEntity(far,MobilizerContent.RELAY.get().defaultBlockState());farRelay.partnerX=far.getX()+10;farRelay.partnerZ=far.getZ();visited.clear();farRelay.visitRoute(visited::add);check(visited.size()==10&&visited.getFirst().equals(far)&&visited.getLast().equals(far.east(9)),"approved integer step advances past float precision boundary without changing endpoints");
            var player=FocusRulesTest.player(helper,"[TTLocomotive]");player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);check(!player.hasCorrectToolForDrops(MobilizerContent.LOCOMOTIVE.get().defaultBlockState()),"source iron material requires a harvesting tool");player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));check(player.hasCorrectToolForDrops(MobilizerContent.LOCOMOTIVE.get().defaultBlockState())&&player.hasCorrectToolForDrops(MobilizerContent.RELAY.get().defaultBlockState()),"source generic iron material accepts even wooden pickaxe");
            long delay=Math.floorMod(2-level.getGameTime(),100);if(delay==0)delay=100;
            helper.runAfterDelay(delay,()->{try{
                cleanup.run();
                var normalPos=center.north(4);var blockedPos=center.north(2);var bedrockPos=center;var unsupportedPos=center.south(2);var torchPos=center.south(4);var callbackPos=center.above(4).north(4);var poweredPos=center.above(4).north(2);
                var normal=carrier(level,normalPos);var blocked=carrier(level,blockedPos);var bedrock=carrier(level,bedrockPos);var unsupported=carrier(level,unsupportedPos);var torch=carrier(level,torchPos);var callbacks=carrier(level,callbackPos);var powered=carrier(level,poweredPos);
                level.setBlock(normalPos.above(),Blocks.CHEST.defaultBlockState(),2);var chest=(ChestBlockEntity)level.getBlockEntity(normalPos.above());var diamonds=new ItemStack(Items.DIAMOND,7);chest.setItem(0,diamonds);
                level.setBlock(blockedPos.east().above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),2);level.setBlock(blockedPos.west().above(),Blocks.GOLD_BLOCK.defaultBlockState(),2);
                level.setBlock(bedrockPos.above(),Blocks.BEDROCK.defaultBlockState(),2);
                level.setBlock(unsupportedPos.above(),MobilizerContent.LOCOMOTIVE.get().defaultBlockState(),2);var unmarked=level.getBlockEntity(unsupportedPos.above());check(!MobilizerBlockEntity.movable(unmarked),"carrier itself has no source passenger opt-in");
                level.setBlock(torchPos.west().above(),Blocks.STONE.defaultBlockState(),2);var wallTorch=Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,Direction.EAST);level.setBlock(torchPos.above(),wallTorch,2);check(wallTorch.canSurvive(level,torchPos.above()),"wall torch has valid source support");
                level.setBlock(callbackPos.above(),Blocks.CHEST.defaultBlockState(),2);var marked=new MarkedChest(callbackPos.above(),level.getBlockState(callbackPos.above()));level.setBlockEntity(marked);var markedStack=new ItemStack(Items.EMERALD,3);marked.setItem(0,markedStack);
                level.setBlock(poweredPos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
                helper.runAfterDelay(102,()->{try{
                    check(level.getBlockEntity(normalPos.east())==normal&&normal.getBlockPos().equals(normalPos.east())&&!normal.dead,"real phase-1 ticker moves the same carrier without marking it destroyed");
                    boolean ae2=thaumic.tinkerer.integration.Ae2Movement.present();var movedChest=(ChestBlockEntity)level.getBlockEntity(normalPos.east().above());
                    check((ae2?movedChest!=chest&&chest.isRemoved()&&ItemStack.isSameItemSameComponents(movedChest.getItem(0),diamonds):movedChest==chest&&movedChest.getItem(0)==diamonds)&&movedChest.getItem(0).getCount()==7&&level.getBlockState(normalPos.above()).isAir(),"native chest retains contents through approved AE2 reconstruction or same-object fallback");
                    check(level.getEntitiesOfClass(ItemEntity.class,new AABB(normalPos).inflate(2)).isEmpty(),"passenger move does not spill chest inventory");
                    check(level.getBlockEntity(blockedPos)==blocked&&blocked.movementDirection==Direction.WEST&&level.getBlockState(blockedPos.east().above()).is(Blocks.DIAMOND_BLOCK)&&level.getBlockState(blockedPos.west().above()).is(Blocks.GOLD_BLOCK),"phase 0 reverses, approved phase-1 check keeps both occupied upper cells intact");
                    check(level.getBlockEntity(bedrockPos.east())==bedrock&&level.getBlockState(bedrockPos.above()).is(Blocks.BEDROCK)&&level.getBlockState(bedrockPos.east().above()).isAir(),"source bedrock passenger is left behind while locomotive moves");
                    check(level.getBlockEntity(unsupportedPos.east())==unsupported&&(ae2?level.getBlockState(unsupportedPos.above()).isAir()&&level.getBlockEntity(unsupportedPos.east().above()) instanceof MobilizerBlockEntity&&level.getBlockEntity(unsupportedPos.east().above())!=unmarked:level.getBlockEntity(unsupportedPos.above())==unmarked),"AE2's native registry moves additional passengers; no-AE2 fallback leaves unmarked addon passenger");
                    check(level.getBlockEntity(torchPos.east())==torch&&level.getBlockState(torchPos.above()).equals(wallTorch)&&level.getBlockState(torchPos.east().above()).isAir(),"approved modern facing-specific placement prevents unsupported wall-torch transfer");
                    var movedMarked=(ChestBlockEntity)level.getBlockEntity(callbackPos.east().above());
                    check(level.getBlockEntity(callbackPos.east())==callbacks&&(ae2?movedMarked!=marked&&movedMarked.getItem(0).getCount()==3&&movedMarked.getItem(0).is(Items.EMERALD):movedMarked==marked&&marked.getItem(0)==markedStack),"passenger uses native AE2 reconstruction or exact fallback stack identity");
                    check(ae2?marked.prepared==0&&marked.completed==0:marked.prepared==1&&marked.completed==1&&marked.before.equals(callbackPos.above())&&marked.after.equals(callbackPos.east().above()),"AE2 sees native CHEST type; legacy marker-only fallback ignores false and calls prepare/done once");
                    check(level.getBlockEntity(poweredPos)==powered&&powered.movementDirection==Direction.EAST,"native redstone power prevents reversal and movement");
                    var persisted=normal.saveWithFullMetadata(level.registryAccess());var loaded=(MobilizerBlockEntity)BlockEntity.loadStatic(normal.getBlockPos(),normal.getBlockState(),persisted,level.registryAccess());check(loaded.linked&&loaded.firstRelayX==normal.firstRelayX&&loaded.secondRelayX==normal.secondRelayX&&loaded.movementDirection==Direction.EAST,"moved carrier retains native link/direction NBT through real loader");
                    System.out.println("TT_MOBILIZER_PASS checks="+checks);helper.succeed();
                }finally{cleanup.run();}});
            }catch(Throwable failure){cleanup.run();throw failure;}});
        }catch(Throwable failure){cleanup.run();throw failure;}
    }
}
