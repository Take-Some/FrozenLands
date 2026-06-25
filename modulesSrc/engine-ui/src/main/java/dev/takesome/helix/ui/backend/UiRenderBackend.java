package dev.takesome.helix.ui.backend;

import dev.takesome.helix.ui.frame.UiFrame;
import dev.takesome.helix.ui.render.UiRenderContext;

/** Render backend adapter. It consumes UiFrame IR only. */
public interface UiRenderBackend {
    void render(UiFrame frame, UiRenderContext target);
}
