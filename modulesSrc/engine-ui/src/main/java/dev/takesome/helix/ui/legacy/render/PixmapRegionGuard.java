package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central guard for Pixmap pixel scans.
 *
 * <p>LibGDX {@code Pixmap#getPixel} is backed by native memory. Invalid
 * coordinates can crash the JVM before Java can throw a recoverable exception.
 * Every alpha scan must validate its full rectangular range here before reading
 * pixels.</p>
 */
final class PixmapRegionGuard {
    private static final Logger LOG = EngineLog.logger(PixmapRegionGuard.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private PixmapRegionGuard() {
    }

    static boolean ensureReadable(String context, TextureRegion region, Pixmap pixmap) {
        if (region == null) {
            warnOnce("null-region:" + context(context),
                    "UI pixmap scan rejected context='{}' reason=null texture region",
                    context(context));
            return false;
        }
        return ensureReadable(
                context,
                pixmap,
                region.getRegionX(),
                region.getRegionY(),
                region.getRegionWidth(),
                region.getRegionHeight()
        );
    }

    static boolean ensureReadable(String context, Pixmap pixmap, int x, int y, int width, int height) {
        String resolvedContext = context(context);
        if (pixmap == null) {
            warnOnce("null-pixmap:" + resolvedContext,
                    "UI pixmap scan rejected context='{}' reason=null pixmap",
                    resolvedContext);
            return false;
        }

        int pixmapW;
        int pixmapH;
        try {
            pixmapW = pixmap.getWidth();
            pixmapH = pixmap.getHeight();
        } catch (RuntimeException ex) {
            warnFailure(resolvedContext + ":inspect", ex);
            return false;
        }

        long right = (long) x + width;
        long bottom = (long) y + height;
        String reason = invalidReason(x, y, width, height, right, bottom, pixmapW, pixmapH);
        if (reason == null) return true;

        String key = resolvedContext
                + ':' + x
                + ':' + y
                + ':' + width
                + ':' + height
                + ':' + pixmapW
                + ':' + pixmapH
                + ':' + reason;
        warnOnce(key,
                "UI pixmap scan rejected context='{}' region=[x={}, y={}, w={}, h={}, right={}, bottom={}] pixmap=[w={}, h={}] reason={}",
                resolvedContext, x, y, width, height, right, bottom, pixmapW, pixmapH, reason);
        return false;
    }

    static void warnFailure(String context, RuntimeException ex) {
        String resolvedContext = context(context);
        String message = ex == null ? "unknown" : String.valueOf(ex.getMessage());
        String key = "failure:" + resolvedContext + ':' + (ex == null ? "null" : ex.getClass().getName()) + ':' + message;
        if (!WARNED.add(key)) return;
        if (ex == null) {
            LOG.warn("UI pixmap scan failed context='{}'", resolvedContext);
        } else {
            LOG.warn("UI pixmap scan failed context='{}': {}", resolvedContext, ex.toString(), ex);
        }
    }

    private static String invalidReason(
            int x,
            int y,
            int width,
            int height,
            long right,
            long bottom,
            int pixmapW,
            int pixmapH
    ) {
        if (pixmapW <= 0 || pixmapH <= 0) return "empty pixmap";
        if (width <= 0 || height <= 0) return "empty region";
        if (x < 0 || y < 0) return "negative origin";
        if (right > pixmapW || bottom > pixmapH) return "region exceeds pixmap bounds";
        return null;
    }

    private static String context(String context) {
        return context == null || context.isBlank() ? "unknown" : context.trim();
    }

    private static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(key)) LOG.warn(message, args);
    }
}
