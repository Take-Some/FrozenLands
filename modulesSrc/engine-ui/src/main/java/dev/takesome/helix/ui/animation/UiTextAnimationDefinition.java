package dev.takesome.helix.ui.animation;

import java.util.List;
import java.util.Map;

/**
 * Declarative text animation selected by UI JSON.
 *
 * The renderer does not branch on concrete animation names. JSON selects a
 * registered effect or pipeline and supplies parameters; UiAnimationPipeline
 * resolves and runs the registered effects.
 */
public final class UiTextAnimationDefinition {
    /** Canonical registered effect id or composed pipeline, e.g. fade, slide, fade+slide. */
    public String effect;

    /** Legacy alias kept for existing JSON documents. Prefer effect. */
    public String type;

    /** Optional explicit pipeline expression. Prefer effects for structured JSON. */
    public String pipeline;

    /** Optional structured pipeline list, e.g. ["typewriter", "fade", "slide"]. */
    public List<String> effects;

    /** Optional free-form parameters for custom registered effects. */
    public Map<String, Object> params;

    /** Seconds before the animation starts after the widget first becomes visible. */
    public float delay;

    /** Seconds used by time-based effects such as fade and slide. */
    public float duration;

    /** Typewriter reveal speed in characters per second. */
    public float speed;

    /** Initial horizontal offset for slide-in animation. */
    public float slideX;

    /** Initial vertical offset for slide-in animation. */
    public float slideY;

    /** Restart the animation when the resolved text value changes. */
    public boolean resetOnTextChange;

    public String effectExpression() {
        if (effects != null && !effects.isEmpty()) {
            StringBuilder out = new StringBuilder();
            for (String item : effects) {
                if (item == null || item.isBlank()) continue;
                if (out.length() > 0) out.append('+');
                out.append(item.trim());
            }
            if (out.length() > 0) return out.toString();
        }
        if (pipeline != null && !pipeline.isBlank()) return pipeline;
        if (effect != null && !effect.isBlank()) return effect;
        return type;
    }

    public float number(String name, float fallback) {
        Object value = param(name);
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value instanceof String) {
            try {
                return Float.parseFloat(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public boolean bool(String name, boolean fallback) {
        Object value = param(name);
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean(((String) value).trim());
        return fallback;
    }

    public String string(String name, String fallback) {
        Object value = param(name);
        return value == null ? fallback : String.valueOf(value);
    }

    private Object param(String name) {
        if (name == null || name.isBlank() || params == null || params.isEmpty()) return null;
        return params.get(name);
    }
}
