package dev.takesome.helix.ui.components.sprite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiSpriteAnimationPlaybackTest {
    @Test
    void loopsFramesByFps() {
        UiSpriteAnimationPlayback playback = new UiSpriteAnimationPlayback(
                UiSpriteAnimationSpec.of("ui/background.sprite", 4, 2, 8, 10f)
        );

        assertEquals(0, playback.currentFrame());
        assertTrue(playback.update(0.11f));
        assertEquals(1, playback.currentFrame());
        playback.seekSeconds(0.82f);
        assertEquals(0, playback.currentFrame());
    }

    @Test
    void onceModeStopsAtLastFrame() {
        UiSpriteAnimationPlayback playback = new UiSpriteAnimationPlayback(
                UiSpriteAnimationSpec.builder("ui/background.sprite")
                        .grid(4, 2)
                        .frameCount(8)
                        .fps(10f)
                        .mode(UiSpritePlaybackMode.ONCE)
                        .maxDeltaSeconds(10f)
                        .build()
        );

        playback.update(10f);
        assertEquals(7, playback.currentFrame());
        assertFalse(playback.playing());
    }

    @Test
    void pingPongModeAlternatesWithoutDuplicatingEdges() {
        UiSpriteAnimationPlayback playback = new UiSpriteAnimationPlayback(
                UiSpriteAnimationSpec.builder("ui/background.sprite")
                        .grid(4, 1)
                        .frameCount(4)
                        .fps(1f)
                        .mode(UiSpritePlaybackMode.PING_PONG)
                        .build()
        );

        assertEquals(0, playback.seekSeconds(0f).currentFrame());
        assertEquals(1, playback.seekSeconds(1f).currentFrame());
        assertEquals(2, playback.seekSeconds(2f).currentFrame());
        assertEquals(3, playback.seekSeconds(3f).currentFrame());
        assertEquals(2, playback.seekSeconds(4f).currentFrame());
        assertEquals(1, playback.seekSeconds(5f).currentFrame());
        assertEquals(0, playback.seekSeconds(6f).currentFrame());
    }
}
