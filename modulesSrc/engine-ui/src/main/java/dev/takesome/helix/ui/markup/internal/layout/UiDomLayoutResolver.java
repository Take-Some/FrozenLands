package dev.takesome.helix.ui.markup.internal.layout;

import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Layout and node-state application for markup-created nodes. */
public final class UiDomLayoutResolver {
    private final UiDomStyleReader reader;

    public UiDomLayoutResolver(UiDomStyleReader reader) {
        this.reader = reader;
    }

    public void setBounds(Node node, Map<String, String> style, float parentW, float parentH, float fallbackW, float fallbackH) {
        float w = reader.number(style, "w", reader.number(style, "width", fallbackW));
        float h = reader.number(style, "h", reader.number(style, "height", fallbackH));
        float x = coordinate(style, "x", parentW, w, 0f);
        float y = coordinate(style, "y", parentH, h, 0f);
        node.setBounds(x, y, Math.max(0f, w), Math.max(0f, h));
    }

    public void applyState(Node node, Map<String, String> style) {
        if (reader.has(style, "visible")) node.setVisible(reader.bool(style, "visible", true));
        if (reader.has(style, "disabled")) node.setEnabled(!reader.bool(style, "disabled", false));
    }

    public boolean hasPosition(Map<String, String> style) {
        return reader.has(style, "x") || reader.has(style, "y");
    }

    private float coordinate(Map<String, String> style, String key, float parentSize, float ownSize, float fallback) {
        String raw = reader.value(style, key);
        if (raw.isBlank()) return fallback;
        if ("center".equalsIgnoreCase(raw)) return parentSize * 0.5f - ownSize * 0.5f;
        if ("end".equalsIgnoreCase(raw) || "right".equalsIgnoreCase(raw) || "bottom".equalsIgnoreCase(raw)) return parentSize - ownSize;
        return reader.number(raw, fallback);
    }
}
