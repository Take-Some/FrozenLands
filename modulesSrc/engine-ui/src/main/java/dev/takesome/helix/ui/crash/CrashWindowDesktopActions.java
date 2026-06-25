package dev.takesome.helix.ui.crash;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;

/** Desktop integration actions used by the crash window. */
final class CrashWindowDesktopActions {
    private CrashWindowDesktopActions() {
    }

    static boolean copyReport(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return true;
        } catch (RuntimeException error) {
            System.err.println("[HELIX] Failed to copy crash report to clipboard: " + error.getMessage());
            return false;
        }
    }

    static void openReport(Path reportPath) {
        if (reportPath == null) {
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop API is not supported");
            }
            Path target = Files.isRegularFile(reportPath) ? reportPath : reportPath.getParent();
            if (target != null) {
                Desktop.getDesktop().open(target.toFile());
            }
        } catch (Exception error) {
            System.err.println("[HELIX] Failed to open crash report: " + error.getMessage());
        }
    }
}
