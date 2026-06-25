package org.takesome.frozenlands.engine.icons.selection;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.util.LinkedHashMap;
import java.util.List;

/** Selection helpers for picking OS/window icon frames from decoded image arrays. */
public final class IcoImageSelector {
    private static final int[] WINDOW_ICON_SIZES = new int[]{16, 24, 32, 48, 64, 128, 256};

    private IcoImageSelector() {
    }

    public static BufferedImage getBestIcon(BufferedImage[] icons) {
        if (icons == null || icons.length == 0) {
            return null;
        }
        BufferedImage best = getBestMatchingIcon(icons, 256, 256);
        if (best != null) {
            return best;
        }
        best = getBestMatchingIcon(icons, 128, 128);
        return best != null ? best : getHighestQualityIcon(icons);
    }

    public static BufferedImage getBestIcon(List<BufferedImage> icons) {
        return icons == null || icons.isEmpty() ? null : getBestIcon(icons.toArray(BufferedImage[]::new));
    }

    public static BufferedImage[] pickBestIcons(BufferedImage[] icons) {
        if (icons == null || icons.length == 0) {
            return new BufferedImage[0];
        }
        LinkedHashMap<String, BufferedImage> unique = new LinkedHashMap<>();
        for (int size : WINDOW_ICON_SIZES) {
            BufferedImage best = getBestMatchingIcon(icons, size, size);
            if (best != null) {
                unique.putIfAbsent(best.getWidth() + "x" + best.getHeight(), best);
            }
        }
        return unique.values().toArray(BufferedImage[]::new);
    }

    public static BufferedImage getIconExactSize(BufferedImage[] icons, int width, int height) {
        requireNonNegativeSize(width, height);
        if (icons == null || icons.length == 0) {
            return null;
        }
        for (BufferedImage icon : icons) {
            if (icon != null && icon.getWidth() == width && icon.getHeight() == height) {
                return icon;
            }
        }
        return null;
    }

    public static BufferedImage getBestMatchingIcon(BufferedImage[] icons, int targetWidth, int targetHeight) {
        requireNonNegativeSize(targetWidth, targetHeight);
        if (icons == null || icons.length == 0) {
            return null;
        }
        BufferedImage best = null;
        long bestScore = Long.MAX_VALUE;
        for (BufferedImage icon : icons) {
            if (icon == null) {
                continue;
            }
            long areaDelta = Math.abs((long) icon.getWidth() * icon.getHeight() - (long) targetWidth * targetHeight);
            long dimensionDelta = Math.abs(icon.getWidth() - targetWidth) + Math.abs(icon.getHeight() - targetHeight);
            long qualityPenalty = qualityPenalty(icon);
            long upscalePenalty = (icon.getWidth() < targetWidth || icon.getHeight() < targetHeight) ? 1_000_000L : 0L;
            long score = areaDelta * 16L + dimensionDelta * 1024L + qualityPenalty + upscalePenalty;
            if (score < bestScore) {
                bestScore = score;
                best = icon;
            }
        }
        return best;
    }

    public static BufferedImage getHighestQualityIcon(BufferedImage[] icons) {
        if (icons == null || icons.length == 0) {
            return null;
        }
        BufferedImage best = null;
        long bestScore = Long.MIN_VALUE;
        for (BufferedImage icon : icons) {
            if (icon == null) {
                continue;
            }
            long score = (long) icon.getWidth() * icon.getHeight() * 256L - qualityPenalty(icon);
            if (score > bestScore) {
                bestScore = score;
                best = icon;
            }
        }
        return best;
    }

    private static long qualityPenalty(BufferedImage image) {
        ColorModel model = image.getColorModel();
        int pixelSize = model == null ? 32 : model.getPixelSize();
        long penalty = Math.max(0, 32 - pixelSize) * 2048L;
        if (model instanceof IndexColorModel) {
            penalty += 8192L;
        }
        return penalty;
    }

    private static void requireNonNegativeSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Icon dimensions must be non-negative");
        }
    }
}
