package org.takesome.frozenlands.engine.icons;

import java.awt.image.BufferedImage;
import java.io.IOException;

final class IcoDibDecoder {
    private IcoDibDecoder() {
    }

    static BufferedImage decode(byte[] data, IcoDirectoryEntry entry, int maxDimension, int maxPixels) throws IOException {
        DibHeader header = DibHeader.read(data, entry, maxDimension, maxPixels);
        DibLayout layout = DibLayout.create(header);
        int[] pixels = new int[header.width() * header.height()];
        boolean hasAlpha = switch (header.bitDepth()) {
            case 1, 4, 8 -> readIndexed(data, header, layout, pixels);
            case 24 -> readRgb24(data, header, layout, pixels);
            case 32 -> readArgb32(data, header, layout, pixels);
            default -> throw new IOException("Unsupported ICO DIB bit depth: " + header.bitDepth());
        };
        if (!hasAlpha && layout.maskEnd() <= header.limit()) {
            applyMask(data, header, layout, pixels);
        }
        BufferedImage image = new BufferedImage(header.width(), header.height(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, header.width(), header.height(), pixels, 0, header.width());
        return image;
    }

    private static boolean readIndexed(byte[] data, DibHeader header, DibLayout layout, int[] pixels) throws IOException {
        int[] palette = readPalette(data, layout.paletteOffset(), layout.paletteEntries());
        for (int y = 0; y < header.height(); y++) {
            int row = layout.xorOffset() + (header.height() - 1 - y) * layout.xorRowBytes();
            for (int x = 0; x < header.width(); x++) {
                int index = switch (header.bitDepth()) {
                    case 1 -> IcoBinary.readPackedBit(data, row, x);
                    case 4 -> IcoBinary.readPackedNibble(data, row, x);
                    case 8 -> data[row + x] & 0xFF;
                    default -> throw new IOException("Unsupported indexed bit depth: " + header.bitDepth());
                };
                if (index < 0 || index >= palette.length) {
                    throw new IOException("ICO DIB palette index out of range: " + index);
                }
                pixels[y * header.width() + x] = palette[index];
            }
        }
        return false;
    }

    private static boolean readRgb24(byte[] data, DibHeader header, DibLayout layout, int[] pixels) {
        forEachPixel(header, layout.xorOffset(), layout.xorRowBytes(), (x, y, p) -> {
            int b = data[p] & 0xFF;
            int g = data[p + 1] & 0xFF;
            int r = data[p + 2] & 0xFF;
            pixels[y * header.width() + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }, 3);
        return false;
    }

    private static boolean readArgb32(byte[] data, DibHeader header, DibLayout layout, int[] pixels) {
        boolean[] hasAlpha = new boolean[]{false};
        forEachPixel(header, layout.xorOffset(), layout.xorRowBytes(), (x, y, p) -> {
            int b = data[p] & 0xFF;
            int g = data[p + 1] & 0xFF;
            int r = data[p + 2] & 0xFF;
            int a = data[p + 3] & 0xFF;
            hasAlpha[0] |= a != 0;
            pixels[y * header.width() + x] = (a << 24) | (r << 16) | (g << 8) | b;
        }, 4);
        if (!hasAlpha[0]) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] |= 0xFF000000;
            }
        }
        return hasAlpha[0];
    }

    private static void applyMask(byte[] data, DibHeader header, DibLayout layout, int[] pixels) {
        for (int y = 0; y < header.height(); y++) {
            int row = layout.maskOffset() + (header.height() - 1 - y) * layout.maskRowBytes();
            for (int x = 0; x < header.width(); x++) {
                if (IcoBinary.readPackedBit(data, row, x) != 0) {
                    pixels[y * header.width() + x] &= 0x00FFFFFF;
                }
            }
        }
    }

    private static int[] readPalette(byte[] data, int offset, int count) throws IOException {
        int[] palette = new int[count];
        if (count == 0) {
            return palette;
        }
        if (offset < 0 || offset + count * 4 > data.length) {
            throw new IOException("Invalid ICO DIB palette bounds");
        }
        for (int i = 0; i < count; i++) {
            int entry = offset + i * 4;
            int b = data[entry] & 0xFF;
            int g = data[entry + 1] & 0xFF;
            int r = data[entry + 2] & 0xFF;
            palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return palette;
    }

    private static void forEachPixel(DibHeader header, int offset, int rowBytes, PixelConsumer consumer, int stride) {
        for (int y = 0; y < header.height(); y++) {
            int row = offset + (header.height() - 1 - y) * rowBytes;
            for (int x = 0; x < header.width(); x++) {
                consumer.accept(x, y, row + x * stride);
            }
        }
    }

    private record DibHeader(int base, int limit, int headerSize, int width, int height, int bitDepth, int colorsUsed) {
        static DibHeader read(byte[] data, IcoDirectoryEntry entry, int maxDimension, int maxPixels) throws IOException {
            int base = entry.imageOffset();
            int limit = entry.imageOffset() + entry.bytesInResource();
            if (base + 40 > data.length || limit > data.length) {
                throw new IOException("Invalid ICO DIB bounds");
            }
            int headerSize = IcoBinary.readI32(data, base);
            if (headerSize < 40 || base + headerSize > limit) {
                throw new IOException("Unsupported ICO DIB header size: " + headerSize);
            }
            int width = IcoBinary.readI32(data, base + 4);
            int height = Math.abs(IcoBinary.readI32(data, base + 8)) / 2;
            int bitDepth = IcoBinary.readU16(data, base + 14);
            int compression = IcoBinary.readI32(data, base + 16);
            int colorsUsed = headerSize >= 40 ? IcoBinary.readI32(data, base + 32) : 0;
            validate(width, height, bitDepth, compression, maxDimension, maxPixels);
            return new DibHeader(base, limit, headerSize, width, height, bitDepth, colorsUsed);
        }

        private static void validate(int width, int height, int bitDepth, int compression, int maxDimension, int maxPixels) throws IOException {
            if (width <= 0 || height <= 0 || width > maxDimension || height > maxDimension) {
                throw new IOException("Invalid ICO DIB dimensions: " + width + "x" + height);
            }
            if ((long) width * height > maxPixels) {
                throw new IOException("ICO DIB image too large: " + width + "x" + height);
            }
            if (!(bitDepth == 1 || bitDepth == 4 || bitDepth == 8 || bitDepth == 24 || bitDepth == 32)) {
                throw new IOException("Unsupported ICO DIB bit depth: " + bitDepth);
            }
            if (compression != 0 && compression != 3) {
                throw new IOException("Unsupported ICO DIB compression: " + compression);
            }
        }
    }

    private record DibLayout(int paletteOffset, int paletteEntries, int xorOffset, int xorRowBytes, int maskOffset, int maskRowBytes, int maskEnd) {
        static DibLayout create(DibHeader header) throws IOException {
            int paletteEntries = header.bitDepth() > 8 ? 0 : Math.max(header.colorsUsed(), 1 << header.bitDepth());
            int paletteOffset = header.base() + header.headerSize();
            int xorOffset = paletteOffset + paletteEntries * 4;
            int xorRowBytes = alignedRowBytes(header.width(), header.bitDepth());
            int xorBytes = checkedMul(xorRowBytes, header.height(), "xor bitmap");
            int maskOffset = xorOffset + xorBytes;
            int maskRowBytes = alignedRowBytes(header.width(), 1);
            int maskBytes = checkedMul(maskRowBytes, header.height(), "and mask");
            int maskEnd = maskOffset + maskBytes;
            if (xorOffset < 0 || xorOffset + xorBytes > header.limit()) {
                throw new IOException("Invalid ICO DIB pixel bounds");
            }
            return new DibLayout(paletteOffset, paletteEntries, xorOffset, xorRowBytes, maskOffset, maskRowBytes, maskEnd);
        }

        private static int alignedRowBytes(int width, int bitDepth) throws IOException {
            long bits = (long) width * bitDepth;
            long bytes = (((bits + 31L) / 32L) * 32L) / 8L;
            if (bytes > Integer.MAX_VALUE) {
                throw new IOException("ICO DIB row is too large");
            }
            return (int) bytes;
        }

        private static int checkedMul(int left, int right, String label) throws IOException {
            long result = (long) left * right;
            if (result > Integer.MAX_VALUE) {
                throw new IOException("ICO DIB " + label + " is too large");
            }
            return (int) result;
        }
    }

    @FunctionalInterface
    private interface PixelConsumer {
        void accept(int x, int y, int offset);
    }
}
