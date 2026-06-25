package org.takesome.frozenlands.engine.icons;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable result of decoding one ICO file. */
public record IcoImageSet(BufferedImage[] images, List<IcoImageInfo> info) {
    public IcoImageSet {
        images = images == null ? new BufferedImage[0] : images.clone();
        info = info == null ? List.of() : List.copyOf(info);
    }

    @Override
    public BufferedImage[] images() {
        return images.clone();
    }

    public boolean isEmpty() {
        return images.length == 0;
    }

    public Set<Dimension> availableSizes() {
        LinkedHashSet<Dimension> sizes = new LinkedHashSet<>();
        for (IcoImageInfo imageInfo : info) {
            sizes.add(new Dimension(imageInfo.width(), imageInfo.height()));
        }
        return Collections.unmodifiableSet(sizes);
    }
}
