package thaumic.tinkerer.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ResearchApi;
import thaumic.tinkerer.TinkererConfig;
import thaumic.tinkerer.crop.InfusedCropBlockEntity;

/** Exact debug flag permission: enabled means all players, including non-operators. */
public final class TinkererCommands {
    public static void register(RegisterCommandsEvent event){
        KamiUnlockedCommand.register(event.getDispatcher());
        event.getDispatcher().register(Commands.literal("maxresearch").requires(source->TinkererConfig.DEBUG_COMMANDS.get())
                .executes(TinkererCommands::maxResearch)
                .then(Commands.argument("ignored",StringArgumentType.greedyString()).executes(TinkererCommands::maxResearch)));
        var count=Commands.argument("count",StringArgumentType.word()).executes(TinkererCommands::setCropTendency)
                .then(Commands.argument("ignored",StringArgumentType.greedyString()).executes(TinkererCommands::setCropTendency));
        event.getDispatcher().register(Commands.literal("setCropTendency").requires(source->TinkererConfig.DEBUG_COMMANDS.get())
                .then(Commands.argument("aspect",StringArgumentType.word()).then(count)));
    }
    private static int maxResearch(CommandContext<CommandSourceStack> context){
        if(context.getSource().getEntity() instanceof ServerPlayer player){
            for(var aspect:Aspect.all().values())ResearchApi.completeAspect(player,aspect,(short)99);
            context.getSource().sendSuccess(()->Component.literal("Added 99 research to all aspects"),false);
        }
        return 1;
    }
    private static int setCropTendency(CommandContext<CommandSourceStack> context)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        if(!(context.getSource().getEntity() instanceof ServerPlayer player))return 1;
        // Source BlockUtils used a fixed server eye offset and a ten-block, liquid-stopping ray.
        var start=player.position().add(0,1.62,0);var end=start.add(player.getLookAngle().scale(10));
        var hit=player.serverLevel().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
        if(hit.getType()==HitResult.Type.MISS)throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.translatable("commands.generic.exception")).create();
        if(!(player.serverLevel().getBlockEntity(hit.getBlockPos()) instanceof InfusedCropBlockEntity crop))return 1;
        String requested=StringArgumentType.getString(context,"aspect");
        for(var aspect:Aspects.PRIMALS)if(requested.toUpperCase().equals(aspect.getName().toUpperCase())){
            try{int amount=Integer.parseInt(StringArgumentType.getString(context,"count"));if(amount<0)throw new NumberFormatException("Negative tendency");crop.primalTendencies.merge(aspect,amount);crop.reduceSaturatedAspects();}
            catch(NumberFormatException error){context.getSource().sendSuccess(()->Component.literal("Invalid number"),false);}
        }
        return 1;
    }
    private TinkererCommands(){}
}
