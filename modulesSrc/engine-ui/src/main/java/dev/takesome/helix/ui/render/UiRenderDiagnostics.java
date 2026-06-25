package dev.takesome.helix.ui.render;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import dev.takesome.helix.ui.definition.UiDocument;
import dev.takesome.helix.ui.definition.UiPanelDefinition;
import dev.takesome.helix.ui.definition.UiWidgetDefinition;
import dev.takesome.helix.ui.diagnostics.UiVisibilityDiagnostics;
import dev.takesome.helix.ui.layout.UiWidgetMeasure;
import dev.takesome.helix.ui.model.UiRect;

/** Logs render-time UI diagnostics without spamming identical messages every frame. */
public final class UiRenderDiagnostics {
    private static final Logger LOG = EngineLog.logger(UiRenderDiagnostics.class);

    private final Set<String> emitted = ConcurrentHashMap.newKeySet();

    public void hiddenPanel(int index, UiPanelDefinition panel, UiVisibilityDiagnostics.Decision decision) {
        if (panel == null || decision == null || decision.visible()) return;
        String id = panel.id == null || panel.id.isBlank() ? "panel#" + index : panel.id;
        hidden("panel", id, index, decision);
    }

    public void hiddenWidget(String panelKey, int index, UiWidgetDefinition widget, UiVisibilityDiagnostics.Decision decision) {
        if (widget == null || decision == null || decision.visible()) return;
        String id = widget.id == null || widget.id.isBlank() ? panelKey + "/widget#" + index : panelKey + "/" + widget.id;
        hidden("widget", id, index, decision);
    }

    public void missingWidgetRenderer(String panelKey, int index, UiWidgetDefinition widget, String primitiveId, Set<String> registeredPrimitives) {
        if (widget == null) return;
        String id = widget.id == null || widget.id.isBlank() ? panelKey + "/widget#" + index : panelKey + "/" + widget.id;
        String key = "missing-renderer|" + id + "|" + primitiveId;
        if (!emitted.add(key)) return;
        LOG.warn("UI widget renderer missing id='{}' type='{}' primitive='{}' resolvedPrimitive='{}' registeredPrimitives={}",
                id,
                clean(widget.type),
                clean(widget.primitive),
                clean(primitiveId),
                registeredPrimitives);
    }

    public void panelContentOverflow(int index, UiPanelDefinition panel, UiWidgetMeasure contentSize, UiRect contentRect, String overflow) {
        if (panel == null || contentSize == null || contentRect == null) return;
        String id = panel.id == null || panel.id.isBlank() ? "panel#" + index : panel.id;
        String key = "panel-overflow|" + id + "|" + contentSize.w() + "x" + contentSize.h() + "|" + contentRect.w + "x" + contentRect.h;
        if (!emitted.add(key)) return;
        LOG.warn("UI panel content overflow id='{}' index={} overflow='{}' contentSize={}x{} viewport={}x{}; increase panel bounds, enable auto-size, or declare scroll/hidden overflow",
                id,
                index,
                clean(overflow),
                contentSize.w(),
                contentSize.h(),
                contentRect.w,
                contentRect.h);
    }

    public void noVisiblePanels(UiDocument document, int declaredPanels) {
        String key = "no-visible-panels|" + declaredPanels;
        if (!emitted.add(key)) return;
        LOG.warn("UI document produced zero visible panels declaredPanels={} fonts={} nineSlices={} bindingRefs={} bindingPacks={}",
                declaredPanels,
                document == null || document.fonts == null ? 0 : document.fonts.size(),
                document == null || document.nineSlices == null ? 0 : document.nineSlices.size(),
                document == null || document.bindingManifests == null ? 0 : document.bindingManifests.size(),
                document == null || document.bindingPacks == null ? 0 : document.bindingPacks.size());
    }

    private void hidden(String scope, String id, int index, UiVisibilityDiagnostics.Decision decision) {
        String key = scope + "|" + id + "|" + decision.target() + "|" + decision.reason() + "|" + decision.result();
        if (!emitted.add(key)) return;
        LOG.debug("UI {} hidden id='{}' index={} target='{}' reason='{}' result={} descriptorFound={} descriptor='{}' source='{}' expression='{}' fallbackKey='{}' default='{}'",
                scope,
                id,
                index,
                decision.target(),
                decision.reason(),
                decision.result(),
                decision.descriptorFound(),
                decision.descriptorSummary(),
                clean(decision.source()),
                clean(decision.expression()),
                clean(decision.fallbackKey()),
                clean(decision.defaultValue()));
    }

    private static String clean(String value) {
        return emptyIfNull(value);
    }
}
