package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.skin.UiElementSkin;
import dev.takesome.helix.ui.components.UiAnimatedPanelNode;
import dev.takesome.helix.ui.components.UiElementNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.css.runtime.UiCssBoxShadow;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.internal.layout.UiDomLayoutResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomSkinResolver;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.ContainerNode;
import dev.takesome.helix.ui.node.Node;

import java.util.Map;

/** Creates structural/container markup nodes. */
public final class UiMarkupContainerFactory {
    private final UiDomStyleReader reader;
    private final UiDomLayoutResolver layout;
    private final UiDomSkinResolver skins;

    public UiMarkupContainerFactory(UiDomStyleReader reader, UiDomLayoutResolver layout, UiDomSkinResolver skins) {
        this.reader = reader;
        this.layout = layout;
        this.skins = skins;
    }

    public Node container(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        ContainerNode node = new ContainerNode();
        layout.setBounds(node, style, parentW, parentH, parentW, parentH);
        if (nodes != null && element != null) nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
        layout.applyState(node, style);
        return node;
    }

    public Node panel(UiDomElement element, Map<String, String> style, float parentW, float parentH, UiDomRetainedNodeFactory nodes) {
        UiPanelNode node = panelNode(panelColor(style), style);
        applyPanelVisualStyle(node, style);
        layout.setBounds(node, style, parentW, parentH, 540f, 430f);
        applyPanelSkin(node, style);
        applyAppearAnimation(node, style);
        if (nodes != null && element != null) nodes.compileChildren(node, element, node.bounds().w, node.bounds().h);
        layout.applyState(node, style);
        return node;
    }

    public Node element(UiDomElement markup, Map<String, String> style, float parentW, float parentH, String kind) {
        UiElementSkin element = skins == null ? null : skins.resolve(style, kind);
        if (element == null) {
            String skin = reader.first(style, "data-skin", "ui-skin", "skin", "src", "path");
            if (skin.isBlank()) return null;
            element = UiElementSkin.of(kind, skin, reader.integer(style, "frame", 0));
        }
        UiElementNode node = new UiElementNode(element);
        layout.setBounds(node, style, parentW, parentH, 64f, 64f);
        layout.applyState(node, style);
        return node;
    }


    private UiColor panelColor(Map<String, String> style) {
        UiColor background = reader.color(style, "background-color", null);
        if (background == null) background = reader.color(style, "background", null);
        return background == null ? UiColor.TRANSPARENT : background;
    }

    private void applyPanelSkin(UiPanelNode node, Map<String, String> style) {
        if (skins == null) return;
        UiElementSkin element = skins.resolve(style, "panel");
        if (element != null) node.setBackgroundSkin(element);
    }

    private UiPanelNode panelNode(UiColor color, Map<String, String> style) {
        return new UiAnimatedPanelNode(color, 0f);
    }

    private void applyPanelVisualStyle(UiPanelNode node, Map<String, String> style) {
        UiColor borderColor = reader.color(style, "border-color", reader.color(style, "borderColor", null));
        float borderWidth = reader.number(style, "border-width", reader.number(style, "borderWidth", 0f));
        if (borderColor != null && borderWidth > 0f) {
            node.setBorder(borderColor, borderWidth);
        }

        UiCssBoxShadow boxShadow = UiCssBoxShadow.parse(reader.value(style, "box-shadow"));
        if (boxShadow.enabled()) {
            node.setShadow(boxShadow.color(), boxShadow.offsetX(), boxShadow.offsetY(), boxShadow.blurRadius(), boxShadow.spreadRadius());
            return;
        }

        UiColor shadowColor = reader.color(style, "shadow-color", reader.color(style, "shadowColor", null));
        if (shadowColor != null && shadowColor.a > 0f) {
            float shadowX = reader.number(style, "shadow-offset-x", reader.number(style, "shadowOffsetX", 0f));
            float shadowY = reader.number(style, "shadow-offset-y", reader.number(style, "shadowOffsetY", 0f));
            node.setShadow(shadowColor, shadowX, shadowY);
        }
    }

    private boolean hasAppearAnimation(Map<String, String> style) {
        String mode = reader.first(style, "appear-animation", "appearAnimation", "appear");
        return !mode.isBlank() && !"none".equalsIgnoreCase(mode) && !"off".equalsIgnoreCase(mode) && !"false".equalsIgnoreCase(mode);
    }

    private void applyAppearAnimation(Node node, Map<String, String> style) {
        if (!(node instanceof UiAnimatedPanelNode animated)) return;
        if (!hasAppearAnimation(style)) return;
        float durationMs = reader.number(style, "appear-ms", reader.number(style, "appearMs", 160f));
        float offsetY = reader.number(style, "appear-offset-y", reader.number(style, "appearOffsetY", -18f));
        animated.slideFrom(offsetY, Math.max(0f, durationMs) / 1000f);
    }
}
