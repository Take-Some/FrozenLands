package org.takesome.frozenlands.engine.icons;

/** Metadata for one frame stored inside an ICO file. */
public record IcoImageInfo(
        int width,
        int height,
        int bitDepth,
        int colors,
        String format,
        int dataSize
) {
}
