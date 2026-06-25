package dev.takesome.helix.ui.crash;


import static dev.takesome.helix.validation.EngineValidator.toStringOrEmpty;
import dev.takesome.helix.crash.CrashReport;
import dev.takesome.helix.icons.window.HelixWindowIconInstaller;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates and owns the native AWT dialog shell around the crash canvas. */
final class CrashWindowDialogLauncher {
    private static final Logger LOG = EngineLog.logger(CrashWindowDialogLauncher.class);

    private CrashWindowDialogLauncher() {
    }

    static void show(CrashReport report, Throwable throwable) {
        try {
            CrashWindowModel model = CrashWindowModel.of(report, throwable);
            boolean customFrame = customFrameEnabled();
            LOG.debug(
                    "Opening crash window: customFrame={}, nativeFrame={}, size={}x{}, template={}, css={}",
                    customFrame,
                    System.getProperty("helix.crash.nativeFrame", ""),
                    CrashWindowLayout.WIDTH,
                    CrashWindowLayout.HEIGHT,
                    CrashWindowLayout.TEMPLATE,
                    CrashWindowLayout.CSS_TEMPLATE
            );

            Dialog dialog = new Dialog((Frame) null, "HELIX Crash", true);
            dialog.setUndecorated(customFrame);
            installWindowIcon(dialog, report);
            dialog.setLayout(new BorderLayout());
            dialog.setResizable(false);
            dialog.setAlwaysOnTop(true);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    dialog.dispose();
                }
            });

            CrashWindowCanvas canvas = new CrashWindowCanvas(dialog, model, customFrame);
            Dimension target = new Dimension(CrashWindowLayout.WIDTH, CrashWindowLayout.HEIGHT);
            canvas.setPreferredSize(target);
            canvas.setMinimumSize(target);
            canvas.setSize(target);
            dialog.add(canvas, BorderLayout.CENTER);
            dialog.setMinimumSize(target);
            dialog.pack();
            if (dialog.getWidth() < CrashWindowLayout.WIDTH || dialog.getHeight() < CrashWindowLayout.HEIGHT) {
                dialog.setSize(target);
            }
            dialog.validate();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            dialog.dispose();
        } catch (Throwable windowFailure) {
            LOG.warn("Failed to show engine.ui crash window", windowFailure);
            System.err.println("[HELIX] Failed to show engine.ui crash window: " + windowFailure.getMessage());
            windowFailure.printStackTrace(System.err);
        }
    }

    private static boolean customFrameEnabled() {
        String nativeFrame = System.getProperty("helix.crash.nativeFrame", "").trim();
        if (!nativeFrame.isBlank()) {
            return !Boolean.parseBoolean(nativeFrame);
        }
        String customFrame = System.getProperty("helix.crash.customFrame", "").trim();
        return customFrame.isBlank() || Boolean.parseBoolean(customFrame);
    }

    private static void installWindowIcon(Dialog dialog, CrashReport report) {
        try {
            String gameId = crashGameId(report);
            HelixWindowIconInstaller.install(
                    dialog,
                    gameId,
                    crashApplicationName(),
                    crashAssetsDirectory(gameId),
                    CrashWindowDialogLauncher.class.getClassLoader()
            );
        } catch (RuntimeException error) {
            LOG.warn("Failed to set crash window icon", error);
            System.err.println("[HELIX] Failed to set crash window icon: " + error.getMessage());
        }
    }

    private static String crashApplicationName() {
        String applicationName = System.getProperty("java.awt.Application.name", "");
        return applicationName.isBlank() ? "HELIX Crash Reporter" : applicationName.trim();
    }

    private static String crashGameId(CrashReport report) {
        String configured = System.getProperty("helix.gameId", "");
        if (!configured.isBlank()) {
            return configured.trim();
        }
        if (report == null || report.path() == null) {
            return "helix";
        }
        Path parent = report.path().toAbsolutePath().normalize().getParent();
        if (parent == null || parent.getFileName() == null) {
            return "helix";
        }
        String name = parent.getFileName().toString();
        return "crash-reports".equalsIgnoreCase(name) || name.isBlank() ? "helix" : name;
    }

    private static Path crashAssetsDirectory(String gameId) {
        Path explicitGameAssets = existingDirectory(System.getProperty("game.assets.dir", ""));
        if (explicitGameAssets != null) {
            return explicitGameAssets;
        }

        Path assetsRoot = existingDirectory(System.getProperty("assets.rootDir", ""));
        if (assetsRoot != null) {
            Path gameAssets = assetsRoot.resolve(gameId).toAbsolutePath().normalize();
            return Files.isDirectory(gameAssets) ? gameAssets : assetsRoot;
        }

        Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        String cwdName = toStringOrEmpty(cwd.getFileName());
        if ("assets".equalsIgnoreCase(cwdName)) {
            Path gameAssets = cwd.resolve(gameId).toAbsolutePath().normalize();
            return Files.isDirectory(gameAssets) ? gameAssets : cwd;
        }

        Path localAssets = cwd.resolve("assets").toAbsolutePath().normalize();
        if (Files.isDirectory(localAssets)) {
            Path gameAssets = localAssets.resolve(gameId).toAbsolutePath().normalize();
            return Files.isDirectory(gameAssets) ? gameAssets : localAssets;
        }
        return null;
    }

    private static Path existingDirectory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(value).toAbsolutePath().normalize();
            return Files.isDirectory(path) ? path : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
