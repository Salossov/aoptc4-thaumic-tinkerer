package thaumic.tinkerer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.research.ResearchApi;
import thaumcraft.api.research.ResearchDefinition;
import thaumic.tinkerer.TinkererConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exact port of 1.7.10 KamiUnlockedCommand ("iskamiunlocked").
 * Evaluates dynamic KAMI research requirements against active player knowledge.
 */
public final class KamiUnlockedCommand {
    public static final String NAME = "iskamiunlocked";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(NAME)
                .executes(KamiUnlockedCommand::execute)
                .then(Commands.argument("ignored",StringArgumentType.greedyString()).executes(KamiUnlockedCommand::execute)));
    }

    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can check KAMI unlock status"));
            return 0;
        }

        if (!TinkererConfig.KAMI_FORCE_ENABLED.get()) {
            source.sendSuccess(() -> Component.literal("No"), false);
            source.sendFailure(Component.literal("KAMI is disabled"));
            return 0;
        }

        Map<String, ResearchDefinition> catalog;
        try {
            catalog = ResearchApi.definitions(player);
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("No"), false);
            source.sendFailure(Component.literal("KAMI research definition is unavailable"));
            return 0;
        }

        ResearchDefinition ichor = catalog.get("thaumictinkerer:ichor");
        if (ichor == null) {
            source.sendSuccess(() -> Component.literal("No"), false);
            source.sendFailure(Component.literal("KAMI research definition is unavailable"));
            return 0;
        }

        // The core has already resolved dynamic hidden parents against this active catalog.
        var unlocked = ResearchApi.knowledge(player).completedResearch();
        List<String> remaining = new ArrayList<>(ichor.hiddenParents());
        remaining.removeAll(unlocked);

        if (remaining.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Yes"), false);
        } else {
            source.sendSuccess(() -> Component.literal("No"), false);
            source.sendSuccess(() -> Component.literal("Remaining: " + String.join(",", remaining)), false);
        }
        return 1;
    }

    private KamiUnlockedCommand() {}
}
