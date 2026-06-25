package dev.takesome.helix.ui.pipeline;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.render.UiRenderDiagnostics;
import dev.takesome.helix.ui.legacy.render.UiPanelRenderEntry;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.diagnostics.UiVisibilityDiagnostics;
import dev.takesome.helix.ui.layout.UiInsets;

import java.util.ArrayList;
import java.util.List;

/** Coordinates panel measurement, widget arrangement and stack layout passes. */
public final class UiPanelLayoutPass {
    private final UiPanelMeasurePass measurePass;
    private final UiWidgetArrangePass arrangePass;
    private final UiStackLayoutPass stackPass;
    private final UiRenderDiagnostics diagnostics;

    public UiPanelLayoutPass() {
        this(new UiPanelMeasurePass(), null, new UiStackLayoutPass(), new UiRenderDiagnostics());
    }

    public UiPanelLayoutPass(UiPanelMeasurePass measurePass, UiWidgetArrangePass arrangePass, UiStackLayoutPass stackPass) {
        this(measurePass, arrangePass, stackPass, new UiRenderDiagnostics());
    }

    public UiPanelLayoutPass(UiPanelMeasurePass measurePass, UiWidgetArrangePass arrangePass, UiStackLayoutPass stackPass, UiRenderDiagnostics diagnostics) {
        this.measurePass = measurePass == null ? new UiPanelMeasurePass() : measurePass;
        this.arrangePass = arrangePass == null ? new UiWidgetArrangePass(this.measurePass) : arrangePass;
        this.stackPass = stackPass == null ? new UiStackLayoutPass() : stackPass;
        this.diagnostics = diagnostics == null ? new UiRenderDiagnostics() : diagnostics;
    }

    public List<UiPanelRenderEntry> layout(UiDocument document, UiBindingSource binding, BitmapFont font, float viewportW, float viewportH) {
        if (document == null || document.panels == null) return List.of();
        ArrayList<UiPanelRenderEntry> panels = new ArrayList<>();
        for (int i = 0; i < document.panels.size(); i++) {
            UiPanelDefinition panel = document.panels.get(i);
            if (panel == null) continue;
            UiVisibilityDiagnostics.Decision decision = visibilityDecision(panelTarget(panel, i, "visible"), panel.visible, binding);
            if (!decision.visible()) {
                diagnostics.hiddenPanel(i, panel, decision);
                continue;
            }
            UiRect rect = measurePass.resolvePanelRect(panel, binding, font, viewportW, viewportH);
            UiWidgetMeasure contentSize = measurePass.measurePanelContent(panel, binding, font);
            UiRect contentRect = contentRect(rect, panel);
            if (overflows(contentSize, contentRect)) {
                diagnostics.panelContentOverflow(i, panel, contentSize, contentRect, panel.overflow);
            }
            arrangePass.arrange(panel, binding, font, rect);
            panels.add(new UiPanelRenderEntry(i, panel, rect));
        }
        List<UiPanelRenderEntry> stacked = stackPass.apply(panels, viewportW, viewportH);
        ArrayList<UiPanelRenderEntry> ordered = new ArrayList<>(stacked);
        ordered.sort(UiPanelRenderEntry.ORDER);
        if (ordered.isEmpty()) diagnostics.noVisiblePanels(document, document.panels.size());
        return List.copyOf(ordered);
    }

    static UiRect contentRect(UiRect rect, UiPanelDefinition panel) {
        if (rect == null) return new UiRect(0f, 0f, 0f, 0f);
        UiInsets padding = UiPanelMeasurePass.panelPadding(panel);
        return new UiRect(
                rect.x + padding.left(),
                rect.y + padding.bottom(),
                Math.max(0f, rect.w - padding.left() - padding.right()),
                Math.max(0f, rect.h - padding.bottom() - padding.top())
        );
    }

    private boolean overflows(UiWidgetMeasure contentSize, UiRect contentRect) {
        if (contentSize == null || contentRect == null) return false;
        return contentSize.w() > contentRect.w + 0.001f || contentSize.h() > contentRect.h + 0.001f;
    }

    public static String panelKey(UiPanelDefinition panel, int index) {
        return panel == null || panel.id == null || panel.id.isBlank() ? "panel#" + index : panel.id;
    }

    public static String panelTarget(UiPanelDefinition panel, int index, String property) {
        return panelKey(panel, index) + "." + property;
    }

    public static String widgetKey(UiWidgetDefinition widget, int index) {
        return widget == null || widget.id == null || widget.id.isBlank() ? "widget#" + index : widget.id;
    }

    public static String widgetTarget(UiWidgetDefinition widget, int index, String property) {
        return widgetKey(widget, index) + "." + property;
    }

    public static UiVisibilityDiagnostics.Decision visibilityDecision(String target, String key, UiBindingSource binding) {
        return UiVisibilityDiagnostics.evaluate(target, key, binding);
    }

    public static boolean visible(String target, String key, UiBindingSource binding) {
        return visibilityDecision(target, key, binding).visible();
    }
}
