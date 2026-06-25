package dev.takesome.helix.ui.css.runtime;

import dev.takesome.helix.ui.model.UiBoxShadow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UiCssBoxShadowParserTest {
    @Test
    void parsesCss3BoxShadowValues() {
        List<UiBoxShadow> shadows = new UiCssBoxShadowParser().parse("0 4px 18px rgba(0,0,0,0.12)");
        assertEquals(1, shadows.size());
        UiBoxShadow shadow = shadows.get(0);
        assertEquals(0f, shadow.offsetX());
        assertEquals(4f, shadow.offsetY());
        assertEquals(18f, shadow.blurRadius());
        assertEquals(0f, shadow.spreadRadius());
        assertEquals(0.12f, shadow.color().a, 0.001f);
        assertFalse(shadow.inset());
    }
}
