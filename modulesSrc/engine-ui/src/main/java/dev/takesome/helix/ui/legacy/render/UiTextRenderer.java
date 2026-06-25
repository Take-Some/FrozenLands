package dev.takesome.helix.ui.legacy.render;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.fonts.render.FontGlyphRenderer;
import dev.takesome.helix.fonts.render.FontRenderRequest;
import dev.takesome.helix.fonts.render.FontScalePolicy;
import dev.takesome.helix.fonts.render.FontTextAlign;
import dev.takesome.helix.fonts.render.FontTextMetrics;
import dev.takesome.helix.fonts.render.FontVerticalAlign;
import dev.takesome.helix.fonts.gdx.render.GdxFontGlyphRenderer;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.i18n.I18nKey;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;
import dev.takesome.helix.ui.animation.UiAnimationPipeline;
import dev.takesome.helix.ui.animation.UiTextAnimationFrame;
import dev.takesome.helix.ui.binding.UiBindingRuntimeSource;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import dev.takesome.helix.ui.binding.EmptyUiBindingSource;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiFontDefinition;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiRect;

final class UiTextRenderer {
    private static final Logger LOG = EngineLog.logger(UiTextRenderer.class);
    private final AssetProvider assets;
    private final UiAnimationPipeline animationPipeline;
    private final Map<String, String> fontAliases = new HashMap<>();
    private final Color animatedColor = new Color();
    private final Color scratch = new Color();
    private final FontGlyphRenderer glyphs = new GdxFontGlyphRenderer();
    private final Set<String> warnedI18nKeys = new LinkedHashSet<>();
    private EngineI18n i18n;

    UiTextRenderer(AssetProvider assets, UiAnimationPipeline animationPipeline) {
        this(assets, animationPipeline, null);
    }

    UiTextRenderer(AssetProvider assets, UiAnimationPipeline animationPipeline, EngineI18n i18n) {
        this.assets = assets;
        this.animationPipeline = animationPipeline;
        this.i18n = i18n;
    }

    void setI18n(EngineI18n i18n) {
        this.i18n = i18n;
    }

    void prepareFonts(UiDocument doc) {
        fontAliases.clear();
        if (doc == null || doc.fonts == null) return;
        for (Map.Entry<String, UiFontDefinition> entry : doc.fonts.entrySet()) {
            UiFontDefinition def = entry.getValue();
            if (entry.getKey() != null && def != null && def.asset != null && !def.asset.isBlank()) {
                fontAliases.put(entry.getKey(), def.asset);
            }
        }
    }

    void text(UiBindingSource binding, SpriteBatch batch, BitmapFont fallbackFont, UiRect panel, UiWidgetDefinition widget, String key) {
        BitmapFont font = fontFor(widget, fallbackFont);
        String value = textValue(widget, binding);
        UiRect textRect = UiWidgetBounds.explicitSize(widget)
                ? UiWidgetBounds.rect(panel, widget)
                : UiWidgetBounds.implicitTextRect(panel, widget, panel.w - UiWidgetBounds.x(widget), glyphs.lineHeight(font, textScale(widget, font), FontScalePolicy.QUARTER_STEP));
        drawAnimated(
                batch,
                font,
                value,
                textRect,
                textScale(widget, font),
                color(widget.color, Color.WHITE),
                UiWidgetBounds.align(widget, TextAlign.LEFT),
                widget,
                key
        );
    }

    void label(UiBindingSource binding, SpriteBatch batch, BitmapFont fallbackFont, UiWidgetDefinition widget, String key, float x, float y) {
        String value = textValue(widget, binding);
        if (value.isEmpty()) return;
        BitmapFont font = fontFor(widget, fallbackFont);
        float lineHeight = glyphs.lineHeight(font, textScale(widget, font), FontScalePolicy.QUARTER_STEP);
        UiRect textRect = new UiRect(x, y - lineHeight, Math.max(0f, UiWidgetBounds.w(widget)), Math.max(0f, lineHeight));
        drawAnimated(batch, font, value, textRect, textScale(widget, font), color(widget.color, Color.WHITE), UiWidgetBounds.align(widget, TextAlign.LEFT), widget, key);
    }

    void label(UiBindingSource binding, SpriteBatch batch, BitmapFont fallbackFont, UiWidgetDefinition widget, String key, UiRect rect) {
        String value = textValue(widget, binding);
        if (value.isEmpty() || rect == null) return;
        BitmapFont font = fontFor(widget, fallbackFont);
        drawAnimated(batch, font, value, rect, textScale(widget, font), color(widget.color, Color.WHITE), UiWidgetBounds.align(widget, TextAlign.LEFT), widget, key);
    }

    String textValue(UiWidgetDefinition widget, UiBindingSource binding) {
        String localized = localizedTemplate(widget);
        if (binding instanceof UiBindingRuntimeSource runtime && widget != null && widget.id != null && !widget.id.isBlank()) {
            return bind(runtime.textTarget(widget.id + ".text", localized), binding);
        }
        return bind(localized, binding);
    }

    private String localizedTemplate(UiWidgetDefinition widget) {
        if (widget == null) return "";
        String key = firstNonBlank(widget.i18nKey, widget.textKey);
        if (!key.isBlank()) {
            if (i18n == null) {
                warnOnce("no-i18n|" + key, "UI document i18n unavailable key='{}' widget='{}'; using raw text", key, widget.id);
            } else {
                String resolved = i18n.resolve(I18nKey.of(key));
                if (resolved != null && !resolved.isBlank() && !missingMarker(resolved, key)) return resolved;
                warnOnce("missing|" + key, "UI document missing i18n key='{}' widget='{}' resolved='{}'; using raw text", key, widget.id, resolved);
            }
        }
        return emptyIfNull(widget.text);
    }

    private void warnOnce(String key, String message, Object... args) {
        synchronized (warnedI18nKeys) {
            if (warnedI18nKeys.add(key)) LOG.warn(message, args);
        }
    }

    private static boolean missingMarker(String resolved, String key) {
        if (resolved == null || key == null) return true;
        String value = resolved.trim();
        return value.equals(key) || value.equals("??" + key + "??") || (value.startsWith("??") && value.endsWith("??"));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    String bind(String template, UiBindingSource binding) {
        if (template == null) return "";
        UiBindingSource safeBinding = binding == null ? EmptyUiBindingSource.INSTANCE : binding;
        String out = template;
        int guard = 0;
        while (guard++ < 64) {
            int a = out.indexOf('{');
            int z = out.indexOf('}', a + 1);
            if (a < 0 || z < 0) break;
            String key = out.substring(a + 1, z).trim();
            out = out.substring(0, a) + safeBinding.text(key) + out.substring(z + 1);
        }
        return out;
    }

    float measureTextWidth(UiBindingSource binding, BitmapFont fallbackFont, UiWidgetDefinition widget) {
        if (widget == null) return 0f;
        BitmapFont font = fontFor(widget, fallbackFont);
        if (font == null) return 0f;
        String value = textValue(widget, binding);
        if (value.isEmpty()) return 0f;
        FontTextMetrics metrics = glyphs.measure(font, value, textScale(widget, font), FontScalePolicy.QUARTER_STEP);
        return Math.max(0f, metrics.width());
    }

    float measureLineHeight(BitmapFont fallbackFont, UiWidgetDefinition widget) {
        BitmapFont font = fontFor(widget, fallbackFont);
        if (font == null) return 0f;
        return glyphs.lineHeight(font, textScale(widget, font), FontScalePolicy.QUARTER_STEP);
    }

    private BitmapFont fontFor(UiWidgetDefinition widget, BitmapFont fallback) {
        String family = widget == null ? "" : emptyIfNull(widget.effectiveFontFamily());
        if (family.isBlank() || assets == null) return fallback;
        String fontId = fontAliases.getOrDefault(family, family);
        BitmapFont selected = assets.font(fontId);
        return selected == null ? fallback : selected;
    }

    private float textScale(UiWidgetDefinition widget, BitmapFont font) {
        if (widget == null) return 1f;
        if (widget.hasFontSize()) return Math.max(0.01f, widget.fontSize / basePixelSize(font));
        return Float.isFinite(widget.scale) && widget.scale > 0f ? widget.scale : 1f;
    }

    private float basePixelSize(BitmapFont font) {
        if (font == null) return 16f;
        float lineHeight = font.getLineHeight();
        if (Float.isFinite(lineHeight) && lineHeight > 0f) return lineHeight;
        float capHeight = font.getCapHeight();
        return Float.isFinite(capHeight) && capHeight > 0f ? capHeight : 16f;
    }

    private void drawAnimated(SpriteBatch batch, BitmapFont font, String value, UiRect rect, float scale, Color color, TextAlign align, UiWidgetDefinition widget, String key) {
        UiTextAnimationFrame frame = animationPipeline.animateText(widget, key, value);
        if (frame == null || !frame.visible || frame.text == null || frame.text.isEmpty()) return;
        Color c = color == null ? Color.WHITE : color;
        if (frame.alpha < 0.999f) {
            animatedColor.set(c.r, c.g, c.b, c.a * MathUtils.clamp(frame.alpha, 0f, 1f));
            c = animatedColor;
        }
        drawText(batch, font, frame.text, shifted(rect, frame.offsetX, frame.offsetY), scale, c, align);
    }

    private UiRect shifted(UiRect rect, float dx, float dy) {
        return new UiRect(rect.x + dx, rect.y + dy, rect.w, rect.h);
    }

    private void drawText(SpriteBatch batch, BitmapFont font, String text, UiRect rect, float scale, Color color, TextAlign align) {
        if (text == null || text.isEmpty() || rect == null || font == null) return;
        FontTextAlign resolvedAlign = rect.w > 0f ? toFontAlign(align) : FontTextAlign.LEFT;
        FontVerticalAlign verticalAlign = rect.h > 0f ? FontVerticalAlign.CENTER : FontVerticalAlign.BASELINE;
        glyphs.render(
                batch,
                null,
                new FontRenderRequest(
                        font,
                        text,
                        rect.x,
                        rect.y,
                        Math.max(0f, rect.w),
                        Math.max(0f, rect.h),
                        scale,
                        color == null ? Color.WHITE : color,
                        resolvedAlign,
                        verticalAlign,
                        false,
                        true,
                        true,
                        FontScalePolicy.QUARTER_STEP
                )
        );
    }

    private FontTextAlign toFontAlign(TextAlign align) {
        TextAlign resolved = align == null ? TextAlign.LEFT : align;
        return switch (resolved) {
            case CENTER -> FontTextAlign.CENTER;
            case RIGHT -> FontTextAlign.RIGHT;
            case LEFT -> FontTextAlign.LEFT;
        };
    }

    private Color color(float[] rgba, Color fallback) {
        if (rgba == null || rgba.length < 3) return fallback;
        return scratch.set(rgba[0], rgba[1], rgba[2], rgba.length > 3 ? rgba[3] : 1f);
    }
}
