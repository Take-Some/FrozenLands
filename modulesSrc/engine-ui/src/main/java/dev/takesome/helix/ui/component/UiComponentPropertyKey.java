package dev.takesome.helix.ui.component;

import java.util.Objects;

/** Typed property key used by all UI components. */
public record UiComponentPropertyKey<T>(String name, Class<T> type) {
    public UiComponentPropertyKey {
        if (name == null || name.isBlank()) {
            throw new UiComponentException("UI component property key must not be blank");
        }
        name = name.trim();
        type = Objects.requireNonNull(type, "type");
    }

    public static <T> UiComponentPropertyKey<T> of(String name, Class<T> type) {
        return new UiComponentPropertyKey<>(name, type);
    }
}
