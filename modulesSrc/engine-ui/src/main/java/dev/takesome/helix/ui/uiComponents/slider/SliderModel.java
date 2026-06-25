package dev.takesome.helix.ui.uiComponents.slider;

import dev.takesome.helix.ui.model.UiRect;

/** Numeric range, snapping and pointer-to-value conversion for sliders. */
final class SliderModel {
    static final double EPSILON = 0.000001;

    private static final float MIN_TRACK_INSET = 8f;
    private static final float MAX_TRACK_INSET = 18f;
    private static final float VALUE_BOX_WIDTH = 52f;
    private static final float VALUE_BOX_GAP = 16f;
    private static final float VALUE_BOX_TOP_INSET = 3f;
    private static final float VALUE_BOX_BOTTOM_INSET = 3f;

    private double min;
    private double max;
    private double step;
    private double value;

    SliderModel(double min, double max, double step, double value) {
        setRange(min, max, step);
        setValue(value);
    }

    double min() { return min; }
    double max() { return max; }
    double step() { return step; }
    double value() { return value; }

    void setRange(double min, double max, double step) {
        if (!Double.isFinite(min)) min = 0.0;
        if (!Double.isFinite(max)) max = min + 1.0;
        if (max < min) {
            double t = min;
            min = max;
            max = t;
        }
        if (Math.abs(max - min) < EPSILON) max = min + 1.0;
        this.min = min;
        this.max = max;
        this.step = Double.isFinite(step) && step > 0.0 ? step : 0.0;
        setValue(value);
    }

    boolean setValue(double nextValue) {
        double next = snap(clamp(nextValue));
        if (Math.abs(value - next) < EPSILON) return false;
        value = next;
        return true;
    }

    double ratio() {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    double valueFromPointer(float pointerX, UiRect bounds) {
        UiRect track = trackBounds(bounds, 1f);
        double ratio = Math.max(0.0, Math.min(1.0, (pointerX - track.x) / Math.max(1f, track.w)));
        return min + (max - min) * ratio;
    }

    UiRect trackBounds(UiRect bounds, float trackHeight) {
        float inset = trackInset(bounds);
        float valueX = bounds.x + bounds.w - valueBoxWidth(bounds);
        float trackRight = Math.max(bounds.x + inset + 1f, valueX - VALUE_BOX_GAP);
        float trackWidth = Math.max(1f, trackRight - (bounds.x + inset));
        float height = Math.max(1f, trackHeight);
        return new UiRect(
                bounds.x + inset,
                bounds.y + bounds.h * 0.5f - height * 0.5f,
                trackWidth,
                height
        );
    }

    UiRect valueBoxBounds(UiRect bounds) {
        float width = valueBoxWidth(bounds);
        return new UiRect(
                bounds.x + bounds.w - width,
                bounds.y + VALUE_BOX_TOP_INSET,
                width,
                Math.max(14f, bounds.h - VALUE_BOX_TOP_INSET - VALUE_BOX_BOTTOM_INSET)
        );
    }

    private float valueBoxWidth(UiRect bounds) {
        return Math.min(VALUE_BOX_WIDTH, Math.max(38f, bounds.w * 0.28f));
    }

    float trackInset(UiRect bounds) {
        return Math.min(MAX_TRACK_INSET, Math.max(MIN_TRACK_INSET, bounds.w * 0.08f));
    }

    private double clamp(double raw) {
        if (!Double.isFinite(raw)) return min;
        return Math.max(min, Math.min(max, raw));
    }

    private double snap(double raw) {
        if (step <= 0.0) return raw;
        return clamp(min + Math.round((raw - min) / step) * step);
    }

    static String format(double value) {
        if (!Double.isFinite(value)) return "0";
        long whole = Math.round(value);
        if (Math.abs(value - whole) < 0.0001) return Long.toString(whole);
        return Double.toString(Math.round(value * 100.0) / 100.0);
    }
}
