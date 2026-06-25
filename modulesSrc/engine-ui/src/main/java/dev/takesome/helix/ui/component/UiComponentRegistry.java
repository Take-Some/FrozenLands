package dev.takesome.helix.ui.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Registry for definition-driven UI component types. */
public final class UiComponentRegistry {
    private final LinkedHashMap<UiComponentTypeId, UiComponentDefinition> definitions = new LinkedHashMap<>();

    public void register(UiComponentDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        UiComponentDefinition previous = definitions.putIfAbsent(definition.type(), definition);
        if (previous != null) {
            throw new UiComponentException("Duplicate UI component type: " + definition.type());
        }
    }

    public UiComponentDefinition require(UiComponentTypeId type) {
        UiComponentDefinition definition = definitions.get(type);
        if (definition == null) throw new UiComponentException("Unknown UI component type: " + type);
        return definition;
    }

    public boolean contains(UiComponentTypeId type) {
        return definitions.containsKey(type);
    }

    public Map<UiComponentTypeId, UiComponentDefinition> definitions() {
        return Collections.unmodifiableMap(definitions);
    }
}
