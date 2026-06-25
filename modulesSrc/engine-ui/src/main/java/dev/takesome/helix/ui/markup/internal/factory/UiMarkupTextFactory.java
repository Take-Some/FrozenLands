package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.i18n.UiDomI18nText;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Creates text/label markup nodes. */
public final class UiMarkupTextFactory {
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;
    private final EngineI18n i18n;
    private final UiBindingSource bindingSource;

    public UiMarkupTextFactory(UiDomStyleReader reader, UiDomLayoutResolver layout) {
        this(reader, layout, null);
    }

    public UiMarkupTextFactory(UiDomStyleReader reader, UiDomLayoutResolver layout, EngineI18n i18n) {
        this(reader, layout, i18n, null);
    }

    public UiMarkupTextFactory(UiDomStyleReader reader, UiDomLayoutResolver layout, EngineI18n i18n, UiBindingSource bindingSource) {
        this.reader = reader;
        this.layout = layout;
        this.i18n = i18n;
        this.bindingSource = bindingSource;
    }

    public Node label(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        String bindText = reader.first(style, "bind-text");
        String content = !bindText.isBlank() && bindingSource != null
                ? bindingSource.text(bindText)
                : UiDomI18nText.textOrLocalized(element, "text", i18n);
        float fontScale = reader.has(style, "font-size")
                ? Math.max(0.01f, reader.number(style, "font-size", 16f) / 16f)
                : reader.number(style, "scale", reader.number(style, "font-scale", 1f));
        UiLabelNode node = new UiLabelNode(
                content,
                fontScale,
                reader.color(style, "color", UiColor.WHITE),
                reader.align(style),
                reader.font(style)
        );
        if (!bindText.isBlank() && bindingSource != null) {
            node.addRuntimeBinding((boundNode, dt) -> node.setText(bindingSource.text(bindText)));
        }
        applyTextPadding(node, style);
        layout.setBounds(node, style, parentW, parentH, parentW, 32f);
        if (nodes != null && element != null && element.childCount() > 0) {
            nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
        }
        layout.applyState(node, style);
        return node;
    }

    private void applyTextPadding(UiLabelNode node, Map<String, String> style) {
        float all = length(reader.value(style, "padding"), 0f);
        float left = length(reader.first(style, "padding-left", "paddingLeft"), all);
        float right = length(reader.first(style, "padding-right", "paddingRight"), all);
        float top = length(reader.first(style, "padding-top", "paddingTop"), all);
        float bottom = length(reader.first(style, "padding-bottom", "paddingBottom"), all);
        node.setPadding(left, right, top, bottom);
    }

    private float length(String raw, float fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.endsWith("px")) value = value.substring(0, value.length() - 2).trim();
        try {
            return Math.max(0f, Float.parseFloat(value));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
