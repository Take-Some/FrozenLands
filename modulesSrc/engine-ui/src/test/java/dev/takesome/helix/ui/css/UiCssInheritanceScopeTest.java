package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssInheritanceScopeTest {
    @Test
    void inheritsNativeTextPropertiesFromContainer() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement section = document.createElement("section");
        UiDomElement label = document.createElement("span");
        section.appendChild(label);
        root.appendChild(section);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse("section { color: #ffd95a; font-family: title; font-size: 18px; } span { text-align: inherit; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("#ffd95a", label.style("color"));
        assertEquals("title", label.style("font-family"));
        assertEquals("18px", label.style("font-size"));
    }

    @Test
    void scopesRulesToClassContainers() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement editor = document.createElement("section");
        UiDomElement game = document.createElement("section");
        UiDomElement editorButton = document.createElement("button");
        UiDomElement gameButton = document.createElement("button");
        editor.classList().add("editor");
        game.classList().add("game");
        editor.appendChild(editorButton);
        game.appendChild(gameButton);
        root.appendChild(editor);
        root.appendChild(game);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse(".editor button { color: #ffffff; } .game button { color: #00ff00; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("#ffffff", editorButton.style("color"));
        assertEquals("#00ff00", gameButton.style("color"));
    }

    @Test
    void scopesRulesToIdContainers() {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("body");
        UiDomElement panel = document.createElement("main");
        UiDomElement button = document.createElement("button");
        panel.setAttribute("id", "inspector");
        panel.appendChild(button);
        root.appendChild(panel);
        document.setRoot(root);

        UiStylesheet stylesheet = new UiCssParser().parse("#inspector button { opacity: 0.75; }");
        new UiCssCascade().apply(document, stylesheet);

        assertEquals("0.75", button.style("opacity"));
    }
}
