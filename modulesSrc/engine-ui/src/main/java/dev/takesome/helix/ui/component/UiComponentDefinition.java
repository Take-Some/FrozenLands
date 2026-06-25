package dev.takesome.helix.ui.component;

import java.util.Set;

/** Runtime definition of one component type. Semantics live here, not in parser switches. */
public record UiComponentDefinition(
        UiComponentTypeId type,
        UiComponentChildrenPolicy childrenPolicy,
        Set<String> supportedProperties,
        UiComponentInstantiator instantiator
) {
    public UiComponentDefinition {
        if (type == null) throw new UiComponentException("UI component definition type is required");
        childrenPolicy = childrenPolicy == null ? UiComponentChildrenPolicy.ALLOW : childrenPolicy;
        supportedProperties = supportedProperties == null ? Set.of() : Set.copyOf(supportedProperties);
    }

    public boolean supportsProperty(String propertyName) {
        return propertyName != null && (supportedProperties.isEmpty() || supportedProperties.contains(propertyName));
    }
}
