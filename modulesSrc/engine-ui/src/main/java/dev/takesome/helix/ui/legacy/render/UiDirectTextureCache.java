package dev.takesome.helix.ui.legacy.render;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import dev.takesome.helix.data.io.DataFiles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Small cache for UI textures referenced directly by path from declarative UI.
 *
 * <p>Textures created from {@link Pixmap} retain TextureData that can later expose
 * the same pixmap to alpha scanners. Disposing that pixmap immediately after the
 * upload leaves Java with a stale native pointer and can crash the JVM inside
 * gdx64.dll when Pixmap#getPixel is called.</p>
 */
final class UiDirectTextureCache implements Disposable {
    private final Map<String, DirectTexture> textures = new HashMap<>();

    TextureRegion region(String source) {
        return region(source, 0);
    }

    TextureRegion region(String source, int frame) {
        DirectTextureRequest request = DirectTextureRequest.parse(source);
        String normalized = DataFiles.normalize(request.path());
        if (normalized.isBlank()) return null;

        DirectTexture direct = textures.get(normalized);
        if (direct == null) {
            FileHandle file = DataFiles.file(normalized);
            if (file == null || !file.exists()) return null;
            direct = loadDirectTexture(file);
            textures.put(normalized, direct);
        }

        TextureRegion full = new TextureRegion(direct.texture);
        if (!request.animated() || request.columns() <= 1 && request.rows() <= 1) return full;
        int columns = Math.max(1, request.columns());
        int rows = Math.max(1, request.rows());
        int frameCount = columns * rows;
        int safeFrame = Math.floorMod(frame, frameCount);
        int frameW = Math.max(1, full.getRegionWidth() / columns);
        int frameH = Math.max(1, full.getRegionHeight() / rows);
        int x = (safeFrame % columns) * frameW;
        int y = (safeFrame / columns) * frameH;
        return new TextureRegion(full, x, y, frameW, frameH);
    }

    boolean looksLikePath(String source) {
        if (source == null || source.isBlank()) return false;
        String normalized = DirectTextureRequest.stripParams(source).replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/")
                || normalized.contains(":")
                || normalized.endsWith(".sprite")
                || normalized.endsWith(".png")
                || normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg")
                || normalized.endsWith(".webp");
    }

    private DirectTexture loadDirectTexture(FileHandle file) {
        Pixmap source = null;
        Pixmap rgba = null;
        boolean keepReadablePixmap = false;
        try {
            source = new Pixmap(file);
            rgba = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
            rgba.setBlending(Pixmap.Blending.None);

            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int pixel = source.getPixel(x, y);
                    rgba.drawPixel(x, y, UiTextureRegionKeys.alpha(pixel) <= 0 ? 0x00000000 : pixel);
                }
            }

            Texture texture = new Texture(rgba);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            keepReadablePixmap = true;
            return new DirectTexture(texture, rgba);
        } finally {
            if (source != null) source.dispose();
            if (!keepReadablePixmap && rgba != null) rgba.dispose();
        }
    }

    @Override
    public void dispose() {
        for (DirectTexture direct : textures.values()) {
            if (direct != null) direct.dispose();
        }
        textures.clear();
    }

    private record DirectTextureRequest(String path, int columns, int rows) {
        boolean animated() {
            return columns > 0 && rows > 0;
        }

        static DirectTextureRequest parse(String source) {
            String raw = trimToEmpty(source);
            String path = stripParams(raw);
            int columns = intParam(raw, "cols", intParam(raw, "columns", 0));
            int rows = intParam(raw, "rows", 0);
            return new DirectTextureRequest(path, columns, rows);
        }

        static String stripParams(String source) {
            String raw = trimToEmpty(source);
            int query = raw.indexOf('?');
            return query < 0 ? raw : raw.substring(0, query);
        }

        private static int intParam(String raw, String name, int fallback) {
            if (raw == null || raw.isBlank()) return fallback;
            int query = raw.indexOf('?');
            if (query < 0 || query >= raw.length() - 1) return fallback;
            String[] parts = raw.substring(query + 1).split("&");
            for (String part : parts) {
                int eq = part.indexOf('=');
                if (eq <= 0) continue;
                String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                if (!name.equals(key)) continue;
                try {
                    return Math.max(0, Integer.parseInt(part.substring(eq + 1).trim()));
                } catch (RuntimeException ignored) {
                    return fallback;
                }
            }
            return fallback;
        }
    }

    private record DirectTexture(Texture texture, Pixmap readablePixmap) implements Disposable {
        @Override
        public void dispose() {
            if (texture != null) texture.dispose();
            if (readablePixmap != null) readablePixmap.dispose();
        }
    }
}
