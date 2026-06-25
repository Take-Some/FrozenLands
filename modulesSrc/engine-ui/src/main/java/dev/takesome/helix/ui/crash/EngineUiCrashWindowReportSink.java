package dev.takesome.helix.ui.crash;

import dev.takesome.helix.crash.CrashReport;
import dev.takesome.helix.crash.sink.CrashReportSink;

import static dev.takesome.helix.validation.EngineValidator.notNull;

/** Crash sink that displays a retained engine.ui crash window. */
public final class EngineUiCrashWindowReportSink implements CrashReportSink {
    @Override
    public void publish(CrashReport report, Throwable throwable) {
        EngineUiCrashWindow.show(notNull(report, "report"), notNull(throwable, "throwable"));
    }
}
