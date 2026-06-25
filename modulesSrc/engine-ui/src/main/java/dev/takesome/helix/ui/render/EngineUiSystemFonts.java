package dev.takesome.helix.ui.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.fonts.FontManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Engine-ui-owned system font resolver. Does not query game asset providers. */
final class EngineUiSystemFonts {
    static final String REGULAR = "engine-ui-system-fs-elliot-pro";
    static final String BOLD = "engine-ui-system-fs-elliot-pro-bold";
    static final String HEAVY = "engine-ui-system-fs-elliot-pro-heavy";

    private static final FontManager MANAGER = new FontManager("engine-ui-system-fonts");
    private static final Map<String, BitmapFont> CACHE = new ConcurrentHashMap<>();

    private EngineUiSystemFonts() {
    }

    static boolean isSystemFont(String fontId) {
        String id = normalize(fontId);
        return REGULAR.equals(id) || BOLD.equals(id) || HEAVY.equals(id);
    }

    static BitmapFont resolve(String fontId, int pixelSize, BitmapFont fallback) {
        String id = normalize(fontId);
        if (!isSystemFont(id)) return fallback;
        int size = Math.max(8, Math.min(64, pixelSize));
        String cacheKey = id + ":" + size;
        try {
            return CACHE.computeIfAbsent(cacheKey, ignored -> MANAGER.registerClasspath(cacheKey, sourceFor(id), size));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String sourceFor(String id) {
        if (BOLD.equals(id)) return "helix/ui/fonts/FSElliotPro-Bold.ttf";
        if (HEAVY.equals(id)) return "helix/ui/fonts/FSElliotPro-Heavy.ttf";
        return "helix/ui/fonts/FSElliotPro.ttf";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
