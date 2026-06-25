package org.takesome.frozenlands.engine.icons;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stateless ICO parser for desktop icon pipelines.
 *
 * <p>Supports PNG-backed ICO frames and common BMP/DIB-backed frames with
 * 1, 4, 8, 24 and 32 bits per pixel.</p>
 */
public final class IcoFileParser {
    public static final int MAX_ICO_SIZE_BYTES = 32 * 1024 * 1024;
    public static final int MAX_ENTRIES = 2048;
    public static final int MAX_IMAGE_DIMENSION = 4096;
    public static final int MAX_DECODED_PIXELS = 4096 * 4096;

    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public IcoImageSet parseSet(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return parseSet(input);
        }
    }

    public IcoImageSet parseSet(InputStream input) throws IOException {
        return parseSet(readAllBytesLimited(input, MAX_ICO_SIZE_BYTES));
    }

    public IcoImageSet parseSet(byte[] data) throws IOException {
        byte[] source = immutableCopy(data);
        Header header = readHeader(source);
        List<Entry> entries = readEntries(source, header.count());
        List<BufferedImage> images = new ArrayList<>(entries.size());
        List<IcoImageInfo> infos = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            Decoded decoded = decodeEntry(source, entry);
            images.add(decoded.image());
            infos.add(decoded.info());
        }
        return new IcoImageSet(images.toArray(BufferedImage[]::new), infos);
    }

    public BufferedImage[] parse(Path path) throws IOException {
        return parseSet(path).images();
    }

    public BufferedImage[] parse(InputStream input) throws IOException {
        return parseSet(input).images();
    }

    public BufferedImage[] parse(byte[] data) throws IOException {
        return parseSet(data).images();
    }

    public List<BufferedImage> parseList(Path path) throws IOException {
        return Arrays.asList(parse(path));
    }

    public List<BufferedImage> parseList(InputStream input) throws IOException {
        return Arrays.asList(parse(input));
    }

    public List<BufferedImage> parseList(byte[] data) throws IOException {
        return Arrays.asList(parse(data));
    }

    private static Decoded decodeEntry(byte[] data, Entry entry) throws IOException {
        int offset = entry.imageOffset();
        int end = offset + entry.bytesInResource();
        if (offset < 0 || end > data.length || offset >= end) {
            throw new IOException("Invalid ICO image bounds");
        }
        byte[] imageData = Arrays.copyOfRange(data, offset, end);
        if (isPng(imageData)) {
            BufferedImage png = ImageIO.read(new ByteArrayInputStream(imageData));
            if (png == null) {
                throw new IOException("Invalid PNG ICO frame");
            }
            BufferedImage argb = ensureArgb(png);
            return new Decoded(argb, entry.toInfo(argb.getWidth(), argb.getHeight(), "png"));
        }
        BufferedImage dib = decodeDibFrame(data, entry);
        return new Decoded(dib, entry.toInfo(dib.getWidth(), dib.getHeight(), "dib"));
    }

    private static BufferedImage decodeDibFrame(byte[] data, Entry entry) throws IOException {
        int base = entry.imageOffset();
        int limit = entry.imageOffset() + entry.bytesInResource();
        if (base + 40 > data.length || limit > data.length) {
            throw new IOException("Invalid ICO DIB bounds");
        }
        int headerSize = readI32(data, base);
        if (headerSize < 40 || base + headerSize > limit) {
            throw new IOException("Unsupported ICO DIB header size: " + headerSize);
        }
        int width = readI32(data, base + 4);
        int storedHeight = readI32(data, base + 8);
        int height = Math.abs(storedHeight) / 2;
        int bitDepth = readU16(data, base + 14);
        int compression = readI32(data, base + 16);
        int colorsUsed = headerSize >= 40 ? readI32(data, base + 32) : 0;
        validateDib(width, height, bitDepth, compression);

        int paletteEntries = paletteEntries(bitDepth, colorsUsed);
        int paletteOffset = base + headerSize;
        int[] palette = readPalette(data, paletteOffset, paletteEntries);
        int xorOffset = paletteOffset + paletteEntries * 4;
        int xorRowBytes = alignedRowBytes(width, bitDepth);
        int xorBytes = checkedMul(xorRowBytes, height, "xor bitmap");
        int maskOffset = xorOffset + xorBytes;
        int maskRowBytes = alignedRowBytes(width, 1);
        int maskBytes = checkedMul(maskRowBytes, height, "and mask");
        if (xorOffset < 0 || xorOffset + xorBytes > limit) {
            throw new IOException("Invalid ICO DIB pixel bounds");
        }

        int[] pixels = new int[width * height];
        boolean hasAlpha = switch (bitDepth) {
            case 1, 4, 8 -> readIndexed(data, width, height, xorOffset, xorRowBytes, bitDepth, palette, pixels);
            case 24 -> readRgb24(data, width, height, xorOffset, xorRowBytes, pixels);
            case 32 -> readArgb32(data, width, height, xorOffset, xorRowBytes, pixels);
            default -> throw new IOException("Unsupported ICO DIB bit depth: " + bitDepth);
        };
        if (!hasAlpha && maskOffset + maskBytes <= limit) {
            applyMask(data, width, height, maskOffset, maskRowBytes, pixels);
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    private static boolean readIndexed(byte[] data, int width, int height, int offset, int rowBytes, int bitDepth, int[] palette, int[] pixels) throws IOException {
        for (int y = 0; y < height; y++) {
            int srcY = height - 1 - y;
            int row = offset + srcY * rowBytes;
            for (int x = 0; x < width; x++) {
                int index = switch (bitDepth) {
                    case 1 -> readPackedBit(data, row, x);
                    case 4 -> readPackedNibble(data, row, x);
                    case 8 -> data[row + x] & 0xFF;
                    default -> throw new IOException("Unsupported indexed bit depth: " + bitDepth);
                };
                if (index < 0 || index >= palette.length) {
                    throw new IOException("ICO DIB palette index out of range: " + index);
                }
                pixels[y * width + x] = palette[index];
            }
        }
        return false;
    }

    private static boolean readRgb24(byte[] data, int width, int height, int offset, int rowBytes, int[] pixels) {
        for (int y = 0; y < height; y++) {
            int srcY = height - 1 - y;
            int row = offset + srcY * rowBytes;
            for (int x = 0; x < width; x++) {
                int p = row + x * 3;
                int b = data[p] & 0xFF;
                int g = data[p + 1] & 0xFF;
                int r = data[p + 2] & 0xFF;
                pixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return false;
    }

    private static boolean readArgb32(byte[] data, int width, int height, int offset, int rowBytes, int[] pixels) {
        boolean hasAlpha = false;
        for (int y = 0; y < height; y++) {
            int srcY = height - 1 - y;
            int row = offset + srcY * rowBytes;
            for (int x = 0; x < width; x++) {
                int p = row + x * 4;
                int b = data[p] & 0xFF;
                int g = data[p + 1] & 0xFF;
                int r = data[p + 2] & 0xFF;
                int a = data[p + 3] & 0xFF;
                hasAlpha |= a != 0;
                pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        if (!hasAlpha) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] |= 0xFF000000;
            }
        }
        return hasAlpha;
    }

    private static void applyMask(byte[] data, int width, int height, int offset, int rowBytes, int[] pixels) {
        for (int y = 0; y < height; y++) {
            int srcY = height - 1 - y;
            int row = offset + srcY * rowBytes;
            for (int x = 0; x < width; x++) {
                if (readPackedBit(data, row, x) != 0) {
                    int i = y * width + x;
                    pixels[i] &= 0x00FFFFFF;
                }
            }
        }
    }

    private static Header readHeader(byte[] data) throws IOException {
        if (data.length < 6) {
            throw new IOException("Invalid ICO: missing header");
        }
        int reserved = readU16(data, 0);
        int type = readU16(data, 2);
        int count = readU16(data, 4);
        if (reserved != 0 || type != 1) {
            throw new IOException("Invalid or unsupported ICO header");
        }
        if (count <= 0 || count > MAX_ENTRIES) {
            throw new IOException("Invalid ICO image count: " + count);
        }
        if (6L + (long) count * 16L > data.length) {
            throw new IOException("Invalid ICO directory table");
        }
        return new Header(count);
    }

    private static List<Entry> readEntries(byte[] data, int count) throws IOException {
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int offset = 6 + i * 16;
            int width = data[offset] == 0 ? 256 : data[offset] & 0xFF;
            int height = data[offset + 1] == 0 ? 256 : data[offset + 1] & 0xFF;
            int colors = data[offset + 2] & 0xFF;
            int planes = readU16(data, offset + 4);
            int bitDepth = readU16(data, offset + 6);
            int bytesInResource = readI32(data, offset + 8);
            int imageOffset = readI32(data, offset + 12);
            if (width <= 0 || height <= 0 || bytesInResource <= 0 || imageOffset < 0 || imageOffset + bytesInResource > data.length) {
                throw new IOException("Invalid ICO directory entry");
            }
            entries.add(new Entry(width, height, colors, planes, bitDepth, bytesInResource, imageOffset));
        }
        return entries;
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

    private static void validateDib(int width, int height, int bitDepth, int compression) throws IOException {
        if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            throw new IOException("Invalid ICO DIB dimensions: " + width + "x" + height);
        }
        if ((long) width * (long) height > MAX_DECODED_PIXELS) {
            throw new IOException("ICO DIB image too large: " + width + "x" + height);
        }
        if (!(bitDepth == 1 || bitDepth == 4 || bitDepth == 8 || bitDepth == 24 || bitDepth == 32)) {
            throw new IOException("Unsupported ICO DIB bit depth: " + bitDepth);
        }
        if (compression != 0 && compression != 3) {
            throw new IOException("Unsupported ICO DIB compression: " + compression);
        }
    }

    private static int paletteEntries(int bitDepth, int colorsUsed) {
        if (bitDepth > 8) {
            return 0;
        }
        return colorsUsed > 0 ? colorsUsed : 1 << bitDepth;
    }

    private static int alignedRowBytes(int width, int bitDepth) throws IOException {
        long bits = (long) width * (long) bitDepth;
        long alignedBits = ((bits + 31L) / 32L) * 32L;
        long bytes = alignedBits / 8L;
        if (bytes > Integer.MAX_VALUE) {
            throw new IOException("ICO DIB row is too large");
        }
        return (int) bytes;
    }

    private static int checkedMul(int left, int right, String label) throws IOException {
        long result = (long) left * (long) right;
        if (result > Integer.MAX_VALUE) {
            throw new IOException("ICO DIB " + label + " is too large");
        }
        return (int) result;
    }

    private static byte[] readAllBytesLimited(InputStream input, int maxBytes) throws IOException {
        if (input == null) {
            throw new IOException("ICO InputStream is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(64 * 1024, maxBytes));
        byte[] buffer = new byte[16 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("ICO stream too large: >" + maxBytes + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static byte[] immutableCopy(byte[] data) throws IOException {
        if (data == null || data.length < 6) {
            throw new IOException("Invalid ICO: too small");
        }
        if (data.length > MAX_ICO_SIZE_BYTES) {
            throw new IOException("ICO too large: " + data.length + " bytes");
        }
        return Arrays.copyOf(data, data.length);
    }

    private static boolean isPng(byte[] data) {
        if (data == null || data.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static int readU16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }

    private static int readI32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static int readPackedBit(byte[] data, int rowOffset, int x) {
        int index = rowOffset + x / 8;
        int bit = 7 - (x % 8);
        return (data[index] >> bit) & 0x01;
    }

    private static int readPackedNibble(byte[] data, int rowOffset, int x) {
        int b = data[rowOffset + x / 2] & 0xFF;
        return (x & 1) == 0 ? (b >> 4) & 0x0F : b & 0x0F;
    }

    private static BufferedImage ensureArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private record Header(int count) {
    }

    private record Entry(int width, int height, int colors, int planes, int bitDepth, int bytesInResource, int imageOffset) {
        IcoImageInfo toInfo(int decodedWidth, int decodedHeight, String format) {
            return new IcoImageInfo(decodedWidth, decodedHeight, bitDepth, colors, format, bytesInResource);
        }
    }

    private record Decoded(BufferedImage image, IcoImageInfo info) {
    }
}
