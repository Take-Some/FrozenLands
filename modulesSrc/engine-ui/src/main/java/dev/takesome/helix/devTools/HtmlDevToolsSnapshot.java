package dev.takesome.helix.devTools;

import java.io.Serializable;
import dev.takesome.helix.devTools.actions.HtmlActionTraceEntry;
import dev.takesome.helix.devTools.css.HtmlCssBoxSnapshot;
import dev.takesome.helix.devTools.css.HtmlCssPropertySnapshot;
import dev.takesome.helix.devTools.diagnostics.HtmlDiagnosticSnapshot;
import dev.takesome.helix.devTools.dom.HtmlElementSnapshot;
import java.util.List;

public record HtmlDevToolsSnapshot(
        HtmlDevToolsSession session,
        boolean available,
        float viewportWidth,
        float viewportHeight,
        List<HtmlElementSnapshot> elements,
        HtmlElementSnapshot selectedElement,
        List<HtmlCssPropertySnapshot> styles,
        List<HtmlCssPropertySnapshot> computed,
        HtmlCssBoxSnapshot layout,
        HtmlCodeView code,
        List<HtmlDiagnosticSnapshot> diagnostics,
        List<HtmlActionTraceEntry> actions,
        boolean canUndo,
        boolean canRedo,
        boolean dirty
) implements Serializable {
    public HtmlDevToolsSnapshot {
        session = session == null ? HtmlDevToolsSession.closed() : session;
        elements = elements == null ? List.of() : List.copyOf(elements);
        styles = styles == null ? List.of() : List.copyOf(styles);
        computed = computed == null ? List.of() : List.copyOf(computed);
        layout = layout == null ? HtmlCssBoxSnapshot.empty() : layout;
        code = code == null ? HtmlCodeView.empty() : code;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static HtmlDevToolsSnapshot empty(HtmlDevToolsSession session) {
        return new HtmlDevToolsSnapshot(session, false, 0f, 0f, List.of(), null, List.of(), List.of(), HtmlCssBoxSnapshot.empty(), HtmlCodeView.empty(), List.of(), List.of(), false, false, false);
    }
}
