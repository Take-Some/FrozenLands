package dev.takesome.helix.ui.markup.internal.style;

import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.css.UiCssFontFamilyResolver;

import java.util.Locale;
import java.util.Map;

/** Typed style value reader for HELIX UI Markup. */
public final class UiDomStyleReader {
    public boolean has(Map<String, String> style, String key) {
        return style.containsKey(key) && !style.getOrDefault(key, "").isBlank();
    }

    public String value(Map<String, String> style, String key) {
        return style.getOrDefault(key, "").trim();
    }

    public String first(Map<String, String> style, String... keys) {
        for (String key : keys) {
            String value = value(style, key);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    public String font(Map<String, String> style) {
        String value = first(style, "font-family", "font");
        return UiCssFontFamilyResolver.resolveEngineFontId(value.isBlank() ? AssetProvider.FONT_STANDART : value, style);
    }

    public TextAlign align(Map<String, String> style) {
        String value = value(style, "align");
        if (value.isBlank()) return TextAlign.LEFT;
        try {
            return TextAlign.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return TextAlign.LEFT;
        }
    }

    public UiColor color(Map<String, String> style, String key, UiColor fallback) {
        String value = value(style, key);
        if (value.isBlank()) return fallback;
        if ("transparent".equalsIgnoreCase(value)) return UiColor.TRANSPARENT;
        if ("white".equalsIgnoreCase(value)) return UiColor.WHITE;
        UiColor base = fallback == null ? UiColor.WHITE : fallback;
        if (value.startsWith("#")) return hex(value, base);
        String[] parts = value.split(",");
        if (parts.length >= 3) {
            float r = number(parts[0], base.r);
            float g = number(parts[1], base.g);
            float b = number(parts[2], base.b);
            float a = parts.length > 3 ? number(parts[3], base.a) : base.a;
            return new UiColor(r, g, b, a);
        }
        return fallback;
    }

    public int integer(Map<String, String> style, String key, int fallback) {
        return Math.round(number(style, key, fallback));
    }

    public float number(Map<String, String> style, String key, float fallback) {
        return number(value(style, key), fallback);
    }

    public float number(String raw, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        try {
            return Float.parseFloat(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public boolean bool(Map<String, String> style, String key, boolean fallback) {
        String raw = value(style, key);
        if (raw.isBlank()) return fallback;
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw) || "on".equalsIgnoreCase(raw);
    }

    private UiColor hex(String value, UiColor fallback) {
        String hex = value.substring(1).trim();
        try {
            if (hex.length() == 6) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), 255);
            if (hex.length() == 8) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), Integer.parseInt(hex.substring(6, 8), 16));
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }
}
