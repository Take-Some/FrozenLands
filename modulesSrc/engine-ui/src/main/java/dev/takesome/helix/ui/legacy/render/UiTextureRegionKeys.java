package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

final class UiTextureRegionKeys {
    private UiTextureRegionKeys() {
    }

    static String sourceKey(TextureRegion source) {
        if (source == null) return "null";
        return System.identityHashCode(source.getTexture())
                + ":" + source.getRegionX()
                + ":" + source.getRegionY()
                + ":" + source.getRegionWidth()
                + "x" + source.getRegionHeight();
    }

    static String keyedSource(String ownerKey, TextureRegion source) {
        return ownerKey + "@" + sourceKey(source);
    }

    static int alpha(int rgba8888) {
        return rgba8888 & 0xFF;
    }
}
