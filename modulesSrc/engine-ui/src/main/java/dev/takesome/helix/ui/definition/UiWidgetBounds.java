package dev.takesome.helix.ui.definition;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiRect;

/** Bounds helpers for JSON-driven widgets. */
public final class UiWidgetBounds {
    private UiWidgetBounds() {
    }

    public static UiRect rect(UiRect panel, UiWidgetDefinition widget) {
        if (panel == null || widget == null) return new UiRect(0f, 0f, 0f, 0f);
        return new UiRect(panel.x + x(widget), panel.y + y(widget), w(widget), h(widget));
    }

    public static UiRect implicitTextRect(UiRect panel, UiWidgetDefinition widget, float fallbackWidth, float fallbackHeight) {
        if (panel == null || widget == null) return new UiRect(0f, 0f, 0f, 0f);
        float rw = w(widget) > 0f ? w(widget) : Math.max(0f, fallbackWidth);
        float rh = h(widget) > 0f ? h(widget) : Math.max(0f, fallbackHeight);
        return new UiRect(panel.x + x(widget), panel.y + y(widget) - rh, rw, rh);
    }

    public static boolean explicitSize(UiWidgetDefinition widget) {
        return widget != null && w(widget) > 0f && h(widget) > 0f;
    }

    public static float x(UiWidgetDefinition widget) {
        return widget == null ? 0f : widget.layoutResolved ? widget.layoutX : widget.x;
    }

    public static float y(UiWidgetDefinition widget) {
        return widget == null ? 0f : widget.layoutResolved ? widget.layoutY : widget.y;
    }

    public static float w(UiWidgetDefinition widget) {
        return widget == null ? 0f : widget.layoutResolved ? widget.layoutW : widget.w;
    }

    public static float h(UiWidgetDefinition widget) {
        return widget == null ? 0f : widget.layoutResolved ? widget.layoutH : widget.h;
    }

    public static void setResolvedLayout(UiWidgetDefinition widget, float x, float y, float w, float h) {
        if (widget == null) return;
        widget.layoutResolved = true;
        widget.layoutX = finiteNonNegativePosition(x);
        widget.layoutY = finiteNonNegativePosition(y);
        widget.layoutW = finiteSize(w);
        widget.layoutH = finiteSize(h);
    }

    public static void clearResolvedLayout(UiWidgetDefinition widget) {
        if (widget == null) return;
        widget.layoutResolved = false;
        widget.layoutX = 0f;
        widget.layoutY = 0f;
        widget.layoutW = 0f;
        widget.layoutH = 0f;
    }

    public static TextAlign align(UiWidgetDefinition widget, TextAlign fallback) {
        if (widget == null || widget.align == null || widget.align.isBlank()) {
            return fallback == null ? TextAlign.LEFT : fallback;
        }
        try {
            return TextAlign.valueOf(widget.align.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException ignored) {
            return fallback == null ? TextAlign.LEFT : fallback;
        }
    }

    private static float finiteSize(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }

    private static float finiteNonNegativePosition(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
