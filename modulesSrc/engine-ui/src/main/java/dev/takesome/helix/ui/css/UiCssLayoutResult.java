package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomElement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Result of resolving CSS bounds/layout against a viewport. */
public final class UiCssLayoutResult {
    private final LinkedHashMap<Integer, UiCssBox> boxes = new LinkedHashMap<>();

    void put(UiDomElement element, UiCssBox box) {
        boxes.put(element.nodeId(), box);
    }

    public Optional<UiCssBox> box(UiDomElement element) {
        if (element == null) return Optional.empty();
        return Optional.ofNullable(boxes.get(element.nodeId()));
    }

    public Map<Integer, UiCssBox> boxes() {
        return Collections.unmodifiableMap(boxes);
    }
}
