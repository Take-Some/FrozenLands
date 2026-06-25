package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal ribbon parsed from a sprite sheet by alpha separators.
 *
 * The sheet may contain any number of horizontal ribbon variants. Each variant
 * row must contain three alpha-separated columns:
 *
 * left cap | stretchable center | right cap
 */
final class AlphaHorizontalRibbon {
    private static final int EXPECTED_COLUMNS = 3;
    private static final int MIN_RUN_LENGTH = 2;

    private final TextureRegion left;
    private final TextureRegion center;
    private final TextureRegion right;
    private final float nativeHeight;

    private AlphaHorizontalRibbon(TextureRegion left, TextureRegion center, TextureRegion right, float nativeHeight) {
        this.left = left;
        this.center = center;
        this.right = right;
        this.nativeHeight = nativeHeight <= 0f ? 1f : nativeHeight;
    }

    static AlphaHorizontalRibbon from(TextureRegion sheet, int rowIndex, int alphaThreshold) {
        if (sheet == null || sheet.getTexture() == null) return null;
        if (sheet.getRegionWidth() <= 0 || sheet.getRegionHeight() <= 0) return null;

        TextureData data = sheet.getTexture().getTextureData();
        if (data == null) return null;

        Pixmap pixmap = null;
        boolean disposePixmap = false;
        try {
            if (!data.isPrepared()) data.prepare();
            pixmap = data.consumePixmap();
            disposePixmap = data.disposePixmap();
            return fromPixmap(sheet, pixmap, rowIndex, alphaThreshold);
        } catch (RuntimeException ex) {
            PixmapRegionGuard.warnFailure("AlphaHorizontalRibbon.from", ex);
            return null;
        } finally {
            if (disposePixmap && pixmap != null) pixmap.dispose();
        }
    }

    void draw(SpriteBatch batch, float x, float y, float w, float h) {
        if (batch == null || w <= 0f || h <= 0f) return;

        float scale = h / nativeHeight;
        if (!Float.isFinite(scale) || scale <= 0f) return;

        float leftW = left.getRegionWidth() * scale;
        float rightW = right.getRegionWidth() * scale;
        float sideScale = Math.min(1f, w / Math.max(1f, leftW + rightW));
        leftW *= sideScale;
        rightW *= sideScale;

        float centerW = Math.max(0f, w - leftW - rightW);
        batch.draw(left, x, y, leftW, h);
        if (centerW > 0f) batch.draw(center, x + leftW, y, centerW, h);
        batch.draw(right, x + w - rightW, y, rightW, h);
    }

    private static AlphaHorizontalRibbon fromPixmap(TextureRegion sheet, Pixmap pixmap, int rowIndex, int alphaThreshold) {
        if (pixmap == null) return null;

        int w = sheet.getRegionWidth();
        int h = sheet.getRegionHeight();
        int x0 = sheet.getRegionX();
        int y0 = sheet.getRegionY();
        int threshold = Math.max(0, Math.min(255, alphaThreshold));

        if (!PixmapRegionGuard.ensureReadable("AlphaHorizontalRibbon.from", pixmap, x0, y0, w, h)) return null;

        List<Run> rows = runs(h, y -> rowOpaque(pixmap, x0, y0 + y, w, threshold));
        if (rows.isEmpty()) return null;

        int safeRow = Math.max(0, Math.min(rowIndex, rows.size() - 1));
        Run row = rows.get(safeRow);
        List<Run> columns = runs(w, x -> columnOpaque(pixmap, x0 + x, y0 + row.start, row.length(), threshold));
        if (columns.size() != EXPECTED_COLUMNS) return null;

        TextureRegion left = sub(sheet, columns.get(0), row);
        TextureRegion center = sub(sheet, columns.get(1), row);
        TextureRegion right = sub(sheet, columns.get(2), row);
        return new AlphaHorizontalRibbon(left, center, right, row.length());
    }

    private static TextureRegion sub(TextureRegion base, Run column, Run row) {
        return new TextureRegion(base, column.start, row.start, column.length(), row.length());
    }

    private static List<Run> runs(int length, AxisProbe probe) {
        ArrayList<Run> out = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < length; i++) {
            boolean opaque = probe.opaque(i);
            if (opaque && start < 0) {
                start = i;
            } else if (!opaque && start >= 0) {
                addRun(out, start, i);
                start = -1;
            }
        }
        if (start >= 0) addRun(out, start, length);
        return out;
    }

    private static void addRun(List<Run> out, int start, int endExclusive) {
        if (endExclusive - start >= MIN_RUN_LENGTH) out.add(new Run(start, endExclusive));
    }

    private static boolean columnOpaque(Pixmap pixmap, int x, int y0, int h, int threshold) {
        for (int y = 0; y < h; y++) if (alpha(pixmap.getPixel(x, y0 + y)) > threshold) return true;
        return false;
    }

    private static boolean rowOpaque(Pixmap pixmap, int x0, int y, int w, int threshold) {
        for (int x = 0; x < w; x++) if (alpha(pixmap.getPixel(x0 + x, y)) > threshold) return true;
        return false;
    }

    private static int alpha(int rgba8888) {
        return rgba8888 & 0xFF;
    }

    @FunctionalInterface
    private interface AxisProbe {
        boolean opaque(int index);
    }

    private record Run(int start, int endExclusive) {
        int length() { return endExclusive - start; }
    }
}
