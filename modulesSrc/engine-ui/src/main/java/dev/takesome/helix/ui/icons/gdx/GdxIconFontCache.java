package dev.takesome.helix.ui.icons.gdx;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.resources.UiIconFontResource;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Shared icon-font atlas cache for UI runtime icon size variants. */
public final class GdxIconFontCache {
    private static final Map<Key, BitmapFont> FONTS = new ConcurrentHashMap<>();

    private GdxIconFontCache() {
    }

    public static BitmapFont font(UiIconFontResource resource, int sizePx, UiIcon icon) {
        if (resource == null || icon == null) {
            return null;
        }
        Key key = new Key(resource.key(), resource.classpathPath(), Math.max(1, sizePx), icon.text());
        return GdxIconFontFactory.create(resource, sizePx, Collections.singleton(icon));
    }

    public static int size() {
        return GdxIconFontFactory.cachedFontCount();
    }

    public static void dispose() {
        GdxIconFontFactory.disposeCached();
        for (BitmapFont font : FONTS.values()) {
            if (font != null) {
                font.dispose();
            }
        }
        FONTS.clear();
    }

    private record Key(String resourceKey, String classpathPath, int sizePx, String text) {
        private Key {
            resourceKey = trimToEmpty(resourceKey);
            classpathPath = trimToEmpty(classpathPath);
            text = emptyIfNull(text);
        }
    }
}
