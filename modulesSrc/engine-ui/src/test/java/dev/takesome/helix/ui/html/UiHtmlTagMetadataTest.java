package dev.takesome.helix.ui.html;



import org.junit.jupiter.api.Test;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertTrue;



final class UiHtmlTagMetadataTest {

    @Test

    void builtInTagsExposeMetadataForEditorAndDocumentation() {

        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();



        assertMeta(registry.require("html"), UiHtmlTagCategory.ROOT, UiHtmlContentModel.FLOW);

        assertMeta(registry.require("body"), UiHtmlTagCategory.ROOT, UiHtmlContentModel.FLOW);

        assertMeta(registry.require("head"), UiHtmlTagCategory.DOCUMENT_METADATA, UiHtmlContentModel.METADATA);

        assertMeta(registry.require("link"), UiHtmlTagCategory.DOCUMENT_METADATA, UiHtmlContentModel.VOID);

        assertMeta(registry.require("meta"), UiHtmlTagCategory.DOCUMENT_METADATA, UiHtmlContentModel.VOID);

        assertMeta(registry.require("title"), UiHtmlTagCategory.DOCUMENT_METADATA, UiHtmlContentModel.TEXT_ONLY);

        assertMeta(registry.require("button"), UiHtmlTagCategory.CONTROL, UiHtmlContentModel.MIXED_CONTROL);

        assertMeta(registry.require("input"), UiHtmlTagCategory.CONTROL, UiHtmlContentModel.VOID);

        assertMeta(registry.require("img"), UiHtmlTagCategory.MEDIA, UiHtmlContentModel.VOID);

        assertMeta(registry.require("style"), UiHtmlTagCategory.STYLE_RAW, UiHtmlContentModel.RAW_TEXT);

        assertMeta(registry.require("span"), UiHtmlTagCategory.TEXT, UiHtmlContentModel.TEXT_ONLY);

    }



    @Test

    void commonAttributesKeepBrowserScriptOutOfTagVocabulary() {

        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();

        UiHtmlTagMeta button = registry.require("button").meta();



        assertTrue(button.allowedAttributes().contains("action"));

        assertTrue(button.allowedAttributes().contains("data-*"));

        assertFalse(button.allowedAttributes().contains("onclick"));

        assertTrue(button.interactive());

    }



    private void assertMeta(UiHtmlTagSpec tag, UiHtmlTagCategory category, UiHtmlContentModel contentModel) {

        UiHtmlTagMeta meta = tag.meta();

        assertEquals(tag.name(), meta.name());

        assertEquals(tag.composerId(), meta.composerId());

        assertEquals(category, meta.category());

        assertEquals(contentModel, meta.contentModel());

        assertEquals(tag.status(), meta.status());

        assertFalse(meta.description().isBlank());

        assertFalse(meta.usefulAction().isBlank());

    }

}
