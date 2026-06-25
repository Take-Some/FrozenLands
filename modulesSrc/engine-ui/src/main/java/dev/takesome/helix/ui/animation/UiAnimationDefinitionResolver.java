package dev.takesome.helix.ui.animation;

import dev.takesome.helix.ui.definition.UiWidgetDefinition;

/** Resolves widget JSON animation declarations into pipeline-ready definitions. */
public final class UiAnimationDefinitionResolver {
    private UiAnimationDefinitionResolver() {}

    public static UiTextAnimationDefinition text(UiWidgetDefinition widget) {
        if (widget == null) return null;
        UiTextAnimationDefinition source = widget.textAnimation != null ? widget.textAnimation : widget.textEffect;
        String selected = firstNonBlank(source == null ? null : source.effectExpression(), widget.animation, widget.effect);
        if (selected == null || selected.isBlank()) return null;
        UiTextAnimationDefinition out = copyOf(source);
        if (out.effect == null || out.effect.isBlank()) out.effect = selected;
        out.delay = firstPositive(out.delay, widget.animationDelay, widget.effectDelay);
        out.duration = firstPositive(out.duration, widget.animationDuration, widget.effectDuration);
        out.speed = firstPositive(out.speed, widget.animationSpeed, widget.effectSpeed);
        if (out.slideX == 0f) out.slideX = widget.slideX;
        if (out.slideY == 0f) out.slideY = widget.slideY;
        return out;
    }

    private static UiTextAnimationDefinition copyOf(UiTextAnimationDefinition source) {
        UiTextAnimationDefinition out = new UiTextAnimationDefinition();
        if (source == null) return out;
        out.effect = source.effect;
        out.type = source.type;
        out.pipeline = source.pipeline;
        out.effects = source.effects;
        out.params = source.params;
        out.delay = source.delay;
        out.duration = source.duration;
        out.speed = source.speed;
        out.slideX = source.slideX;
        out.slideY = source.slideY;
        out.resetOnTextChange = source.bool("resetOnTextChange", source.resetOnTextChange);
        return out;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static float firstPositive(float... values) {
        if (values == null) return 0f;
        for (float value : values) {
            if (value > 0f) return value;
        }
        return 0f;
    }
}
