package dev.takesome.helix.devTools;

import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.SceneNode;
import dev.takesome.helix.ui.render.awt.AwtUiRenderContext;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Map;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.apache.logging.log4j.Logger;

/**
 * Floating native host for engine-ui DevTools content.
 *
 * <p>Swing/AWT is used only as a top-level borderless window host. The visible UI is
 * still rendered by {@link HtmlDevToolsInspectorNode} through engine-ui/AWT render
 * context, not by desktop widget inspector controls.</p>
 */
public final class HtmlDevToolsWindow {
    private static final Logger LOG = EngineLog.logger(HtmlDevToolsWindow.class);
    private static final HtmlDevToolsController CONTROLLER = new HtmlDevToolsController();
    private static volatile HtmlInspectionTarget target = HtmlInspectionTarget.empty();
    private static FloatingHost host;
    private static HtmlDevToolsProcessHost processHost;

    private HtmlDevToolsWindow() {
    }

    public static boolean toggle(HtmlInspectionTarget nextTarget) {
        target = sanitize(nextTarget);
        HtmlDevToolsRuntime.update(target.runtimeSource());
        if (preferChildProcess()) {
            if (processHost != null && processHost.open()) return close();
            return open(target);
        }
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("HTML DevTools floating window unavailable: AWT GraphicsEnvironment is headless");
            return false;
        }
        SwingUtilities.invokeLater(() -> safeRun("toggle floating window", () -> {
            if (host != null && host.open()) closeOnEdt();
            else openOnEdt(target);
        }));
        return true;
    }

    public static boolean open(HtmlInspectionTarget nextTarget) {
        target = sanitize(nextTarget);
        HtmlDevToolsRuntime.update(target.runtimeSource());
        if (preferChildProcess()) {
            CONTROLLER.open();
            if (processHost == null) processHost = new HtmlDevToolsProcessHost(HtmlDevToolsWindow::applyRemoteAction);
            if (processHost.start()) {
                processHost.send(snapshotForProcess());
                return true;
            }
            LOG.warn("HTML DevTools child process unavailable; falling back to in-process JWindow");
        }
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("HTML DevTools floating window unavailable: AWT GraphicsEnvironment is headless");
            return false;
        }
        SwingUtilities.invokeLater(() -> safeRun("open floating window", () -> openOnEdt(target)));
        return true;
    }

    public static boolean refresh(HtmlInspectionTarget nextTarget) {
        target = sanitize(nextTarget);
        if (!HtmlDevToolsRuntime.hasUnsavedChanges()) HtmlDevToolsRuntime.update(target.runtimeSource());
        if (preferChildProcess() && processHost != null && processHost.open()) {
            processHost.send(snapshotForProcess());
            return true;
        }
        FloatingHost current = host;
        if (current != null && current.open()) SwingUtilities.invokeLater(() -> safeRun("refresh floating window", current::repaint));
        return current != null && current.open();
    }

    public static boolean close() {
        if (processHost != null) {
            processHost.close();
            processHost = null;
        }
        if (GraphicsEnvironment.isHeadless()) {
            CONTROLLER.close();
            return true;
        }
        SwingUtilities.invokeLater(() -> safeRun("close floating window", HtmlDevToolsWindow::closeOnEdt));
        return true;
    }

    public static boolean isOpen() {
        if (processHost != null && processHost.open()) return true;
        FloatingHost current = host;
        return current != null && current.open();
    }

    public static HtmlDevToolsSession session() {
        FloatingHost current = host;
        return current == null ? CONTROLLER.session() : current.session();
    }

    public static HtmlInspectionTarget target() {
        return target;
    }

    public static HtmlDevToolsSnapshot snapshot() {
        if (processHost != null && processHost.open()) return snapshotForProcess();
        FloatingHost current = host;
        return current == null ? CONTROLLER.snapshot() : current.snapshot();
    }

    private static boolean preferChildProcess() {
        return Boolean.parseBoolean(System.getProperty("helix.ui.devToolsChildProcess", "true"));
    }

    private static HtmlInspectionTarget activeTarget() {
        return HtmlDevToolsRuntime.hasUnsavedChanges() ? HtmlDevToolsRuntime.target() : target;
    }

    private static HtmlDevToolsSnapshot snapshotForProcess() {
        return new HtmlDevToolsSnapshotFactory().create(CONTROLLER.session().opened(), activeTarget());
    }

    private static void applyRemoteAction(HtmlDevToolsRemoteAction action) {
        if (action == null) return;
        String actionId = action.actionId();
        Map<String, String> data = action.data();
        if ("devtools.undo".equals(actionId)) {
            HtmlDevToolsRuntime.undo();
        } else if ("devtools.redo".equals(actionId)) {
            HtmlDevToolsRuntime.redo();
        } else if ("devtools.save".equals(actionId)) {
            HtmlDevToolsRuntime.saveChanges();
        } else if ("devtools.node.hover".equals(actionId)) {
            if (action.nodeId() > 0) HtmlDevToolsRuntime.highlightNode(action.nodeId());
            else HtmlDevToolsRuntime.clearHighlight();
        } else if ("devtools.tab.select".equals(actionId)) {
            CONTROLLER.selectTab(data.get("tab"));
        } else if ("devtools.node.select".equals(actionId)) {
            CONTROLLER.selectNode(action.nodeId());
        } else if ("devtools.pick.toggle".equals(actionId)) {
            CONTROLLER.togglePicker();
        } else if ("devtools.style.toggle".equals(actionId)) {
            HtmlDevToolsRuntime.toggleStyle(action.nodeId(), data.get("style-name"), data.get("style-value"));
        } else if ("devtools.style.edit.commit".equals(actionId)) {
            HtmlDevToolsRuntime.applyStyle(action.nodeId(), data.get("old-name"), data.get("style-name"), data.get("style-value"));
        } else if ("devtools.color.pick".equals(actionId) || "devtools.value.option".equals(actionId)) {
            HtmlDevToolsRuntime.applyStyle(action.nodeId(), data.get("style-name"), data.get("style-name"), data.get("style-value"));
        } else if ("devtools.style.add.commit".equals(actionId)) {
            HtmlDevToolsRuntime.addStyle(action.nodeId(), data.get("style-name"), data.get("style-value"));
        } else if ("devtools.node.context.delete".equals(actionId)) {
            if (HtmlDevToolsRuntime.deleteNode(action.nodeId())) CONTROLLER.selectNode(0);
        } else if ("devtools.node.context.duplicate".equals(actionId)) {
            int duplicate = HtmlDevToolsRuntime.duplicateNode(action.nodeId());
            if (duplicate > 0) CONTROLLER.selectNode(duplicate);
        } else if ("devtools.node.context.edit".equals(actionId)) {
            HtmlDevToolsRuntime.markNodeEditing(action.nodeId());
            CONTROLLER.selectNode(action.nodeId());
            CONTROLLER.selectTab(HtmlDevToolsTab.STYLES.id());
        } else if ("devtools.close".equals(actionId)) {
            close();
            return;
        }
        if (processHost != null && processHost.open()) processHost.send(snapshotForProcess());
    }

    private static void openOnEdt(HtmlInspectionTarget nextTarget) {
        target = sanitize(nextTarget);
        HtmlDevToolsRuntime.update(target.runtimeSource());
        CONTROLLER.open();
        if (host == null) host = new FloatingHost();
        host.open(target);
    }

    private static void closeOnEdt() {
        CONTROLLER.close();
        if (host != null) host.close();
    }

    private static void safeRun(String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException | Error ex) {
            LOG.warn("HTML DevTools {} failed: {}", operation, ex.getMessage(), ex);
        }
    }

    private static HtmlInspectionTarget sanitize(HtmlInspectionTarget nextTarget) {
        return nextTarget == null ? HtmlInspectionTarget.empty() : nextTarget;
    }

    private static final class FloatingHost {
        private static final int DEFAULT_W = 1040;
        private static final int DEFAULT_H = 680;

        private final JWindow window = new JWindow();
        private final DevToolsPanel panel = new DevToolsPanel(window);
        private final Timer repaintTimer = new Timer(33, ignored -> panel.repaint());

        private FloatingHost() {
            window.setName("HELIX engine-ui DevTools");
            window.setContentPane(panel);
            window.setMinimumSize(new Dimension(720, 420));
            window.setSize(DEFAULT_W, DEFAULT_H);
            window.setAlwaysOnTop(true);
            window.setFocusableWindowState(true);
            window.setType(Window.Type.UTILITY);
            try {
                window.setBackground(new Color(0, 0, 0, 0));
            } catch (RuntimeException unsupportedTranslucency) {
                LOG.debug("HTML DevTools transparent JWindow background unsupported: {}", unsupportedTranslucency.getMessage());
            }
            placeWindow();
        }

        boolean open() {
            return window.isVisible();
        }

        void open(HtmlInspectionTarget nextTarget) {
            if (!HtmlDevToolsRuntime.hasUnsavedChanges()) HtmlDevToolsRuntime.update(sanitize(nextTarget).runtimeSource());
            if (!window.isVisible()) {
                window.setVisible(true);
                LOG.info("HTML DevTools floating window opened at {} size {}x{}", window.getLocation(), window.getWidth(), window.getHeight());
            }
            window.setAlwaysOnTop(true);
            window.toFront();
            window.requestFocus();
            panel.requestFocusInWindow();
            repaintTimer.start();
            panel.repaint();
        }

        void close() {
            repaintTimer.stop();
            window.setVisible(false);
            LOG.info("HTML DevTools floating window closed");
        }

        void repaint() {
            panel.repaint();
        }

        HtmlDevToolsSession session() {
            return panel.session();
        }

        HtmlDevToolsSnapshot snapshot() {
            return panel.snapshot();
        }

        private void placeWindow() {
            Rectangle bounds = screenBounds();
            int x = bounds.x + Math.max(24, (bounds.width - DEFAULT_W) / 2);
            int y = bounds.y + Math.max(24, (bounds.height - DEFAULT_H) / 2);
            window.setLocation(x, y);
        }

        private Rectangle screenBounds() {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            return device == null ? new Rectangle(0, 0, 1280, 720) : device.getDefaultConfiguration().getBounds();
        }
    }

    private static final class DevToolsPanel extends JPanel {
        private final JWindow window;
        private final SceneNode root = new SceneNode(1f, 1f);
        private final HtmlDevToolsInspectorNode inspector;
        private final AwtUiRenderContext renderContext = new AwtUiRenderContext();
        private boolean nativeDrag;
        private Point dragOffset;

        private DevToolsPanel(JWindow window) {
            this.window = window;
            this.inspector = new HtmlDevToolsInspectorNode(HtmlDevToolsWindow::close, true);
            setOpaque(true);
            setBackground(new Color(248, 250, 252));
            setFocusable(true);
            root.add(inspector);
            root.attach();
            installListeners();
        }

        HtmlDevToolsSession session() {
            return inspectorSnapshot().session();
        }

        HtmlDevToolsSnapshot snapshot() {
            return inspectorSnapshot();
        }

        private HtmlDevToolsSnapshot inspectorSnapshot() {
            return new HtmlDevToolsSnapshotFactory().create(CONTROLLER.open(), activeTarget());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            resizeRoot();
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                renderContext.begin(g2, getHeight());
                root.render(renderContext);
            } finally {
                renderContext.end();
                g2.dispose();
            }
        }

        private void resizeRoot() {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            root.resize(w, h);
            inspector.setBounds(0f, 0f, w, h);
        }

        private void installListeners() {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    resizeRoot();
                    repaint();
                }
            });

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    if (nativeDragZone(event)) {
                        nativeDrag = true;
                        Point screen = event.getLocationOnScreen();
                        Point location = window.getLocationOnScreen();
                        dragOffset = new Point(screen.x - location.x, screen.y - location.y);
                        return;
                    }
                    route(mouseDown(event));
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (nativeDrag) {
                        nativeDrag = false;
                        dragOffset = null;
                        return;
                    }
                    route(mouseUp(event));
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (!nativeDragZone(event)) route(mouseClick(event));
                }

                @Override
                public void mouseMoved(MouseEvent event) {
                    route(mouseMove(event));
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    if (nativeDrag && dragOffset != null) {
                        Point screen = event.getLocationOnScreen();
                        window.setLocation(screen.x - dragOffset.x, screen.y - dragOffset.y);
                        return;
                    }
                    route(mouseMove(event));
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    route(UiInputEvent.mouseScroll(engineX(event), engineY(event), 0f, (float) event.getPreciseWheelRotation()));
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    route(UiInputEvent.keyDown(event.getKeyCode()));
                }

                @Override
                public void keyReleased(KeyEvent event) {
                    route(UiInputEvent.keyUp(event.getKeyCode()));
                }

                @Override
                public void keyTyped(KeyEvent event) {
                    char ch = event.getKeyChar();
                    if (!Character.isISOControl(ch)) {
                        route(UiInputEvent.textInput(String.valueOf(ch)));
                    }
                }
            });
        }

        private boolean route(UiInputEvent event) {
            boolean handled = root.handleInput(event);
            if (handled || event.isConsumed()) repaint();
            return handled || event.isConsumed();
        }

        private UiInputEvent mouseMove(MouseEvent event) {
            return UiInputEvent.mouseMove(engineX(event), engineY(event));
        }

        private UiInputEvent mouseDown(MouseEvent event) {
            return UiInputEvent.mouseDown(engineX(event), engineY(event), button(event));
        }

        private UiInputEvent mouseUp(MouseEvent event) {
            return UiInputEvent.mouseUp(engineX(event), engineY(event), button(event));
        }

        private UiInputEvent mouseClick(MouseEvent event) {
            return UiInputEvent.mouseClick(engineX(event), engineY(event), button(event));
        }

        private UiInputEvent.MouseButton button(MouseEvent event) {
            if (SwingUtilities.isRightMouseButton(event)) return UiInputEvent.MouseButton.RIGHT;
            if (SwingUtilities.isMiddleMouseButton(event)) return UiInputEvent.MouseButton.MIDDLE;
            if (SwingUtilities.isLeftMouseButton(event)) return UiInputEvent.MouseButton.LEFT;
            return UiInputEvent.MouseButton.NONE;
        }

        private float engineX(MouseEvent event) {
            return event.getX();
        }

        private float engineY(MouseEvent event) {
            return getHeight() - event.getY();
        }

        private boolean nativeDragZone(MouseEvent event) {
            int y = event.getY();
            int x = event.getX();
            if (y < 0 || y > 88) return false;

            // The whole Material header is draggable, except explicit controls.
            // This keeps tabs/actions clickable while making empty header space work as a drag handle.
            boolean actionControls = y >= 8 && y <= 46 && x >= Math.max(0, getWidth() - 292);
            boolean tabs = y >= 50 && y <= 86 && x >= 12 && x <= Math.min(getWidth() - 12, 592);
            return !actionControls && !tabs;
        }
    }
}
