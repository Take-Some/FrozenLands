package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssChildCombinatorTest {
    @Test
    void directChildCombinatorMatchesOnlyImmediateChildren() {
        UiDomDocument document = UiDomDocument.parse("""
                <body>
                    <section class=\"ribbon\">
                        <h3 class=\"title direct\"></h3>
                        <div class=\"wrapper\">
                            <h3 class=\"title nested\"></h3>
                        </div>
                    </section>
                </body>
                """);
        UiStylesheet stylesheet = new UiCssParser().parse("""
                .ribbon { color: #f2dfb7; font-family: \"Cairopixel\"; text-align: center; }
                .ribbon > .title { color: #ffd95a; }
                """);

        new UiCssCascade().apply(document, stylesheet);

        UiDomElement direct = document.querySelector(".direct").orElseThrow();
        UiDomElement nested = document.querySelector(".nested").orElseThrow();

        assertEquals("#ffd95a", direct.style("color"));
        assertEquals("#f2dfb7", nested.style("color"));
        assertEquals("\"Cairopixel\"", direct.style("font-family"));
        assertEquals("center", direct.style("text-align"));
    }

    @Test
    void directChildCombinatorWorksWithoutWhitespaceAroundGreaterThan() {
        UiDomDocument document = UiDomDocument.parse("<body><section class=\"panel\"><h3 class=\"title\"></h3></section></body>");
        UiStylesheet stylesheet = new UiCssParser().parse("section.panel>.title { color: #ffd95a; }");

        new UiCssCascade().apply(document, stylesheet);

        UiDomElement title = document.querySelector(".title").orElseThrow();
        assertEquals("#ffd95a", title.style("color"));
    }
}
