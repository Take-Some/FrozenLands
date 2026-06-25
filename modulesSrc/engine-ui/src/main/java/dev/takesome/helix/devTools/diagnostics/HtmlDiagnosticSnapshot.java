package dev.takesome.helix.devTools.diagnostics;

import java.io.Serializable;

public record HtmlDiagnosticSnapshot(String severity, String code, String message, String sourcePath, int line, int column, int offset, int length) implements Serializable {
    public HtmlDiagnosticSnapshot {
        severity = severity == null ? "" : severity.trim();
        code = code == null ? "" : code.trim();
        message = message == null ? "" : message.trim();
        sourcePath = sourcePath == null ? "" : sourcePath.trim();
        line = Math.max(0, line);
        column = Math.max(0, column);
        offset = Math.max(0, offset);
        length = Math.max(0, length);
    }
}
