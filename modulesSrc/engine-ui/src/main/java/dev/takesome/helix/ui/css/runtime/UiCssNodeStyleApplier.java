package dev.takesome.helix.ui.css.runtime;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.lowerTrimToEmpty;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiBoxShadow;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.css.UiCssFontFamilyResolver;
import dev.takesome.helix.ui.css.UiCssLength;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.checkbox.UiCheckboxNode;
import dev.takesome.helix.ui.uiComponents.combo.UiComboBoxNode;
import dev.takesome.helix.ui.components.UiElementNode;
import dev.takesome.helix.ui.components.UiIconNode;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.skin.UiSkinResolver;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies already-resolved CSS presented styles to retained UI nodes. */
public final class UiCssNodeStyleApplier {
    private static final Pattern TRANSLATE = Pattern.compile("translate(?:3d)?\\(\\s*([^,()]+)\\s*,\\s*([^,)]+)(?:,\\s*[^)]+)?\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSLATE_X = Pattern.compile("translatex\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSLATE_Y = Pattern.compile("translatey\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);

    private final UiSkinResolver skinResolver;
    private final UiCssBoxShadowParser boxShadowParser = new UiCssBoxShadowParser();

    public UiCssNodeStyleApplier() {
        this(null);
    }

    public UiCssNodeStyleApplier(UiSkinResolver skinResolver) {
        this.skinResolver = skinResolver;
    }

    public void apply(Node node, Map<String, String> style) {
        if (node == null || style == null || style.isEmpty()) return;
        applyBox(node, style);
        applyState(node, style);
        if (node instanceof UiPanelNode panel) applyPanel(panel, style);
        if (node instanceof UiElementNode element) applyElement(element, style);
        if (node instanceof UiLabelNode label) applyLabel(label, style);
        if (node instanceof UiIconNode icon) applyIcon(icon, style);
        if (node instanceof UiButtonNode button) applyButton(button, style);
        if (node instanceof UiCheckboxNode checkbox) applyCheckbox(checkbox, style);
        if (node instanceof UiComboBoxNode comboBox) applyComboBox(comboBox, style);
    }

    public void apply(Node node, Map<String, String> style, Map<String, String> fallbackStyle) {
        if (fallbackStyle != null && !fallbackStyle.isEmpty()) apply(node, fallbackStyle);
        apply(node, style);
    }

    private void applyBox(Node node, Map<String, String> style) {
        UiRect current = node.bounds();
        float w = length(first(style, "width", "w"), current.w);
        float h = length(first(style, "height", "h"), current.h);
        float x = length(first(style, "left", "x"), current.x);
        float y = length(first(style, "top", "y"), current.y);
        float[] translation = translation(style);
        float scale = transformScale(style);
        node.setBounds(x + translation[0], y + translation[1], Math.max(0f, w * scale), Math.max(0f, h * scale));
    }

    private void applyState(Node node, Map<String, String> style) {
        String display = value(style, "display");
        String visibility = value(style, "visibility");
        boolean visible = !"none".equalsIgnoreCase(display) && !"hidden".equalsIgnoreCase(visibility) && !"collapse".equalsIgnoreCase(visibility);
        node.setVisible(visible);
        if (has(style, "pointer-events")) node.setEnabled(!"none".equalsIgnoreCase(value(style, "pointer-events")));
        if (has(style, "overflow") || has(style, "overflow-x") || has(style, "overflow-y")) {
            boolean clip = clipsOverflow(first(style, "overflow", "overflow-x", "overflow-y"));
            node.setClipChildren(clip);
            if (clip) UiCssRuntimeDiagnostics.debugOnce("overflow-clip|" + node.getClass().getName(),
                    "UI CSS overflow clipping enabled nodeType={} overflow='{}' overflowX='{}' overflowY='{}'",
                    node.getClass().getSimpleName(), value(style, "overflow"), value(style, "overflow-x"), value(style, "overflow-y"));
        }
    }

    private void applyPanel(UiPanelNode panel, Map<String, String> style) {
        UiElementSkin skin = skin("panel", style);
        if (skin != null) panel.setBackgroundSkin(skin);

        UiColor color = color(first(style, "background-color", "background"), null);
        UiColor withOpacity = withOpacity(color, opacity(style, 1f));
        if (withOpacity != null) panel.setColor(withOpacity);
        UiColor border = color(value(style, "border-color"), null);
        float borderWidth = length(value(style, "border-width"), 0f);
        if (border != null || borderWidth > 0f) panel.setBorder(border, borderWidth);
        if (has(style, "box-shadow")) {
            List<UiBoxShadow> shadows = boxShadowParser.parse(value(style, "box-shadow"));
            panel.setBoxShadows(shadows);
            if (!shadows.isEmpty()) UiCssRuntimeDiagnostics.debugOnce("box-shadow-apply|" + shadows.size() + '|' + value(style, "box-shadow"),
                    "UI CSS box-shadow applied count={} raw='{}'", shadows.size(), value(style, "box-shadow"));
        }
    }

    private void applyElement(UiElementNode element, Map<String, String> style) {
        UiElementSkin skin = skin("image", style);
        if (skin != null) element.setElement(skin);
    }

    private void applyLabel(UiLabelNode label, Map<String, String> style) {
        UiColor color = color(first(style, "color", "text-color"), null);
        UiColor withOpacity = withOpacity(color, opacity(style, 1f));
        if (withOpacity != null) label.setColor(withOpacity);
        String align = first(style, "text-align", "align");
        if (!align.isBlank()) label.setAlign(align(align));
        String font = value(style, "font-family");
        if (!font.isBlank()) label.setFontId(cssFontFace(font, style));
        String size = value(style, "font-size");
        float resolvedScale = size.isBlank() ? label.scale() : length(size, 16f) / 16f;
        resolvedScale *= transformScale(style);
        label.setScale(Math.max(0.01f, resolvedScale));
    }

    private void applyIcon(UiIconNode icon, Map<String, String> style) {
        UiColor color = color(first(style, "icon-color", "color", "text-color"), null);
        UiColor withOpacity = withOpacity(color, opacity(style, 1f));
        if (withOpacity != null) icon.setColor(withOpacity);

        String size = first(style, "font-size", "icon-size", "iconSize");
        if (!size.isBlank()) {
            icon.setScale(Math.max(0.01f, length(size, 18f) / 32f));
            return;
        }

        String scale = first(style, "icon-scale", "iconScale", "scale");
        if (!scale.isBlank()) {
            icon.setScale(Math.max(0.01f, number(scale, 0.82f)));
            return;
        }

        if (has(style, "width") || has(style, "w") || has(style, "height") || has(style, "h")) {
            UiRect bounds = icon.bounds();
            if (bounds.w > 0f && bounds.h > 0f) icon.setScale(Math.max(0.01f, Math.min(bounds.w, bounds.h) / 32f));
        }
    }

    private void applyButton(UiButtonNode button, Map<String, String> style) {
        float opacity = opacity(style, 1f);
        UiColor background = withOpacity(color(first(style, "background-color", "background", "color"), null), opacity);
        UiColor text = withOpacity(color(first(style, "text-color", "color"), null), opacity);
        button.setColors(background, background, background, background, text);
        UiColor border = color(value(style, "border-color"), null);
        float borderWidth = length(value(style, "border-width"), 0f);
        if (border != null || borderWidth > 0f) button.setBorder(border, borderWidth);
    }

    private void applyCheckbox(UiCheckboxNode checkbox, Map<String, String> style) {
        float opacity = opacity(style, 1f);
        UiColor box = withOpacity(color(first(style, "checkbox-box-color", "box-color", "background-color", "background"), null), opacity);
        UiColor inner = withOpacity(color(first(style, "checkbox-inner-color", "inner-color"), null), opacity);
        UiColor check = withOpacity(color(first(style, "checkbox-check-color", "check-color", "accent-color", "icon-color", "color"), null), opacity);
        UiColor text = withOpacity(color(first(style, "text-color", "color"), null), opacity);
        UiColor border = withOpacity(color(value(style, "border-color"), null), opacity);
        float borderWidth = length(value(style, "border-width"), -1f);
        checkbox.setStyleColors(box, inner, check, text, border, borderWidth);
    }

    private void applyComboBox(UiComboBoxNode comboBox, Map<String, String> style) {
        float opacity = opacity(style, 1f);
        UiColor background = withOpacity(color(first(style, "background-color", "background"), null), opacity);
        UiColor text = withOpacity(color(first(style, "text-color", "color"), null), opacity);
        UiColor icon = withOpacity(color(first(style, "icon-color", "color", "text-color"), null), opacity);
        UiColor border = withOpacity(color(value(style, "border-color"), null), opacity);
        float borderWidth = length(value(style, "border-width"), -1f);
        comboBox.setStyleColors(background, text, icon, border, borderWidth);
        String closedIcon = first(style, "closed-icon", "icon");
        String openIcon = first(style, "open-icon", "icon-open");
        if (!closedIcon.isBlank() || !openIcon.isBlank()) comboBox.setIconIds(closedIcon, openIcon);
    }

    private UiElementSkin skin(String defaultKind, Map<String, String> style) {
        return skinResolver == null ? null : skinResolver.resolve(style, defaultKind);
    }

    private String cssFontFace(String family, Map<String, String> style) {
        return UiCssFontFamilyResolver.resolveEngineFontId(family, style);
    }

    private TextAlign align(String raw) {
        String normalized = trimToEmpty(raw).replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return TextAlign.valueOf(normalized);
        } catch (RuntimeException ignored) {
            return TextAlign.LEFT;
        }
    }

    private float opacity(Map<String, String> style, float fallback) {
        return clamp(number(value(style, "opacity"), fallback), 0f, 1f);
    }

    private UiColor withOpacity(UiColor color, float opacity) {
        if (color == null) return null;
        return new UiColor(color.r, color.g, color.b, clamp(color.a * opacity, 0f, 1f));
    }

    private UiColor color(String raw, UiColor fallback) {
        String value = trimToEmpty(raw);
        if (value.isBlank()) return fallback;
        if ("transparent".equalsIgnoreCase(value)) return UiColor.TRANSPARENT;
        if ("white".equalsIgnoreCase(value)) return UiColor.WHITE;
        if ("black".equalsIgnoreCase(value)) return UiColor.rgba255(0, 0, 0, 255);
        UiColor base = fallback == null ? UiColor.WHITE : fallback;
        if (value.startsWith("#")) return hex(value, base);
        if (value.toLowerCase(Locale.ROOT).startsWith("rgb")) return rgb(value, base);
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

    private UiColor hex(String value, UiColor fallback) {
        String hex = value.substring(1).trim();
        try {
            if (hex.length() == 3) {
                int r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                int g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                int b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
                return UiColor.rgba255(r, g, b, 255);
            }
            if (hex.length() == 6) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), 255);
            if (hex.length() == 8) return UiColor.rgba255(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16), Integer.parseInt(hex.substring(6, 8), 16));
        } catch (RuntimeException ignored) {
        }
        return fallback;
    }

    private UiColor rgb(String raw, UiColor fallback) {
        int open = raw.indexOf('(');
        int close = raw.lastIndexOf(')');
        if (open < 0 || close <= open) return fallback;
        String[] parts = raw.substring(open + 1, close).split("\\s*,\\s*");
        if (parts.length < 3) return fallback;
        UiColor base = fallback == null ? UiColor.WHITE : fallback;
        return new UiColor(channel(parts[0], base.r), channel(parts[1], base.g), channel(parts[2], base.b), parts.length > 3 ? number(parts[3], base.a) : base.a);
    }

    private float channel(String raw, float fallback) {
        String value = trimToEmpty(raw);
        if (value.endsWith("%")) return clamp(number(value.substring(0, value.length() - 1), fallback * 100f) / 100f, 0f, 1f);
        return clamp(number(value, fallback * 255f) / 255f, 0f, 1f);
    }

    private float[] translation(Map<String, String> style) {
        float[] out = translation(value(style, "transform"));
        float[] longhand = translateLonghand(value(style, "translate"));
        out[0] += longhand[0];
        out[1] += longhand[1];
        return out;
    }

    private float[] translateLonghand(String raw) {
        String value = trimToEmpty(raw);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return new float[] {0f, 0f};
        String[] parts = value.replace(',', ' ').trim().split("\\s+");
        float x = parts.length > 0 ? length(parts[0], 0f) : 0f;
        float y = parts.length > 1 ? length(parts[1], 0f) : 0f;
        return new float[] {x, y};
    }

    private float[] translation(String transform) {
        String value = trimToEmpty(transform);
        if (value.isBlank() || "none".equalsIgnoreCase(value)) return new float[] {0f, 0f};
        float x = 0f;
        float y = 0f;
        Matcher translate = TRANSLATE.matcher(value);
        if (translate.find()) {
            x += length(translate.group(1), 0f);
            y += length(translate.group(2), 0f);
        }
        Matcher translateX = TRANSLATE_X.matcher(value);
        if (translateX.find()) x += length(translateX.group(1), 0f);
        Matcher translateY = TRANSLATE_Y.matcher(value);
        if (translateY.find()) y += length(translateY.group(1), 0f);
        return new float[] {x, y};
    }

    private float transformScale(Map<String, String> style) {
        float scale = transformScale(value(style, "transform"));
        String longhand = value(style, "scale");
        if (!longhand.isBlank() && !"none".equalsIgnoreCase(longhand)) scale *= firstScaleValue(longhand);
        return Math.max(0.01f, scale);
    }

    private float firstScaleValue(String raw) {
        String value = trimToEmpty(raw);
        if (value.isBlank()) return 1f;
        String[] parts = value.replace(',', ' ').trim().split("\\s+");
        return Math.max(0.01f, number(parts.length == 0 ? value : parts[0], 1f));
    }

    private float transformScale(String transform) {
        String value = lowerTrimToEmpty(transform, Locale.ROOT);
        if (value.isBlank() || "none".equals(value)) return 1f;
        int start = value.indexOf("scale" + "(");
        if (start < 0) return 1f;
        int open = value.indexOf('(', start);
        int close = value.indexOf(')', open + 1);
        if (open < 0 || close <= open) return 1f;
        String raw = value.substring(open + 1, close).trim();
        int comma = raw.indexOf(',');
        if (comma >= 0) raw = raw.substring(0, comma).trim();
        return Math.max(0.01f, number(raw, 1f));
    }

    private boolean clipsOverflow(String raw) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        return "hidden".equals(value) || "scroll".equals(value) || "auto".equals(value);
    }


    private String first(Map<String, String> style, String... keys) {
        for (String key : keys) {
            String value = value(style, key);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private boolean has(Map<String, String> style, String key) {
        return style.containsKey(key) && !value(style, key).isBlank();
    }

    private String value(Map<String, String> style, String key) {
        return style.getOrDefault(key, "").trim();
    }

    private float length(String raw, float fallback) {
        String value = lowerTrimToEmpty(raw, Locale.ROOT);
        if (value.isBlank() || "auto".equals(value)) return fallback;
        try {
            return UiCssLength.parse(value).resolve(UiCssUnitResolutionContext.defaults(), 1f, fallback);
        } catch (RuntimeException ex) {
            UiCssRuntimeDiagnostics.warnOnce("style-length|" + value,
                    "UI CSS runtime length ignored raw='{}' fallback={} reason='{}'", value, fallback, ex.getMessage());
            return fallback;
        }
    }

    private float number(String raw, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Float.parseFloat(raw.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private String stripQuotes(String value) {
        String out = trimToEmpty(value);
        if ((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'"))) return out.substring(1, out.length() - 1);
        return out;
    }

    private float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
