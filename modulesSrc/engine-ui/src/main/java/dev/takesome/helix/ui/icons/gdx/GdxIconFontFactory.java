package dev.takesome.helix.ui.icons.gdx;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.fonts.definition.FontDefinition;
import dev.takesome.helix.fonts.definition.FontHinting;
import dev.takesome.helix.fonts.FontManager;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.UiIconText;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeStyle;
import dev.takesome.helix.ui.icons.resources.UiIconFontResource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LibGDX font factory for icon-font resources.
 *
 * <p>FreeType parsing is routed through engine-fonts. This factory only translates
 * icon resource descriptors into font parsing descriptors.</p>
 */
public final class GdxIconFontFactory {
    private static final Map<String, BitmapFont> CACHE = new ConcurrentHashMap<>();
    private GdxIconFontFactory() {
    }

    public static BitmapFont createFontAwesomeSolid(int sizePx) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.SOLID, sizePx);
    }

    public static BitmapFont createFontAwesomeSolid(int sizePx, Collection<? extends UiIcon> icons) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.SOLID, sizePx, icons);
    }

    public static BitmapFont createFontAwesomeRegular(int sizePx) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.REGULAR, sizePx);
    }

    public static BitmapFont createFontAwesomeRegular(int sizePx, Collection<? extends UiIcon> icons) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.REGULAR, sizePx, icons);
    }

    public static BitmapFont createFontAwesomeBrands(int sizePx) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.BRANDS, sizePx);
    }

    public static BitmapFont createFontAwesomeBrands(int sizePx, Collection<? extends UiIcon> icons) {
        return FontAwesomeFontFamilyFactory.create(FontAwesomeStyle.BRANDS, sizePx, icons);
    }

    public static Map<String, BitmapFont> createFontAwesomeBundled(int sizePx) {
        return FontAwesomeFontFamilyFactory.createBundled(sizePx);
    }

    public static BitmapFont create(UiIconFontResource resource, int sizePx, Collection<? extends UiIcon> icons) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }

        String characters = UiIconText.characters(icons);
        if (characters.isEmpty()) {
            throw new IllegalArgumentException("icon character set must not be empty");
        }

        String cacheKey = resource.key() + ':' + resource.classpathPath() + ':' + Math.max(1, sizePx) + ':' + characters;
        BitmapFont existing = CACHE.get(cacheKey);
        if (existing != null) return existing;
        FontDefinition definition = FontDefinition.classpath(resource.key(), resource.classpathPath(), Math.max(1, sizePx), characters)
                .withMono(false)
                .withKerning(false)
                .withHinting(FontHinting.LIBGDX_DEFAULT)
                .withFilters(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                .withIntegerPositions(true);
        BitmapFont parsed = new FontManager("engine-ui-icon-fonts").parse(definition);
        CACHE.put(cacheKey, parsed);
        return parsed;
    }
    public static int cachedFontCount() {
        return CACHE.values().size();
    }
    public static void disposeCached() {
        for (BitmapFont font : CACHE.values()) {
            if (font != null) font.dispose();
        }
    }
}
