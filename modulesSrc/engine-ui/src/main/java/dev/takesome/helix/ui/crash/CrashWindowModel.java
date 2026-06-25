package dev.takesome.helix.ui.crash;

import dev.takesome.helix.crash.CrashReport;

import java.nio.file.Path;

/** Immutable view-model consumed by the crash-window template and details node. */
record CrashWindowModel(
        String title,
        String summary,
        String phase,
        String context,
        String exceptionType,
        String fingerprint,
        String fingerprintShort,
        String module,
        String subsystem,
        String timestamp,
        String errorCode,
        String build,
        String memory,
        String memoryProgressWidth,
        String count,
        Path reportPath,
        String reportPathDisplay,
        String details
) {
    static CrashWindowModel of(CrashReport report, Throwable throwable) {
        Path path = report.path().toAbsolutePath().normalize();
        String fingerprint = report.fingerprint();
        String phase = CrashWindowText.blankTo(report.phase(), "runtime");
        return new CrashWindowModel(
                "ENGINE CRASH DETECTED",
                CrashWindowText.summary(throwable),
                phase,
                CrashWindowText.blankTo(report.context(), "runtime"),
                throwable.getClass().getSimpleName(),
                fingerprint,
                CrashWindowText.ellipsizeMiddle(fingerprint, 48),
                CrashWindowText.module(),
                phase,
                CrashWindowText.timestamp(report),
                CrashWindowText.errorCode(throwable),
                CrashWindowText.build(),
                CrashWindowText.memory(),
                CrashWindowText.memoryProgressWidth(190f),
                Integer.toString(report.count()),
                path,
                CrashWindowText.ellipsizeMiddle(path.toString(), 66),
                CrashWindowText.details(report, throwable)
        );
    }
}
