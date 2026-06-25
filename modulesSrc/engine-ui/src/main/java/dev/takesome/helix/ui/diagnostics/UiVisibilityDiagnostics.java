package dev.takesome.helix.ui.diagnostics;


import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import dev.takesome.helix.ui.binding.UiBindingRuntimeSource;
import dev.takesome.helix.ui.binding.UiBindingSource;

/** Evaluates UI visibility with structured diagnostic information. */
public final class UiVisibilityDiagnostics {
    private UiVisibilityDiagnostics() {
    }

    public static Decision evaluate(String target, String fallbackKey, UiBindingSource binding) {
        if (binding instanceof UiBindingRuntimeSource runtime && target != null && !target.isBlank()) {
            return runtime.visibilityTarget(target, fallbackKey);
        }
        return fallbackDecision(target, fallbackKey, binding);
    }

    public static Decision defaultVisible(String target, String fallbackKey) {
        return new Decision(
                true,
                clean(target),
                clean(fallbackKey),
                false,
                "",
                "",
                "",
                "",
                "missing-target-default-visible",
                "true"
        );
    }

    public static Decision fallbackDecision(String target, String fallbackKey, UiBindingSource binding) {
        String fallback = clean(fallbackKey);
        if (fallback.isBlank()) return defaultVisible(target, fallbackKey);
        boolean negated = fallback.startsWith("!");
        String key = negated ? fallback.substring(1) : fallback;
        if (binding == null) {
            boolean visible = !negated;
            return new Decision(
                    visible,
                    clean(target),
                    fallback,
                    false,
                    "",
                    key,
                    "",
                    "",
                    visible ? "fallback-no-binding-default-visible" : "fallback-no-binding-explicit-false",
                    Boolean.toString(visible)
            );
        }
        boolean sourceValue = binding.bool(key);
        boolean visible = negated ? !sourceValue : sourceValue;
        return new Decision(
                visible,
                clean(target),
                fallback,
                false,
                "",
                key,
                "",
                "",
                visible ? "fallback-key-true" : "fallback-key-explicit-false",
                Boolean.toString(visible)
        );
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }

    public record Decision(
            boolean visible,
            String target,
            String fallbackKey,
            boolean descriptorFound,
            String descriptorId,
            String source,
            String expression,
            String defaultValue,
            String reason,
            String result
    ) {
        public boolean hidden() {
            return !visible;
        }

        public boolean missingTarget() {
            return !descriptorFound;
        }

        public boolean hasExpression() {
            return expression != null && !expression.isBlank();
        }

        public boolean hasFallbackKey() {
            return fallbackKey != null && !fallbackKey.isBlank();
        }

        public String descriptorSummary() {
            return descriptorFound ? descriptorId : "<missing>";
        }
    }
}
