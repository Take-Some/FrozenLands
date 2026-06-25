package dev.takesome.helix.ui.css.runtime;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import dev.takesome.helix.ui.model.UiColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parsed single-layer CSS box-shadow value used by retained UI runtime nodes. */
public final class UiCssBoxShadow {
    private static final UiColor DEFAULT_COLOR = new UiColor(0f, 0f, 0f, 0.35f);
    private static final UiCssBoxShadow NONE = new UiCssBoxShadow(false, 0f, 0f, 0f, 0f, null);

    private final boolean enabled;
    private final float offsetX;
    private final float offsetY;
    private final float blurRadius;
    private final float spreadRadius;
    private final UiColor color;

    private UiCssBoxShadow(boolean enabled, float offsetX, float offsetY, float blurRadius, float spreadRadius, UiColor color) {
        this.enabled = enabled;
        this.offsetX = finite(offsetX);
        this.offsetY = finite(offsetY);
        this.blurRadius = Math.max(0f, finite(blurRadius));
        this.spreadRadius = finite(spreadRadius);
        this.color = color;
    }

    public static UiCssBoxShadow none() {
        return NONE;
    }

    public static UiCssBoxShadow parse(String raw) {
        String value = trimToEmpty(raw);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return NONE;

        String layer = firstLayer(value);
        List<String> tokens = tokens(layer);
        if (tokens.isEmpty()) return NONE;

        UiColor color = null;
        ArrayList<String> lengths = new ArrayList<>();
        for (String token : tokens) {
            if (token == null || token.isBlank()) continue;
            if ("inset".equalsIgnoreCase(token)) continue;
            UiColor parsedColor = parseColor(token, null);
            if (parsedColor != null) {
                color = parsedColor;
            } else {
                lengths.add(token);
            }
        }

        if (lengths.size() < 2) return NONE;
        float offsetX = length(lengths.get(0), 0f);
        float offsetY = length(lengths.get(1), 0f);
        float blur = lengths.size() > 2 ? length(lengths.get(2), 0f) : 0f;
        float spread = lengths.size() > 3 ? length(lengths.get(3), 0f) : 0f;
        UiColor resolvedColor = color == null ? DEFAULT_COLOR : color;
        if (resolvedColor.a <= 0f) return NONE;
        return new UiCssBoxShadow(true, offsetX, offsetY, blur, spread, resolvedColor);
    }

    public boolean enabled() {
        return enabled;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public float blurRadius() {
        return blurRadius;
    }

    public float spreadRadius() {
        return spreadRadius;
    }

    public UiColor color() {
        return color;
    }

    private static String firstLayer(String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') depth++;
            if (c == ')' && depth > 0) depth--;
            if (c == ',' && depth == 0) return value.substring(0, i).trim();
        }
        return value;
    }

    private static List<String> tokens(String value) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) && depth == 0) {
                if (!current.isEmpty()) {
                    out.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            if (c == '(') depth++;
            if (c == ')' && depth > 0) depth--;
            current.append(c);
        }
        if (!current.isEmpty()) out.add(current.toString());
        return out;
    }

    private static UiColor parseColor(String raw, UiColor fallback) {
        String value = trimToEmpty(raw);
        if (value.isBlank()) return fallback;
        if ("transparent".equalsIgnoreCase(value)) return UiColor.TRANSPARENT;
        if ("white".equalsIgnoreCase(value)) return UiColor.WHITE;
        if ("black".equalsIgnoreCase(value)) return UiColor.rgba255(0, 0, 0, 255);
        if (value.startsWith("#")) return hex(value, fallback);
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgb(")) return rgb(value, fallback, false);
        if (lower.startsWith("rgba(")) return rgb(value, fallback, true);
        return fallback;
    }

    private static UiColor hex(String value, UiColor fallback) {
        String hex = value.substring(1).trim();
        try {
            if (hex.length() == 3) {
                int r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                int g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                int b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
                return UiColor.rgba255(r, g, b, 255);
            }
            if (hex.length() == 4) {
                int r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                int g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                int b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
                int a = Integer.parseInt(hex.substring(3, 4) + hex.substring(3, 4), 16);
                return UiColor.rgba255(r, g, b, a);
            }
            if (hex.length() == 6) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), 255);
            if (hex.length() == 8) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), Integer.parseInt(hex.substring(6, 8), 16));
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }

    private static UiColor rgb(String raw, UiColor fallback, boolean alphaAllowed) {
        int open = raw.indexOf('(');
        int close = raw.lastIndexOf(')');
        if (open < 0 || close <= open) return fallback;
        String[] parts = raw.substring(open + 1, close).split("\\s*,\\s*");
        if (parts.length < 3) return fallback;
        float r = channel(parts[0], 0f);
        float g = channel(parts[1], 0f);
        float b = channel(parts[2], 0f);
        float a = alphaAllowed && parts.length > 3 ? alpha(parts[3], 1f) : 1f;
        return new UiColor(r, g, b, a);
    }

    private static float channel(String raw, float fallback) {
        String value = trimToEmpty(raw);
        if (value.endsWith("%")) return clamp(number(value.substring(0, value.length() - 1), fallback * 100f) / 100f, 0f, 1f);
        return clamp(number(value, fallback * 255f) / 255f, 0f, 1f);
    }

    private static float alpha(String raw, float fallback) {
        String value = trimToEmpty(raw);
        if (value.endsWith("%")) return clamp(number(value.substring(0, value.length() - 1), fallback * 100f) / 100f, 0f, 1f);
        return clamp(number(value, fallback), 0f, 1f);
    }

    private static float length(String raw, float fallback) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return fallback;
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        return number(value, fallback);
    }

    private static float number(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0f;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
