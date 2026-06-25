package org.takesome.frozenlands.engine.icons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

final class IcoStreams {
    private IcoStreams() {
    }

    static byte[] readAllBytesLimited(InputStream input, int maxBytes) throws IOException {
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

    static byte[] immutableCopy(byte[] data, int maxBytes) throws IOException {
        if (data == null || data.length < 6) {
            throw new IOException("Invalid ICO: too small");
        }
        if (data.length > maxBytes) {
            throw new IOException("ICO too large: " + data.length + " bytes");
        }
        return Arrays.copyOf(data, data.length);
    }
}
