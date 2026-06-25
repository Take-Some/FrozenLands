package dev.takesome.helix.ui.scene;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.render.UiRenderContext;

/**
 * Lifecycle contract for a retained UI scene.
 */
public interface Scene {
    void onEnter();

    void onExit();

    void update(float dt);

    void render(UiRenderContext ctx);

    boolean handleInput(UiInputEvent event);
}
