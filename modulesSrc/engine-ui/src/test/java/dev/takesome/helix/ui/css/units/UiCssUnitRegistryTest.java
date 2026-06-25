package dev.takesome.helix.ui.css.units;

import dev.takesome.helix.ui.css.UiCssLength;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiCssUnitRegistryTest {
    private final UiCssUnitResolutionContext context = UiCssUnitResolutionContext.viewport(800f, 600f, 16f);

    @Test
    void resolvesAbsoluteRootAndViewportUnitsThroughRegistry() {
        assertEquals(10f, UiCssLength.parse("10px").resolve(context, 200f, 0f));
        assertEquals(100f, UiCssLength.parse("50%").resolve(context, 200f, 0f));
        assertEquals(96f, UiCssLength.parse("72pt").resolve(context, 0f, 0f), 0.001f);
        assertEquals(32f, UiCssLength.parse("2rem").resolve(context, 0f, 0f), 0.001f);
        assertEquals(400f, UiCssLength.parse("50vw").resolve(context, 0f, 0f), 0.001f);
        assertEquals(150f, UiCssLength.parse("25vh").resolve(context, 0f, 0f), 0.001f);
    }

    @Test
    void supportsHelixViewportAliasesRequestedForTemplates() {
        assertEquals(400f, UiCssLength.parse("50wv").resolve(context, 0f, 0f), 0.001f);
        assertEquals(150f, UiCssLength.parse("25wh").resolve(context, 0f, 0f), 0.001f);
    }
}
