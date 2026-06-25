package dev.takesome.helix.ui.frame;

import dev.takesome.helix.ui.render.UiRenderContext;

/** Renderer capabilities used while lowering retained UI nodes into frame commands. */
public final class UiFrameRenderCapabilities {
    private final boolean elements;
    private final boolean icons;
    private final boolean images;

    public UiFrameRenderCapabilities(boolean elements, boolean icons, boolean images) {
        this.elements = elements;
        this.icons = icons;
        this.images = images;
    }

    public static UiFrameRenderCapabilities from(UiRenderContext context) {
        if (context == null) return conservative();
        return new UiFrameRenderCapabilities(context.supportsElements(), context.supportsIcons(), context.supportsImages());
    }

    public static UiFrameRenderCapabilities conservative() {
        return new UiFrameRenderCapabilities(false, false, false);
    }

    public static UiFrameRenderCapabilities optimistic() {
        return new UiFrameRenderCapabilities(true, true, true);
    }

    public boolean elements() {
        return elements;
    }

    public boolean icons() {
        return icons;
    }

    public boolean images() {
        return images;
    }
}
