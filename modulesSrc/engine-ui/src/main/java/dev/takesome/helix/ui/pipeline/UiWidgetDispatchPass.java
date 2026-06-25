package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.render.UiRenderDiagnostics;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.diagnostics.UiVisibilityDiagnostics;
import dev.takesome.helix.ui.legacy.render.UiWidgetRenderers;

import java.util.List;

/** Dispatches visible widgets to primitive renderers selected by data. */
public final class UiWidgetDispatchPass {
    private final UiWidgetRenderers widgets;
    private final UiWidgetTracePass traces;
    private final UiRenderDiagnostics diagnostics;

    public UiWidgetDispatchPass(UiWidgetRenderers widgets, UiWidgetTracePass traces) {
        this(widgets, traces, new UiRenderDiagnostics());
    }

    public UiWidgetDispatchPass(UiWidgetRenderers widgets, UiWidgetTracePass traces, UiRenderDiagnostics diagnostics) {
        this.widgets = widgets;
        this.traces = traces;
        this.diagnostics = diagnostics == null ? new UiRenderDiagnostics() : diagnostics;
    }

    public void dispatch(
            UiBindingSource binding,
            SpriteBatch batch,
            BitmapFont font,
            UiRect panelRect,
            List<UiWidgetDefinition> definitions,
            String panelKey,
            float uiTime,
            boolean drawTracers
    ) {
        if (definitions == null || widgets == null) return;
        for (int i = 0; i < definitions.size(); i++) {
            UiWidgetDefinition widget = definitions.get(i);
            if (widget == null) continue;
            String key = panelKey + "/" + UiPanelLayoutPass.widgetKey(widget, i);
            UiVisibilityDiagnostics.Decision decision = UiPanelLayoutPass.visibilityDecision(UiPanelLayoutPass.widgetTarget(widget, i, "visible"), widget.visible, binding);
            if (!decision.visible()) {
                diagnostics.hiddenWidget(panelKey, i, widget, decision);
                continue;
            }
            String primitiveId = widgets.primitiveId(widget);
            boolean rendered = widgets.render(binding, batch, font, panelRect, widget, key, uiTime);
            if (!rendered) diagnostics.missingWidgetRenderer(panelKey, i, widget, primitiveId, widgets.primitiveRegistry().ids());
            if (drawTracers && traces != null) traces.drawWidgetTrace(batch, panelRect, widget);
        }
    }
}
