package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Rectangle inside a texture/material region. Values are data, not code. */
public final class UiRegionDefinition {
    public int x;
    public int y;
    public int w;
    public int h;

    public TextureRegion slice(TextureRegion base) {
        if (base == null) return null;
        if (w <= 0 || h <= 0) return base;
        return new TextureRegion(base, x, y, w, h);
    }
}
