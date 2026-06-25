package dev.takesome.helix.ui.binding;


import static dev.takesome.helix.validation.EngineValidator.emptyIfNull;
import static dev.takesome.helix.validation.EngineValidator.trimToEmpty;
/** Generic UI binding descriptor. Java treats source/expr as data paths, not game semantics. */
public final class UiBindingDescriptor {
    public String id;
    public String target;
    public String source;
    public String expression;
    public String expr;
    public String type;
    public String format;
    /** Optional i18n key for text/format pattern. */
    public String i18nKey;
    /** Alias for i18nKey. */
    public String textKey;
    public String defaultValue;
    public Boolean enabled;

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public String expression() {
        if (expression != null && !expression.isBlank()) return expression.trim();
        return trimToEmpty(expr);
    }

    public String source() {
        return trimToEmpty(source);
    }

    public String target() {
        return trimToEmpty(target);
    }

    public String id() {
        return id == null ? target() : id.trim();
    }

    public String type() {
        return type == null || type.isBlank() ? "string" : type.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String defaultValue() {
        return emptyIfNull(defaultValue);
    }
}
