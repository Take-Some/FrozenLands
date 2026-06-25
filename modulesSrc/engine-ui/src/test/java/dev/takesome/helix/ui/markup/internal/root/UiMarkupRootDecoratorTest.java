package dev.takesome.helix.ui.markup.internal.root;

import dev.takesome.helix.ui.components.UiElementNode;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupNode;
import dev.takesome.helix.ui.markup.internal.style.UiDomStyleReader;
import dev.takesome.helix.ui.node.ContainerNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UiDomRootDecoratorTest {
    @Test
    void unwrapsCssUrlBackgroundImageBeforeCreatingImageSkin() {
        ContainerNode container = new ContainerNode();
        UiMarkupNode root = new UiMarkupNode("main", Map.of(), List.of(), "");
        Map<String, String> style = Map.of("background-image", "url(ui/background.jpg)");

        new UiDomRootDecorator(new UiDomStyleReader()).addRootBackground(container, dom(root), style, 1280f, 720f);

        UiElementNode image = assertInstanceOf(UiElementNode.class, container.children().get(0));
        assertEquals("ui/background.jpg", image.element().source());
    }

    @Test
    void unwrapsQuotedCssUrlBackgroundImageBeforeCreatingImageSkin() {
        ContainerNode container = new ContainerNode();
        UiMarkupNode root = new UiMarkupNode("main", Map.of(), List.of(), "");
        Map<String, String> style = Map.of("background-image", "url(\"ui/settings.png\")");

        new UiDomRootDecorator(new UiDomStyleReader()).addRootBackground(container, dom(root), style, 1280f, 720f);

        UiElementNode image = assertInstanceOf(UiElementNode.class, container.children().get(0));
        assertEquals("ui/settings.png", image.element().source());
    }

    @Test
    void ignoresCssNoneBackgroundImage() {
        ContainerNode container = new ContainerNode();
        UiMarkupNode root = new UiMarkupNode("main", Map.of(), List.of(), "");
        Map<String, String> style = Map.of("background-image", "none");

        new UiDomRootDecorator(new UiDomStyleReader()).addRootBackground(container, dom(root), style, 1280f, 720f);

        assertEquals(0, container.children().size());
    }

    private UiDomElement dom(UiMarkupNode root) {
        return new UiMarkupDocument(root).dom().renderRoot();
    }
}
