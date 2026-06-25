package dev.takesome.helix.ui.crash;

import dev.takesome.helix.events.api.EngineEvent;
import dev.takesome.helix.events.api.EventContext;
import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.awt.Dialog;

/** Handles semantic actions emitted by the crash-window markup. */
final class CrashWindowActionController {
    private static final Logger LOG = EngineLog.logger(CrashWindowActionController.class);

    private final Dialog dialog;
    private final EventBus events;
    private final CrashWindowModel model;
    private final CrashWindowCopyFeedback copyFeedback;
    private final Runnable repaint;

    CrashWindowActionController(
            Dialog dialog,
            EventBus events,
            CrashWindowModel model,
            CrashWindowCopyFeedback copyFeedback,
            Runnable repaint
    ) {
        this.dialog = dialog;
        this.events = events;
        this.model = model;
        this.copyFeedback = copyFeedback;
        this.repaint = repaint;
    }

    void handleAction(String type, EventContext ctx) {
        LOG.debug("Crash window action dispatched: {}", type);
        if (CrashWindowActionIds.COPY.equals(type)) {
            boolean copied = CrashWindowDesktopActions.copyReport(model.details());
            events.emit(EngineEvent.highPriority(copied ? CrashWindowActionIds.COPY_CONFIRMED : CrashWindowActionIds.COPY_FAILED));
            return;
        }
        if (CrashWindowActionIds.COPY_CONFIRMED.equals(type)) {
            copyFeedback.mark(true, repaint);
            return;
        }
        if (CrashWindowActionIds.COPY_FAILED.equals(type)) {
            copyFeedback.mark(false, repaint);
            return;
        }
        if (CrashWindowActionIds.OPEN.equals(type)) {
            CrashWindowDesktopActions.openReport(model.reportPath());
            return;
        }
        if (CrashWindowActionIds.CLOSE.equals(type)
                || CrashWindowActionIds.RESTART.equals(type)
                || CrashWindowActionIds.SAFE.equals(type)) {
            dialog.dispose();
            return;
        }
        if (CrashWindowActionIds.MINIMIZE.equals(type) || CrashWindowActionIds.MAXIMIZE.equals(type)) {
            repaint.run();
        }
    }
}
