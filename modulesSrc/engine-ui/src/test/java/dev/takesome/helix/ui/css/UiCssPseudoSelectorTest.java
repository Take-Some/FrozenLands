package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssPseudoSelectorTest {
    @Test
    void appliesNativePseudoClassesFromDomStateAndAttributes() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement button = document.createElement("button");
        UiDomElement input = document.createElement("input");
        input.setAttribute("disabled", "");
        input.setAttribute("checked", "");
        button.setPseudoClass("hover", true);
        button.setPseudoClass("active", true);
        root.appendChild(button);
        root.appendChild(input);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse("button:hover { color: #fff; } button:active { opacity: 0.5; } input:disabled { pointer-events: none; } input:checked { border-color: #0f0; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("#fff", button.style("color"));
        assertEquals("0.5", button.style("opacity"));
        assertEquals("none", input.style("pointer-events"));
        assertEquals("#0f0", input.style("border-color"));
    }

    @Test
    void storesNativeBeforeAfterPseudoElementStyles() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement item = document.createElement("span");
        item.classList().add("item");
        root.appendChild(item);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse(".item::before { content: \"*\"; color: #ffd95a; } .item::after { content: \"!\"; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("\"*\"", item.pseudoStyle("before", "content"));
        assertEquals("#ffd95a", item.pseudoStyle("before", "color"));
        assertEquals("\"!\"", item.pseudoStyle("after", "content"));
    }

    @Test
    void storesLegacySingleColonBeforeAfterPseudoElementStyles() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement item = document.createElement("span");
        item.classList().add("item");
        root.appendChild(item);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse(".item:before { content: \">\"; width: 8px; } .item:after { content: \"!\"; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("\">\"", item.pseudoStyle("before", "content"));
        assertEquals("8px", item.pseudoStyle("before", "width"));
        assertEquals("\"!\"", item.pseudoStyle("after", "content"));
    }

}
