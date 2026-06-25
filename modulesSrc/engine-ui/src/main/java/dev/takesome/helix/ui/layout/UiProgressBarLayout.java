package dev.takesome.helix.ui.layout;

import com.badlogic.gdx.math.MathUtils;

/**
 * Splits a progress-bar widget into non-overlapping label, wrapper track and fill regions.
 */
public final class UiProgressBarLayout {
    public static final float DEFAULT_LABEL_HEIGHT = 11f;
    public static final float DEFAULT_LABEL_GAP = 3f;
    public static final float DEFAULT_TRACK_HEIGHT = 10f;
    public static final UiInsets DEFAULT_TRACK_INSETS = UiInsets.of(6f, 3f, 6f, 3f);

    private UiProgressBarLayout() {
    }

    public static Resolved resolve(
            float x,
            float y,
            float w,
            float h,
            float requestedLabelHeight,
            float requestedLabelGap,
            float requestedTrackHeight,
            UiInsets requestedTrackInsets,
            float progress
    ) {
        UiBox bounds = UiBox.of(x, y, w, h);
        if (bounds.empty()) return Resolved.empty(bounds);

        float labelHeight = requestedLabelHeight > 0f ? requestedLabelHeight : DEFAULT_LABEL_HEIGHT;
        float labelGap = requestedLabelGap >= 0f ? requestedLabelGap : DEFAULT_LABEL_GAP;
        float trackHeight = requestedTrackHeight > 0f ? requestedTrackHeight : DEFAULT_TRACK_HEIGHT;

        float required = labelHeight + labelGap + trackHeight;
        if (required > bounds.h()) {
            float scale = bounds.h() / Math.max(1f, required);
            labelHeight = Math.max(0f, labelHeight * scale);
            labelGap = Math.max(0f, labelGap * scale);
            trackHeight = Math.max(1f, trackHeight * scale);
        }

        UiBox track = UiBox.of(bounds.x(), bounds.y(), bounds.w(), Math.min(trackHeight, bounds.h()));
        float labelY = track.top() + labelGap;
        float labelH = Math.max(0f, bounds.top() - labelY);
        UiBox label = UiBox.of(bounds.x(), labelY, bounds.w(), labelH);

        UiInsets insets = requestedTrackInsets == null ? DEFAULT_TRACK_INSETS : requestedTrackInsets;
        UiBox fillArea = track.inset(insets);
        UiBox fill = fillArea.withWidth(fillArea.w() * MathUtils.clamp(progress, 0f, 1f));
        float labelBaselineY = label.empty() ? bounds.top() : label.top() - 2f;

        return new Resolved(bounds, label, track, fillArea, fill, labelBaselineY);
    }

    public record Resolved(
            UiBox bounds,
            UiBox label,
            UiBox track,
            UiBox fillArea,
            UiBox fill,
            float labelBaselineY
    ) {
        private static Resolved empty(UiBox bounds) {
            UiBox zero = UiBox.of(bounds.x(), bounds.y(), 0f, 0f);
            return new Resolved(bounds, zero, zero, zero, zero, bounds.y());
        }
    }
}
