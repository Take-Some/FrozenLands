package dev.takesome.helix.ui.layout;

import java.util.Objects;
import dev.takesome.helix.ui.model.UiRect;

public final class UiLayoutResolver {
    private UiLayoutResolver() {
    }

    /**
     * Resolves an anchored layout rect into bottom-left screen coordinates.
     * For TOP_RIGHT/BOTTOM_RIGHT anchors, negative x means inset from the right edge.
     * For TOP_* anchors, y means inset from the top edge.
     */
    public static UiRect resolve(UiPanelLayout panel, float viewportW, float viewportH) {
        Objects.requireNonNull(panel, "panel");

        switch (panel.anchor) {
            case TOP_LEFT:
                return new UiRect(panel.x, viewportH - panel.y - panel.h, panel.w, panel.h);
            case TOP_RIGHT:
                return new UiRect(viewportW + panel.x - panel.w, viewportH - panel.y - panel.h, panel.w, panel.h);
            case BOTTOM_LEFT:
                return new UiRect(panel.x, panel.y, panel.w, panel.h);
            case BOTTOM_RIGHT:
                return new UiRect(viewportW + panel.x - panel.w, panel.y, panel.w, panel.h);
            case CENTER:
                return new UiRect((viewportW - panel.w) * 0.5f + panel.x, (viewportH - panel.h) * 0.5f + panel.y, panel.w, panel.h);
            default:
                throw new IllegalStateException("Unhandled anchor: " + panel.anchor);
        }
    }
}
