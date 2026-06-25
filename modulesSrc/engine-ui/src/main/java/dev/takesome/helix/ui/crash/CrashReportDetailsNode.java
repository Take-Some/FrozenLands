package dev.takesome.helix.ui.crash;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import dev.takesome.helix.ui.model.UiRect;
import dev.takesome.helix.ui.node.Node;
import dev.takesome.helix.ui.render.UiRenderContext;
import dev.takesome.helix.ui.render.awt.AwtUiRenderContext;

/** Scrollable details node used by the desktop crash window. */
final class CrashReportDetailsNode extends Node {
    private static final float SCROLLBAR_GUTTER = 30f;

    private final String details;
    private float scrollY;
    private float contentHeight = 1f;

    CrashReportDetailsNode(String details) {
        this.details = emptyIfNull(details);
    }

    void scroll(float delta) {
        float max = Math.max(0f, contentHeight - bounds().h);
        scrollY = Math.max(0f, Math.min(max, scrollY + delta));
        markDirty();
    }

    @Override
    protected void onRender(UiRenderContext ctx) {
        UiRect bounds = absoluteBounds();
        if (ctx instanceof AwtUiRenderContext awt) {
            UiRect textBounds = new UiRect(
                    bounds.x,
                    bounds.y,
                    Math.max(1f, bounds.w - SCROLLBAR_GUTTER),
                    bounds.h
            );
            UiRect scrollbarBounds = new UiRect(
                    bounds.x + bounds.w - SCROLLBAR_GUTTER,
                    bounds.y,
                    SCROLLBAR_GUTTER,
                    bounds.h
            );
            contentHeight = Math.max(bounds.h, awt.measureDetailsHeight(details));
            awt.drawDetails(details, textBounds, scrollY);
            awt.drawScrollbar(scrollbarBounds, scrollY, contentHeight);
            return;
        }
        ctx.text(details, bounds, 0.8f, null, null, "mono");
    }
}
