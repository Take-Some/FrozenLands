package dev.takesome.helix.ui.css;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssLayoutUnitsTest {
    @Test
    void layoutResolvesViewportRootFontAndPointUnitsAgainstRuntimeContext() {
        UiDomDocument document = UiDomDocument.parse("""
                <body id="root">
                  <section id="panel">
                    <div id="child" />
                  </section>
                </body>
                """);
        UiStylesheet stylesheet = new UiCssParser().parse("""
                body { width: 100vw; height: 100vh; margin: 0; padding: 0; font-size: 20px; }
                section { width: 50vw; height: 25vh; margin-top: 1rem; padding-left: 12pt; padding-top: 6pt; }
                div { width: 10wv; height: 10wh; }
                """);

        new UiCssCascade().apply(document, stylesheet);
        UiCssLayoutResult result = new UiCssLayoutEngine().layout(document, 800f, 600f);

        UiDomElement root = document.getElementById("root").orElseThrow();
        UiDomElement panel = document.getElementById("panel").orElseThrow();
        UiDomElement child = document.getElementById("child").orElseThrow();

        UiCssBox rootBox = result.box(root).orElseThrow();
        UiCssBox panelBox = result.box(panel).orElseThrow();
        UiCssBox childBox = result.box(child).orElseThrow();

        assertEquals(800f, rootBox.width(), 0.001f);
        assertEquals(600f, rootBox.height(), 0.001f);
        assertEquals(400f, panelBox.width(), 0.001f);
        assertEquals(150f, panelBox.height(), 0.001f);
        assertEquals(20f, rootBox.height() - panelBox.y() - panelBox.height(), 0.001f);
        assertEquals(16f, childBox.x(), 0.001f);
        assertEquals(28f, rootBox.height() - childBox.y() - childBox.height(), 0.001f);
        assertEquals(80f, childBox.width(), 0.001f);
        assertEquals(60f, childBox.height(), 0.001f);
    }

    @Test
    void intrinsicTextMetricsUseConfiguredMeasurerFontIdScaleAndFontSize() {
        UiDomDocument document = UiDomDocument.parse("""
                <body id="root">
                  <h3 id="title">ABC</h3>
                </body>
                """);
        UiStylesheet stylesheet = new UiCssParser().parse("""
                body { width: 800px; height: 600px; margin: 0; padding: 0; }
                h3 { width: fit-content; height: fit-content; font-family: "Public Pixel"; font-weight: normal; font-size: 32px; padding-left: 4px; padding-right: 6px; padding-top: 2px; padding-bottom: 3px; }
                """);
        RecordingTextMeasurer measurer = new RecordingTextMeasurer(123f, 45f);

        new UiCssCascade().apply(document, stylesheet);
        UiCssLayoutResult result = new UiCssLayoutEngine(UiCssPropertyRegistry.loadBuiltins(), measurer).layout(document, 800f, 600f);

        UiDomElement title = document.getElementById("title").orElseThrow();
        UiCssBox titleBox = result.box(title).orElseThrow();

        assertEquals("ABC", measurer.text);
        assertEquals("Public Pixel", measurer.fontId);
        assertEquals(2f, measurer.scale, 0.001f);
        assertEquals(32f, measurer.fallbackFontSize, 0.001f);
        assertEquals(133f, titleBox.width(), 0.001f);
        assertEquals(50f, titleBox.height(), 0.001f);
    }

    private static final class RecordingTextMeasurer implements UiIntrinsicTextMeasurer {
        private final float width;
        private final float height;
        private String text;
        private String fontId;
        private float scale;
        private float fallbackFontSize;

        private RecordingTextMeasurer(float width, float height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public UiIntrinsicTextMetrics measure(String text, String fontId, float scale, float fallbackFontSize) {
            this.text = text;
            this.fontId = fontId;
            this.scale = scale;
            this.fallbackFontSize = fallbackFontSize;
            return new UiIntrinsicTextMetrics(width, height);
        }
    }

}
