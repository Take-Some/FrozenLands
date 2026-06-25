package dev.takesome.helix.ui.legacy.render;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiRect;

/** Shared text metrics for positioning text inside UI bounds. */
public final class UiTextMetrics {
    private UiTextMetrics() {
    }

    public static float alignedX(UiRect rect, float textWidth, TextAlign align) {
        if (rect == null) return 0f;
        TextAlign resolved = align == null ? TextAlign.LEFT : align;
        return switch (resolved) {
            case CENTER -> rect.x + (rect.w - Math.max(0f, textWidth)) * 0.5f;
            case RIGHT -> rect.right() - Math.max(0f, textWidth);
            case LEFT -> rect.x;
        };
    }

    /** Returns the baseline Y for vertically centering a single line inside rect. */
    public static float centeredBaselineY(float textHeight, UiRect rect) {
        if (rect == null) return 0f;
        return rect.y + (rect.h + Math.max(0f, textHeight)) * 0.5f;
    }

    public static UiRect inset(UiRect rect, float left, float right, float top, float bottom) {
        if (rect == null) return null;
        float x = rect.x + Math.max(0f, left);
        float y = rect.y + Math.max(0f, bottom);
        float w = Math.max(0f, rect.w - Math.max(0f, left) - Math.max(0f, right));
        float h = Math.max(0f, rect.h - Math.max(0f, top) - Math.max(0f, bottom));
        return new UiRect(x, y, w, h);
    }
}
