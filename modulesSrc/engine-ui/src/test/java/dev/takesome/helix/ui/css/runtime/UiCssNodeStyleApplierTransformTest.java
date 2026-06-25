package dev.takesome.helix.ui.css.runtime;

import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssNodeStyleApplierTransformTest {
    @Test
    void appliesTransformTranslateScaleAndTranslateLonghandToRetainedBounds() {
        UiPanelNode node = new UiPanelNode(UiColor.TRANSPARENT);
        node.setBounds(10f, 20f, 100f, 40f);

        new UiCssNodeStyleApplier().apply(node, Map.of(
                "x", "10",
                "y", "20",
                "width", "100",
                "height", "40",
                "transform", "translate(2px, 3px) scale(1.1)",
                "translate", "5px 7px",
                "scale", "1.2"
        ));

        assertEquals(17f, node.bounds().x, 0.001f);
        assertEquals(30f, node.bounds().y, 0.001f);
        assertEquals(132f, node.bounds().w, 0.001f);
        assertEquals(52.8f, node.bounds().h, 0.001f);
    }
}
