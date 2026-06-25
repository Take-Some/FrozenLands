package dev.takesome.helix.ui.crash;

import dev.takesome.helix.events.api.EngineEvent;
import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.node.Node;

import java.awt.Canvas;
import java.awt.Dialog;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

/** Bridges AWT input into the engine.ui input/event protocol. */
final class CrashWindowInputController {
    private final Canvas canvas;
    private final Dialog dialog;
    private final Node root;
    private final EventBus events;
    private final CrashWindowModel model;
    private final CrashReportDetailsNode details;
    private final CrashWindowFrameDragController frameDrag;

    CrashWindowInputController(
            Canvas canvas,
            Dialog dialog,
            Node root,
            EventBus events,
            CrashWindowModel model,
            CrashReportDetailsNode details,
            boolean customFrame
    ) {
        this.canvas = canvas;
        this.dialog = dialog;
        this.root = root;
        this.events = events;
        this.model = model;
        this.details = details;
        this.frameDrag = new CrashWindowFrameDragController(dialog, customFrame);
    }

    void install() {
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                root.handleInput(UiInputEvent.mouseMove(event.getX(), uiY(event)));
                canvas.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (frameDrag.drag(event)) {
                    return;
                }
                root.handleInput(UiInputEvent.mouseMove(event.getX(), uiY(event)));
                canvas.repaint();
            }
        });
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                canvas.requestFocusInWindow();
                if (frameDrag.press(event, uiY(event))) {
                    canvas.repaint();
                    return;
                }
                root.handleInput(UiInputEvent.mouseDown(event.getX(), uiY(event), button(event)));
                canvas.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (frameDrag.release()) {
                    canvas.repaint();
                    return;
                }
                root.handleInput(UiInputEvent.mouseUp(event.getX(), uiY(event), button(event)));
                canvas.repaint();
            }
        });
        canvas.addMouseWheelListener(this::mouseWheelMoved);
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
                if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_C) {
                    events.emit(EngineEvent.highPriority(CrashWindowActionIds.COPY));
                }
                if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_O) {
                    CrashWindowDesktopActions.openReport(model.reportPath());
                }
            }
        });
    }

    private void mouseWheelMoved(MouseWheelEvent event) {
        details.scroll(event.getPreciseWheelRotation() > 0 ? 42f : -42f);
        canvas.repaint();
    }

    private float uiY(MouseEvent event) {
        return canvas.getHeight() - event.getY();
    }

    private static UiInputEvent.MouseButton button(MouseEvent event) {
        return switch (event.getButton()) {
            case MouseEvent.BUTTON2 -> UiInputEvent.MouseButton.MIDDLE;
            case MouseEvent.BUTTON3 -> UiInputEvent.MouseButton.RIGHT;
            default -> UiInputEvent.MouseButton.LEFT;
        };
    }
}
