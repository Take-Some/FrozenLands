package dev.takesome.helix.ui.dom;

import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiDomDocumentTest {
    @Test
    void parsesMarkupAndSupportsBasicQueries() {
        UiDomDocument document = UiDomDocument.parse("""
                <body id=\"editor\" class=\"shell root\">
                  <section id=\"project\" class=\"dock left\">
                    <button id=\"save\" class=\"primary action\" text=\"Save\" />
                    <label class=\"caption\">Ready</label>
                  </section>
                </body>
                """);

        assertEquals("body", document.root().tagName());
        assertEquals("editor", document.root().id());
        assertTrue(document.querySelector(":root").isPresent());
        assertEquals("project", document.querySelector("section.left").orElseThrow().id());
        assertEquals("save", document.querySelector("button#save.primary").orElseThrow().id());
        assertEquals(1, document.getElementsByClassName("caption").size());
        assertEquals(1, document.getElementsByTagName("button").size());
        assertEquals("Ready", document.querySelector("label.caption").orElseThrow().textContent());
    }


    @Test
    void exposesDocumentHeadBodyAndRenderRoot() {
        UiDomDocument document = UiDomDocument.parse("""
                <html>
                    <head><meta charset="UTF-8"/></head>
                    <body><section id="content"></section></body>
                </html>
                """);

        assertEquals("html", document.documentElement().tagName());
        assertEquals("head", document.head().orElseThrow().tagName());
        assertEquals("body", document.body().orElseThrow().tagName());
        assertEquals("body", document.renderRoot().tagName());
        assertEquals("content", document.renderRoot().children().get(0).nodeName().equals("section") ? ((UiDomElement) document.renderRoot().children().get(0)).id() : "");
    }

    @Test
    void tracksAttributeClassAndStyleMutations() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        document.setRoot(root);
        document.drainMutations();

        root.setAttribute("id", "main");
        root.classList().add("editor");
        root.setComputedStyle("font-family", "Noesis UI");

        List<UiDomMutation> mutations = document.drainMutations();
        assertEquals(3, mutations.size());
        assertEquals(UiDomMutationType.ATTRIBUTE_CHANGED, mutations.get(0).type());
        assertEquals(UiDomMutationType.ATTRIBUTE_CHANGED, mutations.get(1).type());
        assertEquals(UiDomMutationType.STYLE_CHANGED, mutations.get(2).type());
        assertEquals("main", document.getElementById("main").orElseThrow().id());
        assertTrue(root.classList().contains("editor"));
        assertEquals("Noesis UI", root.style("font-family"));
    }

    @Test
    void supportsTreeMutationAndTraversal() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement panel = document.createElement("section");
        UiDomText text = document.createText("Runtime Inspector");

        document.setRoot(root);
        root.appendChild(panel);
        panel.appendChild(text);

        assertEquals(3, UiDomTraversal.depthFirst(root).size());
        assertEquals("Runtime Inspector", root.textContent());
        assertTrue(text.hasParent());

        panel.removeChild(text);
        assertFalse(text.hasParent());
        assertEquals("", root.textContent());
    }
    @Test
    void markupDocumentOwnsCanonicalDom() {
        UiMarkupDocument markup = new UiMarkupParser().parse("""
                <body id="menu" data-action="open" data-target-id="settings">
                    <section id="panel" class="popup card">Ready</section>
                </body>
                """);

        UiDomDocument dom = markup.dom();
        assertEquals("html", dom.documentElement().tagName());
        assertEquals("body", dom.renderRoot().tagName());
        assertEquals("menu", dom.renderRoot().id());
        assertEquals("open", dom.renderRoot().data("action"));
        assertEquals("settings", dom.renderRoot().dataset().get("targetId"));
        assertTrue(dom.querySelector("section.card").isPresent());
        assertTrue(dom.mutations().isEmpty());
        assertFalse(dom.root().dirty());
    }

    @Test
    void elementSupportsDatasetMatchesAndClosest() {
        UiDomDocument document = UiDomDocument.parse("""
                <body><section id="panel" class="popup"><button id="ok" data-action="confirm"></button></section></body>
                """);

        UiDomElement button = document.getElementById("ok").orElseThrow();
        assertEquals("confirm", button.data("action"));
        assertTrue(button.matches("button#ok"));
        assertEquals("panel", button.closest("section.popup").id());
    }

    @Test
    void rootSelectorMatchesOnlyDocumentRoot() {
        UiDomDocument document = UiDomDocument.parse("""
                <html class="shell"><head></head><body id="page" class="shell"><section id="content"></section></body></html>
                """);

        UiDomElement html = document.documentElement();
        UiDomElement body = document.body().orElseThrow();

        assertTrue(html.matches(":root"));
        assertTrue(html.matches("html:root"));
        assertTrue(html.matches(".shell:root"));
        assertFalse(body.matches(":root"));
        assertFalse(body.matches("body:root"));
        assertEquals("html", document.querySelector(":root").orElseThrow().tagName());
    }

}
