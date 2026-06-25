package dev.takesome.helix.ui.animation;

/** Registered UI text animation effect. Effects are composed by UiAnimationPipeline. */
public interface UiTextAnimationEffect {
    String id();

    void apply(UiTextAnimationDefinition definition, UiTextAnimationRuntime runtime, UiTextAnimationFrame frame);
}
