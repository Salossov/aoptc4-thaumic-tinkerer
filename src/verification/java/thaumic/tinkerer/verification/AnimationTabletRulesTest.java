package thaumic.tinkerer.verification;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.crafting.*;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.block.*;
import thaumic.tinkerer.menu.AnimationTabletMenu;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class AnimationTabletRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void tick(ServerLevel level,BlockPos pos,int count){var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);BlockEntityTicker ticker=((EntityBlock)state.getBlock()).getTicker(level,state,tile.getType());for(int i=0;i<count;i++)ticker.tick(level,pos,state,tile);}
    private static Cow cow(ServerLevel level,BlockPos pos){var cow=new Cow(EntityType.COW,level);cow.setNoAi(true);cow.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(cow);return cow;}
    @GameTest(template="empty",timeoutTicks=1200)
    public static void animationTablet(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+220,(anchor.getZ()>>4)*16+8);
        var columns=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int x=(pos.getX()-20)>>4;x<=(pos.getX()+20)>>4;x++)for(int z=(pos.getZ()-20)>>4;z<=(pos.getZ()+20)>>4;z++)columns.add(new net.minecraft.world.level.ChunkPos(x,z));
        var loader=new DislocationServerRulesTest.Peer(helper,"TTTabletOriginalLoader");var request=thaumcraft.api.world.ChunkPreparationApi.prepare(loader.player,columns);
        var cleanup=new Runnable(){boolean closed;public void run(){if(closed)return;closed=true;request.close();loader.close();}};
        helper.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo test){}public void testPassed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testFailed(GameTestInfo test,GameTestRunner runner){cleanup.run();}public void testAddedForRerun(GameTestInfo old,GameTestInfo next,GameTestRunner runner){}});
        helper.onEachTick(new Runnable(){boolean done;public void run(){if(done||!request.advance(2))return;done=true;try{runAnimationTablet(helper);}catch(Exception failure){cleanup.run();throw new RuntimeException(failure);}}});
    }
    private static void runAnimationTablet(GameTestHelper helper)throws Exception{
        VerificationReports.install();checks=0;var level=helper.getLevel();var anchor=helper.absolutePos(BlockPos.ZERO);var pos=new BlockPos((anchor.getX()>>4)*16+8,anchor.getY()+220,(anchor.getZ()>>4)*16+8);var front=pos.north();var fake=FocusRulesTest.player(helper,"[TTTablet]");
        var messages=new ArrayList<Component>();
        var player=new ServerPlayer(level.getServer(),level,fake.getGameProfile(),ClientInformation.createDefault()){
            @Override public BlockPos adjustSpawnLocation(ServerLevel world,BlockPos spawn){return spawn;}
            // FakePlayer's inert listener handles ordinary menu packets, but has no Netty channel for the two-argument system-chat send path.
            @Override public void displayClientMessage(Component message,boolean actionBar){messages.add(message.copy());}
        };player.connection=fake.connection;player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        // Native ContainerData synchronization queries negotiated channel attributes even for vanilla data packets.
        var channel=new io.netty.channel.embedded.EmbeddedChannel(player.connection.getConnection());
        var changed=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(pos.offset(-3,-1,-3),pos.offset(3,3,3)))changed.add(p.immutable());var entities=new ArrayList<Entity>();
        Runnable cleanup=()->{player.closeContainer();player.getInventory().clearContent();entities.forEach(Entity::discard);for(var p:changed){if(level.getBlockEntity(p) instanceof Container c)c.clearContent();level.setBlock(p,Blocks.AIR.defaultBlockState(),2);}level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(6)).forEach(Entity::discard);channel.finishAndReleaseAll();};
        try{
            for(var p:changed)level.setBlock(p,Blocks.AIR.defaultBlockState(),2);var state=AnimationTabletContent.BLOCK.get().defaultBlockState();level.setBlock(pos,state,3);var tile=(AnimationTabletBlockEntity)level.getBlockEntity(pos);
            check(tile!=null&&tile.leftClick&&!tile.redstone&&tile.swingProgress==0&&tile.getContainerSize()==1&&tile.getMaxStackSize()==64,"original defaults and unrestricted single 64-stack slot");
            check(state.getDestroySpeed(level,pos)==3&&state.getBlock().getExplosionResistance()==30&&!state.canOcclude()&&state.getCollisionShape(level,pos).bounds().maxY==.125,"original iron slab geometry and effective resistance");
            for(int i=0;i<4;i++){player.setYRot(i*90);var place=AnimationTabletContent.BLOCK.get().getStateForPlacement(new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(AnimationTabletContent.BLOCK.get()),new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));check(place.getValue(AnimationTabletBlock.FACING)==new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[i],"source placement yaw quadrant "+i);}
            for(var side:Direction.values()){var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,side);var input=new ItemStack(Items.STONE,64);check(handler!=null&&handler.getSlots()==1&&handler.insertItem(0,input,false).isEmpty()&&tile.getItem(0).getCount()==64&&handler.extractItem(0,64,false).getCount()==64,"unrestricted native automation "+side);}
            var tabletPlayer=tile.player();tabletPlayer.prepare();check(tabletPlayer.getUUID().toString().equals("a8f026a0-135b-11e4-9191-0800200c9a66")&&tabletPlayer.getGameProfile().getName().equals("[ThaumcraftTablet]"),"original unowned fixed fake-player identity");
            check(tabletPlayer.getX()==pos.getX()+.5&&tabletPlayer.getY()==pos.getY()+1.6-(double)1.62f&&tabletPlayer.legacyRawY()==pos.getY()+1.6&&tabletPlayer.getZ()==pos.getZ()+.5&&tabletPlayer.getYRot()==180&&tabletPlayer.getXRot()==-15&&!tabletPlayer.isCreative(),"source raw Y translated to modern feet; orientation and survival reset");
            check(Math.abs(tabletPlayer.getEyeY()-(pos.getY()+1.6))<1e-12,"native tablet eye retains original physical height instead of adding another 1.62");
            tabletPlayer.getInventory().setItem(0,new ItemStack(Items.GOLD_INGOT));tabletPlayer.getInventory().setItem(1,new ItemStack(Items.IRON_INGOT));tabletPlayer.prepare();
            check(tabletPlayer.getInventory().isEmpty()&&level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).stream().anyMatch(e->e.getItem().is(Items.IRON_INGOT))&&level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).stream().noneMatch(e->e.getItem().is(Items.GOLD_INGOT)),"source drops nonselected inventory then clears held slot");level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).forEach(Entity::discard);
            for(int i=0;i<4;i++){tile.rotation(i);check(tile.target().equals(pos.relative(AnimationTabletBlock.DIRECTIONS[i]))&&tile.reportedRotation()==i,"source target direction "+i);}tile.rotation(0);tile.redstone=true;
            check(!tile.detect(),"empty adjacent cube does not trigger automatic mode");level.setBlock(front,Blocks.STONE.defaultBlockState(),2);check(tile.detect(),"solid target detected");
            tile.setItem(0,new ItemStack(Items.IRON_PICKAXE));tile.initiateSwing();check(tile.swingProgress==1,"source swing starts at one degree");tick(level,pos,14);check(tile.swingProgress==43&&!tile.isBreaking(),"fourteen ticks reach 43 degrees before hit");tick(level,pos,1);check(tile.swingProgress==46&&!tile.isBreaking(),"fifteenth tick reaches 46 without early hit");tick(level,pos,1);check(tile.swingProgress==43&&tile.isBreaking(),"sixteenth tick hits and reverses by three degrees");
            check(tabletPlayer.getMainHandItem().is(Items.IRON_PICKAXE),"held item installed on swing hit");tick(level,pos,1);check(tabletPlayer.getMainHandItem().isEmpty()&&tile.isBreaking(),"source fake held slot is empty between hits while block breaking continues");
            tile.stopBreaking();tile.swingProgress=0;Consumer<PlayerInteractEvent.LeftClickBlock> cancel=e->{if(e.getEntity()==tabletPlayer)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(cancel);try{tile.findEntities();tile.swingHit();check(!tile.isBreaking()&&level.getBlockState(front).is(Blocks.STONE),"source left-click event cancellation stops mining");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
            tile.setItem(0,new ItemStack(Items.IRON_PICKAXE));tile.swingProgress=0;tile.initiateSwing();tick(level,pos,1);var saved=tile.saveWithFullMetadata(level.registryAccess());var loaded=(AnimationTabletBlockEntity)BlockEntity.loadStatic(pos,state,saved,level.registryAccess());loaded.setLevel(level);AnimationTabletBlockEntity.tick(level,pos,state,loaded);
            check(saved.getInt("mod")==3&&loaded.swingProgress==4&&!loaded.isBreaking()&&loaded.getItem(0).is(Items.IRON_PICKAXE),"source saves swing mod but does not restore it, preserving stalled loaded swing");
            var update=tile.getUpdateTag(level.registryAccess());check(!update.contains("progress")&&!update.contains("mod")&&update.contains("leftClick")&&update.contains("redstone"),"native sync retains source custom fields excluding swing progress/mod");check(loaded.triggerEvent(0,0)&&loaded.swingProgress==1&&!loaded.triggerEvent(1,0),"source block event zero restarts animation");
            tile.swingProgress=0;tile.stopBreaking();level.setBlock(front,Blocks.DIRT.defaultBlockState(),2);tile.setItem(0,new ItemStack(Items.IRON_PICKAXE));tile.findEntities();tile.swingHit();tick(level,pos,80);
            check(level.getBlockState(front).isAir()&&tile.getItem(0).getDamageValue()==1,"actual source airborne mining continuation destroys dirt and wears stored tool: state="+level.getBlockState(front)+", wear="+tile.getItem(0).getDamageValue());
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).forEach(Entity::discard);
            var victim=cow(level,front);entities.add(victim);tile.setItem(0,new ItemStack(Items.IRON_SWORD));tile.swingProgress=0;tile.leftClick=true;tile.findEntities();tabletPlayer.prepare();tile.swingHit();
            check(Math.abs(victim.getHealth()-4)<.001&&tile.getItem(0).getDamageValue()==1,"actual native attack fully charged, no introduced cooldown reduction, sword wear: health="+victim.getHealth()+", wear="+tile.getItem(0).getDamageValue()+", damage="+tabletPlayer.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));victim.discard();
            level.setBlock(front,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),2);level.setBlock(front.below(),Blocks.STONE.defaultBlockState(),2);tile.leftClick=false;tile.setItem(0,new ItemStack(ClothTestMod.TABLET_PROBE));tile.findEntities();ClothTestMod.TabletProbe.calls.clear();ClothTestMod.TabletProbe.replaceHand=false;tile.swingHit();
            check(level.getBlockState(front).is(Blocks.LEVER)&&level.getBlockState(front).getValue(LeverBlock.POWERED)&&ClothTestMod.TabletProbe.calls.equals(List.of("first")),"source ignores successful item-first result, then real block activation stops later item callbacks");
            level.setBlock(front,Blocks.STONE.defaultBlockState(),2);tile.setItem(0,new ItemStack(ClothTestMod.TABLET_PROBE));ClothTestMod.TabletProbe.calls.clear();tile.findEntities();tile.swingHit();
            check(ClothTestMod.TabletProbe.calls.equals(List.of("first","useOn","use"))&&tile.getItem(0).is(ClothTestMod.TABLET_PROBE),"source ordered item callbacks and ignored right-click replacement return");
            ClothTestMod.TabletProbe.replaceHand=true;tile.swingHit();check(tile.getItem(0).is(Items.EMERALD),"actual fake-player held replacement stored instead of method return diamond");ClothTestMod.TabletProbe.replaceHand=false;
            level.setBlock(front,Blocks.AIR.defaultBlockState(),2);var milkCow=cow(level,front);entities.add(milkCow);tile.setItem(0,new ItemStack(Items.BUCKET));tile.findEntities();tile.swingHit();check(tile.getItem(0).is(Items.MILK_BUCKET),"actual native cow interaction replaces held bucket");milkCow.discard();
            var feedCow=cow(level,front);entities.add(feedCow);tile.setItem(0,new ItemStack(Items.WHEAT,2));tile.findEntities();tile.swingHit();check(feedCow.isInLove()&&tile.getItem(0).getCount()==1,"actual native animal feeding consumes one wheat in survival mode");feedCow.discard();
            tile.swingProgress=0;var menu=new AnimationTabletMenu(80,player.getInventory(),tile);check(menu.slots.size()==37&&menu.getSlot(0).x==80&&menu.getSlot(0).y==15&&menu.getSlot(1).x==8&&menu.getSlot(28).y==142,"original tablet menu coordinates");tile.clearContent();player.getInventory().setItem(9,new ItemStack(Items.COBBLESTONE,64));check(menu.quickMoveStack(player,1).getCount()==64&&tile.getItem(0).getCount()==64,"native menu shift-click accepts a full arbitrary stack");
            check(menu.clickMenuButton(player,1)&&tile.leftClick&&menu.clickMenuButton(player,2)&&!tile.leftClick&&!menu.clickMenuButton(player,3),"native menu radio actions and invalid button");boolean before=tile.redstone;check(menu.clickMenuButton(player,0)&&tile.redstone!=before,"native menu redstone toggle");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:casting_wand"))));tile.stopBreaking();tile.swingProgress=0;level.setBlock(pos,tile.getBlockState().setValue(AnimationTabletBlock.POWERED,false),2);tile.rotation(0);var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);tile.getBlockState().useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,hit);
            check(tile.rotation()==1,"actual wand rotates in source N,S,W,E order");tile.initiateSwing();tile.getBlockState().useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,hit);check(tile.rotation()==1&&!messages.isEmpty(),"wand cannot rotate a swinging tablet and reports the source message");
            tile.clearContent();tile.swingProgress=0;player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);tile.getBlockState().useWithoutItem(level,player,hit);check(player.containerMenu instanceof AnimationTabletMenu,"actual native ServerPlayer opens tablet menu");
            var research=AddonRegistry.research().get("thaumictinkerer:animation_tablet");check(research.warp()==1&&research.complexity()==3&&research.column()==-8&&research.row()==2&&research.parents().equals(List.of("thaumictinkerer:magnets"))&&!research.secondary()&&research.hiddenParents().isEmpty(),"source tablet research values and warp");
            var recipe=(IArcaneRecipe)level.getRecipeManager().byKey(ThaumicTinkerer.id("animation_tablet")).orElseThrow().value();
            var blank=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:golem_core_blank")));
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,2,List.of(new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.IRON_INGOT),new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.IRON_INGOT),blank,new ItemStack(Items.IRON_INGOT)));
            check(recipe.matches(input,level)&&recipe.research().equals("thaumictinkerer:animation_tablet")&&recipe.vis(input).equals(Map.of("aer",25,"ordo",15,"ignis",10))&&recipe.assemble(input,level.registryAccess()).is(AnimationTabletContent.BLOCK.get().asItem()),"actual arcane recipe preserves source pattern, blank core metadata 100 and vis costs");
            var wrong=new ArrayList<>(input.items());wrong.set(4,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:golem_core_fill"))));check(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3,2,wrong),level),"filled core metadata zero cannot replace original blank core");
            check(BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.parse("thaumcraft:tool")).isPresent(),"native core registers the existing original rotation sound");
            if(GolemConnectorContent.enabled())AnimationTabletPeripheralChecks.run(tile);else check(BuiltInRegistries.BLOCK.containsKey(ThaumicTinkerer.id("animation_tablet")),"tablet remains registered without ComputerCraft");
            tile.rotation(0);tile.redstone=true;tile.leftClick=true;tile.stopBreaking();tile.swingProgress=0;tile.clearContent();
            var abovePower=pos.above().east();level.setBlock(abovePower,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);level.neighborChanged(pos,Blocks.REDSTONE_BLOCK,pos.above());
            check(tile.getBlockState().getValue(AnimationTabletBlock.POWERED)&&tile.reportedRotation()==8,"source power at block above latches powered metadata");
            level.setBlock(abovePower,Blocks.AIR.defaultBlockState(),3);level.neighborChanged(pos,Blocks.AIR,pos.above());
            // Flush block events/scheduled ticks created by direct source-state tests before proving the real rising edge.
            helper.runAfterDelay(3,()->{try{
                check(tile.swingProgress==0&&tile.isEmpty()&&!tile.getBlockState().getValue(AnimationTabletBlock.POWERED),"prior events drained with no tool before isolated native redstone scenario");
                tile.setItem(0,new ItemStack(Items.IRON_SWORD));var naturalCow=cow(level,front);entities.add(naturalCow);level.setBlock(front.below(),Blocks.STONE.defaultBlockState(),2);var poweredPos=pos.east();level.setBlock(poweredPos,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
                check(tile.getBlockState().getValue(AnimationTabletBlock.POWERED),"actual adjacent power placement raises source latch without a manual interaction");
                helper.runAfterDelay(22,()->{try{
                check(Math.abs(naturalCow.getHealth()-4)<.001&&tile.getItem(0).getDamageValue()==1,"native redstone rising edge schedules actual world swing and attack");
                level.setBlock(poweredPos,Blocks.AIR.defaultBlockState(),3);level.neighborChanged(pos,Blocks.AIR,pos.above());check(!tile.getBlockState().getValue(AnimationTabletBlock.POWERED),"falling edge clears power latch");
                tile.getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal("Tablet sword"));level.destroyBlock(pos,true,player);var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));check(drops.stream().anyMatch(e->e.getItem().is(AnimationTabletContent.BLOCK.get().asItem()))&&drops.stream().anyMatch(e->e.getItem().is(Items.IRON_SWORD)&&e.getItem().getHoverName().getString().equals("Tablet sword")&&e.getItem().getDamageValue()==1),"breaking preserves actual stored tool components and device drop");
                System.out.println("TT_ANIMATION_TABLET_PASS checks="+checks);helper.succeed();
                }finally{cleanup.run();}});
            }catch(Throwable failure){cleanup.run();throw failure;}});
        }catch(Throwable failure){failure.printStackTrace();cleanup.run();throw failure;}
    }
}
