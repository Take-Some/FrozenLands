package dev.takesome.helix.ui.layout;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import java.util.Objects;

/**
 * Data-driven UI section declaration from a UI document.
 * Coordinates are resolved to bottom-left screen coordinates by UiLayoutResolver.
 */
public final class UiPanelLayout {
    public final UiAnchor anchor;
    public final float x;
    public final float y;
    public final float w;
    public final float h;
    public final String mode;

    public UiPanelLayout(UiAnchor anchor, float x, float y, float w, float h) {
        this(anchor, x, y, w, h, "");
    }

    public UiPanelLayout(UiAnchor anchor, float x, float y, float w, float h, String mode) {
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.mode = emptyIfNull(mode);
    }
}
