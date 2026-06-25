package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;
import dev.takesome.helix.ui.model.UiRect;

/**
 * Alpha-driven horizontal UI sheet parser.
 *
 * Each opaque horizontal row is a separate item. Inside each row the parser uses
 * alpha-separated horizontal segments as: start cap, stretchable center, end cap.
 * This matches ribbon/sword sheets where every row contains a complete variant.
 */
final class AlphaUiSheet {
    private static final int MIN_ROW_HEIGHT = 2;
    private static final int MIN_COLUMN_WIDTH = 1;

    private final Row[] rows;

    private AlphaUiSheet(Row[] rows) {
        this.rows = rows;
    }

    static AlphaUiSheet parse(TextureRegion source, int alphaThreshold) {
        if (source == null || source.getTexture() == null) return null;
        if (source.getRegionWidth() <= 0 || source.getRegionHeight() <= 0) return null;

        TextureData data = source.getTexture().getTextureData();
        if (data == null) return null;

        Pixmap pixmap = null;
        boolean disposePixmap = false;
        try {
            if (!data.isPrepared()) data.prepare();
            pixmap = data.consumePixmap();
            disposePixmap = data.disposePixmap();
            return parsePixmap(source, pixmap, alphaThreshold);
        } catch (RuntimeException ex) {
            PixmapRegionGuard.warnFailure("AlphaUiSheet.parse", ex);
            return null;
        } finally {
            if (disposePixmap && pixmap != null) pixmap.dispose();
        }
    }

    TextureRegion item(int index) {
        Row row = row(index);
        return row == null ? null : row.full;
    }

    UiRect contentBounds(int rowIndex, UiRect target) {
        Row row = row(rowIndex);
        if (row == null || target == null || target.w <= 0f || target.h <= 0f) return target;

        RibbonLayout layout = ribbonLayout(row, target.w, target.h);
        if (layout == null || layout.centerW <= 0f) return target;
        return new UiRect(target.x + layout.leftW, target.y, layout.centerW, target.h);
    }

    boolean drawHorizontalStretch(SpriteBatch batch, int rowIndex, float x, float y, float w, float h) {
        return drawHorizontalStretch(batch, rowIndex, x, y, w, h, null);
    }

    boolean drawHorizontalStretch(SpriteBatch batch, int rowIndex, float x, float y, float w, float h, Color tint) {
        Row row = row(rowIndex);
        if (batch == null || row == null || w <= 0f || h <= 0f) return false;

        Color oldColor = null;
        if (tint != null) {
            oldColor = batch.getColor().cpy();
            batch.setColor(tint);
        }

        try {
            RibbonLayout layout = ribbonLayout(row, w, h);
            if (layout == null) return false;

            if (layout.leftW > 0f) batch.draw(row.left, x, y, layout.leftW, h);
            if (layout.centerW > 0f) batch.draw(row.center, x + layout.leftW, y, layout.centerW, h);
            if (layout.rightW > 0f) batch.draw(row.right, x + w - layout.rightW, y, layout.rightW, h);
            return true;
        } finally {
            if (oldColor != null) batch.setColor(oldColor);
        }
    }

    private Row row(int index) {
        if (rows.length == 0) return null;
        return rows[Math.floorMod(index, rows.length)];
    }

    private static RibbonLayout ribbonLayout(Row row, float w, float h) {
        if (row == null || w <= 0f || h <= 0f) return null;
        float scale = h / Math.max(1f, row.full.getRegionHeight());
        if (!Float.isFinite(scale) || scale <= 0f) return null;

        float leftW = Math.min(row.left.getRegionWidth() * scale, w * 0.5f);
        float rightW = Math.min(row.right.getRegionWidth() * scale, Math.max(0f, w - leftW));
        float centerW = Math.max(0f, w - leftW - rightW);
        return new RibbonLayout(leftW, centerW, rightW);
    }

    private static AlphaUiSheet parsePixmap(TextureRegion source, Pixmap pixmap, int alphaThreshold) {
        if (pixmap == null) return null;
        int threshold = Math.max(0, Math.min(255, alphaThreshold));
        int w = source.getRegionWidth();
        int h = source.getRegionHeight();
        int x0 = source.getRegionX();
        int y0 = source.getRegionY();

        if (!PixmapRegionGuard.ensureReadable("AlphaUiSheet.parse", pixmap, x0, y0, w, h)) return null;

        List<Run> rowRuns = rowRuns(pixmap, x0, y0, w, h, threshold);
        if (rowRuns.isEmpty()) return null;

        ArrayList<Row> rows = new ArrayList<>(rowRuns.size());
        for (Run rowRun : rowRuns) {
            Row row = rowFromRun(source, pixmap, x0, y0, w, threshold, rowRun);
            if (row != null) rows.add(row);
        }
        return rows.isEmpty() ? null : new AlphaUiSheet(rows.toArray(Row[]::new));
    }

    private static Row rowFromRun(TextureRegion source, Pixmap pixmap, int x0, int y0, int w, int threshold, Run rowRun) {
        List<Run> columns = columnRuns(pixmap, x0, y0, w, threshold, rowRun);
        if (columns.isEmpty()) return null;

        Run first = columns.get(0);
        Run last = columns.get(columns.size() - 1);
        int rowX = first.start;
        int rowW = last.endExclusive - first.start;
        int rowH = rowRun.length();
        TextureRegion full = new TextureRegion(source, rowX, rowRun.start, rowW, rowH);

        if (columns.size() >= 3) {
            Run left = first;
            Run center = new Run(columns.get(1).start, columns.get(columns.size() - 2).endExclusive);
            Run right = last;
            return new Row(
                    full,
                    new TextureRegion(source, left.start, rowRun.start, left.length(), rowH),
                    new TextureRegion(source, center.start, rowRun.start, center.length(), rowH),
                    new TextureRegion(source, right.start, rowRun.start, right.length(), rowH)
            );
        }

        if (columns.size() == 2) {
            Run left = first;
            Run right = last;
            Run center = left.length() >= right.length() ? left : right;
            return new Row(
                    full,
                    new TextureRegion(source, left.start, rowRun.start, left.length(), rowH),
                    new TextureRegion(source, center.start, rowRun.start, center.length(), rowH),
                    new TextureRegion(source, right.start, rowRun.start, right.length(), rowH)
            );
        }

        Run only = first;
        int cap = Math.max(1, Math.min(rowH, only.length() / 3));
        if (only.length() <= cap * 2 + 1) return new Row(full, full, full, full);
        return new Row(
                full,
                new TextureRegion(source, only.start, rowRun.start, cap, rowH),
                new TextureRegion(source, only.start + cap, rowRun.start, only.length() - cap * 2, rowH),
                new TextureRegion(source, only.endExclusive - cap, rowRun.start, cap, rowH)
        );
    }

    private static List<Run> rowRuns(Pixmap pixmap, int x0, int y0, int w, int h, int threshold) {
        ArrayList<Run> runs = new ArrayList<>();
        int start = -1;
        for (int y = 0; y < h; y++) {
            boolean opaque = rowOpaque(pixmap, x0, y0 + y, w, threshold);
            if (opaque && start < 0) start = y;
            else if (!opaque && start >= 0) {
                addRun(runs, start, y, MIN_ROW_HEIGHT);
                start = -1;
            }
        }
        if (start >= 0) addRun(runs, start, h, MIN_ROW_HEIGHT);
        return runs;
    }

    private static List<Run> columnRuns(Pixmap pixmap, int x0, int y0, int w, int threshold, Run rowRun) {
        ArrayList<Run> runs = new ArrayList<>();
        int start = -1;
        for (int x = 0; x < w; x++) {
            boolean opaque = columnOpaque(pixmap, x0 + x, y0, threshold, rowRun);
            if (opaque && start < 0) start = x;
            else if (!opaque && start >= 0) {
                addRun(runs, start, x, MIN_COLUMN_WIDTH);
                start = -1;
            }
        }
        if (start >= 0) addRun(runs, start, w, MIN_COLUMN_WIDTH);
        return runs;
    }

    private static void addRun(List<Run> runs, int start, int endExclusive, int minLength) {
        if (endExclusive - start >= minLength) runs.add(new Run(start, endExclusive));
    }

    private static boolean rowOpaque(Pixmap pixmap, int x0, int y, int w, int threshold) {
        for (int x = 0; x < w; x++) {
            if (alpha(pixmap.getPixel(x0 + x, y)) > threshold) return true;
        }
        return false;
    }

    private static boolean columnOpaque(Pixmap pixmap, int x, int y0, int threshold, Run rowRun) {
        for (int y = rowRun.start; y < rowRun.endExclusive; y++) {
            if (alpha(pixmap.getPixel(x, y0 + y)) > threshold) return true;
        }
        return false;
    }

    private static int alpha(int rgba8888) {
        return rgba8888 & 0xFF;
    }

    private record Row(TextureRegion full, TextureRegion left, TextureRegion center, TextureRegion right) {}
    private record RibbonLayout(float leftW, float centerW, float rightW) {}
    private record Run(int start, int endExclusive) {
        int length() { return endExclusive - start; }
    }
}
