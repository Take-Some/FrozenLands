package org.takesome.frozenlands.engine.icons;

record IcoDirectoryEntry(
        int width,
        int height,
        int colors,
        int planes,
        int bitDepth,
        int bytesInResource,
        int imageOffset
) {
    IcoImageInfo toInfo(int decodedWidth, int decodedHeight, String format) {
        return new IcoImageInfo(decodedWidth, decodedHeight, bitDepth, colors, format, bytesInResource);
    }
}
