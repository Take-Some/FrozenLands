package dev.takesome.helix.ui.component;

import dev.takesome.helix.ui.node.UiComponent;

import java.util.Map;
import java.util.Objects;

/** Creates runtime components from data descriptors through registered type definitions. */
public final class UiComponentFactory {
    private final UiComponentRegistry registry;

    public UiComponentFactory(UiComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public UiComponent create(UiComponentDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        UiComponentDefinition definition = registry.require(descriptor.type());
        if (definition.instantiator() == null) {
            throw new UiComponentException("UI component type has no instantiator: " + descriptor.type());
        }

        for (Map.Entry<String, Object> property : descriptor.properties().entrySet()) {
            if (!definition.supportsProperty(property.getKey())) {
                throw new UiComponentException(
                        "Property `" + property.getKey() + "` is not supported by component type `" + descriptor.type() + "`"
                );
            }
        }

        UiComponent component = definition.instantiator().create(descriptor);
        component.setComponentId(descriptor.id());
        component.setComponentType(descriptor.type());
        for (Map.Entry<String, Object> property : descriptor.properties().entrySet()) {
            component.setPropertyRaw(property.getKey(), property.getValue());
        }
        return component;
    }
}
