package dev.takesome.helix.ui.icons.gdx;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeBundle;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeStyle;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LibGDX factory for the bundled Font Awesome font family.
 */
public final class FontAwesomeFontFamilyFactory {
    private FontAwesomeFontFamilyFactory() {
    }

    public static BitmapFont create(FontAwesomeStyle style, int sizePx) {
        if (style == null) throw new IllegalArgumentException("style must not be null");
        return create(style, sizePx, style.icons());
    }

    public static BitmapFont create(FontAwesomeStyle style, int sizePx, Collection<? extends UiIcon> icons) {
        if (style == null) throw new IllegalArgumentException("style must not be null");
        return GdxIconFontFactory.create(style.resource(), sizePx, icons);
    }

    public static Map<String, BitmapFont> createBundled(int sizePx) {
        LinkedHashMap<String, BitmapFont> fonts = new LinkedHashMap<>();
        for (FontAwesomeStyle style : FontAwesomeBundle.styles()) {
            fonts.put(style.resource().key(), create(style, sizePx));
        }
        return fonts;
    }
}
