package dev.takesome.helix.devTools;

import dev.takesome.helix.devTools.actions.HtmlActionTraceEntry;
import dev.takesome.helix.devTools.diagnostics.HtmlDiagnosticSnapshot;
import dev.takesome.helix.ui.runtime.UiRuntimeInspectionSource;
import java.util.List;

public record HtmlInspectionTarget(
        UiRuntimeInspectionSource runtimeSource,
        String sourcePath,
        String sourceText,
        List<HtmlDiagnosticSnapshot> diagnostics,
        List<HtmlActionTraceEntry> actions
) {
    public HtmlInspectionTarget {
        sourcePath = sourcePath == null ? "" : sourcePath.trim();
        sourceText = sourceText == null ? "" : sourceText;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static HtmlInspectionTarget empty() {
        return new HtmlInspectionTarget(UiRuntimeInspectionSource.empty(), "", "", List.of(), List.of());
    }

    public static HtmlInspectionTarget runtime(UiRuntimeInspectionSource source) {
        return new HtmlInspectionTarget(source == null ? UiRuntimeInspectionSource.empty() : source, "", "", List.of(), List.of());
    }

    public boolean available() { return runtimeSource != null && runtimeSource.available(); }
}
