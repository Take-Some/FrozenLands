package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssDomBridgeTest {
    @Test
    void layoutSupportsCenteredExplicitPositionKeywords() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("screen");
        UiDomElement panel = document.createElement("panel");

        document.setRoot(root);
        root.appendChild(panel);

        panel.setComputedStyle("position", "absolute");
        panel.setComputedStyle("x", "center");
        panel.setComputedStyle("y", "center");
        panel.setComputedStyle("width", "200");
        panel.setComputedStyle("height", "100");

        UiCssLayoutResult result = new UiCssLayoutEngine().layout(document, 800f, 600f);
        UiCssBox box = result.box(panel).orElseThrow();

        assertEquals(300f, box.x(), 0.001f);
        assertEquals(250f, box.y(), 0.001f);
        assertEquals(200f, box.width(), 0.001f);
        assertEquals(100f, box.height(), 0.001f);
    }

    @Test
    void layoutSupportsEndExplicitPositionKeywords() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("screen");
        UiDomElement panel = document.createElement("panel");

        document.setRoot(root);
        root.appendChild(panel);

        panel.setComputedStyle("position", "absolute");
        panel.setComputedStyle("x", "right");
        panel.setComputedStyle("y", "bottom");
        panel.setComputedStyle("width", "200");
        panel.setComputedStyle("height", "100");

        UiCssLayoutResult result = new UiCssLayoutEngine().layout(document, 800f, 600f);
        UiCssBox box = result.box(panel).orElseThrow();

        assertEquals(600f, box.x(), 0.001f);
        assertEquals(500f, box.y(), 0.001f);
        assertEquals(200f, box.width(), 0.001f);
        assertEquals(100f, box.height(), 0.001f);
    }
}
