package thaumic.tinkerer.verification;

import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.addon.*;
import thaumcraft.api.research.*;
import thaumic.tinkerer.*;
import thaumic.tinkerer.item.ShareTomeItem;
import thaumic.tinkerer.util.ItemData;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class ShareTomeRulesTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new net.minecraft.gametest.framework.GameTestAssertException(label);}
    public static void fixture(AddonRegistrationEvent event){
        for(String key:List.of("test_share_warp","test_share_sibling"))AddonRegistry.research(new ResearchDefinition(
                "thaumictinkerer:"+key,"thaumictinkerer:tinkering",List.of("GOGGLES"),Map.of(),1,false,false,0,20,List.of(),List.of(),"minecraft:book",false,false,false,false,
                key.equals("test_share_warp")?List.of("thaumictinkerer:test_share_sibling"):List.of(),List.of(),false,key.equals("test_share_warp")?3:0));
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void shareTome(GameTestHelper helper)throws Exception{
        VerificationReports.install();checks=0;var level=helper.getLevel();var id=ThaumicTinkerer.id("share_tome");
        var suffix=UUID.randomUUID().toString().substring(0,8);var author=FocusRulesTest.player(helper,"TTA"+suffix);var reader=FocusRulesTest.player(helper,"TTR"+suffix);
        var item=ThaumicTinkerer.SHARE_TOME.get();var book=new ItemStack(item);boolean enabled=TinkererConfig.SURVIVAL_SHARE_TOME.get();
        try{
            check(book.getMaxStackSize()==1&&book.getRarity()==Rarity.EPIC&&!book.hasFoil(),"original singleton epic book without an invented foil");
            check(ShareTomeItem.authorName(book).equals("[none]")&&ShareTomeItem.authorId(book)==null,"new tome is unassigned");
            var lines=new ArrayList<Component>();item.appendHoverText(book,Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
            check(lines.size()==1&&lines.getFirst().equals(Component.translatable("tooltip.thaumictinkerer.share_tome.unassigned")),"unassigned tooltip uses original translation");
            var initial="thaumictinkerer:unknown_"+suffix;var later="@thaumictinkerer:later_"+suffix;
            check(ResearchSharingApi.complete(author,initial)&&ResearchApi.knows(author,initial),"original completion accepts a retained unknown research key");
            ResearchSharingApi.complete(author,"thaumictinkerer:test_share_warp");
            check(!ResearchApi.knows(author,"thaumictinkerer:test_share_sibling")&&!ResearchApi.knows(author,"GOGGLES"),"sharing completion adds neither missing prerequisites nor unshared sibling research");
            var snapshot=ResearchSharingApi.completed(author.server,author.getUUID());
            boolean immutable=false;try{snapshot.add("forged");}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"public UUID knowledge read is immutable");
            author.setItemInHand(InteractionHand.MAIN_HAND,book);var written=item.use(level,author,InteractionHand.MAIN_HAND);
            check(written.getResult().consumesAction()&&written.getObject()==book&&ShareTomeItem.authorId(book).equals(author.getUUID()),"actual use binds the held tome to its author UUID");
            check(ShareTomeItem.authorName(book).equals(author.getGameProfile().getName())&&ItemData.read(book).getList("research",8).size()==snapshot.size(),"original player name and initial research snapshot are retained");
            lines.clear();item.appendHoverText(book,Item.TooltipContext.EMPTY,lines,TooltipFlag.NORMAL);
            check(lines.getFirst().equals(Component.translatable("tooltip.thaumictinkerer.share_tome.author",author.getGameProfile().getName())),"bound tooltip retains the display name");
            ResearchSharingApi.complete(author,later);
            check(!snapshot.contains(later)&&ResearchSharingApi.completed(author.server,author.getUUID()).contains(later),"new knowledge does not mutate a prior snapshot");
            ItemData.update(book,tag->tag.putString("player","OldDisplayName"));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(author));
            var file=author.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("thaumcraft/players/"+author.getUUID()+".thaum");
            check(Files.isRegularFile(file)&&ResearchSharingApi.completed(author.server,author.getUUID()).contains(later),"logout evicts the native cache and UUID lookup reloads current offline data from its actual file");
            book=ItemStack.parse(level.registryAccess(),book.save(level.registryAccess())).orElseThrow();
            check(ShareTomeItem.authorId(book).equals(author.getUUID())&&ShareTomeItem.authorName(book).equals("OldDisplayName"),"UUID and display name survive real item serialization");
            reader.setItemInHand(InteractionHand.OFF_HAND,book);var before=ResearchApi.knowledge(reader);item.use(level,reader,InteractionHand.OFF_HAND);
            check(ResearchApi.knows(reader,initial)&&ResearchApi.knows(reader,later)&&ResearchApi.knows(reader,"thaumictinkerer:test_share_warp"),"actual offhand use imports live offline knowledge, including a post-binding in-progress key");
            check(!ResearchApi.knows(reader,"thaumictinkerer:test_share_sibling")&&!ResearchApi.knows(reader,"GOGGLES"),"recipient does not acquire unshared prerequisites or siblings");
            var after=ResearchApi.knowledge(reader);
            check(after.permanentWarp()-before.permanentWarp()==2&&after.stickyWarp()-before.stickyWarp()==1,"new forbidden research grants its original permanent/sticky warp split");
            item.use(level,reader,InteractionHand.OFF_HAND);
            check(ResearchApi.knowledge(reader).revision()==after.revision()&&ResearchApi.knowledge(reader).totalWarp()==after.totalWarp(),"repeated reading adds neither revisions nor duplicate warp");
            check(ShareTomeItem.authorId(book).equals(author.getUUID())&&book.getCount()==1&&book.getDamageValue()==0,"reading never consumes, damages or rebinds the tome");
            var missing=new ItemStack(item);ItemData.update(missing,tag->{tag.putString("player","Missing");tag.putUUID("authorUUID",UUID.randomUUID());});
            reader.setItemInHand(InteractionHand.MAIN_HAND,missing);item.use(level,reader,InteractionHand.MAIN_HAND);
            check(ResearchApi.knowledge(reader).revision()==after.revision(),"unknown author UUID yields no invented knowledge");
            var threadResult=CompletableFuture.supplyAsync(()->{try{ResearchSharingApi.completed(author.server,author.getUUID());return false;}catch(IllegalStateException expected){return true;}}).join();
            check(threadResult,"offline knowledge read rejects a worker thread");
            check(CompletableFuture.supplyAsync(()->{try{ResearchSharingApi.complete(reader,"forged");return false;}catch(IllegalStateException expected){return true;}}).join(),"knowledge mutation rejects a worker thread");
            check(level.getRecipeManager().byKey(id).isPresent()==enabled,"actual recipe loading follows the startup survival switch");
            var definition=AddonRegistry.research().get(id.toString());
            check(definition.autoUnlock()&&definition.stub()&&definition.round()&&definition.column()==0&&definition.row()==-1&&definition.parents().isEmpty(),"original root research flags, position and lack of parents");
            check(definition.recipePages().isEmpty()!=enabled&&definition.pageOrder().size()==(enabled?2:1),"recipe page appears only when its recipe is enabled");
            if(enabled){
                var recipe=(CraftingRecipe)level.getRecipeManager().byKey(id).orElseThrow().value();var grid=new ArrayList<ItemStack>();for(int i=0;i<9;i++)grid.add(ItemStack.EMPTY);
                var ink=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:scribing_tools")));grid.set(1,ink);grid.set(3,new ItemStack(Items.PAPER));grid.set(4,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:thaumonomicon"))));grid.set(5,new ItemStack(Items.PAPER));grid.set(7,new ItemStack(Items.PAPER));
                check(recipe.matches(CraftingInput.of(3,3,grid),level),"exact release-164 paper/thaumonomicon/inkwell layout matches");ink.setDamageValue(1);
                check(!recipe.matches(CraftingInput.of(3,3,grid),level),"original metadata-zero inkwell excludes a used tool");ink.setDamageValue(0);
                var output=recipe.assemble(CraftingInput.of(3,3,grid),level.registryAccess());check(output.is(item)&&output.getCount()==1&&ShareTomeItem.authorId(output)==null,"crafting produces one fresh unbound tome");
            }
            System.out.println("TT_SHARE_TOME_PASS enabled="+enabled+" checks="+checks);helper.succeed();
        }finally{author.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);reader.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);reader.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
    }
}
