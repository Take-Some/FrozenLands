package org.takesome.frozenlands.engine.icons;

import java.awt.image.BufferedImage;
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

    public IcoImageSet parseSet(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return parseSet(input);
        }
    }

    public IcoImageSet parseSet(InputStream input) throws IOException {
        return parseSet(IcoStreams.readAllBytesLimited(input, MAX_ICO_SIZE_BYTES));
    }

    public IcoImageSet parseSet(byte[] data) throws IOException {
        byte[] source = IcoStreams.immutableCopy(data, MAX_ICO_SIZE_BYTES);
        List<IcoDirectoryEntry> entries = IcoDirectoryReader.read(source, MAX_ENTRIES);
        List<BufferedImage> images = new ArrayList<>(entries.size());
        List<IcoImageInfo> infos = new ArrayList<>(entries.size());

        for (IcoDirectoryEntry entry : entries) {
            IcoDecodedImage decoded = IcoFrameDecoder.decode(source, entry, MAX_IMAGE_DIMENSION, MAX_DECODED_PIXELS);
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
}
