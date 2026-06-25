package dev.takesome.helix.ui.css.runtime;

import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class UiCssRuntimeDiagnostics {
    private static final Logger LOG = EngineLog.logger(UiCssRuntimeDiagnostics.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> DEBUGGED = ConcurrentHashMap.newKeySet();

    private UiCssRuntimeDiagnostics() {
    }

    public static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(cleanKey(key))) LOG.warn(message, args);
    }

    public static void debugOnce(String key, String message, Object... args) {
        if (DEBUGGED.add(cleanKey(key))) LOG.debug(message, args);
    }

    private static String cleanKey(String key) {
        return key == null || key.isBlank() ? "unknown" : key;
    }
}
