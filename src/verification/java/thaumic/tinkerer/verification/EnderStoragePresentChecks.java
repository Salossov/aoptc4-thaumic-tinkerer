package thaumic.tinkerer.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.AddonRegistry;
import thaumcraft.api.aspects.Aspects;
import thaumcraft.api.wands.WandApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.item.FocusContent;
import thaumic.tinkerer.util.ItemData;

import static thaumic.tinkerer.verification.EnderStorageRulesTest.*;

final class EnderStoragePresentChecks {
    static void run(GameTestHelper helper){
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));
        var fixture=new NativePlayerFixture(helper,"TTEnderStorage",pos);var player=fixture.player;
        player.setPos(pos.getX()+.5,pos.getY()+.5-1.62,pos.getZ()-3.5);player.setXRot(0);player.setYRot(0);
        var item=FocusContent.ENDER_CHEST.get();var focus=new ItemStack(item);var wand=FocusRulesTest.wand(player,focus);
        var block=codechicken.enderstorage.init.EnderStorageModContent.ENDER_CHEST_BLOCK.get();
        level.setBlock(pos,block.defaultBlockState(),3);
        var chest=(codechicken.enderstorage.tile.TileEnderChest)level.getBlockEntity(pos);
        var colours=new codechicken.enderstorage.api.Frequency(codechicken.lib.colour.EnumColour.RED,codechicken.lib.colour.EnumColour.GREEN,codechicken.lib.colour.EnumColour.BLUE);
        chest.setFreq(colours);
        var manager=codechicken.enderstorage.manager.EnderStorageManager.instance(false);
        var publicStorage=manager.getStorage(colours,codechicken.enderstorage.storage.EnderItemStorage.TYPE);
        var privateFrequency=colours.withOwner(player);
        var privateStorage=manager.getStorage(privateFrequency,codechicken.enderstorage.storage.EnderItemStorage.TYPE);
        var foreignFrequency=new codechicken.enderstorage.api.Frequency(colours.left(),colours.middle(),colours.right(),java.util.Optional.of(java.util.UUID.randomUUID()),privateFrequency.ownerName());
        var foreignStorage=manager.getStorage(foreignFrequency,codechicken.enderstorage.storage.EnderItemStorage.TYPE);
        publicStorage.setItem(0,new ItemStack(Items.DIAMOND,3));privateStorage.setItem(0,new ItemStack(Items.EMERALD,7));foreignStorage.setItem(0,new ItemStack(Items.IRON_INGOT,5));
        int[] stage={0},ticks={0};boolean[] done={false};
        Runnable cleanup=()->{player.closeContainer();level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);publicStorage.clearStorage();privateStorage.clearStorage();foreignStorage.clearStorage();player.getEnderChestInventory().clearContent();fixture.close();};
        helper.onEachTick(()->{
            if(done[0]||ticks[0]++%10!=0)return;
            try{
                switch(stage[0]++){
                    case 0->{
                        player.getEnderChestInventory().setItem(0,new ItemStack(Items.APPLE,2));use(player,wand);
                        check(player.containerMenu instanceof ChestMenu&&player.containerMenu.getSlot(0).getItem().is(Items.APPLE),"unbound focus opens vanilla storage even while looking at an EnderStorage chest");
                        check(!WandApi.focus(wand).has(DataComponents.CUSTOM_DATA),"opening alone does not persist the original transient empty tag");player.closeContainer();
                    }
                    case 1->{
                        for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);player.setShiftKeyDown(true);use(player,wand);
                        var bound=WandApi.focus(wand);var tag=ItemData.read(bound);
                        check(tag.getBoolean("ender")&&tag.getInt("freq")==3803&&tag.getString("owner").equals("global"),"free public binding keeps exact original wool-metadata nibble order");
                        check(player.containerMenu==player.inventoryMenu&&WandApi.stored(wand,Aspects.ORDER)==0,"binding requires no vis and opens no menu");
                        check(item.getSortingHelper(bound).equals("ENDERCHEST3803global")&&!tooltip(bound).contains("global"),"original public sorting and suppressed global signature");
                        WandApi.focus(wand,roundTrip(player,bound));level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                    }
                    case 2->{
                        player.setShiftKeyDown(false);use(player,wand);check(player.containerMenu==player.inventoryMenu,"unaffordable bound cast opens no storage");
                        for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,1000);
                        var bound=WandApi.focus(wand);var cost=WandApi.quote(wand,player,item.getVisCost(bound));use(player,wand);
                        check(player.containerMenu instanceof codechicken.enderstorage.container.ContainerEnderItemStorage menu&&menu.chestInv==publicStorage,"serialized binding opens native EnderStorage menu after the source chest is removed");
                        check(player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND)&&player.containerMenu.getSlot(0).getItem().getCount()==3,"native public channel contains original shared items");
                        player.containerMenu.getSlot(0).set(new ItemStack(Items.GOLD_INGOT,2));
                        check(publicStorage.getItem(0).is(Items.GOLD_INGOT)&&WandApi.stored(wand,Aspects.ORDER)==1000-cost.get(Aspects.ORDER)&&WandApi.stored(wand,Aspects.ENTROPY)==1000-cost.get(Aspects.ENTROPY),"native menu writes shared storage and charges exactly once");player.closeContainer();
                    }
                    case 3->{
                        level.setBlock(pos,block.defaultBlockState(),3);((codechicken.enderstorage.tile.TileEnderChest)level.getBlockEntity(pos)).setFreq(privateFrequency);
                        for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);player.setShiftKeyDown(true);use(player,wand);
                        var bound=roundTrip(player,WandApi.focus(wand));bound.set(DataComponents.CUSTOM_NAME,Component.literal("TT Frequency Title"));WandApi.focus(wand,bound);
                        check(tooltip(bound).contains(player.getGameProfile().getName())&&item.getSortingHelper(bound).equals("ENDERCHEST3803"+player.getGameProfile().getName()),"private binding keeps original owner signature and sorting");
                        Files.createDirectories(SAVED_FOCUS.getParent());Files.writeString(SAVED_FOCUS,bound.save(player.registryAccess()).toString());
                    }
                    case 4->{
                        player.setShiftKeyDown(false);for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,1000);fixture.packets.clear();use(player,wand);
                        check(player.containerMenu instanceof codechicken.enderstorage.container.ContainerEnderItemStorage menu&&menu.chestInv==privateStorage&&menu.chestInv!=foreignStorage,"owner UUID selects the private channel even when another UUID has the same name and colours");
                        check(player.containerMenu.getSlot(0).getItem().is(Items.EMERALD)&&foreignStorage.getItem(0).is(Items.IRON_INGOT),"private channel remains separate from public and same-name owners");
                        var payload=fixture.packets.stream().filter(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet&&packet.payload() instanceof net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload).map(p->(net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload)((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)p).payload()).findFirst().orElseThrow();
                        check(payload.name().getString().equals("TT Frequency Title"),"native screen packet uses the actual focus display name");
                        var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload.additionalData()),player.registryAccess());
                        try{var input=new codechicken.lib.data.MCDataByteBuf(buf);check(codechicken.enderstorage.api.Frequency.readFromPacket(input).equals(privateFrequency)&&input.readByte()==privateStorage.getSize(),"native menu payload carries exact frequency, owner UUID and configured storage size");}finally{buf.release();}
                        player.closeContainer();
                    }
                    case 5->{
                        level.setBlock(pos,Blocks.OBSIDIAN.defaultBlockState(),3);player.setShiftKeyDown(true);for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,0);use(player,wand);
                        var bound=WandApi.focus(wand);var tag=ItemData.read(bound);
                        check(!tag.getBoolean("ender")&&tag.getInt("freq")==-1&&tag.getString("owner").equals(player.getGameProfile().getName())&&!tag.contains("enderstorage_frequency"),"free obsidian reset stores original vanilla marker and clears native binding");
                        check(item.getSortingHelper(bound).equals("ENDERCHEST-VANILLA")&&tooltip(bound).contains(player.getGameProfile().getName()),"vanilla reset preserves original owner tooltip even though it has no private frequency");
                    }
                    case 6->{
                        player.setShiftKeyDown(false);for(var aspect:Aspects.PRIMALS)WandApi.set(wand,aspect,1000);use(player,wand);
                        check(player.containerMenu instanceof ChestMenu&&player.containerMenu.getSlot(0).getItem().is(Items.APPLE),"reset returns to the same persistent vanilla ender inventory");
                        done[0]=true;cleanup.run();System.out.println("TT_ENDERSTORAGE_NATIVE_PASS public=true private_uuid=true native_menu_packet=true free_binding=true free_reset=true serialized_binding=true");helper.succeed();
                    }
                }
            }catch(Throwable error){done[0]=true;cleanup.run();if(error instanceof RuntimeException failure)throw failure;throw new GameTestAssertException(error.toString());}
        });
    }
    private EnderStoragePresentChecks(){}
}
