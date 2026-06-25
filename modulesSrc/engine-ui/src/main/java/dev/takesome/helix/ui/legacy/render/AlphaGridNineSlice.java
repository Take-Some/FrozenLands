package dev.takesome.helix.ui.legacy.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

/** Builds a nine-slice grid by detecting separated texture islands dynamically. */
final class AlphaGridNineSlice {
    private static final int EXPECTED_RUNS = 3;
    private static final int MIN_RUN_LENGTH = 2;
    private static final int MAX_TINY_GAP = 2;
    private static final int BACKGROUND_RGB_TOLERANCE = 3;

    private AlphaGridNineSlice() {}

    static NineSlice<TextureRegion> from(TextureRegion base, int alphaThreshold) {
        if (base == null || base.getTexture() == null) return null;
        if (base.getRegionWidth() <= 0 || base.getRegionHeight() <= 0) return null;

        TextureData data = base.getTexture().getTextureData();
        if (data == null) return null;

        Pixmap pixmap = null;
        boolean disposePixmap = false;
        try {
            if (!data.isPrepared()) data.prepare();
            pixmap = data.consumePixmap();
            disposePixmap = data.disposePixmap();
            return fromPixmap(base, pixmap, alphaThreshold);
        } catch (RuntimeException ex) {
            PixmapRegionGuard.warnFailure("AlphaGridNineSlice.from", ex);
            return null;
        } finally {
            if (disposePixmap && pixmap != null) pixmap.dispose();
        }
    }

    private static NineSlice<TextureRegion> fromPixmap(TextureRegion base, Pixmap pixmap, int alphaThreshold) {
        if (pixmap == null) return null;
        int w = base.getRegionWidth();
        int h = base.getRegionHeight();
        int x0 = base.getRegionX();
        int y0 = base.getRegionY();
        int threshold = Math.max(0, Math.min(255, alphaThreshold));

        if (!PixmapRegionGuard.ensureReadable("AlphaGridNineSlice.from", pixmap, x0, y0, w, h)) return null;

        BackgroundKey background = BackgroundKey.infer(pixmap, x0, y0, w, h, threshold);
        int minColumnPixels = minimumCoverage(h);
        int minRowPixels = minimumCoverage(w);

        List<Run> columns = runs(w, x -> columnActive(pixmap, x0 + x, y0, h, threshold, minColumnPixels, background));
        List<Run> rows = runs(h, y -> rowActive(pixmap, x0, y0 + y, w, threshold, minRowPixels, background));
        if (columns.size() != EXPECTED_RUNS || rows.size() != EXPECTED_RUNS) return null;

        Run left = columns.get(0);
        Run center = columns.get(1);
        Run right = columns.get(2);
        Run top = rows.get(0);
        Run middle = rows.get(1);
        Run bottom = rows.get(2);

        return new NineSlice<>(
                sub(base, left, top), sub(base, center, top), sub(base, right, top),
                sub(base, left, middle), sub(base, center, middle), sub(base, right, middle),
                sub(base, left, bottom), sub(base, center, bottom), sub(base, right, bottom)
        );
    }

    private static TextureRegion sub(TextureRegion base, Run column, Run row) {
        return new TextureRegion(base, column.start, row.start, column.length(), row.length());
    }

    private static List<Run> runs(int length, AxisProbe probe) {
        ArrayList<Run> out = new ArrayList<>(EXPECTED_RUNS);
        int start = -1;
        for (int i = 0; i < length; i++) {
            boolean active = probe.active(i);
            if (active && start < 0) start = i;
            else if (!active && start >= 0) {
                addRun(out, start, i);
                start = -1;
            }
        }
        if (start >= 0) addRun(out, start, length);
        return mergeTinyGaps(out, MAX_TINY_GAP);
    }

    private static void addRun(List<Run> out, int start, int endExclusive) {
        if (endExclusive - start >= MIN_RUN_LENGTH) out.add(new Run(start, endExclusive));
    }

    private static List<Run> mergeTinyGaps(List<Run> input, int maxGap) {
        if (input.size() < 2) return input;

        ArrayList<Run> out = new ArrayList<>(input.size());
        Run current = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            Run next = input.get(i);
            int gap = next.start - current.endExclusive;
            if (gap <= maxGap) {
                current = new Run(current.start, next.endExclusive);
            } else {
                out.add(current);
                current = next;
            }
        }
        out.add(current);
        return out;
    }

    private static boolean columnActive(
            Pixmap pixmap,
            int x,
            int y0,
            int h,
            int threshold,
            int minimumPixels,
            BackgroundKey background
    ) {
        int activePixels = 0;
        for (int y = 0; y < h; y++) {
            if (isActive(pixmap.getPixel(x, y0 + y), threshold, background) && ++activePixels >= minimumPixels) return true;
        }
        return false;
    }

    private static boolean rowActive(
            Pixmap pixmap,
            int x0,
            int y,
            int w,
            int threshold,
            int minimumPixels,
            BackgroundKey background
    ) {
        int activePixels = 0;
        for (int x = 0; x < w; x++) {
            if (isActive(pixmap.getPixel(x0 + x, y), threshold, background) && ++activePixels >= minimumPixels) return true;
        }
        return false;
    }

    private static boolean isActive(int rgba8888, int threshold, BackgroundKey background) {
        if (alpha(rgba8888) <= threshold) return false;
        return background == null || !background.matches(rgba8888);
    }

    private static int minimumCoverage(int axisSpan) {
        return Math.max(2, axisSpan / 512);
    }

    private static int alpha(int rgba8888) {
        return rgba8888 & 0xFF;
    }

    @FunctionalInterface
    private interface AxisProbe {
        boolean active(int index);
    }

    private record Run(int start, int endExclusive) {
        int length() { return endExclusive - start; }
    }

    /**
     * Some generated assets are exported with an opaque solid preview backdrop
     * instead of real alpha. Treat the dominant corner color as empty space so
     * old single-texture buttons and new spaced 3x3 atlases both resolve.
     */
    private record BackgroundKey(int r, int g, int b) {
        static BackgroundKey infer(Pixmap pixmap, int x0, int y0, int w, int h, int threshold) {
            int[] samples = {
                    pixmap.getPixel(x0, y0),
                    pixmap.getPixel(x0 + w - 1, y0),
                    pixmap.getPixel(x0, y0 + h - 1),
                    pixmap.getPixel(x0 + w - 1, y0 + h - 1)
            };

            int opaque = 0;
            int r = 0;
            int g = 0;
            int b = 0;
            int firstOpaque = 0;
            for (int sample : samples) {
                if (alpha(sample) <= threshold) continue;
                if (opaque == 0) firstOpaque = sample;
                else if (!sameRgb(firstOpaque, sample)) return null;
                opaque++;
                r += red(sample);
                g += green(sample);
                b += blue(sample);
            }
            if (opaque != samples.length) return null;

            return new BackgroundKey(r / opaque, g / opaque, b / opaque);
        }

        boolean matches(int rgba8888) {
            return Math.abs(red(rgba8888) - r) <= BACKGROUND_RGB_TOLERANCE
                    && Math.abs(green(rgba8888) - g) <= BACKGROUND_RGB_TOLERANCE
                    && Math.abs(blue(rgba8888) - b) <= BACKGROUND_RGB_TOLERANCE;
        }

        private static boolean sameRgb(int a, int b) {
            return Math.abs(red(a) - red(b)) <= BACKGROUND_RGB_TOLERANCE
                    && Math.abs(green(a) - green(b)) <= BACKGROUND_RGB_TOLERANCE
                    && Math.abs(blue(a) - blue(b)) <= BACKGROUND_RGB_TOLERANCE;
        }

        private static int red(int rgba8888) {
            return (rgba8888 >>> 24) & 0xFF;
        }

        private static int green(int rgba8888) {
            return (rgba8888 >>> 16) & 0xFF;
        }

        private static int blue(int rgba8888) {
            return (rgba8888 >>> 8) & 0xFF;
        }
    }
}
