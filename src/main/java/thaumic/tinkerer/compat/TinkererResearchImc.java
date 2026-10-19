package thaumic.tinkerer.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumic.tinkerer.ThaumicTinkerer;
import thaumic.tinkerer.api.InterModCommsOperations;
import thaumic.tinkerer.kami.KamiResearch;

/**
 * Handles incoming InterModComms messages for Thaumic Tinkerer research blacklist.
 * Faithfully ports 1.7.10 ThaumicTinkerer.HandleIMCMessages to NeoForge IMC.
 */
public final class TinkererResearchImc {
    private static final Logger LOGGER = LoggerFactory.getLogger("ThaumicTinkerer");

    public static void register(IEventBus bus) {
        bus.addListener(TinkererResearchImc::onInterModProcess);
    }

    public static void onInterModProcess(InterModProcessEvent event) {
        process();
    }

    public static int process() {
        return processMessages(InterModComms.getMessages(ThaumicTinkerer.ID, TinkererResearchImc::isBlacklistMethod));
    }

    public static boolean isBlacklistMethod(String method) {
        return method != null && method.equalsIgnoreCase(InterModCommsOperations.ADD_RESEARCH_BLACKLIST);
    }

    public static int processMessages(Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) return 0;
        int processed = 0;
        for (var it = messages.iterator(); it.hasNext(); ) {
            if (processMessage(it.next())) {
                processed++;
            }
        }
        return processed;
    }

    public static boolean processMessage(InterModComms.IMCMessage message) {
        if (message == null) return false;
        if (!isBlacklistMethod(message.method())) return false;

        Supplier<?> supplier = message.messageSupplier();
        if (supplier == null) {
            LOGGER.warn("Received AddResearchBlacklist IMC message from mod {} with null payload supplier", message.senderModId());
            return false;
        }

        Object payload;
        try {
            payload = supplier.get();
        } catch (Exception t) {
            LOGGER.warn("Failed to retrieve IMC payload for {} from mod {}: {}", message.method(), message.senderModId(), t.getMessage(), t);
            return false;
        }

        if (payload instanceof String str) {
            // Exact source split: retain spaces, case, duplicates and interior empty prefixes.
            KamiResearch.BLACKLIST.addAll(Arrays.asList(str.split(",")));
            return true;
        }
        if (payload instanceof Iterable<?> iterable) {
            var prefixes = new ArrayList<String>();
            try {
                for (Object elem : iterable) {
                    if (elem instanceof String prefix) prefixes.add(prefix);
                    else LOGGER.warn("Non-string prefix in AddResearchBlacklist from mod {}", message.senderModId());
                }
            } catch (Exception failure) {
                LOGGER.warn("Failed to read AddResearchBlacklist iterable from mod {}", message.senderModId(), failure);
                return false;
            }
            KamiResearch.BLACKLIST.addAll(prefixes);
            return !prefixes.isEmpty();
        }
        LOGGER.warn("Unsupported IMC payload type {} for AddResearchBlacklist from mod {}",
                payload == null ? "null" : payload.getClass().getName(), message.senderModId());
        return false;
    }

    private TinkererResearchImc() {}
}
