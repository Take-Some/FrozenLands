package dev.takesome.helix.ui.crash;

import dev.takesome.helix.crash.CrashReporterService;
import dev.takesome.helix.crash.CrashReporterSettings;
import dev.takesome.helix.crash.count.InMemoryCrashCounter;
import dev.takesome.helix.crash.fingerprint.StackTopCrashFingerprinter;
import dev.takesome.helix.crash.format.PlainTextCrashReportFormatter;
import dev.takesome.helix.crash.io.FileCrashReportWriter;
import dev.takesome.helix.crash.sink.CompositeCrashReportSink;
import dev.takesome.helix.crash.sink.ConsoleCrashReportSink;
import dev.takesome.helix.crash.sink.CrashReportSink;
import dev.takesome.helix.crash.state.CrashPhaseTracker;

import java.util.ArrayList;
import java.util.List;

import static dev.takesome.helix.validation.EngineValidator.notNull;

/** Factory for desktop crash reporter instances backed by engine.ui presentation. */
public final class EngineUiCrashReporter {
    private EngineUiCrashReporter() {
    }

    public static CrashReporterService create(CrashReporterSettings settings) {
        CrashReporterSettings safeSettings = notNull(settings, "settings");
        List<CrashReportSink> sinks = new ArrayList<>();
        if (safeSettings.echoReportsToStderr()) {
            sinks.add(new ConsoleCrashReportSink());
        }
        if (safeSettings.showCrashWindow()) {
            sinks.add(new EngineUiCrashWindowReportSink());
        }
        CrashReportSink sink = sinks.isEmpty() ? CrashReportSink.noop() : new CompositeCrashReportSink(sinks);
        return new CrashReporterService(
                safeSettings,
                new CrashPhaseTracker(),
                new StackTopCrashFingerprinter(),
                new InMemoryCrashCounter(),
                new PlainTextCrashReportFormatter(safeSettings),
                new FileCrashReportWriter(safeSettings),
                sink
        );
    }
}
