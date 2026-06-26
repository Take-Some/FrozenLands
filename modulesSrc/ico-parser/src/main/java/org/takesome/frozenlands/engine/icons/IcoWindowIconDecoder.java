package org.takesome.frozenlands.engine.icons;

import org.takesome.frozenlands.engine.host.WindowIconDecoder;
import org.takesome.frozenlands.engine.icons.selection.IcoImageSelector;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class IcoWindowIconDecoder implements WindowIconDecoder {
    @Override
    public BufferedImage[] decode(Path iconPath) throws IOException {
        return IcoImageSelector.pickBestIcons(new IcoFileParser().parse(iconPath));
    }
}
