package thaumic.tinkerer.verification;

import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.TinkererConfig;

/** Run with separate false/true startup profiles; never change a global config during parallel tests. */
@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ResearchModeRulesTest {
    private static void check(boolean value,String message){if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(message);}
    private static boolean request(ServerPlayer player,String research)throws ReflectiveOperationException{
        // Exercise the real book menu, not a replacement purchase implementation.
        return (boolean)player.containerMenu.getClass().getMethod("createNote",ServerPlayer.class,String.class).invoke(player.containerMenu,player,research);
    }
    @GameTest(template="summon_space",batch="research_mode",timeoutTicks=100)
    public static void realCoreAndAddonResearchFollowServerMode(GameTestHelper helper)throws Exception{
        VerificationReports.install();boolean easy=TinkererConfig.EASY_RESEARCH.get();
        check(ResearchApi.serverEasyMode()==easy,"actual startup option reaches the public server contract");
        for(var key:new String[]{"GOGGLES","thaumictinkerer:spell_cloth","thaumictinkerer:enchanter","thaumictinkerer:ichor","thaumictinkerer:gas_remover"}){
            try(var fixture=new NativePlayerFixture(helper,"TTMode"+UUID.randomUUID().toString().substring(0,8),helper.absolutePos(new BlockPos(8,4,8)))){
                var player=fixture.player;var definition=ResearchApi.definitions(player).get(key);
                check(definition!=null&&!definition.secondary()&&!definition.autoUnlock()&&!definition.aspects().isEmpty(),"real primary research in each namespace");
                if(key.equals("thaumictinkerer:enchanter")||key.equals("thaumictinkerer:ichor"))
                    check(definition.complexity()==3,"TC4 ResearchItem limits the source's complexity 5 to effective 3");
                if(key.equals("thaumictinkerer:gas_remover"))check(definition.complexity()==1,"TC4 ResearchItem raises constructor complexity 0 to effective 1");
                for(var parent:definition.parents())ResearchApi.grant(player,parent);
                for(var parent:definition.hiddenParents())ResearchApi.grant(player,parent);
                for(var tag:definition.aspects().keySet())ResearchApi.completeAspect(player,Aspect.getAspect(tag),(short)20);
                var book=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumonomicon"));
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(book));book.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
                check(!ResearchApi.knows(player,key),"target not already completed by setup");
                var before=new LinkedHashMap<>(ResearchApi.knowledge(player).aspectPools());
                if(easy){
                    String first=definition.aspects().keySet().iterator().next();
                    ResearchApi.completeAspect(player,Aspect.getAspect(first),(short)0);
                    check(!request(player,key)&&!ResearchApi.knows(player,key),"insufficient points reject purchase without a note");
                    check(ResearchApi.knowledge(player).aspectPools().entrySet().stream().allMatch(e->e.getValue().equals(e.getKey().equals(first)?0:before.get(e.getKey()))),"failed purchase does not partially debit other aspects");
                    ResearchApi.completeAspect(player,Aspect.getAspect(first),(short)20);
                    check(request(player,key)&&ResearchApi.knows(player,key),"easy mode completes primary research without paper/ink");
                    var after=ResearchApi.knowledge(player).aspectPools();
                    for(var entry:before.entrySet())check(after.get(entry.getKey())==entry.getValue()-definition.aspects().getOrDefault(entry.getKey(),0),"only source aspect prices are debited");
                    check(!request(player,key)&&after.equals(ResearchApi.knowledge(player).aspectPools()),"repeat request cannot charge again");
                    check(player.getInventory().countItem(Items.PAPER)==0,"easy purchase needs no paper");
                }else{
                    check(!request(player,key)&&before.equals(ResearchApi.knowledge(player).aspectPools()),"normal primary research refuses missing supplies without spending aspects");
                    var ink=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:scribing_tools"));
                    player.getInventory().add(new ItemStack(Items.PAPER,2));player.getInventory().add(new ItemStack(ink));
                    check(request(player,key)&&!ResearchApi.knows(player,key),"normal primary action creates a note instead of completing research");
                    check(player.getInventory().countItem(Items.PAPER)==1&&before.equals(ResearchApi.knowledge(player).aspectPools()),"normal note costs one paper and no aspect points");
                    check(player.getInventory().items.stream().filter(s->s.is(ink)).anyMatch(s->s.getDamageValue()==1),"normal note consumes one ink use");
                }
                CompoundTag snapshot=null;
                for(var packet:fixture.packets)if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload().type().id().equals(ResourceLocation.parse("thaumcraft:knowledge_snapshot")))
                    snapshot=(CompoundTag)custom.payload().getClass().getMethod("data").invoke(custom.payload());
                check(snapshot!=null&&snapshot.contains("easyResearch",Tag.TAG_BYTE)&&snapshot.getBoolean("easyResearch")==easy,"actual outbound knowledge snapshot carries server mode");
                check(!ResearchApi.definitions(player).get(key).secondary(),"mode never mutates research flags or recipe definitions");
                player.closeContainer();
            }
        }
        helper.succeed();
    }
    private ResearchModeRulesTest(){}
}
