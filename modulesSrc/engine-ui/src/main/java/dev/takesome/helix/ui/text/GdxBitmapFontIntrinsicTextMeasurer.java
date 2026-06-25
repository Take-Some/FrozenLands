package dev.takesome.helix.ui.text;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.fonts.render.FontGlyphRenderer;
import dev.takesome.helix.fonts.render.FontScalePolicy;
import dev.takesome.helix.fonts.render.FontTextMetrics;
import dev.takesome.helix.fonts.gdx.render.GdxFontGlyphRenderer;
import dev.takesome.helix.ui.css.UiIntrinsicTextMeasurer;
import dev.takesome.helix.ui.css.UiIntrinsicTextMetrics;

public final class GdxBitmapFontIntrinsicTextMeasurer implements UiIntrinsicTextMeasurer {
    private final BitmapFont fallbackFont;
    private final BitmapFont buttonFont;
    private final AssetProvider assets;
    private final FontGlyphRenderer glyphs = new GdxFontGlyphRenderer();

    public GdxBitmapFontIntrinsicTextMeasurer(BitmapFont fallbackFont, BitmapFont buttonFont, AssetProvider assets) {
        this.fallbackFont = fallbackFont;
        this.buttonFont = buttonFont == null ? fallbackFont : buttonFont;
        this.assets = assets;
    }

    @Override
    public synchronized UiIntrinsicTextMetrics measure(String text, String fontId, float scale, float fallbackFontSize) {
        ResolvedFont selected = fontFor(fontId, scale, fallbackFontSize);
        BitmapFont font = selected.font();
        if (font == null) return UiIntrinsicTextMeasurer.heuristic().measure(text, fontId, scale, fallbackFontSize);
        String normalized = normalize(text);
        if (normalized.isBlank()) return UiIntrinsicTextMetrics.ZERO;

        FontTextMetrics metrics = glyphs.measure(font, normalized, selected.renderScale(), FontScalePolicy.QUARTER_STEP);
        return new UiIntrinsicTextMetrics(metrics.width(), metrics.height());
    }

    private ResolvedFont fontFor(String fontId, float scale, float fallbackFontSize) {
        String key = fontId == null || fontId.isBlank() ? AssetProvider.FONT_STANDART : fontId.trim();
        BitmapFont fallback = fallbackFont == null ? buttonFont : fallbackFont;
        if (assets != null) {
            BitmapFont base = safeFont(key, fallback);
            if (assets.supportsFontSizeVariants()) {
                BitmapFont selected = safeFont(key, requestedPixelSize(scale, base, fallbackFontSize), base);
                if (selected != null) return new ResolvedFont(selected, 1f);
            }
            if (base != null) return new ResolvedFont(base, safeScale(scale));
        }
        return new ResolvedFont(fallback, safeScale(scale));
    }

    private BitmapFont safeFont(String key, BitmapFont fallback) {
        try {
            BitmapFont selected = assets.font(key);
            return selected == null ? fallback : selected;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private BitmapFont safeFont(String key, int pixelSize, BitmapFont fallback) {
        try {
            BitmapFont selected = assets.font(key, pixelSize);
            return selected == null ? fallback : selected;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int requestedPixelSize(float scale, BitmapFont baseFont, float fallbackFontSize) {
        return Math.max(1, Math.round(basePixelSize(baseFont, fallbackFontSize) * safeScale(scale)));
    }

    private float basePixelSize(BitmapFont baseFont, float fallbackFontSize) {
        if (baseFont != null) {
            float lineHeight = baseFont.getLineHeight();
            if (Float.isFinite(lineHeight) && lineHeight > 0f) return lineHeight;
            float capHeight = baseFont.getCapHeight();
            if (Float.isFinite(capHeight) && capHeight > 0f) return capHeight;
        }
        return Float.isFinite(fallbackFontSize) && fallbackFontSize > 0f ? fallbackFontSize : 16f;
    }

    private float safeScale(float scale) {
        return Float.isFinite(scale) && scale > 0f ? scale : 1f;
    }

    private record ResolvedFont(BitmapFont font, float renderScale) {
    }

    private String normalize(String text) {
        return emptyIfNull(text).replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }
}
