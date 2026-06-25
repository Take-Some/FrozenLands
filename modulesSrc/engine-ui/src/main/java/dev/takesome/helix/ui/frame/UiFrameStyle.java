package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.model.UiColor;

/** Resolved style values attached to a UI IR node. */
public final class UiFrameStyle {
    private static final UiFrameStyle EMPTY = new UiFrameStyle(null, null, null, 0f, 1f);

    private final UiColor backgroundColor;
    private final UiColor foregroundColor;
    private final UiColor borderColor;
    private final float borderWidth;
    private final float opacity;

    public UiFrameStyle(UiColor backgroundColor, UiColor foregroundColor, UiColor borderColor, float borderWidth, float opacity) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
        this.borderWidth = Float.isFinite(borderWidth) ? Math.max(0f, borderWidth) : 0f;
        this.opacity = Float.isFinite(opacity) ? Math.max(0f, Math.min(1f, opacity)) : 1f;
    }

    public static UiFrameStyle empty() {
        return EMPTY;
    }

    public UiColor backgroundColor() {
        return backgroundColor;
    }

    public UiColor foregroundColor() {
        return foregroundColor;
    }

    public UiColor borderColor() {
        return borderColor;
    }

    public float borderWidth() {
        return borderWidth;
    }

    public float opacity() {
        return opacity;
    }
}
