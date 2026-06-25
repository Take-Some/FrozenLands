package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssMustHaveVocabularyTest {
    @Test
    void loadsMustHaveLayoutTextPolishTransformAndAnimationProperties() {
        UiCssPropertyRegistry registry = UiCssPropertyRegistry.loadBuiltins();

        for (String property : List.of(
                "margin", "margin-left", "margin-right", "margin-top", "margin-bottom",
                "right", "bottom", "min-width", "min-height", "max-width", "max-height",
                "box-sizing", "overflow", "overflow-x", "overflow-y",
                "justify-content", "align-items", "align-self", "flex", "flex-grow", "flex-shrink", "flex-basis", "flex-wrap",
                "line-height", "white-space", "text-overflow", "letter-spacing", "text-transform", "vertical-align",
                "box-shadow", "outline", "outline-color", "outline-width", "outline-style", "cursor",
                "translate", "rotate", "scale", "transform-origin",
                "animation", "animation-name", "animation-duration", "animation-delay", "animation-timing-function", "animation-iteration-count", "animation-direction", "animation-fill-mode", "animation-play-state"
        )) {
            assertEquals(property, registry.require(property).name());
        }
    }

    @Test
    void expandsMarginShorthandIntoLonghands() {
        UiDomDocument document = UiDomDocument.parse("<body><section class=\"box\"></section></body>");
        UiStylesheet stylesheet = new UiCssParser().parse(".box { margin: 4px 8px 12px 16px; }");
        new UiCssCascade().apply(document, stylesheet);

        UiDomElement box = document.querySelector(".box").orElseThrow();
        assertEquals("4px", box.style("margin-top"));
        assertEquals("8px", box.style("margin-right"));
        assertEquals("12px", box.style("margin-bottom"));
        assertEquals("16px", box.style("margin-left"));
    }
}
