package dev.takesome.helix.ui.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** One generic property storage for every runtime UI component. */
public final class UiComponentPropertyBag {
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    public <T> void set(UiComponentPropertyKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        if (value != null && !key.type().isInstance(value)) {
            throw new UiComponentException(
                    "Invalid UI component property type for `" + key.name() + "`: expected "
                            + key.type().getName() + ", got " + value.getClass().getName()
            );
        }
        setRaw(key.name(), value);
    }

    public void setRaw(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new UiComponentException("UI component property name must not be blank");
        }
        values.put(name.trim(), value);
    }

    public void putAll(Map<String, ?> properties) {
        if (properties == null || properties.isEmpty()) return;
        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            setRaw(entry.getKey(), entry.getValue());
        }
    }

    public Optional<Object> getRaw(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return Optional.ofNullable(values.get(name.trim()));
    }

    public <T> Optional<T> get(UiComponentPropertyKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object value = values.get(key.name());
        if (value == null) return Optional.empty();
        if (!key.type().isInstance(value)) {
            throw new UiComponentException(
                    "Stored UI component property `" + key.name() + "` has invalid type"
            );
        }
        return Optional.of(key.type().cast(value));
    }

    public boolean contains(String name) {
        return name != null && values.containsKey(name.trim());
    }

    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
