package dev.takesome.helix.ui.crash;

import dev.takesome.helix.crash.CrashReport;

import java.awt.GraphicsEnvironment;

/** Public desktop crash-window entry point. */
public final class EngineUiCrashWindow {
    private EngineUiCrashWindow() {
    }

    public static void show(CrashReport report, Throwable throwable) {
        if (report == null || throwable == null || GraphicsEnvironment.isHeadless()) {
            return;
        }
        CrashWindowDialogLauncher.show(report, throwable);
    }
}
