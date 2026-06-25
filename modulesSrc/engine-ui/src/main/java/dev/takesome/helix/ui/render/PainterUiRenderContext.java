package dev.takesome.helix.ui.render;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.render.UiPainter;
import dev.takesome.helix.ui.model.UiRect;

/**
 * UiRenderContext adapter over the existing UiPainter contract.
 */
public final class PainterUiRenderContext implements UiRenderContext {
    private final UiPainter<?> painter;

    public PainterUiRenderContext(UiPainter<?> painter) {
        if (painter == null) throw new IllegalArgumentException("painter must not be null");
        this.painter = painter;
    }

    @Override
    public void fill(UiRect rect, UiColor color) {
        if (rect == null) return;
        painter.fill(rect.x, rect.y, rect.w, rect.h, color);
    }

    @Override
    public void text(String text, UiRect rect, float scale, UiColor color, TextAlign align) {
        if (rect == null || text == null || text.isEmpty()) return;

        TextAlign resolvedAlign = align == null ? TextAlign.LEFT : align;
        float x = textX(rect, resolvedAlign);
        float y = rect.y + rect.h * 0.5f;

        painter.text(text, x, y, scale, color, resolvedAlign);
    }

    private float textX(UiRect rect, TextAlign align) {
        switch (align) {
            case CENTER:
                return rect.x + rect.w * 0.5f;
            case RIGHT:
                return rect.right();
            case LEFT:
            default:
                return rect.x;
        }
    }
}
