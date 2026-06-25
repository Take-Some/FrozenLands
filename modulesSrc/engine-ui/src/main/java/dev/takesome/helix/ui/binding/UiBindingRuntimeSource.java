package dev.takesome.helix.ui.binding;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import dev.takesome.helix.i18n.EngineI18n;
import dev.takesome.helix.i18n.I18nKey;
import dev.takesome.helix.ui.binding.UiBindingSource;
import dev.takesome.helix.ui.diagnostics.UiVisibilityDiagnostics;

/** UiBindingSource wrapper that resolves descriptor targets before falling back to legacy keys. */
public final class UiBindingRuntimeSource implements UiBindingSource {
    private final UiBindingSource source;
    private final UiBindingRegistry registry;
    private final UiBindingExpressionEvaluator expressions = new UiBindingExpressionEvaluator();
    private final EngineI18n i18n;

    public UiBindingRuntimeSource(UiBindingSource source, UiBindingRegistry registry) {
        this(source, registry, null);
    }

    public UiBindingRuntimeSource(UiBindingSource source, UiBindingRegistry registry, EngineI18n i18n) {
        this.source = source == null ? EmptySource.INSTANCE : source;
        this.registry = registry == null ? new UiBindingRegistry() : registry;
        this.i18n = i18n;
    }

    public String textTarget(String target, String fallbackTemplate) {
        UiBindingDescriptor descriptor = registry.findTarget(target).orElse(null);
        if (descriptor == null) return template(fallbackTemplate);
        String value = readText(descriptor);
        String pattern = localizedPattern(descriptor);
        if (pattern != null && !pattern.isBlank()) {
            return format(pattern, value);
        }
        return value;
    }

    public float numberTarget(String target, String fallbackKey) {
        UiBindingDescriptor descriptor = registry.findTarget(target).orElse(null);
        if (descriptor == null) return source.number(fallbackKey);
        String expression = descriptor.expression();
        if (!expression.isBlank()) return expressions.number(expression, source, parseFloat(descriptor.defaultValue(), 0f));
        String key = descriptor.source();
        if (key.isBlank()) return parseFloat(descriptor.defaultValue(), 0f);
        return source.number(key);
    }

    public boolean boolTarget(String target, String fallbackKey) {
        return visibilityTarget(target, fallbackKey).visible();
    }

    public UiVisibilityDiagnostics.Decision visibilityTarget(String target, String fallbackKey) {
        UiBindingDescriptor descriptor = registry.findTarget(target).orElse(null);
        if (descriptor == null) return UiVisibilityDiagnostics.fallbackDecision(target, fallbackKey, source);
        String expression = descriptor.expression();
        if (!expression.isBlank()) {
            boolean visible = expressions.bool(expression, source, Boolean.parseBoolean(descriptor.defaultValue()));
            return decision(visible, target, fallbackKey, descriptor, "", expression,
                    visible ? "descriptor-expression-true" : "descriptor-expression-explicit-false");
        }
        String key = descriptor.source();
        if (key.isBlank()) {
            boolean visible = Boolean.parseBoolean(descriptor.defaultValue());
            return decision(visible, target, fallbackKey, descriptor, "", "",
                    visible ? "descriptor-default-true" : "descriptor-default-explicit-false");
        }
        boolean visible = source.bool(key);
        return decision(visible, target, fallbackKey, descriptor, key, "",
                visible ? "descriptor-source-true" : "descriptor-source-explicit-false");
    }

    @Override
    public String text(String key) {
        return source.text(key);
    }

    @Override
    public float number(String key) {
        return source.number(key);
    }

    @Override
    public boolean bool(String key) {
        return source.bool(key);
    }


    private static UiVisibilityDiagnostics.Decision decision(
            boolean visible,
            String target,
            String fallbackKey,
            UiBindingDescriptor descriptor,
            String source,
            String expression,
            String reason
    ) {
        return new UiVisibilityDiagnostics.Decision(
                visible,
                clean(target),
                clean(fallbackKey),
                true,
                textOrEmpty(descriptor, item -> item.id()),
                clean(source),
                clean(expression),
                textOrEmpty(descriptor, item -> item.defaultValue()),
                reason,
                Boolean.toString(visible)
        );
    }

    private static String clean(String value) {
        return trimToEmpty(value);
    }
    private String localizedPattern(UiBindingDescriptor descriptor) {
        if (descriptor == null) return "";
        String key = firstNonBlank(descriptor.i18nKey, descriptor.textKey);
        if (!key.isBlank() && i18n != null) {
            String resolved = i18n.resolve(I18nKey.of(key));
            if (resolved != null && !resolved.isBlank() && !missingMarker(resolved, key)) return resolved;
        }
        return emptyIfNull(descriptor.format);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private static boolean missingMarker(String resolved, String key) {
        if (resolved == null || key == null) return true;
        String value = resolved.trim();
        return value.equals(key) || value.equals("??" + key + "??") || (value.startsWith("??") && value.endsWith("??"));
    }

    private String readText(UiBindingDescriptor descriptor) {
        String type = descriptor.type();
        String expression = descriptor.expression();
        if (!expression.isBlank()) {
            if ("boolean".equals(type) || "bool".equals(type)) {
                return Boolean.toString(expressions.bool(expression, source, Boolean.parseBoolean(descriptor.defaultValue())));
            }
            return formatNumber(expressions.number(expression, source, parseFloat(descriptor.defaultValue(), 0f)));
        }
        String key = descriptor.source();
        if (key.isBlank()) return descriptor.defaultValue();
        if ("number".equals(type) || "float".equals(type) || "integer".equals(type) || "progress".equals(type)) {
            return formatNumber(source.number(key));
        }
        if ("boolean".equals(type) || "bool".equals(type)) {
            return Boolean.toString(source.bool(key));
        }
        String value = source.text(key);
        return value == null || value.isBlank() ? descriptor.defaultValue() : value;
    }

    private String template(String value) {
        return format(value, "");
    }

    private String format(String value, String primaryValue) {
        if (value == null) return "";
        String out = value;
        int guard = 0;
        while (guard++ < 64) {
            int a = out.indexOf('{');
            int z = out.indexOf('}', a + 1);
            if (a < 0 || z < 0) break;
            String key = out.substring(a + 1, z).trim();
            String replacement = "value".equals(key) ? primaryValue : source.text(key);
            out = out.substring(0, a) + replacement + out.substring(z + 1);
        }
        return out;
    }

    private static String formatNumber(float value) {
        if (!Float.isFinite(value)) return "0";
        if (Math.abs(value - Math.round(value)) < 0.0001f) return Integer.toString(Math.round(value));
        return Float.toString(value);
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private enum EmptySource implements UiBindingSource {
        INSTANCE;

        @Override
        public String text(String key) { return ""; }

        @Override
        public float number(String key) { return 0f; }

        @Override
        public boolean bool(String key) { return false; }
    }
}
