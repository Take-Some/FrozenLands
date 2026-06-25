package dev.takesome.helix.ui.legacy.render;

import java.util.Objects;
import dev.takesome.helix.ui.model.UiRect;

/** Shared geometry resolver for rendering and content bounds of nine-slice UI. */
final class UiNineSliceLayout {
    private UiNineSliceLayout() {
    }

    static <TTextureRegion> Resolved resolve(
            NineSlice<TTextureRegion> slice,
            RegionMetrics<TTextureRegion> metrics,
            float x,
            float y,
            float w,
            float h
    ) {
        Objects.requireNonNull(slice, "nineSlice");
        Objects.requireNonNull(metrics, "metrics");
        if (w <= 0f || h <= 0f) return null;

        float leftW = max(metrics.width(slice.topLeft), metrics.width(slice.left), metrics.width(slice.bottomLeft));
        float rightW = max(metrics.width(slice.topRight), metrics.width(slice.right), metrics.width(slice.bottomRight));
        float topH = max(metrics.height(slice.topLeft), metrics.height(slice.top), metrics.height(slice.topRight));
        float bottomH = max(metrics.height(slice.bottomLeft), metrics.height(slice.bottom), metrics.height(slice.bottomRight));

        float horizontalBorder = leftW + rightW;
        float verticalBorder = topH + bottomH;
        if (horizontalBorder <= 0f || verticalBorder <= 0f) return null;

        float borderScale = Math.min(1f, Math.min(w / horizontalBorder, h / verticalBorder));
        if (!Float.isFinite(borderScale) || borderScale <= 0f) return null;

        float targetLeftW = leftW * borderScale;
        float targetRightW = rightW * borderScale;
        float targetTopH = topH * borderScale;
        float targetBottomH = bottomH * borderScale;

        float centerX = x + targetLeftW;
        float centerY = y + targetBottomH;
        float centerW = Math.max(0f, w - targetLeftW - targetRightW);
        float centerH = Math.max(0f, h - targetTopH - targetBottomH);
        float rightX = x + w - targetRightW;
        float topY = y + h - targetTopH;

        return new Resolved(
                x, y, w, h,
                targetLeftW, targetRightW, targetTopH, targetBottomH,
                centerX, centerY, centerW, centerH,
                rightX, topY
        );
    }

    private static float max(float a, float b, float c) {
        return Math.max(a, Math.max(b, c));
    }

    record Resolved(
            float x,
            float y,
            float w,
            float h,
            float leftW,
            float rightW,
            float topH,
            float bottomH,
            float centerX,
            float centerY,
            float centerW,
            float centerH,
            float rightX,
            float topY
    ) {
        UiRect contentBounds(UiRect fallback) {
            if (centerW <= 0f || centerH <= 0f) return fallback;
            return new UiRect(centerX, centerY, centerW, centerH);
        }
    }
}
