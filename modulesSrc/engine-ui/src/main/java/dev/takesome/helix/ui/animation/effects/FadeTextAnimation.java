package dev.takesome.helix.ui.animation.effects;

import dev.takesome.helix.ui.animation.UiTextAnimationDefinition;
import dev.takesome.helix.ui.animation.UiTextAnimationEffect;
import dev.takesome.helix.ui.animation.UiTextAnimationFrame;
import dev.takesome.helix.ui.animation.UiTextAnimationRuntime;

/** Fades text alpha from transparent to fully visible. */
public final class FadeTextAnimation implements UiTextAnimationEffect {
    public static final String ID = "fade";
    public static final float DEFAULT_DURATION = 0.35f;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void apply(UiTextAnimationDefinition definition, UiTextAnimationRuntime runtime, UiTextAnimationFrame frame) {
        if (frame == null || !frame.visible) return;
        frame.alpha *= runtime.progress(definition, DEFAULT_DURATION);
    }
}
