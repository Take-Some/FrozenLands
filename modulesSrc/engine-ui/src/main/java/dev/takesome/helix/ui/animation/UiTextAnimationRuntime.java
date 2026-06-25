package dev.takesome.helix.ui.animation;

/** Runtime values supplied to one registered UI text animation effect. */
public record UiTextAnimationRuntime(
        String id,
        String fullText,
        float localTime
) {
    public float progress(UiTextAnimationDefinition definition, float fallbackDuration) {
        float baseDuration = definition != null && definition.duration > 0f ? definition.duration : fallbackDuration;
        float duration = definition == null ? baseDuration : definition.number("duration", baseDuration);
        if (duration <= 0f) return 1f;
        return clamp01(localTime / duration);
    }

    public static float clamp01(float value) {
        if (Float.isNaN(value) || value <= 0f) return 0f;
        if (value >= 1f) return 1f;
        return value;
    }
}
