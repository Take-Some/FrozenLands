package dev.takesome.helix.ui.binding;

import java.util.Collections;
import java.util.List;

/** Data-friendly UI binding pack manifest. */
public final class UiBindingManifest {
    public static final String DEFAULT_NAMESPACE = "default";

    public String id;
    public String namespace;
    public Boolean enabled;
    public List<UiBindingDescriptor> bindings;

    public String namespace() {
        if (namespace != null && !namespace.isBlank()) return namespace.trim();
        if (id != null && !id.isBlank()) return id.trim();
        return DEFAULT_NAMESPACE;
    }

    public boolean enabled() {
        return enabled == null || enabled;
    }

    public List<UiBindingDescriptor> entries() {
        return bindings == null ? List.of() : Collections.unmodifiableList(bindings);
    }

    public static UiBindingManifest of(String namespace, UiBindingDescriptor... descriptors) {
        UiBindingManifest manifest = new UiBindingManifest();
        manifest.id = namespace;
        manifest.namespace = namespace;
        manifest.enabled = true;
        manifest.bindings = descriptors == null ? List.of() : List.of(descriptors);
        return manifest;
    }

    public static UiBindingDescriptor binding(String id, String target, String source, String type, String format) {
        UiBindingDescriptor descriptor = new UiBindingDescriptor();
        descriptor.id = id;
        descriptor.target = target;
        descriptor.source = source;
        descriptor.type = type;
        descriptor.format = format;
        descriptor.enabled = true;
        return descriptor;
    }
}
