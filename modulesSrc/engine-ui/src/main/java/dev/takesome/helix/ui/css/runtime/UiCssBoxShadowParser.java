package dev.takesome.helix.ui.css.runtime;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UiCssBoxShadowParser {
    public List<UiBoxShadow> parse(String rawValue) {
        String raw = trimToEmpty(rawValue);
        if (raw.isBlank() || "none".equalsIgnoreCase(raw)) return List.of();
        ArrayList<UiBoxShadow> out = new ArrayList<>();
        for (String item : splitTopLevel(raw, ',')) {
            UiBoxShadow parsed = parseOne(item.trim(), raw);
            if (parsed != null && parsed.visible()) out.add(parsed);
        }
        if (out.size() > 1) UiCssRuntimeDiagnostics.debugOnce("box-shadow:multi:" + raw,
                "UI CSS parsed multiple box shadows count={} raw='{}'", out.size(), raw);
        return List.copyOf(out);
    }

    private UiBoxShadow parseOne(String value, String source) {
        if (value.isBlank()) return null;
        ArrayList<Float> lengths = new ArrayList<>();
        UiColor color = null;
        boolean inset = false;
        for (String token : splitWhitespace(value)) {
            String normalized = token.toLowerCase(Locale.ROOT);
            if ("inset".equals(normalized)) {
                inset = true;
                continue;
            }
            UiColor parsedColor = color(token);
            if (parsedColor != null) {
                color = parsedColor;
                continue;
            }
            Float length = length(token);
            if (length != null) {
                lengths.add(length);
                continue;
            }
            UiCssRuntimeDiagnostics.warnOnce("box-shadow-token:" + source + ':' + token,
                    "UI CSS box-shadow token ignored raw='{}' token='{}' reason=unsupported-token", source, token);
        }
        if (lengths.size() < 2) {
            UiCssRuntimeDiagnostics.warnOnce("box-shadow-lengths:" + source,
                    "UI CSS box-shadow ignored raw='{}' reason=requires at least offset-x and offset-y", source);
            return null;
        }
        float blur = lengths.size() > 2 ? lengths.get(2) : 0f;
        if (blur < 0f) {
            UiCssRuntimeDiagnostics.warnOnce("box-shadow-negative-blur:" + source,
                    "UI CSS box-shadow negative blur clamped raw='{}' blur={}", source, blur);
            blur = 0f;
        }
        float spread = lengths.size() > 3 ? lengths.get(3) : 0f;
        if (lengths.size() > 4) {
            UiCssRuntimeDiagnostics.warnOnce("box-shadow-extra-lengths:" + source,
                    "UI CSS box-shadow extra lengths ignored raw='{}' lengthCount={}", source, lengths.size());
        }
        UiColor resolvedColor = color == null ? new UiColor(0f, 0f, 0f, 0.25f) : color;
        return new UiBoxShadow(lengths.get(0), lengths.get(1), blur, spread, resolvedColor, inset);
    }

    private List<String> splitWhitespace(String value) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(') depth++;
            if (ch == ')' && depth > 0) depth--;
            if (Character.isWhitespace(ch) && depth == 0) {
                add(out, current);
                continue;
            }
            current.append(ch);
        }
        add(out, current);
        return out;
    }

    private List<String> splitTopLevel(String value, char delimiter) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (quote != 0) {
                current.append(ch);
                if (ch == quote) quote = 0;
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                current.append(ch);
                continue;
            }
            if (ch == '(') depth++;
            if (ch == ')' && depth > 0) depth--;
            if (ch == delimiter && depth == 0) {
                add(out, current);
                continue;
            }
            current.append(ch);
        }
        add(out, current);
        return out;
    }

    private void add(List<String> out, StringBuilder current) {
        String value = current.toString().trim();
        if (!value.isBlank()) out.add(value);
        current.setLength(0);
    }

    private Float length(String raw) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return null;
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        if (value.endsWith("%") || value.endsWith("em") || value.endsWith("rem")) return null;
        try {
            return Float.parseFloat(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private UiColor color(String raw) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank()) return null;
        if ("transparent".equals(value)) return UiColor.TRANSPARENT;
        if ("black".equals(value)) return UiColor.rgba255(0, 0, 0, 255);
        if ("white".equals(value)) return UiColor.WHITE;
        if (value.startsWith("#")) return hex(value);
        if (value.startsWith("rgb(")) return rgb(value, false);
        if (value.startsWith("rgba(")) return rgb(value, true);
        return null;
    }

    private UiColor hex(String value) {
        String hex = value.substring(1);
        try {
            if (hex.length() == 3) return UiColor.rgba255(
                    Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16),
                    Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16),
                    Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16),
                    255);
            if (hex.length() == 6) return UiColor.rgba255(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16),
                    255);
            if (hex.length() == 8) return UiColor.rgba255(
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16),
                    Integer.parseInt(hex.substring(6, 8), 16));
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private UiColor rgb(String value, boolean alpha) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open < 0 || close <= open) return null;
        String[] parts = value.substring(open + 1, close).split("\\s*,\\s*");
        if (parts.length < 3) return null;
        float r = channel(parts[0]);
        float g = channel(parts[1]);
        float b = channel(parts[2]);
        float a = alpha && parts.length > 3 ? alpha(parts[3]) : 1f;
        return new UiColor(r, g, b, a);
    }

    private float channel(String raw) {
        String value = raw.trim();
        if (value.endsWith("%")) return clamp(number(value.substring(0, value.length() - 1), 0f) / 100f);
        return clamp(number(value, 0f) / 255f);
    }

    private float alpha(String raw) {
        String value = raw.trim();
        if (value.endsWith("%")) return clamp(number(value.substring(0, value.length() - 1), 100f) / 100f);
        return clamp(number(value, 1f));
    }

    private float number(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
