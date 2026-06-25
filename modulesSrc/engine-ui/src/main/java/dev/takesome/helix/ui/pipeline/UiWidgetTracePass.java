package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.definition.UiWidgetBounds;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderers;

/** Draws optional panel/widget debug tracing bounds. */
public final class UiWidgetTracePass {
    private final UiWidgetRenderers widgets;

    public UiWidgetTracePass(UiWidgetRenderers widgets) {
        this.widgets = widgets;
    }

    public void drawPanelTrace(SpriteBatch batch, UiRect rect) {
        drawTrace(batch, rect, 0);
    }

    public void drawWidgetTrace(SpriteBatch batch, UiRect panelRect, UiWidgetDefinition widget) {
        drawTrace(batch, widgetTracerRect(panelRect, widget), 1);
    }

    public void drawTrace(SpriteBatch batch, UiRect rect, int depth) {
        if (widgets == null || batch == null || rect == null || rect.w <= 0f || rect.h <= 0f) return;
        float[] color = depth == 0
                ? new float[]{0.20f, 0.85f, 1.00f, 0.70f}
                : new float[]{1.00f, 0.72f, 0.18f, 0.70f};
        float[] center = new float[]{1.00f, 1.00f, 0.20f, 0.38f};
        float t = depth == 0 ? 2f : 1f;
        widgets.fillRect(batch, rect.x, rect.y, rect.w, t, color, 1f);
        widgets.fillRect(batch, rect.x, rect.top() - t, rect.w, t, color, 1f);
        widgets.fillRect(batch, rect.x, rect.y, t, rect.h, color, 1f);
        widgets.fillRect(batch, rect.right() - t, rect.y, t, rect.h, color, 1f);
        widgets.fillRect(batch, rect.x, rect.y + rect.h * 0.5f, rect.w, 1f, center, 1f);
        widgets.fillRect(batch, rect.x + rect.w * 0.5f, rect.y, 1f, rect.h, center, 1f);
    }

    public UiRect widgetTracerRect(UiRect panelRect, UiWidgetDefinition widget) {
        if (UiWidgetBounds.explicitSize(widget)) return UiWidgetBounds.rect(panelRect, widget);
        return UiWidgetBounds.implicitTextRect(panelRect, widget, Math.max(24f, panelRect.w - UiWidgetBounds.x(widget)), 18f);
    }
}
