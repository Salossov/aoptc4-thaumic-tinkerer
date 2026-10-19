package thaumic.tinkerer.verification;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.*;
import thaumic.tinkerer.crop.*;

@GameTestHolder(ThaumicTinkerer.ID)
@PrefixGameTestTemplate(false)
public final class DebugCommandRulesTest {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="summon_space",batch="debug_commands",timeoutTicks=100)
    public static void maxResearchNativeCommand(GameTestHelper helper)throws Exception{
        VerificationReports.install();var pos=helper.absolutePos(new BlockPos(16,5,16));boolean enabled=TinkererConfig.DEBUG_COMMANDS.get();
        try(var fixture=new NativePlayerFixture(helper,"TTDebugResearch",pos)){
            var player=fixture.player;var source=player.createCommandSourceStack().withPermission(0);var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
            TinkererConfig.DEBUG_COMMANDS.set(false);
            boolean denied=false;try{dispatcher.execute("maxresearch",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            check(denied,"debug flag denies the command even when it is present in the registered tree");
            TinkererConfig.DEBUG_COMMANDS.set(true);check(!source.hasPermission(2),"fixture executes as a non-operator");
            ResearchApi.completeAspect(player,Aspects.AIR,(short)180);var before=Set.copyOf(ResearchApi.knowledge(player).completedResearch());
            check(dispatcher.execute("maxresearch",source)==1,"actual Brigadier command accepts a non-operator when original debug flag is enabled");
            var state=ResearchApi.knowledge(player);
            check(Aspect.all().values().stream().allMatch(a->state.knows(a)&&state.pool(a)==99),"maxresearch discovers every registered aspect and SETS all pools to 99, including reducing 180");
            check(state.completedResearch().equals(before),"maxresearch does not unlock research entries");
            dispatcher.execute("maxresearch ignored arguments",source);
            check(ResearchApi.knowledge(player).pool(Aspects.AIR)==99,"repeated execution with ignored source arguments remains 99 rather than adding again");
            check(fixture.packets.stream().anyMatch(p->p instanceof ClientboundSystemChatPacket chat&&chat.content().getString().equals("Added 99 research to all aspects")),"original literal success message is preserved despite its inaccurate wording");
            check(!ResearchApi.completeAspect(player,null,(short)99),"native null aspect completion remains a no-op");
            check(dispatcher.execute("maxresearch",helper.getLevel().getServer().createCommandSourceStack())==1,"source console invocation remains a silent no-op");
        }finally{TinkererConfig.DEBUG_COMMANDS.set(enabled);}
        System.out.println("TT_DEBUG_MAXRESEARCH_NATIVE_PASS aspects="+Aspect.all().size());helper.succeed();
    }
    @GameTest(template="summon_space",batch="debug_commands",timeoutTicks=100)
    public static void setCropTendencyNativeCommand(GameTestHelper helper)throws Exception{
        VerificationReports.install();var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(16,5,16));boolean enabled=TinkererConfig.DEBUG_COMMANDS.get();
        level.setBlock(pos.below(),InfusedContent.SOIL.get().defaultBlockState(),2);level.setBlock(pos,InfusedContent.CROP.get().defaultBlockState(),2);
        var crop=(InfusedCropBlockEntity)level.getBlockEntity(pos);crop.aspect=Aspects.FIRE;crop.primalTendencies=new AspectList().add(Aspects.WATER,10);
        try(var fixture=new NativePlayerFixture(helper,"TTDebugCrop",pos)){
            var player=fixture.player;player.setPos(pos.getX()+.5,pos.getY()-1.5,pos.getZ()-3.5);player.setYRot(0);player.setXRot(0);
            var source=player.createCommandSourceStack().withPermission(0);var dispatcher=level.getServer().getCommands().getDispatcher();TinkererConfig.DEBUG_COMMANDS.set(true);
            dispatcher.execute("setCropTendency aQuA 20",source);check(crop.primalTendencies.getAmount(Aspects.WATER)==20,"native ten-block ray finds crop and case-insensitive primal name merges by maximum");
            dispatcher.execute("setCropTendency Aqua 5 ignored",source);check(crop.primalTendencies.getAmount(Aspects.WATER)==20,"lower request does not reduce tendency or add five, and extra source arguments are ignored");
            dispatcher.execute("setCropTendency Potentia 40",source);check(crop.primalTendencies.size()==1,"compound aspect names are silently ignored");
            dispatcher.execute("setCropTendency Aqua invalid",source);check(crop.primalTendencies.getAmount(Aspects.WATER)==20&&fixture.packets.stream().anyMatch(p->p instanceof ClientboundSystemChatPacket chat&&chat.content().getString().equals("Invalid number")),"bad count preserves crop and sends original literal error");
            fixture.packets.clear();dispatcher.execute("setCropTendency Aer -1",source);check(!crop.primalTendencies.asMap().containsKey(Aspects.AIR)&&crop.primalTendencies.getAmount(Aspects.WATER)==20&&fixture.packets.stream().anyMatch(p->p instanceof ClientboundSystemChatPacket chat&&chat.content().getString().equals("Invalid number")),"approved negative-count rejection preserves every tendency and sends original numeric error");
            dispatcher.execute("setCropTendency Terra 0",source);check(crop.primalTendencies.asMap().containsKey(Aspects.EARTH)&&crop.primalTendencies.getAmount(Aspects.EARTH)==0,"zero remains a discovered tendency entry");
            dispatcher.execute("setCropTendency Aqua 75",source);check(crop.primalTendencies.getAmount(Aspects.WATER)==50&&crop.primalTendencies.visSize()==50,"approved saturation remains a strict real-unit cap after command");
            player.setYRot(180);boolean miss=false;try{dispatcher.execute("setCropTendency Aqua 1",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){miss=true;}check(miss&&crop.primalTendencies.getAmount(Aspects.WATER)==50,"missed ray preserves the original generic command error and does not alter the crop");
            check(dispatcher.execute("setCropTendency Aqua 10",level.getServer().createCommandSourceStack())==1,"console cannot modify a player-targeted crop");
        }finally{TinkererConfig.DEBUG_COMMANDS.set(enabled);level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);}
        System.out.println("TT_DEBUG_CROP_COMMAND_NATIVE_PASS");helper.succeed();
    }
    private DebugCommandRulesTest(){}
}
