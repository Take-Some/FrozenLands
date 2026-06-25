package dev.takesome.helix.ui.crash;

import java.util.concurrent.TimeUnit;

/** Geometry and resource constants for the engine.ui crash reporter. */
final class CrashWindowLayout {
    static final int WIDTH = 1000;
    static final int HEIGHT = 672;

    static final String TEMPLATE = "/helix/ui/crash/crash-window.ui.html";
    static final String CSS_TEMPLATE = "/helix/ui/crash/crashWindow.css";

    static final long COPY_FEEDBACK_MILLIS = 1_250L;
    static final long COPY_FEEDBACK_NANOS = TimeUnit.MILLISECONDS.toNanos(COPY_FEEDBACK_MILLIS);

    static final float DETAILS_X = 46f;
    static final float DETAILS_Y = 144f;
    static final float DETAILS_W = 546f;
    static final float DETAILS_H = 124f;

    static final float TITLEBAR_X_MIN = 0f;
    static final float TITLEBAR_X_MAX = 1000f;
    static final float TITLEBAR_Y_MIN = 614f;
    static final float TITLEBAR_Y_MAX = 672f;

    static final float FRAME_BUTTON_X_MIN = 840f;
    static final float FRAME_BUTTON_X_MAX = 964f;
    static final float FRAME_BUTTON_Y_MIN = 622f;
    static final float FRAME_BUTTON_Y_MAX = 664f;

    private CrashWindowLayout() {
    }
}
