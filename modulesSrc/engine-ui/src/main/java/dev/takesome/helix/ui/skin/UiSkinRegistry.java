package dev.takesome.helix.ui.skin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Mutable descriptor registry scoped to one UI compilation pass. */
public final class UiSkinRegistry {
    private final LinkedHashMap<String, UiSkinDescriptor> descriptors = new LinkedHashMap<>();

    public void clear() {
        descriptors.clear();
    }

    public void register(UiSkinDescriptor descriptor) {
        if (descriptor == null) return;
        descriptors.put(descriptor.id(), descriptor);
    }

    public Optional<UiSkinDescriptor> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(descriptors.get(id.trim()));
    }

    public Map<String, UiSkinDescriptor> descriptors() {
        return Map.copyOf(descriptors);
    }
}
