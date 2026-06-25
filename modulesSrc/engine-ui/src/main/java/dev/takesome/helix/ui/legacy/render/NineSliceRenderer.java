package dev.takesome.helix.ui.legacy.render;

import java.util.Objects;
import dev.takesome.helix.ui.render.UiPainter;

/**
 * Draws a nine-slice panel as one continuous surface.
 *
 * Border regions keep their native size when the target rect is large enough.
 * If the target rect is smaller than the native border sum, the whole border
 * grid is uniformly reduced so corners, edges and center still touch each other
 * without gaps or accidental rectangular backdrops.
 */
public final class NineSliceRenderer<TTextureRegion> {
    private final UiPainter<TTextureRegion> painter;
    private final RegionMetrics<TTextureRegion> metrics;

    public NineSliceRenderer(UiPainter<TTextureRegion> painter, RegionMetrics<TTextureRegion> metrics) {
        this.painter = Objects.requireNonNull(painter, "painter");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    public void draw(NineSlice<TTextureRegion> s, float x, float y, float w, float h) {
        Objects.requireNonNull(s, "nineSlice");
        UiNineSliceLayout.Resolved layout = UiNineSliceLayout.resolve(s, metrics, x, y, w, h);
        if (layout == null) return;

        painter.draw(s.topLeft, layout.x(), layout.topY(), layout.leftW(), layout.topH());
        painter.draw(s.topRight, layout.rightX(), layout.topY(), layout.rightW(), layout.topH());
        painter.draw(s.bottomLeft, layout.x(), layout.y(), layout.leftW(), layout.bottomH());
        painter.draw(s.bottomRight, layout.rightX(), layout.y(), layout.rightW(), layout.bottomH());

        if (layout.centerW() > 0f) {
            painter.draw(s.top, layout.centerX(), layout.topY(), layout.centerW(), layout.topH());
            painter.draw(s.bottom, layout.centerX(), layout.y(), layout.centerW(), layout.bottomH());
        }

        if (layout.centerH() > 0f) {
            painter.draw(s.left, layout.x(), layout.centerY(), layout.leftW(), layout.centerH());
            painter.draw(s.right, layout.rightX(), layout.centerY(), layout.rightW(), layout.centerH());
        }

        if (layout.centerW() > 0f && layout.centerH() > 0f) {
            painter.draw(s.center, layout.centerX(), layout.centerY(), layout.centerW(), layout.centerH());
        }
    }
}
