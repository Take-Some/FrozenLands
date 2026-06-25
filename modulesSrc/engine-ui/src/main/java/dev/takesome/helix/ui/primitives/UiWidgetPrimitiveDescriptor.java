package dev.takesome.helix.ui.primitives;

import java.util.Arrays;
import java.util.List;

/** Runtime descriptor for one widget primitive renderer id and aliases. */
public record UiWidgetPrimitiveDescriptor(
        String id,
        UiWidgetPrimitiveRenderer renderer,
        List<String> aliases
) {
    public UiWidgetPrimitiveDescriptor {
        id = UiWidgetPrimitiveRendererRegistry.normalize(id);
        if (id.isBlank()) throw new IllegalArgumentException("primitive id must not be blank");
        if (renderer == null) throw new IllegalArgumentException("primitive renderer must not be null");
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public static UiWidgetPrimitiveDescriptor of(String id, UiWidgetPrimitiveRenderer renderer, String... aliases) {
        return new UiWidgetPrimitiveDescriptor(id, renderer, aliases == null ? List.of() : Arrays.asList(aliases));
    }
}
