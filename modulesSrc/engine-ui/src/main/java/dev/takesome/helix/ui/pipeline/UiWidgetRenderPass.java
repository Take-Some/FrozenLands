package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderDiagnostics;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderers;

/** Coordinates widget dispatch and optional trace rendering for one panel. */
public final class UiWidgetRenderPass {
    private final UiWidgetTracePass traces;
    private final UiWidgetDispatchPass dispatch;
    private final UiRenderDiagnostics diagnostics;

    public UiWidgetRenderPass(UiWidgetRenderers widgets) {
        this(widgets, new UiRenderDiagnostics());
    }

    public UiWidgetRenderPass(UiWidgetRenderers widgets, UiRenderDiagnostics diagnostics) {
        this.diagnostics = diagnostics == null ? new UiRenderDiagnostics() : diagnostics;
        this.traces = new UiWidgetTracePass(widgets);
        this.dispatch = new UiWidgetDispatchPass(widgets, traces, this.diagnostics);
    }

    public void renderWidgets(
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            UiRect panelRect,
            java.util.List<UiWidgetDefinition> definitions,
            String panelKey,
            float uiTime,
            boolean drawTracers
    ) {
        dispatch.dispatch(binding, batch, font, panelRect, definitions, panelKey, uiTime, drawTracers);
    }

    public void drawTracer(SpriteBatch batch, UiRect rect, int depth) {
        traces.drawTrace(batch, rect, depth);
    }

    public UiWidgetTracePass traces() {
        return traces;
    }

    public UiWidgetDispatchPass dispatch() {
        return dispatch;
    }
}
