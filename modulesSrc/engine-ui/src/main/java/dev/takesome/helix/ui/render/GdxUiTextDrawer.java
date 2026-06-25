package dev.takesome.helix.ui.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.fonts.render.FontGlyphRenderer;
import dev.takesome.helix.fonts.render.FontRenderRequest;
import dev.takesome.helix.fonts.render.FontScalePolicy;
import dev.takesome.helix.fonts.render.FontTextAlign;
import dev.takesome.helix.fonts.gdx.render.GdxFontGlyphRenderer;
import dev.takesome.helix.ui.render.GdxUiPainter;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.gdx.GdxIconFontCache;
import dev.takesome.helix.ui.icons.resources.UiIconFontResource;
import dev.takesome.helix.ui.icons.resources.UiIconFontResources;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class GdxUiTextDrawer {
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont buttonFont;
    private final BitmapFont fallbackIconFont;
    private final Map<String, BitmapFont> iconFonts;
    private final AssetProvider assets;
    private final FontGlyphRenderer glyphs = new GdxFontGlyphRenderer();
    private final Map<String, BitmapFont> iconSizeVariants = new ConcurrentHashMap<>();

    GdxUiTextDrawer(
            SpriteBatch batch,
            ShapeRenderer shapes,
            BitmapFont font,
            BitmapFont buttonFont,
            BitmapFont fallbackIconFont,
            Map<String, BitmapFont> iconFonts,
            AssetProvider assets,
            GdxUiPainter painter
    ) {
        this.batch = batch;
        this.font = font;
        this.buttonFont = buttonFont == null ? font : buttonFont;
        this.fallbackIconFont = fallbackIconFont;
        this.iconFonts = copyIconFonts(iconFonts);
        this.assets = assets;
    }

    void text(Matrix4 projection, String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        text(projection, text, rect, scale, color, align, null);
    }

    void text(Matrix4 projection, String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        ResolvedFont selected = fontFor(fontId, scale, font);
        drawTextWithFont(projection, selected.font(), text, rect, selected.renderScale(), color, align);
    }

    void buttonText(Matrix4 projection, String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        buttonText(projection, text, rect, scale, color, align, null);
    }

    void buttonText(Matrix4 projection, String text, UiRect rect, float scale, UiColor color, TextAlign align, String fontId) {
        ResolvedFont selected = fontFor(fontId, scale, font);
        drawTextWithFont(projection, selected.font(), text, rect, selected.renderScale(), color, align);
    }

    boolean supportsIcons() {
        return fallbackIconFont != null || !iconFonts.isEmpty();
    }

    boolean icon(Matrix4 projection, UiIcon icon, UiRect rect, float scale, UiColor color, TextAlign align) {
        if (icon == null) return false;
        ResolvedFont selectedIconFont = iconFontFor(icon, scale);
        if (selectedIconFont.font() == null) return false;
        drawTextWithFont(projection, selectedIconFont.font(), icon.text(), rect, selectedIconFont.renderScale(), color, align);
        return true;
    }

    private ResolvedFont fontFor(String fontId, float scale, BitmapFont fallback) {
        int requestedSize = requestedPixelSize(scale, fallback);
        if (EngineUiSystemFonts.isSystemFont(fontId)) {
            BitmapFont selected = EngineUiSystemFonts.resolve(fontId, requestedSize, fallback);
            return new ResolvedFont(selected == null ? fallback : selected, 1f);
        }
        if (assets == null) return new ResolvedFont(fallback, safeScale(scale));
        String key = fontId == null || fontId.isBlank() ? AssetProvider.FONT_STANDART : fontId.trim();
        BitmapFont base = safeFont(key, fallback);
        if (assets.supportsFontSizeVariants()) {
            BitmapFont selected = safeFont(key, requestedPixelSize(scale, base), base);
            return new ResolvedFont(selected == null ? base : selected, 1f);
        }
        return new ResolvedFont(base, safeScale(scale));
    }

    private ResolvedFont iconFontFor(UiIcon icon, float scale) {
        String key = UiIconFontResources.key(icon);
        BitmapFont base = iconFonts.get(key);
        if (base == null) base = fallbackIconFont;
        if (base == null) return new ResolvedFont(null, safeScale(scale));
        int pixelSize = requestedPixelSize(scale, base);
        BitmapFont variant = iconSizeVariants.computeIfAbsent(key + ":" + pixelSize + ":" + icon.text(), ignored -> createIconVariant(icon, pixelSize));
        if (variant == null) return new ResolvedFont(base, safeScale(scale));
        return new ResolvedFont(variant, 1f);
    }

    private BitmapFont createIconVariant(UiIcon icon, int pixelSize) {
        UiIconFontResource resource = iconResource(icon);
        if (resource == null) return null;
        try {
            return GdxIconFontCache.font(resource, pixelSize, icon);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private UiIconFontResource iconResource(UiIcon icon) {
        String key = UiIconFontResources.key(icon);
        for (UiIconFontResource resource : UiIconFontResources.fontAwesomeBundled()) {
            if (resource.key().equals(key)) return resource;
        }
        return null;
    }

    private void drawTextWithFont(Matrix4 projection, BitmapFont selectedFont, String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        if (rect == null || text == null || text.isEmpty()) return;
        BitmapFont resolvedFont = selectedFont == null ? font : selectedFont;
        if (resolvedFont == null) return;
        glyphs.render(
                batch,
                projection,
                FontRenderRequest.centered(
                        resolvedFont,
                        text,
                        rect.x,
                        rect.y,
                        rect.w,
                        rect.h,
                        scale,
                        toGdx(color),
                        toFontAlign(align),
                        true,
                        FontScalePolicy.EXACT
                )
        );
    }

    private Color toGdx(UiColor color) {
        UiColor c = color == null ? UiColor.WHITE : color;
        return new Color(c.r, c.g, c.b, c.a);
    }

    private FontTextAlign toFontAlign(TextAlign align) {
        TextAlign resolved = align == null ? TextAlign.LEFT : align;
        return switch (resolved) {
            case CENTER -> FontTextAlign.CENTER;
            case RIGHT -> FontTextAlign.RIGHT;
            case LEFT -> FontTextAlign.LEFT;
        };
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

    private int requestedPixelSize(float scale, BitmapFont baseFont) {
        return Math.max(1, Math.round(basePixelSize(baseFont) * safeScale(scale)));
    }

    private float basePixelSize(BitmapFont baseFont) {
        if (baseFont == null) return 16f;
        float lineHeight = baseFont.getLineHeight();
        if (Float.isFinite(lineHeight) && lineHeight > 0f) return lineHeight;
        float capHeight = baseFont.getCapHeight();
        if (Float.isFinite(capHeight) && capHeight > 0f) return capHeight;
        return 16f;
    }

    private float safeScale(float scale) {
        return Float.isFinite(scale) && scale > 0f ? scale : 1f;
    }

    private record ResolvedFont(BitmapFont font, float renderScale) {
    }

    void dispose() {
        // Values are shared through GdxIconFontCache. The drawer owns only local references.
        iconSizeVariants.clear();
    }

    private static Map<String, BitmapFont> copyIconFonts(Map<String, BitmapFont> input) {
        if (input == null || input.isEmpty()) return Collections.emptyMap();
        LinkedHashMap<String, BitmapFont> copy = new LinkedHashMap<>();
        for (Map.Entry<String, BitmapFont> entry : input.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                copy.put(entry.getKey().trim(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }
}
