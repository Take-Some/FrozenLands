package org.takesome.frozenlands.engine.icons;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

final class IcoFrameDecoder {
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private IcoFrameDecoder() {
    }

    static IcoDecodedImage decode(byte[] data, IcoDirectoryEntry entry, int maxDimension, int maxPixels) throws IOException {
        int offset = entry.imageOffset();
        int end = offset + entry.bytesInResource();
        if (offset < 0 || end > data.length || offset >= end) {
            throw new IOException("Invalid ICO image bounds");
        }

        byte[] imageData = Arrays.copyOfRange(data, offset, end);
        if (isPng(imageData)) {
            BufferedImage image = decodePng(imageData);
            return new IcoDecodedImage(image, entry.toInfo(image.getWidth(), image.getHeight(), "png"));
        }

        BufferedImage dib = IcoDibDecoder.decode(data, entry, maxDimension, maxPixels);
        return new IcoDecodedImage(dib, entry.toInfo(dib.getWidth(), dib.getHeight(), "dib"));
    }

    private static BufferedImage decodePng(byte[] imageData) throws IOException {
        BufferedImage png = ImageIO.read(new ByteArrayInputStream(imageData));
        if (png == null) {
            throw new IOException("Invalid PNG ICO frame");
        }
        return ensureArgb(png);
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
}
