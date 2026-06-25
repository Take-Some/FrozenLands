package dev.takesome.helix.ui.css;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiCssParserKeyframesTest {
    @Test
    void parsesKeyframesAndSamplesInterpolatedFrameStyles() {
        UiStylesheet stylesheet = new UiCssParser().parse("""
                @keyframes pulse {
                    from { opacity: 0; transform: translate(0px, 0px) scale(1); }
                    to { opacity: 1; transform: translate(10px, 4px) scale(1.2); }
                }
                .badge { animation: pulse 1s ease infinite alternate both; }
                """);

        assertTrue(stylesheet.keyframes().containsKey("pulse"));
        assertEquals("0.5", stylesheet.keyframes().get("pulse").sample(0.5).get("opacity"));
        assertEquals("translate(5px, 2px) scale(1.1)", stylesheet.keyframes().get("pulse").sample(0.5).get("transform"));
        assertEquals(1, stylesheet.rules().size());
    }
}
