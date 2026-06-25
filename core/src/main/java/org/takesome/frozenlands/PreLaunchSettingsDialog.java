package org.takesome.frozenlands;

import com.jme3.system.AppSettings;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.BackingStoreException;

final class PreLaunchSettingsDialog {
    private static final String PREFERENCES_KEY = "FrozenLands";
    private static final Integer[] SAMPLE_OPTIONS = {0, 2, 4, 8, 16};

    private PreLaunchSettingsDialog() {
    }

    static AppSettings request(String title) {
        AppSettings settings = defaultSettings(title);
        loadSavedSettings(settings);
        if (GraphicsEnvironment.isHeadless()) {
            return settings;
        }

        AtomicReference<AppSettings> selected = new AtomicReference<>();
        Runnable showDialog = () -> selected.set(showDialog(settings));
        if (SwingUtilities.isEventDispatchThread()) {
            showDialog.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(showDialog);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            } catch (InvocationTargetException exception) {
                throw new IllegalStateException("Pre-launch settings dialog failed", exception.getCause());
            }
        }
        return selected.get();
    }

    private static AppSettings defaultSettings(String title) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle(title);
        DisplayMode currentMode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDisplayMode();
        settings.setResolution(currentMode.getWidth(), currentMode.getHeight());
        settings.setWindowSize(currentMode.getWidth(), currentMode.getHeight());
        settings.setFullscreen(false);
        settings.setCenterWindow(true);
        settings.setSamples(0);
        settings.setVSync(false);
        return settings;
    }

    private static void loadSavedSettings(AppSettings settings) {
        try {
            settings.load(PREFERENCES_KEY);
        } catch (BackingStoreException ignored) {
        }
    }

    private static AppSettings showDialog(AppSettings initialSettings) {
        SettingsForm form = new SettingsForm(initialSettings);
        JDialog dialog = new JDialog((java.awt.Frame) null, "FrozenLands PreLaunch", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(form.panel(), BorderLayout.CENTER);
        dialog.add(buttonPanel(dialog, form), BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return form.result();
    }

    private static JPanel buttonPanel(JDialog dialog, SettingsForm form) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> {
            form.cancel();
            dialog.dispose();
        });
        JButton start = new JButton("Start");
        start.addActionListener(event -> {
            form.confirm();
            dialog.dispose();
        });
        panel.add(cancel);
        panel.add(start);
        return panel;
    }

    private static final class SettingsForm {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JComboBox<Resolution> resolution;
        private final JComboBox<Integer> samples;
        private final JCheckBox fullscreen;
        private final JCheckBox centerWindow;
        private final JCheckBox vsync;
        private final AppSettings initialSettings;
        private AppSettings result;

        private SettingsForm(AppSettings initialSettings) {
            this.initialSettings = initialSettings;
            this.resolution = new JComboBox<>(availableResolutions().toArray(new Resolution[0]));
            this.samples = new JComboBox<>(SAMPLE_OPTIONS);
            this.fullscreen = new JCheckBox("Fullscreen");
            this.centerWindow = new JCheckBox("Center game window");
            this.vsync = new JCheckBox("VSync");

            selectResolution(new Resolution(initialSettings.getWidth(), initialSettings.getHeight()));
            samples.setSelectedItem(normalizeSamples(initialSettings.getSamples()));
            fullscreen.setSelected(initialSettings.isFullscreen());
            centerWindow.setSelected(initialSettings.getCenterWindow());
            vsync.setSelected(initialSettings.isVSync());
            buildPanel();
        }

        private JPanel panel() {
            return panel;
        }

        private AppSettings result() {
            return result;
        }

        private void confirm() {
            AppSettings settings = new AppSettings(true);
            settings.copyFrom(initialSettings);
            Resolution selectedResolution = (Resolution) resolution.getSelectedItem();
            boolean fullscreenSelected = fullscreen.isSelected();
            if (selectedResolution != null) {
                applyDisplayMode(settings, selectedResolution, fullscreenSelected);
            }
            settings.setFullscreen(fullscreenSelected);
            settings.setCenterWindow(!fullscreenSelected && centerWindow.isSelected());
            settings.setSamples(fullscreenSelected ? 0 : (Integer) Objects.requireNonNull(samples.getSelectedItem()));
            settings.setVSync(vsync.isSelected());
            settings.setTitle(initialSettings.getTitle());
            try {
                settings.save(PREFERENCES_KEY);
            } catch (BackingStoreException ignored) {
            }
            result = settings;
        }

        private void cancel() {
            result = null;
        }

        private void buildPanel() {
            panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
            addRow(0, "Resolution", resolution);
            addRow(1, "Anti-aliasing samples", samples);
            addRow(2, "Window mode", fullscreen);
            addRow(3, "Placement", centerWindow);
            addRow(4, "Sync", vsync);
        }

        private void addRow(int row, String label, Component component) {
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.WEST;
            labelConstraints.insets = new Insets(4, 0, 4, 12);
            panel.add(new JLabel(label), labelConstraints);

            GridBagConstraints valueConstraints = new GridBagConstraints();
            valueConstraints.gridx = 1;
            valueConstraints.gridy = row;
            valueConstraints.weightx = 1.0;
            valueConstraints.fill = GridBagConstraints.HORIZONTAL;
            valueConstraints.insets = new Insets(4, 0, 4, 0);
            panel.add(component, valueConstraints);
        }

        private void selectResolution(Resolution target) {
            for (int i = 0; i < resolution.getItemCount(); i++) {
                Resolution option = resolution.getItemAt(i);
                if (option.equals(target)) {
                    resolution.setSelectedIndex(i);
                    return;
                }
            }
            resolution.addItem(target);
            resolution.setSelectedItem(target);
        }
    }

    private static Integer normalizeSamples(int samples) {
        for (Integer option : SAMPLE_OPTIONS) {
            if (option == samples) {
                return option;
            }
        }
        return SAMPLE_OPTIONS[0];
    }

    private static void applyDisplayMode(AppSettings settings, Resolution resolution, boolean fullscreen) {
        settings.setResolution(resolution.width, resolution.height);
        settings.setWindowSize(resolution.width, resolution.height);
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
            if (mode.getWidth() != resolution.width || mode.getHeight() != resolution.height) {
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

    private static List<Resolution> availableResolutions() {
        TreeSet<Resolution> resolutions = new TreeSet<>(Comparator
                .comparingInt((Resolution resolution) -> resolution.width)
                .thenComparingInt(resolution -> resolution.height));
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        for (DisplayMode mode : device.getDisplayModes()) {
            if (mode.getWidth() >= 800 && mode.getHeight() >= 600) {
                resolutions.add(new Resolution(mode.getWidth(), mode.getHeight()));
            }
        }
        DisplayMode currentMode = device.getDisplayMode();
        resolutions.add(new Resolution(currentMode.getWidth(), currentMode.getHeight()));
        return new ArrayList<>(resolutions);
    }

    private static final class Resolution {
        private final int width;
        private final int height;

        private Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Resolution resolution)) {
                return false;
            }
            return width == resolution.width && height == resolution.height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(width, height);
        }

        @Override
        public String toString() {
            return width + " x " + height;
        }
    }
}
