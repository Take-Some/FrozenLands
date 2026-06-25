package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssShorthandExpansionTest {
    @Test
    void expandsWebDesignerShorthandsIntoLonghands() {
        UiDomDocument document = UiDomDocument.parse("<body><section class=\"box\" /></body>");
        UiStylesheet stylesheet = new UiCssParser().parse(".box { padding: 8px 12px; border: 1px solid #fff; border-radius: 4px 8px; background: #111 url(\"ui/panel.png\") center / cover no-repeat; font: bold 14px title; transition: opacity 200ms ease 50ms; }");

        new UiCssCascade().apply(document, stylesheet);

        UiDomElement box = document.querySelector(".box").orElseThrow();
        assertEquals("8px", box.style("padding-top"));
        assertEquals("12px", box.style("padding-right"));
        assertEquals("8px", box.style("padding-bottom"));
        assertEquals("12px", box.style("padding-left"));
        assertEquals("1px", box.style("border-width"));
        assertEquals("solid", box.style("border-style"));
        assertEquals("#fff", box.style("border-color"));
        assertEquals("4px", box.style("border-top-left-radius"));
        assertEquals("8px", box.style("border-top-right-radius"));
        assertEquals("4px", box.style("border-bottom-right-radius"));
        assertEquals("8px", box.style("border-bottom-left-radius"));
        assertEquals("#111", box.style("background-color"));
        assertEquals("url(\"ui/panel.png\")", box.style("background-image"));
        assertEquals("center", box.style("background-position"));
        assertEquals("cover", box.style("background-size"));
        assertEquals("no-repeat", box.style("background-repeat"));
        assertEquals("bold", box.style("font-weight"));
        assertEquals("14px", box.style("font-size"));
        assertEquals("title", box.style("font-family"));
        assertEquals("opacity", box.style("transition-property"));
        assertEquals("200ms", box.style("transition-duration"));
        assertEquals("50ms", box.style("transition-delay"));
        assertEquals("ease", box.style("transition-timing-function"));
    }
}
