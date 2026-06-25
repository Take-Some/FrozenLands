package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.module.UiDomModuleGate;
import dev.takesome.helix.ui.markup.internal.style.UiMarkupCssResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Creates menu-list containers from retained markup. */
public final class UiMarkupMenuListFactory {
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;

    public UiMarkupMenuListFactory(
            UiMarkupCssResolver styles,
            UiDomStyleReader reader,
            UiDomLayoutResolver layout,
            UiDomModuleGate moduleGate
    ) {
        this.reader = reader;
        this.layout = layout;
    }

    public Node menuList(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        UiPanelNode node = new UiPanelNode(reader.color(style, "color", UiColor.TRANSPARENT));
        layout.setBounds(node, style, parentW, parentH, 220f, 160f);
        if (nodes != null && element != null) nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
        layout.applyState(node, style);
        return node;
    }
}
