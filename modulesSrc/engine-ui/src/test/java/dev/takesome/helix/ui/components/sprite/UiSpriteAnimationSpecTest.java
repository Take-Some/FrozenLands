package dev.takesome.helix.ui.components.sprite;

import dev.takesome.helix.ui.skin.UiElementSkin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiSpriteAnimationSpecTest {
    @Test
    void buildsSafeSpriteSheetSourceAndClampsFrameCountToGrid() {
        UiSpriteAnimationSpec spec = UiSpriteAnimationSpec.builder("ui/background.sprite")
                .grid(4, 2)
                .frameCount(99)
                .fps(10f)
                .build();

        assertEquals(4, spec.columns());
        assertEquals(2, spec.rows());
        assertEquals(8, spec.frameCount());
        assertEquals("ui/background.sprite?cols=4&rows=2", spec.sourceWithLayout());
        assertTrue(spec.animated());
    }

    @Test
    void keepsExistingQueryWhenAppendingSheetLayout() {
        UiSpriteAnimationSpec spec = UiSpriteAnimationSpec.of("ui/background.sprite?variant=night", 4, 2, 8, 10f);
        assertEquals("ui/background.sprite?variant=night&cols=4&rows=2", spec.sourceWithLayout());
    }

    @Test
    void createsSkinForSafeFrame() {
        UiSpriteAnimationSpec spec = UiSpriteAnimationSpec.of("ui/background.sprite", 4, 2, 8, 10f);
        UiElementSkin skin = spec.skin(10);
        assertEquals(2, skin.frame());
        assertEquals("ui/background.sprite?cols=4&rows=2", skin.source());
    }
}
