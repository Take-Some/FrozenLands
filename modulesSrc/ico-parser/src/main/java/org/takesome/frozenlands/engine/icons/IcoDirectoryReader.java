package org.takesome.frozenlands.engine.icons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class IcoDirectoryReader {
    private IcoDirectoryReader() {
    }

    static List<IcoDirectoryEntry> read(byte[] data, int maxEntries) throws IOException {
        int count = readHeader(data, maxEntries);
        List<IcoDirectoryEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int offset = 6 + i * 16;
            int width = data[offset] == 0 ? 256 : data[offset] & 0xFF;
            int height = data[offset + 1] == 0 ? 256 : data[offset + 1] & 0xFF;
            int colors = data[offset + 2] & 0xFF;
            int planes = IcoBinary.readU16(data, offset + 4);
            int bitDepth = IcoBinary.readU16(data, offset + 6);
            int bytesInResource = IcoBinary.readI32(data, offset + 8);
            int imageOffset = IcoBinary.readI32(data, offset + 12);
            validateEntry(data, width, height, bytesInResource, imageOffset);
            entries.add(new IcoDirectoryEntry(width, height, colors, planes, bitDepth, bytesInResource, imageOffset));
        }
        return List.copyOf(entries);
    }

    private static int readHeader(byte[] data, int maxEntries) throws IOException {
        if (data.length < 6) {
            throw new IOException("Invalid ICO: missing header");
        }
        int reserved = IcoBinary.readU16(data, 0);
        int type = IcoBinary.readU16(data, 2);
        int count = IcoBinary.readU16(data, 4);
        if (reserved != 0 || type != 1) {
            throw new IOException("Invalid or unsupported ICO header");
        }
        if (count <= 0 || count > maxEntries) {
            throw new IOException("Invalid ICO image count: " + count);
        }
        if (6L + (long) count * 16L > data.length) {
            throw new IOException("Invalid ICO directory table");
        }
        return count;
    }

    private static void validateEntry(byte[] data, int width, int height, int bytesInResource, int imageOffset) throws IOException {
        long end = (long) imageOffset + bytesInResource;
        if (width <= 0 || height <= 0 || bytesInResource <= 0 || imageOffset < 0 || end > data.length) {
            throw new IOException("Invalid ICO directory entry");
        }
    }
}
