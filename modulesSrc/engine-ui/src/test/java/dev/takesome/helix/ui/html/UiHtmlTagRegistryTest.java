package dev.takesome.helix.ui.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiHtmlTagRegistryTest {
    @Test
    void loadsOnlyNativeHtmlTagsFromDefinitionFiles() {
        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();

        assertEquals("html", registry.require("html").name());
        assertEquals("head", registry.require("head").name());
        assertEquals("link", registry.require("link").name());
        assertEquals("meta", registry.require("meta").name());
        assertEquals("title", registry.require("title").name());
        assertEquals("button", registry.require("button").name());
        assertEquals("img", registry.require("img").name());
        assertEquals("main", registry.require("main").name());
        assertEquals("section", registry.require("section").name());
        assertEquals("span", registry.require("span").name());
        assertEquals("label", registry.require("label").name());
        assertEquals("dialog", registry.require("dialog").name());
        assertEquals("input", registry.require("input").name());
    }

    @Test
    void rejectsRemovedEngineTags() {
        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();

        assertThrows(UiHtmlException.class, () -> registry.require("screen"));
        assertThrows(UiHtmlException.class, () -> registry.require("panel"));
        assertThrows(UiHtmlException.class, () -> registry.require("ribbon"));
        assertThrows(UiHtmlException.class, () -> registry.require("text"));
        assertThrows(UiHtmlException.class, () -> registry.require("menu-list"));
        assertThrows(UiHtmlException.class, () -> registry.require("image"));
        assertThrows(UiHtmlException.class, () -> registry.require("settings-fields"));
        assertThrows(UiHtmlException.class, () -> registry.require("input-capture"));
        assertThrows(UiHtmlException.class, () -> registry.require("key-capture"));
        assertThrows(UiHtmlException.class, () -> registry.require("combo-box"));
        assertThrows(UiHtmlException.class, () -> registry.require("slider"));
        assertThrows(UiHtmlException.class, () -> registry.require("checkbox"));
    }
}
