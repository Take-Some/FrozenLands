package dev.takesome.helix.ui.markup.internal.compile;

import dev.takesome.helix.ui.markup.UiMarkupDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.markup.UiMarkupParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class UiMarkupHeadLinkStylesheetTest {
    @Test
    void loadsStylesheetFromHeadLinkAndKeepsHeadOutOfRenderStyles() throws Exception {
        Path css = Files.createTempFile("helix-head-link", ".css");
        Files.writeString(css, "#start { x: 17; y: 23; width: 123; height: 45; }", StandardCharsets.UTF_8);
        String href = css.toAbsolutePath().toString().replace(java.io.File.separatorChar, '/');

        UiMarkupDocument document = new UiMarkupParser().parse("""
                <html>
                  <head>
                    <meta charset="UTF-8">
                    <link rel="stylesheet" href="%s">
                  </head>
                  <body>
                    <button id="start">Run</button>
                  </body>
                </html>
                """.formatted(href));

        UiDomElement button = (UiDomElement) document.dom().renderRoot().children().get(0);
        UiDomComputedStyles computed = new UiDomStyleBridge().compute(document, 800f, 600f);
        Map<String, String> style = computed.base().get(button);

        assertNotNull(style);
        assertEquals("17", style.get("x"));
        assertEquals("23", style.get("y"));
        assertEquals("123", style.get("width"));
        assertEquals("45", style.get("height"));
        assertEquals("none", computed.base().get(document.dom().head().orElseThrow()).get("display"));
    }

    @Test
    void resolvesStylesheetHrefRelativeToMarkupSourcePath() throws Exception {
        Path root = Files.createTempDirectory("helix-relative-style");
        Path documentPath = root.resolve("ui/scenes/main_menu.ui.html");
        Path css = root.resolve("ui/scenes/styles/topdown.ui.css");
        Files.createDirectories(css.getParent());
        Files.writeString(css, "#start { x: 31; y: 41; width: 151; height: 61; }", StandardCharsets.UTF_8);

        UiMarkupDocument document = new UiMarkupParser().parse("""
                <html>
                  <head>
                    <link rel="stylesheet" href="styles/topdown.ui.css">
                  </head>
                  <body>
                    <button id="start">Run</button>
                  </body>
                </html>
                """, documentPath.toAbsolutePath().toString().replace(java.io.File.separatorChar, '/'));

        UiDomElement button = (UiDomElement) document.dom().renderRoot().children().get(0);
        UiDomComputedStyles computed = new UiDomStyleBridge().compute(document, 800f, 600f);
        Map<String, String> style = computed.base().get(button);

        assertNotNull(style);
        assertEquals("31", style.get("x"));
        assertEquals("41", style.get("y"));
        assertEquals("151", style.get("width"));
        assertEquals("61", style.get("height"));
    }

}
