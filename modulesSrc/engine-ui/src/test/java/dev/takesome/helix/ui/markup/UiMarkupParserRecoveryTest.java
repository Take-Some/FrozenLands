package dev.takesome.helix.ui.markup;

import dev.takesome.helix.ui.html.UiHtmlTagRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiMarkupParserRecoveryTest {
    private final UiMarkupParser parser = new UiMarkupParser();

    @Test
    void wrongClosingTagDoesNotAbortDocument() {
        UiMarkupDocument document = parser.parse("""
                <body>
                    <main>
                        <section>
                            <button action="topdown.start">Run</div>
                        </section>
                    </main>
                </body>
                """);

        UiMarkupNode body = document.root();
        UiMarkupNode main = body.children().get(0);
        UiMarkupNode section = main.children().get(0);
        UiMarkupNode button = section.children().get(0);

        assertEquals("body", body.tag());
        assertEquals("main", main.tag());
        assertEquals("section", section.tag());
        assertEquals("button", button.tag());
        assertEquals("Run", button.text());
    }

    @Test
    void nestedBlocksSurviveAncestorClosingTag() {
        UiMarkupDocument document = parser.parse("<div><section><button>Run</div>");

        UiMarkupNode root = document.root();
        assertEquals("div", root.tag());
        assertEquals("section", root.children().get(0).tag());
        assertEquals("button", root.children().get(0).children().get(0).tag());
        assertEquals("Run", root.children().get(0).children().get(0).text());
    }

    @Test
    void eofAutoClosesOpenElements() {
        UiMarkupDocument document = parser.parse("<body><main><section><span>Open");

        UiMarkupNode body = document.root();
        assertEquals("body", body.tag());
        assertEquals("main", body.children().get(0).tag());
        assertEquals("section", body.children().get(0).children().get(0).tag());
        assertEquals("span", body.children().get(0).children().get(0).children().get(0).tag());
        assertEquals("Open", body.children().get(0).children().get(0).children().get(0).text());
    }

    @Test
    void voidElementDoesNotRequireClosingTag() {
        UiMarkupDocument document = parser.parse("<body><img src=\"ui/logo.png\"><input type=\"checkbox\" checked></body>");

        UiMarkupNode body = document.root();
        assertEquals("img", body.children().get(0).tag());
        assertEquals("ui/logo.png", body.children().get(0).attributes().get("src"));
        assertEquals("input", body.children().get(1).tag());
        assertEquals("checkbox", body.children().get(1).attributes().get("type"));
        assertEquals("true", body.children().get(1).attributes().get("checked"));
    }

    @Test
    void styleContentIsParsedAsRawText() {
        UiMarkupDocument document = parser.parse("""
                <body>
                    <style>
                        .main-menu-button:hover { opacity: 0.82; }
                        .main-menu-button::before { content: "<"; }
                    </style>
                    <button class="main-menu-button">Run</button>
                </body>
                """);

        UiMarkupNode style = document.root().children().get(0);
        assertEquals("style", style.tag());
        assertTrue(style.text().contains(".main-menu-button:hover"));
        assertTrue(style.text().contains("content: \"<\""));
        assertEquals("button", document.root().children().get(1).tag());
    }


    @Test
    void headAndBodyScaffoldBuildsDocumentWithRenderRoot() {
        UiMarkupDocument document = parser.parse("""
                <head>
                    <meta charset="UTF-8">
                    <link rel="stylesheet" href="ui/hud.css">
                </head>
                <body>
                    <button action="topdown.start">Run</button>
                </body>
                """);

        assertEquals("html", document.root().tag());
        assertEquals("head", document.head().orElseThrow().tag());
        assertEquals("body", document.renderRoot().tag());
        assertEquals("button", document.renderRoot().children().get(0).tag());
        assertEquals("ui/hud.css", document.head().orElseThrow().children().get(1).attributes().get("href"));
    }

    @Test
    void tolerantRuntimeRegistryProvidesFallbackForUnknownTags() {
        UiHtmlTagRegistry registry = UiHtmlTagRegistry.loadBuiltins();

        assertEquals("div", registry.resolveOrFallback("unknown-widget").name());
        assertEquals("section", registry.resolveOrFallback("panel").name());
        assertEquals("img", registry.resolveOrFallback("image").name());
    }

    @Test
    void booleanAndUnquotedAttributesAreAccepted() {
        UiMarkupDocument document = parser.parse("<button disabled action=topdown.start data-slot={{slotId}}>Run</button>");

        UiMarkupNode button = document.root();
        assertEquals("button", button.tag());
        assertEquals("true", button.attributes().get("disabled"));
        assertEquals("topdown.start", button.attributes().get("action"));
        assertEquals("{{slotId}}", button.attributes().get("data-slot"));
        assertFalse(button.attributes().containsKey("onclick"));
    }



    @Test
    void optionalParagraphEndTagsAreAutoClosed() {
        UiMarkupDocument document = parser.parse("<body><p>First<p>Second</body>");

        UiMarkupNode body = document.root();
        assertEquals("p", body.children().get(0).tag());
        assertEquals("First", body.children().get(0).text());
        assertEquals("p", body.children().get(1).tag());
        assertEquals("Second", body.children().get(1).text());
    }

    @Test
    void optionalListItemEndTagsAreAutoClosed() {
        UiMarkupDocument document = parser.parse("<ul><li>One<li>Two<li>Three</ul>");

        UiMarkupNode ul = document.root();
        assertEquals("ul", ul.tag());
        assertEquals(3, ul.children().size());
        assertEquals("One", ul.children().get(0).text());
        assertEquals("Two", ul.children().get(1).text());
        assertEquals("Three", ul.children().get(2).text());
    }

    @Test
    void tableCellsAndRowsAreAutoClosed() {
        UiMarkupDocument document = parser.parse("<table><tbody><tr><td>A<td>B<tr><td>C</tbody></table>");

        UiMarkupNode table = document.root();
        UiMarkupNode tbody = table.children().get(0);
        assertEquals("tbody", tbody.tag());
        assertEquals(2, tbody.children().size());
        assertEquals("A", tbody.children().get(0).children().get(0).text());
        assertEquals("B", tbody.children().get(0).children().get(1).text());
        assertEquals("C", tbody.children().get(1).children().get(0).text());
    }

    @Test
    void textareaPreservesWhitespaceAndDecodesEntities() {
        UiMarkupDocument document = parser.parse("<body><textarea>Line 1\n  &lt;Line 2&gt;</textarea></body>");

        UiMarkupNode textarea = document.root().children().get(0);
        assertEquals("textarea", textarea.tag());
        assertTrue(textarea.text().contains("Line 1\n  <Line 2>"));
    }

    @Test
    void diagnosticsReportRecoveredMarkupProblems() {
        UiMarkupDocument document = parser.parse("""
                <body>
                    <unknown-widget onclick="legacy()">Broken</wrong-tag>
                    <section>Open
                </body>
                """);

        assertTrue(document.hasDiagnostics());
        assertTrue(document.diagnostics().stream().anyMatch(d -> "html.unknown-tag-fallback".equals(d.code())));
        assertTrue(document.diagnostics().stream().anyMatch(d -> "html.unsupported-attribute".equals(d.code())));
        assertTrue(document.diagnostics().stream().anyMatch(d -> "html.unexpected-closing-tag".equals(d.code())));
        assertTrue(document.diagnostics().stream().anyMatch(d -> "html.mismatched-closing-tag".equals(d.code())));
        assertTrue(document.diagnostics().stream().allMatch(d -> d.line() >= 1 && d.column() >= 1));
    }

    @Test
    void strictModeFailsOnRecoverableDiagnostics() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("<body><button>Run</div></body>", UiMarkupParseMode.STRICT));
    }

    @Test
    void diagnosticsExposeSourceSpanForEditorUnderlineAndNavigation() {
        UiMarkupDocument document = parser.parse("<body><unknown-widget>Broken</unknown-widget></body>", UiMarkupParseMode.EDITOR);

        assertTrue(document.hasDiagnostics());
        assertTrue(document.diagnostics().stream().allMatch(d -> d.offset() >= 0 && d.length() >= 1 && d.endOffset() > d.offset()));
        assertTrue(document.diagnostics().stream().allMatch(d -> !d.jumpTarget().isBlank()));
    }
}
