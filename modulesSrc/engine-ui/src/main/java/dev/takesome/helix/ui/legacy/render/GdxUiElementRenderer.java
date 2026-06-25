package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import dev.takesome.helix.assets.api.AssetProvider;
import dev.takesome.helix.materials.api.MaterialProvider;
import dev.takesome.helix.ui.skin.UiSkinRect;
import dev.takesome.helix.ui.skin.UiSkinThreeSlice;
import dev.takesome.helix.ui.skin.UiSliceScaleMode;

import java.util.HashMap;
import java.util.Map;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.render.UiPainter;

/**
 * LibGDX renderer for declarative retained-UI elements produced by data-driven UI runtime.
 *
 * <p>Panels and buttons use the alpha-grid nine-slice pipeline. Legacy ribbons use
 * alpha-row horizontal stretch parsing. Descriptor-backed three-slice skins use
 * explicit source rectangles, which is the preferred path for composite UI chrome
 * with reusable caps and a stretchable middle.</p>
 */
public final class GdxUiElementRenderer implements Disposable {
    private static final int ALPHA_GRID_THRESHOLD = 8;
    private static final int ALPHA_SHEET_THRESHOLD = 8;

    private final UiElementAssetResolver resolver;
    private final NineSliceRenderer<TextureRegion> nine;

    private final Map<String, NineSlice<TextureRegion>> nineSlices = new HashMap<>();
    private final Map<String, AlphaUiSheet> ribbonSheets = new HashMap<>();
    private final UiContentBoundsCache contentBounds = new UiContentBoundsCache(ALPHA_GRID_THRESHOLD);

    public GdxUiElementRenderer(UiPainter<TextureRegion> painter, AssetProvider assets, MaterialProvider materials) {
        if (painter == null) throw new IllegalArgumentException("painter must not be null");
        this.resolver = new UiElementAssetResolver(assets, materials);
        this.nine = new NineSliceRenderer<>(painter, TextureRegionMetrics.INSTANCE);
    }

    public boolean draw(SpriteBatch batch, UiElementSkin element, UiRect rect) {
        return draw(batch, element, rect, null);
    }

    public boolean draw(SpriteBatch batch, UiElementSkin element, UiRect rect, UiColor tint) {
        if (batch == null || element == null || rect == null || !element.hasSource()) return false;
        if (rect.w <= 0f || rect.h <= 0f) return false;

        Color previousColor = null;
        if (tint != null) {
            previousColor = new Color(batch.getColor());
            batch.setColor(tint.r, tint.g, tint.b, tint.a);
        }

        try {
            if (element.usesThreeSlice() && drawThreeSlice(batch, element, rect)) {
                return true;
            }

            if (element.usesNineSlice()) {
                if (drawNineSlice(element, rect)) return true;

                TextureRegion image = region(element);
                if (image == null) return false;
                batch.draw(image, rect.x, rect.y, rect.w, rect.h);
                return true;
            }

            if (element.usesRibbonSheet()) {
                return drawRibbon(batch, element, rect);
            }

            TextureRegion image = region(element);
            if (image == null) return false;
            batch.draw(image, rect.x, rect.y, rect.w, rect.h);
            return true;
        } finally {
            if (previousColor != null) batch.setColor(previousColor);
        }
    }

    public UiRect contentBounds(UiElementSkin element, UiRect rect) {
        if (element == null || rect == null || rect.w <= 0f || rect.h <= 0f) return rect;

        if (element.usesThreeSlice()) {
            UiRect content = threeSliceContentBounds(element, rect);
            if (content != null) return content;
        }

        TextureRegion source = region(element);
        if (source == null) return rect;

        if (element.usesNineSlice()) {
            NineSlice<TextureRegion> slice = nineSlice(element);
            return slice == null ? contentBounds.resolve(element, source, rect) : contentBounds.resolveNineSlice(slice, rect);
        }

        if (element.usesRibbonSheet()) {
            return contentBounds.resolveRibbon(ribbonSheet(element, source), element.frame(), rect);
        }

        return contentBounds.resolve(element, source, rect);
    }

    private boolean drawNineSlice(UiElementSkin element, UiRect rect) {
        NineSlice<TextureRegion> slice = nineSlice(element);
        if (slice == null) return false;
        nine.draw(slice, rect.x, rect.y, rect.w, rect.h);
        return true;
    }

    private boolean drawThreeSlice(SpriteBatch batch, UiElementSkin element, UiRect rect) {
        if (element == null || element.descriptor() == null || !element.descriptor().usesThreeSlice()) return false;
        TextureRegion source = region(element);
        if (source == null) return false;

        UiSkinThreeSlice slice = element.descriptor().threeSlice();
        ThreeSliceLayout layout = threeSliceLayout(slice, rect.w, rect.h);
        if (layout == null) return false;

        TextureRegion left = subRegion(source, slice.left());
        TextureRegion middle = subRegion(source, slice.middle());
        TextureRegion right = subRegion(source, slice.right());
        if (left == null || middle == null || right == null) return false;

        if (layout.leftW > 0f) batch.draw(left, rect.x, rect.y, layout.leftW, rect.h);
        if (layout.middleW > 0f) drawMiddle(batch, middle, rect.x + layout.leftW, rect.y, layout.middleW, rect.h, slice.mode(), layout.scale);
        if (layout.rightW > 0f) batch.draw(right, rect.x + rect.w - layout.rightW, rect.y, layout.rightW, rect.h);
        return true;
    }

    private UiRect threeSliceContentBounds(UiElementSkin element, UiRect target) {
        if (element == null || element.descriptor() == null || !element.descriptor().usesThreeSlice()) return null;
        ThreeSliceLayout layout = threeSliceLayout(element.descriptor().threeSlice(), target.w, target.h);
        if (layout == null) return null;
        return new UiRect(target.x + layout.leftW, target.y, layout.middleW, target.h);
    }

    private ThreeSliceLayout threeSliceLayout(UiSkinThreeSlice slice, float w, float h) {
        if (slice == null || !slice.valid() || w <= 0f || h <= 0f) return null;
        float nominalHeight = Math.max(1f, slice.nominalHeight());
        float scale = h / nominalHeight;
        if (!Float.isFinite(scale) || scale <= 0f) return null;

        float leftW = Math.min(slice.left().w() * scale, w * 0.5f);
        float rightW = Math.min(slice.right().w() * scale, Math.max(0f, w - leftW));
        float middleW = Math.max(0f, w - leftW - rightW);
        return new ThreeSliceLayout(leftW, middleW, rightW, scale);
    }

    private void drawMiddle(SpriteBatch batch, TextureRegion middle, float x, float y, float w, float h, UiSliceScaleMode mode, float scale) {
        if (batch == null || middle == null || w <= 0f || h <= 0f) return;
        if (mode != UiSliceScaleMode.REPEAT) {
            batch.draw(middle, x, y, w, h);
            return;
        }

        float tileW = Math.max(1f, middle.getRegionWidth() * Math.max(0.0001f, scale));
        float cursor = x;
        float remaining = w;
        while (remaining > 0.01f) {
            float drawW = Math.min(tileW, remaining);
            TextureRegion drawRegion = middle;
            if (drawW < tileW - 0.01f) {
                int srcW = Math.max(1, Math.min(middle.getRegionWidth(), Math.round(middle.getRegionWidth() * (drawW / tileW))));
                drawRegion = new TextureRegion(middle, 0, 0, srcW, middle.getRegionHeight());
            }
            batch.draw(drawRegion, cursor, y, drawW, h);
            cursor += drawW;
            remaining -= drawW;
        }
    }

    private TextureRegion subRegion(TextureRegion source, UiSkinRect rect) {
        if (source == null || rect == null || !rect.valid()) return null;
        return new TextureRegion(source, rect.x(), rect.y(), rect.w(), rect.h());
    }

    private boolean drawRibbon(SpriteBatch batch, UiElementSkin element, UiRect rect) {
        TextureRegion source = region(element);
        if (source == null) return false;

        AlphaUiSheet sheet = ribbonSheet(element, source);

        if (sheet != null && sheet.drawHorizontalStretch(batch, element.frame(), rect.x, rect.y, rect.w, rect.h)) {
            return true;
        }

        batch.draw(source, rect.x, rect.y, rect.w, rect.h);
        return true;
    }

    private NineSlice<TextureRegion> nineSlice(UiElementSkin element) {
        String key = element.cacheKey();
        NineSlice<TextureRegion> cached = nineSlices.get(key);
        if (cached != null) return cached;

        TextureRegion source = region(element);
        if (source == null) return null;

        NineSlice<TextureRegion> parsed = AlphaGridNineSlice.from(source, ALPHA_GRID_THRESHOLD);
        if (parsed != null) nineSlices.put(key, parsed);
        return parsed;
    }

    private AlphaUiSheet ribbonSheet(UiElementSkin element, TextureRegion source) {
        if (element == null || source == null) return null;
        String key = element.cacheKey();
        AlphaUiSheet cached = ribbonSheets.get(key);
        if (cached != null) return cached;

        AlphaUiSheet parsed = AlphaUiSheet.parse(source, ALPHA_SHEET_THRESHOLD);
        if (parsed != null) ribbonSheets.put(key, parsed);
        return parsed;
    }

    private TextureRegion region(UiElementSkin element) {
        return resolver.region(element);
    }

    @Override
    public void dispose() {
        resolver.dispose();
        nineSlices.clear();
        ribbonSheets.clear();
        contentBounds.clear();
    }

    private record ThreeSliceLayout(float leftW, float middleW, float rightW, float scale) {}
}
