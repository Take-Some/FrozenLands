package dev.takesome.helix.ui.crash;


import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import dev.takesome.helix.crash.CrashReport;

import java.util.Locale;

/** Formatting helpers for crash-window metadata and report text. */
final class CrashWindowText {
    private CrashWindowText() {
    }

    static String module() {
        return blankTo(System.getProperty("helix.crash.module", ""), "Helix.Core");
    }

    static String timestamp(CrashReport report) {
        return textOrEmpty(report, item -> toStringOrEmpty(item.timestamp()));
    }

    static String errorCode(Throwable throwable) {
        if (throwable == null) {
            return "JVM_UNKNOWN";
        }
        return "JVM_" + throwable.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    static String build() {
        String version = blankTo(System.getProperty("helix.version", ""), "dev");
        String build = System.getProperty("helix.build", "").trim();
        return build.isBlank() ? version : version + " (" + build + ")";
    }

    static String memory() {
        Runtime runtime = Runtime.getRuntime();
        long used = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long max = Math.max(used, runtime.maxMemory());
        return mib(used) + " / " + mib(max);
    }

    static String memoryProgressWidth(float trackWidth) {
        Runtime runtime = Runtime.getRuntime();
        long used = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long max = Math.max(used, runtime.maxMemory());
        if (max <= 0L || trackWidth <= 0f) {
            return "0";
        }
        float ratio = Math.max(0f, Math.min(1f, used / (float) max));
        int width = Math.round(trackWidth * ratio);
        if (used > 0L && width <= 0) {
            width = 3;
        }
        return Integer.toString(Math.max(0, width));
    }

    static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    static String summary(Throwable throwable) {
        String type = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return type;
        }
        return ellipsize(type + ": " + message.trim(), 118);
    }

    static String ellipsize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    static String ellipsizeMiddle(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        if (maxLength < 8) {
            return ellipsize(normalized, maxLength);
        }
        int head = Math.max(1, (maxLength - 1) / 2);
        int tail = Math.max(1, maxLength - 1 - head);
        return normalized.substring(0, head) + "…" + normalized.substring(normalized.length() - tail);
    }

    static String details(CrashReport report, Throwable throwable) {
        return report.shortText()
                + System.lineSeparator()
                + System.lineSeparator()
                + "Exception type: " + throwable.getClass().getName()
                + System.lineSeparator()
                + "Exception message: " + String.valueOf(throwable.getMessage())
                + System.lineSeparator()
                + System.lineSeparator()
                + report.stackTrace();
    }

    private static String mib(long bytes) {
        return (bytes / (1024L * 1024L)) + " MB";
    }
}
