package dev.takesome.helix.ui.crash;

import java.awt.Dialog;
import java.awt.Point;
import java.awt.event.MouseEvent;

/** Custom undecorated-frame drag behavior for the crash dialog titlebar. */
final class CrashWindowFrameDragController {
    private final Dialog dialog;
    private final boolean customFrame;
    private Point dragMouse;
    private Point dragWindow;

    CrashWindowFrameDragController(Dialog dialog, boolean customFrame) {
        this.dialog = dialog;
        this.customFrame = customFrame;
    }

    boolean press(MouseEvent event, float uiY) {
        if (!customFrame) {
            return false;
        }
        float x = event.getX();
        if (!insideTitlebar(x, uiY)) {
            return false;
        }
        if (insideFrameButton(x, uiY)) {
            return false;
        }
        dragMouse = event.getLocationOnScreen();
        dragWindow = dialog.getLocation();
        return true;
    }

    boolean drag(MouseEvent event) {
        if (!customFrame || dragMouse == null || dragWindow == null) {
            return false;
        }
        Point current = event.getLocationOnScreen();
        dialog.setLocation(
                dragWindow.x + current.x - dragMouse.x,
                dragWindow.y + current.y - dragMouse.y
        );
        return true;
    }

    boolean release() {
        if (!customFrame) {
            return false;
        }
        boolean wasDragging = dragMouse != null;
        dragMouse = null;
        dragWindow = null;
        return wasDragging;
    }

    private static boolean insideTitlebar(float x, float y) {
        return x >= CrashWindowLayout.TITLEBAR_X_MIN
                && x <= CrashWindowLayout.TITLEBAR_X_MAX
                && y >= CrashWindowLayout.TITLEBAR_Y_MIN
                && y <= CrashWindowLayout.TITLEBAR_Y_MAX;
    }

    private static boolean insideFrameButton(float x, float y) {
        return x >= CrashWindowLayout.FRAME_BUTTON_X_MIN
                && x <= CrashWindowLayout.FRAME_BUTTON_X_MAX
                && y >= CrashWindowLayout.FRAME_BUTTON_Y_MIN
                && y <= CrashWindowLayout.FRAME_BUTTON_Y_MAX;
    }
}
