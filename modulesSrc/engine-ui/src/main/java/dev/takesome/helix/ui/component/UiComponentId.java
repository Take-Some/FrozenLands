package dev.takesome.helix.ui.component;

import java.util.Objects;

/** Stable logical id of a retained UI component. */
public record UiComponentId(String value) {
    public UiComponentId {
        if (value == null || value.isBlank()) {
            throw new UiComponentException("UI component id must not be blank");
        }
        value = value.trim();
    }

    public static UiComponentId of(String value) {
        return new UiComponentId(value);
    }

    public static UiComponentId generated(long sequence) {
        return new UiComponentId("ui-component-" + sequence);
    }

    @Override
    public String toString() {
        return value;
    }
}
