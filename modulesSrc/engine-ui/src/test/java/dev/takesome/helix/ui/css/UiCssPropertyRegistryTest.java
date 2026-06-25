package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.css.properties.layout.DisplayCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PositionCssProperty;
import dev.takesome.helix.ui.css.properties.layout.WidthCssProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UiCssPropertyRegistryTest {
    @Test
    void loadsCssOptionsFromDefinitionFiles() {
        UiCssPropertyRegistry registry = UiCssPropertyRegistry.loadBuiltins();
        assertInstanceOf(DisplayCssProperty.class, registry.require("display"));
        assertInstanceOf(PositionCssProperty.class, registry.require("position"));
        assertInstanceOf(WidthCssProperty.class, registry.require("w"));
        assertEquals("width", registry.canonicalName("w"));
    }
}
