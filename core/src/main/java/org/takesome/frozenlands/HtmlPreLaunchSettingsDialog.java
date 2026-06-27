package org.takesome.frozenlands;

import com.jme3.system.AppSettings;
import dev.takesome.htmldom.desktop.HtmlDomSwingPanel;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.BackingStoreException;

final class HtmlPreLaunchSettingsDialog {
    private static final String PREFERENCES_KEY = "FrozenLands";
    private static final Path UI_ROOT = Path.of("assets", "ui");
    private static final int[] SAMPLE_VALUES = {0, 2, 4, 8, 16};
    private static final String[] SAMPLE_LABELS = {"Off", "2x MSAA", "4x MSAA", "8x MSAA", "16x MSAA"};

    private HtmlPreLaunchSettingsDialog() {
    }

    static AppSettings request(String title, AppSettings initialSettings) {
        HtmlPreLaunchSettingsDialog.Controller controller = new Controller(title, initialSettings);
        return controller.show();
    }

    private static final class Controller {
        private final String title;
        private final AppSettings initialSettings;
        private final List<Resolution> resolutions;
        private final Resolution nativeResolution;
        private final AtomicReference<AppSettings> result = new AtomicReference<>();

        private HtmlDomSwingPanel panel;
        private JDialog dialog;
        private int resolutionIndex;
        private int sampleIndex;
        private boolean fullscreen;
        private boolean centerWindow;
        private boolean vsync;
        private Preset preset = Preset.CUSTOM;

        private Controller(String title, AppSettings initialSettings) {
            this.title = title;
            this.initialSettings = Objects.requireNonNull(initialSettings, "initialSettings");
            this.nativeResolution = currentResolution();
            this.resolutions = availableResolutions(nativeResolution);
            applyInitialSettings(initialSettings);
        }

        private AppSettings show() {
            try {
                panel = new HtmlDomSwingPanel(readHtml(), readCss());
                panel.setPreferredSize(new Dimension(960, 640));
                panel.setMinimumSize(new Dimension(860, 560));
                panel.setSize(960, 640);
                installActions();
                refresh();
                panel.ensureLayout();

                dialog = new JDialog((java.awt.Frame) null, "FrozenLands Launcher", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setContentPane(panel);
                installKeyboardShortcuts(dialog);
                dialog.pack();
                dialog.setMinimumSize(new Dimension(860, 560));
                dialog.setLocationRelativeTo(null);
                SwingUtilities.invokeLater(panel::requestFocusInWindow);
                dialog.setVisible(true);
                return result.get();
            } catch (RuntimeException | IOException exception) {
                throw new IllegalStateException("HTML pre-launch settings dialog failed", exception);
            }
        }

        private void installKeyboardShortcuts(JDialog dialog) {
            dialog.getRootPane().registerKeyboardAction(event -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            dialog.getRootPane().registerKeyboardAction(event -> confirm(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        private void installActions() {
            on("preset-performance", () -> applyPreset(Preset.PERFORMANCE));
            on("preset-balanced", () -> applyPreset(Preset.BALANCED));
            on("preset-quality", () -> applyPreset(Preset.QUALITY));
            on("preset-fullscreen", () -> applyPreset(Preset.NATIVE_FULLSCREEN));
            on("resolution-prev", () -> stepResolution(-1));
            on("resolution-next", () -> stepResolution(1));
            on("msaa-prev", () -> stepSamples(-1));
            on("msaa-next", () -> stepSamples(1));
            on("fullscreen-toggle", this::toggleFullscreen);
            on("center-toggle", this::toggleCenterWindow);
            on("vsync-toggle", this::toggleVsync);
            on("prelaunch-reset", this::resetDefaults);
            on("prelaunch-cancel", this::cancel);
            on("prelaunch-launch", this::confirm);
        }

        private void on(String id, Runnable action) {
            panel.addEventListener(id, "click", event -> {
                event.preventDefault();
                System.out.println("[PreLaunch] click id=" + id);
                action.run();
                panel.invalidateLayout();
                panel.ensureLayout();
                panel.repaintHost();
            });
        }

        private void applyInitialSettings(AppSettings settings) {
            Resolution selected = new Resolution(settings.getWidth(), settings.getHeight());
            resolutionIndex = Math.max(0, resolutions.indexOf(selected));
            if (resolutionIndex < 0) {
                resolutions.add(selected);
                resolutions.sort(Comparator.comparingInt((Resolution resolution) -> resolution.width()).thenComparingInt(Resolution::height));
                resolutionIndex = resolutions.indexOf(selected);
            }
            sampleIndex = sampleIndex(settings.getSamples());
            fullscreen = settings.isFullscreen();
            centerWindow = settings.getCenterWindow();
            vsync = settings.isVSync();
            if (fullscreen) {
                sampleIndex = 0;
                centerWindow = false;
            }
            preset = Preset.CUSTOM;
        }

        private void resetDefaults() {
            Resolution selected = nativeResolution;
            resolutionIndex = Math.max(0, resolutions.indexOf(selected));
            sampleIndex = 0;
            fullscreen = false;
            centerWindow = true;
            vsync = false;
            preset = Preset.CUSTOM;
            refresh();
        }

        private void applyPreset(Preset nextPreset) {
            preset = nextPreset;
            resolutionIndex = Math.max(0, resolutions.indexOf(nativeResolution));
            switch (nextPreset) {
                case PERFORMANCE -> {
                    fullscreen = false;
                    sampleIndex = 0;
                    centerWindow = true;
                    vsync = false;
                }
                case BALANCED -> {
                    fullscreen = false;
                    sampleIndex = sampleIndex(4);
                    centerWindow = true;
                    vsync = true;
                }
                case QUALITY -> {
                    fullscreen = false;
                    sampleIndex = sampleIndex(8);
                    centerWindow = true;
                    vsync = true;
                }
                case NATIVE_FULLSCREEN -> {
                    fullscreen = true;
                    sampleIndex = 0;
                    centerWindow = false;
                    vsync = true;
                }
                case CUSTOM -> {
                }
            }
            refresh();
        }

        private void stepResolution(int delta) {
            resolutionIndex = wrap(resolutionIndex + delta, resolutions.size());
            preset = Preset.CUSTOM;
            refresh();
        }

        private void stepSamples(int delta) {
            if (fullscreen) {
                return;
            }
            sampleIndex = wrap(sampleIndex + delta, SAMPLE_VALUES.length);
            preset = Preset.CUSTOM;
            refresh();
        }

        private void toggleFullscreen() {
            fullscreen = !fullscreen;
            if (fullscreen) {
                sampleIndex = 0;
                centerWindow = false;
            } else {
                centerWindow = true;
            }
            preset = Preset.CUSTOM;
            refresh();
        }

        private void toggleCenterWindow() {
            if (fullscreen) {
                return;
            }
            centerWindow = !centerWindow;
            preset = Preset.CUSTOM;
            refresh();
        }

        private void toggleVsync() {
            vsync = !vsync;
            preset = Preset.CUSTOM;
            refresh();
        }

        private void cancel() {
            result.set(null);
            if (dialog != null) {
                dialog.dispose();
            }
        }

        private void confirm() {
            AppSettings settings = new AppSettings(true);
            settings.copyFrom(initialSettings);
            Resolution selected = selectedResolution();
            applyDisplayMode(settings, selected, fullscreen);
            settings.setFullscreen(fullscreen);
            settings.setCenterWindow(!fullscreen && centerWindow);
            settings.setSamples(fullscreen ? 0 : SAMPLE_VALUES[sampleIndex]);
            settings.setVSync(vsync);
            settings.setTitle(title);
            try {
                settings.save(PREFERENCES_KEY);
            } catch (BackingStoreException ignored) {
            }
            result.set(settings);
            if (dialog != null) {
                dialog.dispose();
            }
        }

        private void refresh() {
            Resolution selected = selectedResolution();
            setText("resolution-value", selected.shortText() + (selected.equals(nativeResolution) ? " · Native" : ""));
            setText("msaa-value", fullscreen ? "Off" : SAMPLE_LABELS[sampleIndex]);
            setText("fullscreen-toggle", fullscreen ? "Fullscreen" : "Windowed");
            setText("center-toggle", fullscreen ? "Center window: Disabled" : "Center window: " + (centerWindow ? "On" : "Off"));
            setText("vsync-toggle", "VSync: " + (vsync ? "On" : "Off"));
            setText("prelaunch-summary", summary(selected));
            setSelected("preset-performance", preset == Preset.PERFORMANCE);
            setSelected("preset-balanced", preset == Preset.BALANCED);
            setSelected("preset-quality", preset == Preset.QUALITY);
            setSelected("preset-fullscreen", preset == Preset.NATIVE_FULLSCREEN);
            setEnabled("fullscreen-toggle", fullscreen);
            setEnabled("center-toggle", !fullscreen && centerWindow);
            setEnabled("vsync-toggle", vsync);
            setAttribute("center-toggle", "aria-disabled", fullscreen ? "true" : "false");
            panel.invalidateLayout();
            panel.ensureLayout();
            panel.repaintHost();
        }

        private String summary(Resolution selected) {
            String mode = fullscreen ? "Fullscreen" : "Windowed";
            String samples = fullscreen ? "MSAA off" : SAMPLE_LABELS[sampleIndex];
            String sync = vsync ? "VSync on" : "VSync off";
            return mode + " · " + selected.shortText() + " · " + samples + " · " + sync;
        }

        private Resolution selectedResolution() {
            return resolutions.get(Math.max(0, Math.min(resolutionIndex, resolutions.size() - 1)));
        }

        private void setText(String id, String value) {
            panel.document().getElementById(id).ifPresent(element -> {
                element.clearChildren();
                element.appendChild(element.ownerDocument().createText(value == null ? "" : value));
            });
        }

        private void setSelected(String id, boolean selected) {
            setAttribute(id, "data-selected", selected ? "true" : "false");
            setAttribute(id, "aria-pressed", selected ? "true" : "false");
        }

        private void setEnabled(String id, boolean enabled) {
            setAttribute(id, "data-enabled", enabled ? "true" : "false");
        }

        private void setAttribute(String id, String name, String value) {
            panel.document().getElementById(id).ifPresent(element -> element.setAttribute(name, value));
        }

        private String readHtml() throws IOException {
            return Files.readString(UI_ROOT.resolve("app/prelaunch.html"), StandardCharsets.UTF_8);
        }

        private String readCss() throws IOException {
            String[] styles = {
                    "screens/prelaunch.css"
            };
            StringBuilder css = new StringBuilder(8192);
            for (String style : styles) {
                css.append("\n/* ").append(style).append(" */\n");
                css.append(Files.readString(UI_ROOT.resolve(style), StandardCharsets.UTF_8));
            }
            return css.toString();
        }
    }

    private enum Preset {
        PERFORMANCE,
        BALANCED,
        QUALITY,
        NATIVE_FULLSCREEN,
        CUSTOM
    }

    private record Resolution(int width, int height) {
        String shortText() {
            return width + " × " + height;
        }
    }

    private static int wrap(int value, int size) {
        if (size <= 0) {
            return 0;
        }
        int result = value % size;
        return result < 0 ? result + size : result;
    }

    private static int sampleIndex(int samples) {
        for (int i = 0; i < SAMPLE_VALUES.length; i++) {
            if (SAMPLE_VALUES[i] == samples) {
                return i;
            }
        }
        return 0;
    }

    private static Resolution currentResolution() {
        DisplayMode mode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        return new Resolution(mode.getWidth(), mode.getHeight());
    }

    private static List<Resolution> availableResolutions(Resolution nativeResolution) {
        TreeSet<Resolution> unique = new TreeSet<>(Comparator
                .comparingInt((Resolution resolution) -> resolution.width)
                .thenComparingInt(resolution -> resolution.height));
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        for (DisplayMode mode : device.getDisplayModes()) {
            int width = mode.getWidth();
            int height = mode.getHeight();
            if (width >= 1024 && height >= 720) {
                unique.add(new Resolution(width, height));
            }
        }
        unique.add(nativeResolution);
        return new ArrayList<>(unique);
    }

    private static void applyDisplayMode(AppSettings settings, Resolution resolution, boolean fullscreen) {
        settings.setResolution(resolution.width(), resolution.height());
        settings.setWindowSize(resolution.width(), resolution.height());
        if (!fullscreen) {
            return;
        }
        DisplayMode mode = bestDisplayMode(resolution);
        settings.setBitsPerPixel(mode.getBitDepth());
        settings.setFrequency(mode.getRefreshRate());
    }

    private static DisplayMode bestDisplayMode(Resolution resolution) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode best = device.getDisplayMode();
        for (DisplayMode mode : device.getDisplayModes()) {
            if (mode.getWidth() != resolution.width() || mode.getHeight() != resolution.height()) {
                continue;
            }
            if (isBetterDisplayMode(mode, best)) {
                best = mode;
            }
        }
        return best;
    }

    private static boolean isBetterDisplayMode(DisplayMode candidate, DisplayMode current) {
        int candidateDepth = normalizeDisplayValue(candidate.getBitDepth());
        int currentDepth = normalizeDisplayValue(current.getBitDepth());
        if (candidateDepth != currentDepth) {
            return candidateDepth > currentDepth;
        }
        int candidateRefresh = normalizeDisplayValue(candidate.getRefreshRate());
        int currentRefresh = normalizeDisplayValue(current.getRefreshRate());
        return candidateRefresh > currentRefresh;
    }

    private static int normalizeDisplayValue(int value) {
        return value == DisplayMode.BIT_DEPTH_MULTI || value == DisplayMode.REFRESH_RATE_UNKNOWN ? 0 : value;
    }
}
