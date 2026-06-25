package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

final class UiGeneratedTextures implements Disposable {
    private static final String SOLID_WHITE_KEY = "__ui.solid.white";

    private final Map<String, TextureRegion> colorReplacedRegions = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private TextureRegion solidRegion;

    TextureRegion solidRegion() {
        if (solidRegion != null) return solidRegion;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            textures.put(SOLID_WHITE_KEY, texture);
            solidRegion = new TextureRegion(texture);
            return solidRegion;
        } finally {
            pixmap.dispose();
        }
    }

    TextureRegion colorReplacedRawRegion(String id, TextureRegion source, Color replacement) {
        if (replacement == null) return source;
        String key = colorReplacementKey(id, replacement);
        TextureRegion cached = colorReplacedRegions.get(key);
        if (cached != null) return cached;
        TextureRegion replaced = createColorReplacedRegion(source, replacement);
        if (replaced == null) return source;
        colorReplacedRegions.put(key, replaced);
        return replaced;
    }

    private TextureRegion createColorReplacedRegion(TextureRegion source, Color replacement) {
        if (source == null || source.getTexture() == null || replacement == null) return null;
        int w = source.getRegionWidth();
        int h = source.getRegionHeight();
        if (w <= 0 || h <= 0) return null;
        TextureData data = source.getTexture().getTextureData();
        if (data == null) return null;

        Pixmap input = null;
        Pixmap output = null;
        boolean disposeInput = false;
        try {
            if (!data.isPrepared()) data.prepare();
            input = data.consumePixmap();
            disposeInput = data.disposePixmap();
            output = new Pixmap(w, h, Pixmap.Format.RGBA8888);

            int rr = MathUtils.clamp(Math.round(replacement.r * 255f), 0, 255);
            int gg = MathUtils.clamp(Math.round(replacement.g * 255f), 0, 255);
            int bb = MathUtils.clamp(Math.round(replacement.b * 255f), 0, 255);
            float alphaScale = MathUtils.clamp(replacement.a, 0f, 1f);
            int x0 = source.getRegionX();
            int y0 = source.getRegionY();

            if (!PixmapRegionGuard.ensureReadable("UiGeneratedTextures.colorReplace", input, x0, y0, w, h)) {
                return null;
            }

            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    int rgba = input.getPixel(x0 + px, y0 + py);
                    int alpha = MathUtils.clamp(Math.round(UiTextureRegionKeys.alpha(rgba) * alphaScale), 0, 255);
                    output.drawPixel(px, py, (rr << 24) | (gg << 16) | (bb << 8) | alpha);
                }
            }
            Texture texture = new Texture(output);
            texture.setFilter(source.getTexture().getMinFilter(), source.getTexture().getMagFilter());
            textures.put(colorReplacementKey(source, replacement), texture);
            return new TextureRegion(texture);
        } catch (RuntimeException ex) {
            PixmapRegionGuard.warnFailure("UiGeneratedTextures.colorReplace", ex);
            return null;
        } finally {
            if (output != null) output.dispose();
            if (disposeInput && input != null) input.dispose();
        }
    }

    String colorReplacementKey(String id, Color color) {
        return id + "#replace:" + colorKey(color);
    }

    private String colorReplacementKey(TextureRegion source, Color color) {
        return UiTextureRegionKeys.sourceKey(source) + "#replace:" + colorKey(color);
    }

    private String colorKey(Color color) {
        if (color == null) return "none";
        int r = MathUtils.clamp(Math.round(color.r * 255f), 0, 255);
        int g = MathUtils.clamp(Math.round(color.g * 255f), 0, 255);
        int b = MathUtils.clamp(Math.round(color.b * 255f), 0, 255);
        int a = MathUtils.clamp(Math.round(color.a * 255f), 0, 255);
        return Integer.toHexString((r << 24) | (g << 16) | (b << 8) | a);
    }

    @Override
    public void dispose() {
        colorReplacedRegions.clear();
        solidRegion = null;
        for (Texture texture : textures.values()) {
            if (texture != null) texture.dispose();
        }
        textures.clear();
    }
}
