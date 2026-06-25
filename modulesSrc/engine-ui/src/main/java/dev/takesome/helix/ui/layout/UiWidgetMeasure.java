package dev.takesome.helix.ui.layout;

/** Measured widget size in UI pixels before final arrangement. */
public record UiWidgetMeasure(float w, float h) {
    public static final UiWidgetMeasure ZERO = new UiWidgetMeasure(0f, 0f);

    public UiWidgetMeasure {
        w = Float.isFinite(w) ? Math.max(0f, w) : 0f;
        h = Float.isFinite(h) ? Math.max(0f, h) : 0f;
    }
}
