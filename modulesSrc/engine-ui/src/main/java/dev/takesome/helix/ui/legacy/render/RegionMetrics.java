package dev.takesome.helix.ui.legacy.render;

/**
 * Adapter for the real engine TextureRegion type.
 */
public interface RegionMetrics<TTextureRegion> {
    float width(TTextureRegion region);
    float height(TTextureRegion region);
}
