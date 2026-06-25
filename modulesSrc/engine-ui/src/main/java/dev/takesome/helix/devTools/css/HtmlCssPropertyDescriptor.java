package dev.takesome.helix.devTools.css;

import java.util.List;

/** CSS property metadata exposed by the F12 inspector from the live engine registry. */
public record HtmlCssPropertyDescriptor(
        String name,
        List<String> aliases,
        String initialValue,
        String valueType,
        String status,
        boolean attributeFallback,
        String replacement
) {
    public HtmlCssPropertyDescriptor {
        name = clean(name);
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        initialValue = initialValue == null ? "" : initialValue.trim();
        valueType = clean(valueType);
        status = clean(status);
        replacement = replacement == null ? "" : replacement.trim();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
