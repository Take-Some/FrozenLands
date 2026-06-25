package dev.takesome.helix.ui.html;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiHtmlMustHaveVocabularyTest {
    @Test
    void loadsMustHaveEditorAndDiagnosticsTags() {
        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();

        for (String tag : List.of(
                "html", "head", "link", "meta", "title", "form", "select", "option", "textarea", "progress", "hr", "br", "pre", "code",
                "table", "thead", "tbody", "tr", "th", "td", "template"
        )) {
            assertEquals(tag, registry.require(tag).name());
        }
    }

    @Test
    void loadsCommonAttributesAndGenericDataPayloadAttributes() {
        UiHtmlAttributeRegistry registry = UiHtmlAttributeRegistry.loadBuiltins();

        for (String attribute : List.of(
                "id", "class", "style", "value", "disabled", "src", "width", "height",
                "bind", "bind-text", "bind-value", "bind-visible", "bind-class", "bind-style",
                "placeholder", "readonly", "required", "min", "max", "step", "selected"
        )) {
            assertEquals(attribute, registry.require(attribute).name());
        }

        assertEquals("data-*", registry.require("data-asset-id").name());
        assertEquals("data-*", registry.require("data-component-id").name());
    }
}
