package dev.takesome.helix.ui.animation.effects;

import dev.takesome.helix.ui.animation.UiTextAnimationDefinition;
import dev.takesome.helix.ui.animation.UiTextAnimationEffect;
import dev.takesome.helix.ui.animation.UiTextAnimationFrame;
import dev.takesome.helix.ui.animation.UiTextAnimationRuntime;

/** Reveals text progressively by character count. */
public final class TypewriterTextAnimation implements UiTextAnimationEffect {
    public static final String ID = "typewriter";
    public static final float DEFAULT_SPEED = 36f;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(UiTextAnimationDefinition definition, UiTextAnimationRuntime runtime, UiTextAnimationFrame frame) {
        if (frame == null || !frame.visible || frame.text == null || frame.text.isEmpty()) return;
        float configuredSpeed = definition == null ? 0f : definition.speed;
        float speed = definition == null
                ? DEFAULT_SPEED
                : definition.number("speed", configuredSpeed > 0f ? configuredSpeed : DEFAULT_SPEED);
        float configuredDuration = definition == null ? 0f : definition.duration;
        float duration = definition == null ? 0f : definition.number("duration", configuredDuration);

        int visibleChars;
        if (speed <= 0f && duration > 0f) {
            visibleChars = Math.round(frame.text.length() * runtime.progress(definition, duration));
        } else {
            visibleChars = Math.round(runtime.localTime() * speed);
        }
        visibleChars = clamp(visibleChars, 0, frame.text.length());
        if (visibleChars < frame.text.length()) frame.text = frame.text.substring(0, visibleChars);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
