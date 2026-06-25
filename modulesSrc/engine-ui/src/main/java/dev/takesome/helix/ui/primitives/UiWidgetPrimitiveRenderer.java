package dev.takesome.helix.ui.primitives;

import dev.takesome.helix.ui.legacy.render.UiWidgetRenderContext;

/** Renders one JSON-driven widget primitive. */
@FunctionalInterface
public interface UiWidgetPrimitiveRenderer {
    void render(UiWidgetRenderContext context);
}
