package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;
import dev.takesome.helix.ui.legacy.render.UiRelativeBounds;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.skin.UiElementSkin;

final class UiContentBoundsCache {
    private final int alphaThreshold;
    private final Map<String, UiRelativeBounds> contentBounds = new HashMap<>();

    UiContentBoundsCache(int alphaThreshold) {
        this.alphaThreshold = alphaThreshold;
    }

    UiRect resolve(UiElementSkin element, TextureRegion source, UiRect target) {
        if (target == null || target.w <= 0f || target.h <= 0f) return target;
        UiRelativeBounds relative = relativeContentBounds(element, source);
        return relative == null ? target : relative.apply(target);
    }

    UiRect resolveNineSlice(NineSlice<TextureRegion> slice, UiRect target) {
        if (target == null || target.w <= 0f || target.h <= 0f || slice == null) return target;

        UiNineSliceLayout.Resolved layout = UiNineSliceLayout.resolve(
                slice,
                TextureRegionMetrics.INSTANCE,
                target.x,
                target.y,
                target.w,
                target.h
        );
        return layout == null ? target : layout.contentBounds(target);
    }

    UiRect resolveRibbon(AlphaUiSheet sheet, int frame, UiRect target) {
        if (target == null || target.w <= 0f || target.h <= 0f || sheet == null) return target;
        UiRect bounds = sheet.contentBounds(frame, target);
        return bounds == null ? target : bounds;
    }

    void clear() {
        contentBounds.clear();
    }

    private UiRelativeBounds relativeContentBounds(UiElementSkin element, TextureRegion source) {
        String ownerKey = element == null ? "null" : element.cacheKey();
        String key = UiTextureRegionKeys.keyedSource(ownerKey, source);
        UiRelativeBounds cached = contentBounds.get(key);
        if (cached != null) return cached;

        UiRelativeBounds parsed = parseContentBounds(source);
        UiRelativeBounds resolved = parsed == null ? UiRelativeBounds.FULL : parsed;
        contentBounds.put(key, resolved);
        return resolved;
    }

    private UiRelativeBounds parseContentBounds(TextureRegion source) {
        if (source == null || source.getTexture() == null) return null;
        int w = source.getRegionWidth();
        int h = source.getRegionHeight();
        if (w <= 0 || h <= 0) return null;

        TextureData data = source.getTexture().getTextureData();
        if (data == null) return null;

        Pixmap pixmap = null;
        boolean disposePixmap = false;
        try {
            if (!data.isPrepared()) data.prepare();
            pixmap = data.consumePixmap();
            disposePixmap = data.disposePixmap();
            return parseContentBounds(source, pixmap);
        } catch (RuntimeException ex) {
            PixmapRegionGuard.warnFailure("UiContentBoundsCache.parse", ex);
            return null;
        } finally {
            if (disposePixmap && pixmap != null) pixmap.dispose();
        }
    }

    private UiRelativeBounds parseContentBounds(TextureRegion source, Pixmap pixmap) {
        if (pixmap == null) return null;

        int w = source.getRegionWidth();
        int h = source.getRegionHeight();
        int x0 = source.getRegionX();
        int y0 = source.getRegionY();

        if (!PixmapRegionGuard.ensureReadable("UiContentBoundsCache.parse", pixmap, x0, y0, w, h)) return null;

        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (UiTextureRegionKeys.alpha(pixmap.getPixel(x0 + x, y0 + y)) <= alphaThreshold) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) return null;

        float leftInset = minX / (float) w;
        float rightInset = (w - maxX - 1) / (float) w;
        float topInset = minY / (float) h;
        float bottomInset = (h - maxY - 1) / (float) h;
        return UiRelativeBounds.of(leftInset, rightInset, topInset, bottomInset);
    }
}
