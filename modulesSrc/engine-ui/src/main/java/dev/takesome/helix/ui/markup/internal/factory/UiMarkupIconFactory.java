package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.components.UiIconNode;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.icons.UiIcon;
import dev.takesome.helix.ui.icons.fontawesome.FontAwesomeStyle;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Creates retained Font Awesome icon markup nodes. */
public final class UiMarkupIconFactory {
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;

    public UiMarkupIconFactory(UiDomStyleReader reader, UiDomLayoutResolver layout) {
        this.reader = reader;
        this.layout = layout;
    }

    public Node icon(UiDomElement markup, Map<String, String> style, float parentW, float parentH) {
        UiIcon icon = resolveIcon(style);
        if (icon == null) return null;

        UiIconNode node = new UiIconNode(icon);
        UiColor color = reader.color(style, "icon-color", reader.color(style, "color", UiColor.WHITE));
        node.setColor(color);
        layout.setBounds(node, style, parentW, parentH, 18f, 18f);
        node.setScale(iconScale(style, node.bounds().w, node.bounds().h));
        node.setOpacity(reader.number(style, "opacity", 1f));
        layout.applyState(node, style);
        return node;
    }

    private float iconScale(Map<String, String> style, float width, float height) {
        String fontSize = reader.first(style, "font-size", "icon-size", "iconSize");
        if (!fontSize.isBlank()) return Math.max(0.01f, reader.number(fontSize, 18f) / 32f);

        String explicitScale = reader.first(style, "icon-scale", "iconScale", "scale");
        if (!explicitScale.isBlank()) return Math.max(0.01f, reader.number(explicitScale, 0.82f));

        if ((reader.has(style, "width") || reader.has(style, "w") || reader.has(style, "height") || reader.has(style, "h"))
                && width > 0f && height > 0f) {
            return Math.max(0.01f, Math.min(width, height) / 32f);
        }
        return 0.82f;
    }

    private UiIcon resolveIcon(Map<String, String> style) {
        String value = reader.first(style, "data-icon", "icon", "fa", "font-awesome", "fontAwesome");
        if (value.isBlank()) return null;
        return FontAwesomeStyle.SOLID.registry().find(value).orElse(null);
    }
}
