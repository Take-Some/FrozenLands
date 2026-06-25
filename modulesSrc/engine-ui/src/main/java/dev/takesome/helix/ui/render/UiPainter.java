package dev.takesome.helix.ui.render;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;

/**
 * Minimal draw API consumed by HUD renderers.
 * Implement this once over the engine batch/font/shape renderer.
 */
public interface UiPainter<TTextureRegion> {
    void draw(TTextureRegion region, float x, float y, float w, float h);

    void fill(float x, float y, float w, float h, UiColor color);

    void text(String text, float x, float y, float scale, UiColor color, TextAlign align);
}
