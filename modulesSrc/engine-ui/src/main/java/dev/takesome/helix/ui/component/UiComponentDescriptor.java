package dev.takesome.helix.ui.component;

import java.util.List;
import java.util.Map;

/** Data/Lua/HTML-facing description of a component before runtime instantiation. */
public record UiComponentDescriptor(
        UiComponentId id,
        UiComponentTypeId type,
        Map<String, Object> properties,
        List<UiComponentDescriptor> children
) {
    public UiComponentDescriptor {
        if (id == null) throw new UiComponentException("UiComponentDescriptor.id is required");
        if (type == null) throw new UiComponentException("UiComponentDescriptor.type is required");
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
