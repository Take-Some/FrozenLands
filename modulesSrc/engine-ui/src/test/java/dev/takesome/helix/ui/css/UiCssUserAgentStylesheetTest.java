package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssUserAgentStylesheetTest {
    @Test
    void appliesInvisibleUserAgentDefaultsBeforeAuthorStyles() {
        UiDomDocument document = UiDomDocument.parse("<body><button>Run</button></body>");
        new UiCssCascade().apply(document, UiStylesheet.empty());

        UiDomElement body = document.querySelector("body").orElseThrow();
        UiDomElement button = document.querySelector("button").orElseThrow();
        assertEquals("8px", body.style("margin-top"));
        assertEquals("2px", button.style("padding-top"));
    }

    @Test
    void authorUniversalResetOverridesUserAgentTagDefaults() {
        UiDomDocument document = UiDomDocument.parse("<body><button>Run</button></body>");
        UiStylesheet author = new UiCssParser().parse("* { margin: 0; padding: 0; }");
        new UiCssCascade().apply(document, author);

        UiDomElement body = document.querySelector("body").orElseThrow();
        UiDomElement button = document.querySelector("button").orElseThrow();
        assertEquals("0", body.style("margin-top"));
        assertEquals("0", button.style("padding-top"));
    }
}
