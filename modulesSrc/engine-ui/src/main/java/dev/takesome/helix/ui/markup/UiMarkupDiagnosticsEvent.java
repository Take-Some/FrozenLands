package dev.takesome.helix.ui.markup;



import dev.takesome.helix.events.api.Event;

import dev.takesome.helix.ui.html.UiHtmlDiagnostic;



import java.util.List;



/** Event emitted when a markup document compiles with recoverable parser diagnostics. */

public final class UiMarkupDiagnosticsEvent implements Event {

    public static final String TYPE = "ui.markup.diagnostics";



    private final long timestamp = System.currentTimeMillis();

    private final List<UiHtmlDiagnostic> diagnostics;



    public UiMarkupDiagnosticsEvent(List<UiHtmlDiagnostic> diagnostics) {

        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);

    }



    public long timestamp() { return timestamp; }

    public String type() { return TYPE; }

    public String dedupKey() { return TYPE + ":" + timestamp; }

    public List<UiHtmlDiagnostic> diagnostics() { return diagnostics; }

    public boolean hasDiagnostics() { return !diagnostics.isEmpty(); }

}
