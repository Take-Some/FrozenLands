package dev.takesome.helix.ui.animation.effects;

import dev.takesome.helix.ui.animation.UiTextAnimationDefinition;
import dev.takesome.helix.ui.animation.UiTextAnimationEffect;
import dev.takesome.helix.ui.animation.UiTextAnimationFrame;
import dev.takesome.helix.ui.animation.UiTextAnimationRuntime;

/** Slides text from an initial offset toward its final position. */
public final class SlideTextAnimation implements UiTextAnimationEffect {
    public static final String ID = "slide";
    public static final float DEFAULT_DURATION = 0.35f;
    public static final float DEFAULT_OFFSET_Y = -12f;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(UiTextAnimationDefinition definition, UiTextAnimationRuntime runtime, UiTextAnimationFrame frame) {
        if (frame == null || !frame.visible) return;
        float progress = runtime.progress(definition, DEFAULT_DURATION);
        float sx = definition == null ? 0f : definition.number("slideX", definition.slideX);
        float sy = definition == null ? 0f : definition.number("slideY", definition.slideY);
        if (sx == 0f && sy == 0f) sy = DEFAULT_OFFSET_Y;
        float inverse = 1f - progress;
        frame.offsetX += sx * inverse;
        frame.offsetY += sy * inverse;
    }
}
