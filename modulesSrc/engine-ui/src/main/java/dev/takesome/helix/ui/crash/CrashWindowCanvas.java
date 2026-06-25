package dev.takesome.helix.ui.crash;

import dev.takesome.helix.events.bus.EventBus;
import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.markup.UiMarkupCompiler;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupParser;
import dev.takesome.helix.ui.node.Node;
import org.apache.logging.log4j.Logger;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Engine.ui canvas used as the crash dialog content surface. */
final class CrashWindowCanvas extends Canvas {
    private static final Logger LOG = EngineLog.logger(CrashWindowCanvas.class);

    private final Node root;
    private final CrashReportDetailsNode details;
    private final CrashWindowRenderer renderer;

    CrashWindowCanvas(Dialog dialog, CrashWindowModel model, boolean customFrame) {
        setPreferredSize(new Dimension(CrashWindowLayout.WIDTH, CrashWindowLayout.HEIGHT));
        setFocusable(true);
        setBackground(Color.WHITE);

        EventBus eventBus = new EventBus(0, 0, true);
        UiMarkupDocument document = new UiMarkupParser().parse(CrashWindowTemplates.render(model));
        this.root = new UiMarkupCompiler(eventBus).compile(document, CrashWindowLayout.WIDTH, CrashWindowLayout.HEIGHT);
        this.details = new CrashReportDetailsNode(model.details());
        this.details.setBounds(
                CrashWindowLayout.DETAILS_X,
                CrashWindowLayout.DETAILS_Y,
                CrashWindowLayout.DETAILS_W,
                CrashWindowLayout.DETAILS_H
        );
        this.root.add(details);
        this.root.attach();

        CrashWindowCopyFeedback copyFeedback = new CrashWindowCopyFeedback();
        this.renderer = new CrashWindowRenderer(root, copyFeedback);
        CrashWindowActionController actions = new CrashWindowActionController(dialog, eventBus, model, copyFeedback, this::repaint);
        eventBus.register((event, ctx) -> actions.handleAction(event.type(), ctx));

        LOG.debug("Crash window UI compiled: root={}, detailsBounds={}", root.getClass().getSimpleName(), details.bounds());
        new CrashWindowInputController(this, dialog, root, eventBus, model, details, customFrame).install();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(CrashWindowLayout.WIDTH, CrashWindowLayout.HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(CrashWindowLayout.WIDTH, CrashWindowLayout.HEIGHT);
    }

    @Override
    public void paint(Graphics graphics) {
        renderer.render((Graphics2D) graphics, getWidth(), getHeight());
    }

    @Override
    public void update(Graphics graphics) {
        paint(graphics);
    }
}
