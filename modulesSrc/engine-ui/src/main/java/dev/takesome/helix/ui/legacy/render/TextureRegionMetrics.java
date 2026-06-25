package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class TextureRegionMetrics implements RegionMetrics<TextureRegion> {
    public static final TextureRegionMetrics INSTANCE = new TextureRegionMetrics();

    private TextureRegionMetrics() {}

    @Override
    public float width(TextureRegion region) {
        return region == null ? 0f : region.getRegionWidth();
    }

    @Override
    public float height(TextureRegion region) {
        return region == null ? 0f : region.getRegionHeight();
    }
}
