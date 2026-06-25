package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.uiComponents.image.UiImageNode;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Creates retained image markup nodes. */
public final class UiMarkupImageFactory {
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;

    public UiMarkupImageFactory(UiDomStyleReader reader, UiDomLayoutResolver layout) {
        this.reader = reader;
        this.layout = layout;
    }

    public Node image(UiDomElement markup, Map<String, String> style, float parentW, float parentH) {
        String source = reader.first(style, "src", "source", "path", "href", "skin");
        if (source.isBlank()) return null;
        UiImageNode node = new UiImageNode(source);
        node.setOpacity(reader.number(style, "opacity", 1f));
        layout.setBounds(node, style, parentW, parentH, 64f, 64f);
        layout.applyState(node, style);
        return node;
    }
}
