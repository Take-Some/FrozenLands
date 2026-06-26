package org.takesome.frozenlands.engine.host;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Decodes a launcher/window icon from an external module jar. */
public interface WindowIconDecoder {
    BufferedImage[] decode(Path iconPath) throws IOException;
}
